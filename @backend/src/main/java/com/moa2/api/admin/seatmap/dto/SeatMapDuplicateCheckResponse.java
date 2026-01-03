package com.moa2.api.admin.seatmap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapDuplicateCheckResponse {
    private boolean isDuplicate;
}

