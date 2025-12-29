package com.moa2.api.admin.show.dto;

import com.moa2.global.model.Region;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "장소 등록 요청")
public class VenueCreateRequest {
    @Schema(description = "시설명", example = "예술의전당", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "시설명은 필수입니다")
    private String name;

    @Schema(description = "홀명", example = "오페라극장", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "홀명은 필수입니다")
    private String hallName;

    @Schema(description = "지역", example = "SEOUL", allowableValues = {"SEOUL", "GYEONGGI", "INCHEON", "BUSAN", "DAEGU", "DAEJEON", "GWANGJU", "ULSAN", "SEJONG", "GANGWON", "CHUNGBUK", "CHUNGNAM", "JEONBUK", "JEONNAM", "GYEONGBUK", "GYEONGNAM", "JEJU"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "지역은 필수입니다")
    private Region region;

    @Schema(description = "주소", example = "서울특별시 서초구 남부순환로 2406")
    private String address;

    @Schema(description = "좌석 수", example = "2000")
    private Integer totalSeats;

    @Schema(description = "위도", example = "37.5665")
    private Double latitude;

    @Schema(description = "경도", example = "126.9780")
    private Double longitude;

    @Schema(description = "좌석 배치도 이미지 URL (상대 경로)", example = "/images/seat-layouts/venue1.jpg")
    private String seatLayoutImageUrl;
}

