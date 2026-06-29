package com.fidely.domain.dto.response.statistics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private long totalCustomers;
    private long totalStampsGiven;
    private long totalRewardsRedeemed;
    private Double estimatedRetainedRevenue;
    private Integer averageDaysBetweenVisits;
    private List<ActivityLogResponse> recentActivity;
}