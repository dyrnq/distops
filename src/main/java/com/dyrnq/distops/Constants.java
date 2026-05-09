package com.dyrnq.distops;

import java.util.regex.Pattern;

public interface Constants {
    String YAML_CONFIG = "registry_config_yml_template";
    String INI_CONFIG = "registry_supervisor_template";
    String VERSION = "v1.0.0";
    String BUILD_DATETIME ="2026-04-24 14:53:38";
    Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[ayb]\\$(\\d{2})\\$.*");
    int BCRYPT_COST=12;
}
