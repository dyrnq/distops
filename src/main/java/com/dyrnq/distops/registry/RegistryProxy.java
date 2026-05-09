package com.dyrnq.distops.registry;

import lombok.Data;

@Data
public class RegistryProxy {
    private String remoteurl;
    private String username;
    private String password;
    private String ttl;
}
