package com.moa2.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moa2.global.model.ErrorCode;

import java.util.List;

/**
 * 실패 응답 시 data 필드에 담기는 에러 상세 정보
 * - code: 에러 코드 (ErrorCode ENUM)
 * - conflictSeatIds: SEAT_CONFLICT 시 충돌 좌석 번호 목록 (nullable)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        List<String> conflictSeatIds) {
    public static ErrorResponse of(ErrorCode code) {
        return new ErrorResponse(code.name(), null);
    }

    public static ErrorResponse ofConflict(List<String> conflictSeatIds) {
        return new ErrorResponse(ErrorCode.SEAT_CONFLICT.name(), conflictSeatIds);
    }
}
