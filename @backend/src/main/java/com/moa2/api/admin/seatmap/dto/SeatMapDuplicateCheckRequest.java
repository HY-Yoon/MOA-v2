package com.moa2.api.admin.seatmap.dto;

import com.moa2.global.model.Region;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatMapDuplicateCheckRequest {
    private Region region; // 지역 enum (optional)
    private String venueName; // 공연장명 (optional)
    private String hallName; // 홀명 (optional)
}

