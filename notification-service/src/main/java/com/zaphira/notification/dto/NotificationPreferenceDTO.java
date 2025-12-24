package com.zaphira.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * NotificationPreferenceDTO - for managing user notification preferences
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceDTO {
    
    private String userId;
    private String eventType;
    private Boolean isEnabled;
    private List<String> channels;
    private String quietStart;  // HH:mm
    private String quietEnd;    // HH:mm
}
