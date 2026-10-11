package com.resumeradar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCommentDto {

    @NotNull(message = "Version ID is required")
    private Long resumeVersionId;

    @NotBlank(message = "Comment text is required")
    private String comment;
}
