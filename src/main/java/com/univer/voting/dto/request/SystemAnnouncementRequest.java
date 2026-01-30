package com.univer.voting.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAnnouncementRequest {
    private String title;
    private String message;
    private String actionUrl;
}
