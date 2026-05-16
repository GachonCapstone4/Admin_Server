package com.emailagent.dto.response.admin.monitoring;

import java.util.List;

public record AdminMonitoringDashboardListResponse(
        List<AdminMonitoringDashboardResponse> dashboards
) {
}
