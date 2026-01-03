package com.moa2.api.admin.seatmap.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatMapDuplicateCheckRequest {
    private String region; // 한글 지역명 (optional)
    private String venueName; // 공연장명 (optional)
    private String hallName; // 홀명 (optional)
}

