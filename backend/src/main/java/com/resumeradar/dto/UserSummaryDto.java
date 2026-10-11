package com.resumeradar.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDto {
    private Long id;
    private String name;
    private String email;
    private String roles;
    private LocalDateTime createdAt;
    private long resumeCount;
    private long versionCount;
}
