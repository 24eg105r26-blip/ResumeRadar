package com.resumeradar.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtsIssueDto {
    private String type;
    private String description;
}
