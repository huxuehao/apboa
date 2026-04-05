package com.hxh.apboa.agent.service.impl;

import com.hxh.apboa.agent.mapper.ChatMessageMapper;
import com.hxh.apboa.agent.mapper.ChatSessionMapper;
import com.hxh.apboa.agent.service.AgentStatisticsService;
import com.hxh.apboa.common.vo.AgentStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 描述：智能体统计分析Service实现
 *
 * @author huxuehao
 **/
@Service
@RequiredArgsConstructor
public class AgentStatisticsServiceImpl implements AgentStatisticsService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public AgentStatisticsVO getAgentTrends(Long agentId, Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        String startDateStr = startDate.format(DATE_FMT);

        // 构建日期骨架，确保每天都有数据点
        List<String> dateRange = buildDateRange(startDate, endDate);

        // 查询各维度数据
        List<Map<String, Object>> sessionData = chatSessionMapper.countSessionsByDay(agentId, startDateStr);
        List<Map<String, Object>> activeUserData = chatSessionMapper.countActiveUsersByDay(agentId, startDateStr);
        List<Map<String, Object>> messageData = chatMessageMapper.countMessagesByDay(agentId, startDateStr);
        List<Map<String, Object>> avgRoundsData = chatMessageMapper.avgRoundsByDay(agentId, startDateStr);

        return AgentStatisticsVO.builder()
                .sessionTrend(fillTrend(dateRange, sessionData))
                .activeUserTrend(fillTrend(dateRange, activeUserData))
                .messageTrend(fillTrend(dateRange, messageData))
                .avgRoundsTrend(fillTrend(dateRange, avgRoundsData))
                .build();
    }

    /**
     * 构建日期范围列表
     */
    private List<String> buildDateRange(LocalDate start, LocalDate end) {
        List<String> dates = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(current.format(DATE_FMT));
            current = current.plusDays(1);
        }
        return dates;
    }

    /**
     * 用日期骨架填充趋势数据，缺失日期填0
     */
    private List<AgentStatisticsVO.TrendItem> fillTrend(List<String> dateRange, List<Map<String, Object>> rawData) {
        // 将查询结果转换为 date -> value 的映射
        Map<String, Double> dataMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rawData) {
            String date = String.valueOf(row.get("date"));
            Object val = row.get("value");
            double value = val instanceof Number ? ((Number) val).doubleValue() : 0.0;
            dataMap.put(date, value);
        }

        List<AgentStatisticsVO.TrendItem> result = new ArrayList<>();
        for (String date : dateRange) {
            result.add(AgentStatisticsVO.TrendItem.builder()
                    .date(date)
                    .value(dataMap.getOrDefault(date, 0.0))
                    .build());
        }
        return result;
    }
}
