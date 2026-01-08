package com.moa2.api.admin.seatmap.dto;

import com.moa2.global.model.Region;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatMapListRequest {
    private Region region; // 지역 enum (SEOUL, GYEONGGI 등)
    private String venueName; // 공연장명
    private String hallName; // 홀명
    private int page = 0;
    private int size = 20;
}

