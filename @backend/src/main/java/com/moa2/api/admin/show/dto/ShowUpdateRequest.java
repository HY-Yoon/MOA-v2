package com.moa2.api.admin.show.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "공연 수정 요청")
public class ShowUpdateRequest {
    @Schema(description = "공연 제목", example = "위키드")
    private String title;

    @Schema(description = "상영 시간(분)", example = "150")
    private Integer runningTime;

    @Schema(description = "포스터 URL (상대 경로)", example = "/images/posters/poster.jpg")
    private String posterUrl;

    @Schema(description = "상세 이미지 URL 목록 (상대 경로)", example = "[\"/images/details/detail1.jpg\", \"/images/details/detail2.jpg\"]")
    private String[] detailImageUrls;
    
    @Schema(description = "출연진 정보 (단순 문자열)", example = "김배우, 이배우, 박배우")
    private String cast;
}

