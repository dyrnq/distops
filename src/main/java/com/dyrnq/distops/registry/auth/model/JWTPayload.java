package com.dyrnq.distops.registry.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JWTPayload {
    @JsonProperty("iss")
    private String issuer;
    
    @JsonProperty("sub")
    private String subject;
    
    @JsonProperty("aud")
    private String audience;
    
    @JsonProperty("exp")
    private long expiration;
    
    @JsonProperty("nbf")
    private long notBefore;
    
    @JsonProperty("iat")
    private long issuedAt;
    
    @JsonProperty("jti")
    private String jwtId;
    
    @JsonProperty("access")
    private List<ResourceAccess> access;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceAccess {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("actions")
        private List<String> actions;
    }
}
