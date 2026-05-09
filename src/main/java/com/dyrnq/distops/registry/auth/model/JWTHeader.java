package com.dyrnq.distops.registry.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JWTHeader {
    @JsonProperty("typ")
    private String type;
    
    @JsonProperty("alg")
    private String algorithm;
    
    @JsonProperty("kid")
    private String keyId;
}
