package com.dyrnq.distops;

import java.util.regex.Pattern;

public interface Constants {
    String YAML_CONFIG = "registry_config_yml_template";
    String INI_CONFIG = "registry_supervisor_template";
    Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[ayb]\\$(\\d{2})\\$.*");
    int BCRYPT_COST = 12;
}
