package com.fidely.domain.dto.response.statistics;

import java.util.List;

public record DashboardResponse(
        long totalCustomers,
        long totalCardsThisMonth,
        long totalStampsGiven,
        long totalRewardsRedeemed,
        Double estimatedRetainedRevenue,
        Integer averageDaysBetweenVisits,
        List<ActivityLogResponse> recentActivity
) {
}
