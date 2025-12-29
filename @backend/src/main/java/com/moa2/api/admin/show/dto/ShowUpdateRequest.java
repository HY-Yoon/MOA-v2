package com.moa2.api.admin.show.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ShowUpdateRequest {
    private String title;
    private Integer runningTime;
    private String posterUrl;
    private String[] detailImageUrls;
    
    @Valid
    private List<CastRequest> casts;

    @Getter
    @Setter
    public static class CastRequest {
        private Long castId;
        private String role;
        private List<Long> scheduleIds;
    }
}

