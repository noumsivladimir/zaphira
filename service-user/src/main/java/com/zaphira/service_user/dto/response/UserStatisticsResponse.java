package com.zaphira.service_user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatisticsResponse {

    private Long totalUsers;
    private Long activeUsers;
    private Long inactiveUsers;
    private Long suspendedUsers;
    private Long closedUsers;
    private Long newUsersToday;
    private Long newUsersThisWeek;
    private Long newUsersThisMonth;
    private Long kycVerifiedUsers;
    private Long kycPendingUsers;
    private Long kycRejectedUsers;
}