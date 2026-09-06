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

import org.jsoup.Jsoup;

/**
 * 知识资讯服务:
 * - 每天早上 9:00(@Scheduled)自动抓取公开资讯源(RSS/Atom),最新知识入库;
 * - 单条按 source_url 唯一去重,入库强制标注来源(source_name/source_url),落实版权要求;
 * - 正文入库(2026-09-06 起):优先取 RSS 自带全文(content:encoded / Atom content),
 *   否则抓取原文页抽取正文,转为纯文本存 content,站内阅读;失败退化为"标题+摘要+原文链接"模式。
 *   所有条目保留原文链接与版权声明,如若侵权可联系删除。
 */
@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    /** 每个源单次最多入库条数,避免单一源刷屏 */
    private static final int MAX_ITEMS_PER_FEED = 30;

    /** 每个源单次最多抓取正文的条数(限制外网请求量,超出部分保持纯链接模式) */
    private static final int MAX_CONTENT_FETCH_PER_FEED = 10;

    /** 正文最大字符数,防止异常长文占库 */
    private static final int MAX_CONTENT_LEN = 20000;

    /** 原文页正文容器选择器(按优先级),均未命中则退化为 body 文本 */
    private static final String[] ARTICLE_SELECTORS = {
            "article", "div.p_mainnew", "div.article-content", "div.article__content",
            "div.entry-content", "div.post-content", "div.block_content", "div.article",
            "div#content", "div.content", "main"
    };

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

    /** 分页列表(游客可访问);附带去重后的分类列表供前端筛选;不回传大字段正文 */
    public Map<String, Object> page(String category, int page, int size) {
        LambdaQueryWrapper<KnowledgeNews> wrapper = new LambdaQueryWrapper<KnowledgeNews>()
                .select(KnowledgeNews.class, f -> !"content".equals(f.getProperty()))
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
        int contentFetched = 0;
        for (KnowledgeNews item : parseFeed(feed, resp.body())) {
            Long exists = newsMapper.selectCount(new LambdaQueryWrapper<KnowledgeNews>()
                    .eq(KnowledgeNews::getSourceUrl, item.getSourceUrl()));
            if (exists != null && exists > 0) {
                continue;
            }
            // 正文来源 1:RSS 自带全文(content:encoded / Atom content)已在 parseFeed 中抽取;
            // 正文来源 2:每源限量抓取原文页正文,超出限额或抓取失败保持纯链接模式
            if ((item.getContent() == null || item.getContent().isBlank())
                    && contentFetched < MAX_CONTENT_FETCH_PER_FEED) {
                item.setContent(fetchArticleText(item.getSourceUrl()));
                contentFetched++;
            }
            item.setTitle(truncate(item.getTitle(), 500));
            item.setSummary(truncate(item.getSummary(), 1000));
            item.setSourceUrl(truncate(item.getSourceUrl(), 760));
            item.setContent(truncate(item.getContent(), MAX_CONTENT_LEN));
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
            // 正文来源 1:RSS 自带全文(content:encoded 为 RSS 惯例,Atom 用 <content>)
            news.setContent(htmlToText(firstNonBlank(
                    textOf(el, "content:encoded"), atomContentOf(el))));
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

    /** Atom <content>:type="src" 的是外链占位无正文,跳过 */
    private String atomContentOf(Element parent) {
        NodeList nodes = parent.getElementsByTagName("content");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getAttribute("src").isBlank()) {
                String text = el.getTextContent().trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    /** 抓取原文页并抽取正文纯文本;任何失败返回空串(退化为纯链接模式) */
    private String fetchArticleText(String url) {
        if (url == null || !url.startsWith("http")) {
            return "";
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0 (compatible; LearnPlatform-RAG/1.0; graduation-project)")
                    .header("Accept", "text/html,application/xhtml+xml,*/*")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                return "";
            }
            org.jsoup.nodes.Document doc = Jsoup.parse(resp.body(), url);
            doc.select("script,style,noscript,nav,header,footer,aside,iframe,form,svg,div.comment,section.comment").remove();
            org.jsoup.nodes.Element best = null;
            for (String selector : ARTICLE_SELECTORS) {
                org.jsoup.nodes.Element candidate = doc.select(selector).stream()
                        .filter(e -> e.text().length() > 200)
                        .findFirst().orElse(null);
                if (candidate != null) {
                    best = candidate;
                    break;
                }
            }
            if (best == null) {
                best = doc.body();
            }
            return best == null ? "" : htmlToText(best.html());
        } catch (Exception e) {
            log.debug("[资讯] 原文正文抓取失败,退化为链接模式: {} - {}", url, e.getMessage());
            return "";
        }
    }

    /** HTML 转纯文本:块级元素换行,保留段落结构;输出不含任何 HTML 标签(防 XSS,前端按纯文本渲染) */
    private String htmlToText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        doc.select("br").append("~NL~");
        doc.select("p,div,li,tr,h1,h2,h3,h4,h5,h6,blockquote,pre,section,article").prepend("~NL~");
        String text = doc.text()
                .replace("~NL~", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        return text;
    }

    /** 分页列表不回传大字段正文,详情接口单独取 */
    public KnowledgeNews detail(Long newsId) {
        return newsMapper.selectById(newsId);
    }

    /** 为缺失正文的存量资讯补抓正文(管理员手动触发),单次最多 50 条,返回补抓成功条数 */
    public synchronized int backfillContent() {
        List<KnowledgeNews> missing = newsMapper.selectList(new LambdaQueryWrapper<KnowledgeNews>()
                .isNull(KnowledgeNews::getContent)
                .orderByDesc(KnowledgeNews::getFetchedAt)
                .last("LIMIT 50"));
        int filled = 0;
        for (KnowledgeNews item : missing) {
            String text = fetchArticleText(item.getSourceUrl());
            if (!text.isBlank()) {
                item.setContent(truncate(text, MAX_CONTENT_LEN));
                newsMapper.updateById(item);
                filled++;
            }
        }
        return filled;
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

    private String firstNonBlank(String a, String b) {
        return firstNonBlank(a, b, "");
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
