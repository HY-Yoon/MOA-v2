package com.moa2.api.admin.seatmap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "좌석배치도 등록 요청")
public class SeatMapCreateRequest {
    
    @Schema(description = "지역", example = "서울", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "지역은 필수입니다")
    private String region;
    
    @Schema(description = "공연장명", example = "올림픽공원", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "공연장명은 필수입니다")
    private String venueName;
    
    @Schema(description = "홀명", example = "KSPO DOME", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "홀명은 필수입니다")
    private String hallName;
    
    @Schema(description = "캔버스 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "캔버스 정보는 필수입니다")
    private CanvasRequest canvas;
    
    @Schema(description = "구역 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "구역 목록은 필수입니다")
    private List<SectionRequest> sections;
    
    @Schema(description = "좌석 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "좌석 목록은 필수입니다")
    private List<SeatRequest> seats;
    
    @Getter
    @Setter
    @Schema(description = "캔버스 정보")
    public static class CanvasRequest {
        @Schema(description = "너비", example = "1200", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "너비는 필수입니다")
        private Integer width;
        
        @Schema(description = "높이", example = "800", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "높이는 필수입니다")
        private Integer height;
        
        @Schema(description = "좌석 반지름", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "좌석 반지름은 필수입니다")
        private Integer seatRadius;
    }
    
    @Getter
    @Setter
    @Schema(description = "구역 정보")
    public static class SectionRequest {
        @Schema(description = "구역 ID", example = "A", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "구역 ID는 필수입니다")
        private String sectionId;
        
        @Schema(description = "구역명", example = "A구역", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "구역명은 필수입니다")
        private String name;
        
        @Schema(description = "색상", example = "#FF6B6B", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "색상은 필수입니다")
        private String color;
        
        @Schema(description = "가격", example = "120000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "가격은 필수입니다")
        private Integer price;
    }
    
    @Getter
    @Setter
    @Schema(description = "좌석 정보")
    public static class SeatRequest {
        @Schema(description = "좌석 ID", example = "A-1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "좌석 ID는 필수입니다")
        private String seatId;
        
        @Schema(description = "구역 ID", example = "A", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "구역 ID는 필수입니다")
        private String sectionId;
        
        @Schema(description = "행", example = "A", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "행은 필수입니다")
        private String row;
        
        @Schema(description = "번호", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "번호는 필수입니다")
        private Integer number;
        
        @Schema(description = "X 좌표", example = "120", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "X 좌표는 필수입니다")
        private Integer x;
        
        @Schema(description = "Y 좌표", example = "200", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Y 좌표는 필수입니다")
        private Integer y;
    }
}

