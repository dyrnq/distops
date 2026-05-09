package com.dyrnq.distops.registry.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    private String user;
    private String password;
    private String account;
    private String service;
    private String remoteAddr;
    private List<Scope> scopes;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Scope {
        private String type;
        private String classType;
        private String name;
        private List<String> actions;
    }
}
