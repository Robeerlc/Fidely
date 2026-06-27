package com.fidely.dto.response.statistics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private long totalCustomers;
    private long totalStampsGiven;
    private long totalRewardsRedeemed;
    private List<ActivityLogResponse> recentActivity;
}