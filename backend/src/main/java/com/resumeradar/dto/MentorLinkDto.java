package com.resumeradar.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorLinkDto {

    @NotBlank(message = "Share code is required")
    private String shareCode;
}
