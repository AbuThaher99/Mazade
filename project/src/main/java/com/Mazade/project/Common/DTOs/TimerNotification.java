package com.Mazade.project.Common.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerNotification {
    private Long postId;
    private String event; // TIMER_STARTED, TIMER_STOPPED, TIMER_EXTENDED, TIMER_EXPIRED
    private long remainingSeconds;
    private String timestamp;
    private String message;
}