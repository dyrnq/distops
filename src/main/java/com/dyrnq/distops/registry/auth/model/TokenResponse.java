package com.dyrnq.distops.registry.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.noear.snack4.annotation.ONodeAttr;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    @JsonProperty("access_token")
    @ONodeAttr(name="access_token")
    private String accessToken;
    
    @JsonProperty("token")
    @ONodeAttr(name="token")
    private String token;

    @JsonProperty("expires_in")
    @ONodeAttr(name="expires_in")
    private Integer expiresIn;

    @JsonProperty("issued_at")
    @ONodeAttr(name="issued_at")
    private String issuedAt;
}
