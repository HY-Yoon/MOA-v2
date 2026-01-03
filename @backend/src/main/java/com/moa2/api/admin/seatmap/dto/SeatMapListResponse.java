package com.moa2.api.admin.seatmap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapListResponse {
    private String seatMapId; // "SM001" 형식
    private String region;
    private String venueName;
    private String hallName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

