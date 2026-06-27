package com.fidely.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private long totalCustomers;
    private long totalStampsGiven;
    private long totalRewardsRedeemed;
    private List<ActivityLogDto> recentActivity;
}