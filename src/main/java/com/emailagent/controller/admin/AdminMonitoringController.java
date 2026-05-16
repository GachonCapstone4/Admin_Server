package com.emailagent.controller.admin;

import com.emailagent.dto.response.admin.monitoring.AdminMonitoringDashboardListResponse;
import com.emailagent.dto.response.admin.monitoring.AdminMonitoringDashboardResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/monitoring")
@RequiredArgsConstructor
public class AdminMonitoringController {

    private static final TypeReference<List<AdminMonitoringDashboardResponse>> DASHBOARD_LIST_TYPE =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;

    @Value("${admin.monitoring.grafana-dashboards:[]}")
    private String grafanaDashboardsJson;

    @GetMapping("/dashboards")
    public AdminMonitoringDashboardListResponse getGrafanaDashboards() {
        return new AdminMonitoringDashboardListResponse(parseDashboards());
    }

    private List<AdminMonitoringDashboardResponse> parseDashboards() {
        if (!StringUtils.hasText(grafanaDashboardsJson)) {
            return List.of();
        }

        try {
            List<AdminMonitoringDashboardResponse> dashboards =
                    objectMapper.readValue(grafanaDashboardsJson, DASHBOARD_LIST_TYPE);

            return dashboards.stream()
                    .filter(this::isValidDashboard)
                    .toList();
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Grafana dashboard 설정을 읽지 못했습니다.",
                    e
            );
        }
    }

    private boolean isValidDashboard(AdminMonitoringDashboardResponse dashboard) {
        return dashboard != null
                && StringUtils.hasText(dashboard.key())
                && StringUtils.hasText(dashboard.label())
                && StringUtils.hasText(dashboard.url());
    }
}
