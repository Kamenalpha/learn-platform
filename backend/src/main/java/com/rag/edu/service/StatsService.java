package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rag.edu.entity.DocChunk;
import com.rag.edu.entity.DocResource;
import com.rag.edu.entity.QaRecord;
import com.rag.edu.entity.SysUser;
import com.rag.edu.mapper.DocChunkMapper;
import com.rag.edu.mapper.DocResourceMapper;
import com.rag.edu.mapper.QaRecordMapper;
import com.rag.edu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统运行统计(管理员看板)
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final SysUserMapper userMapper;
    private final DocResourceMapper resourceMapper;
    private final DocChunkMapper chunkMapper;
    private final QaRecordMapper qaRecordMapper;

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("docCount", resourceMapper.selectCount(null));
        data.put("chunkCount", chunkMapper.selectCount(null));
        data.put("qaCount", qaRecordMapper.selectCount(null));
        data.put("parsedCount", resourceMapper.selectCount(
                new LambdaQueryWrapper<DocResource>()
                        .eq(DocResource::getParseStatus, 1)));
        return data;
    }

    public List<Map<String, Object>> qaTrend(int days) {
        return qaRecordMapper.countDaily(LocalDateTime.now().minusDays(days));
    }

    public List<Map<String, Object>> docsByCourse() {
        return resourceMapper.countByCourse();
    }

    /** 问答日志(分页) */
    public Map<String, Object> qaLogs(long page, long size) {
        Page<QaRecord> result = qaRecordMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<QaRecord>()
                        .orderByDesc(QaRecord::getCreateTime));
        result.getRecords().forEach(r -> {
            r.setAnswer(abbreviate(r.getAnswer(), 80));
            r.setReference(abbreviate(r.getReference(), 120));
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", result.getTotal());
        data.put("records", result.getRecords());
        return data;
    }

    private String abbreviate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
