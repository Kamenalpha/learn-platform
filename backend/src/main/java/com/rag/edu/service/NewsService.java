package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rag.edu.entity.KnowledgeNews;
import com.rag.edu.mapper.KnowledgeNewsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 知识资讯服务:
 * - 每天早上 9:00(@Scheduled)自动抓取公开资讯源(RSS/Atom),最新知识入库;
 * - 单条按 source_url 唯一去重,入库强制标注来源(source_name/source_url),落实版权要求;
 * - 抓取仅取标题/摘要/链接用于导航,不转载正文,如若侵权可联系删除。
 */
@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    /** 每个源单次最多入库条数,避免单一源刷屏 */
    private static final int MAX_ITEMS_PER_FEED = 30;

    /** 资讯源配置:来源名|分类|RSS地址,逗号分隔(见 application.yml news.rss-feeds) */
    @Value("${news.rss-feeds}")
    private String rssFeeds;

    private final KnowledgeNewsMapper newsMapper;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public NewsService(KnowledgeNewsMapper newsMapper) {
        this.newsMapper = newsMapper;
    }

    /** 每天早上 9:00 自动获取最新的知识资讯;可用 NEWS_FETCH_CRON 环境变量覆盖 */
    @Scheduled(cron = "${news.fetch-cron:0 0 9 * * ?}")
    public void scheduledFetch() {
        try {
            int added = fetchAndSave();
            log.info("[资讯] 每日定时抓取完成,新增 {} 条", added);
        } catch (Exception e) {
            log.error("[资讯] 每日定时抓取失败", e);
        }
    }

    /** 抓取全部资讯源并去重入库,返回本次新增条数(单个源失败不影响其它源) */
    public synchronized int fetchAndSave() {
        int added = 0;
        for (String[] feed : parseFeeds()) {
            try {
                added += fetchFeed(feed);
            } catch (Exception e) {
                log.warn("[资讯] 抓取源失败,已跳过:{} - {}", feed[0], e.getMessage());
            }
        }
        return added;
    }

    /** 分页列表(游客可访问);附带去重后的分类列表供前端筛选 */
    public Map<String, Object> page(String category, int page, int size) {
        LambdaQueryWrapper<KnowledgeNews> wrapper = new LambdaQueryWrapper<KnowledgeNews>()
                .eq(category != null && !category.isBlank() && !"全部".equals(category),
                        KnowledgeNews::getCategory, category)
                .orderByDesc(KnowledgeNews::getFetchedAt)
                .orderByDesc(KnowledgeNews::getNewsId);
        Page<KnowledgeNews> result = newsMapper.selectPage(new Page<>(page, Math.min(size, 50)), wrapper);

        Set<String> categories = new LinkedHashSet<>();
        newsMapper.selectObjs(new QueryWrapper<KnowledgeNews>()
                .select("DISTINCT category").isNotNull("category")).forEach(o -> categories.add(String.valueOf(o)));

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("categories", categories);
        return data;
    }

    private int fetchFeed(String[] feed) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(feed[2]))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Mozilla/5.0 (compatible; LearnPlatform-RAG/1.0; graduation-project)")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                .GET()
                .build();
        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + resp.statusCode());
        }
        int added = 0;
        for (KnowledgeNews item : parseFeed(feed, resp.body())) {
            Long exists = newsMapper.selectCount(new LambdaQueryWrapper<KnowledgeNews>()
                    .eq(KnowledgeNews::getSourceUrl, item.getSourceUrl()));
            if (exists != null && exists > 0) {
                continue;
            }
            item.setTitle(truncate(item.getTitle(), 500));
            item.setSummary(truncate(item.getSummary(), 1000));
            item.setSourceUrl(truncate(item.getSourceUrl(), 760));
            newsMapper.insert(item);
            added++;
        }
        return added;
    }

    /** 解析 RSS 2.0(<item>)与 Atom(<entry>)两种格式 */
    private List<KnowledgeNews> parseFeed(String[] feed, String xml) throws Exception {
        xml = softenHtmlEntities(xml);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // XXE 防护:外部内容源不可信,禁用 DTD 与外部实体
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

        List<KnowledgeNews> list = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("item");
        if (nodes.getLength() == 0) {
            nodes = doc.getElementsByTagName("entry"); // Atom
        }
        for (int i = 0; i < nodes.getLength() && list.size() < MAX_ITEMS_PER_FEED; i++) {
            Element el = (Element) nodes.item(i);
            String title = textOf(el, "title");
            String link = linkOf(el);
            if (title.isBlank() || link.isBlank()) {
                continue;
            }
            String summary = stripHtml(firstNonBlank(
                    textOf(el, "description"), textOf(el, "summary"), textOf(el, "content")));

            KnowledgeNews news = new KnowledgeNews();
            news.setTitle(title.trim());
            news.setSummary(summary);
            news.setSourceName(feed[0]);
            news.setSourceUrl(link.trim());
            news.setCategory(feed[1]);
            news.setPublishedAt(parseDate(firstNonBlank(
                    textOf(el, "pubDate"), textOf(el, "published"), textOf(el, "updated"))));
            news.setFetchedAt(LocalDateTime.now());
            list.add(news);
        }
        return list;
    }

    /** 配置格式:来源名|分类|RSS地址,逗号分隔(见 application.yml news.rss-feeds) */
    private List<String[]> parseFeeds() {
        List<String[]> feeds = new ArrayList<>();
        for (String raw : rssFeeds.split(",")) {
            String[] parts = raw.trim().split("\\|");
            if (parts.length == 3 && !parts[2].isBlank()) {
                feeds.add(new String[]{parts[0].trim(), parts[1].trim(), parts[2].trim()});
            }
        }
        return feeds;
    }

    /** RSS 正文常含 XML 未声明的 HTML 实体(如 &nbsp;),先替换避免整篇解析失败 */
    private String softenHtmlEntities(String xml) {
        return xml.replace("&nbsp;", " ")
                .replace("&hellip;", "…")
                .replace("&mdash;", "—")
                .replace("&ndash;", "–")
                .replace("&ldquo;", "“")
                .replace("&rdquo;", "”")
                .replace("&lsquo;", "‘")
                .replace("&rsquo;", "’")
                .replace("&middot;", "·")
                .replace("&bull;", "•")
                .replace("&deg;", "°")
                .replace("&times;", "×")
                .replace("&laquo;", "《")
                .replace("&raquo;", "》");
    }

    private String textOf(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    /** RSS 用 <link> 文本,Atom 用 <link href="..."> */
    private String linkOf(Element parent) {
        NodeList nodes = parent.getElementsByTagName("link");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String href = el.getAttribute("href");
            if (!href.isBlank()) {
                return href;
            }
            String text = el.getTextContent().trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private String stripHtml(String text) {
        return text.replaceAll("(?s)<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** 兼容 RFC-822(RSS)与 ISO-8601(Atom)两种时间格式,失败返回 null */
    private LocalDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw.trim(),
                    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US))
                    .atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(raw.trim()).atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
        } catch (Exception ignored) {
        }
        return null;
    }

    private String firstNonBlank(String a, String b, String c) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return c == null ? "" : c;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
