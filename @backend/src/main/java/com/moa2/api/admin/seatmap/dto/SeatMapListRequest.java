package com.moa2.api.admin.seatmap.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatMapListRequest {
    private String region; // 한글 지역명 (예: "서울", "경기")
    private String venueName; // 공연장명
    private String hallName; // 홀명
    private int page = 0;
    private int size = 20;
}

