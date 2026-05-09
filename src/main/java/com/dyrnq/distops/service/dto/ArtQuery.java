package com.dyrnq.distops.service.dto;

import lombok.Data;

@Data
public class ArtQuery {
    private String instName;
    private String tagName;
    private String fullName;
    private String repoName;
    private String digest;
}
