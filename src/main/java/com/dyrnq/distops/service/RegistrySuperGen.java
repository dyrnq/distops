package com.dyrnq.distops.service;

import cn.hutool.core.io.resource.ResourceUtil;
import com.dyrnq.distops.Constants;
import com.dyrnq.distops.dso.GlobalConfigMapper;
import com.dyrnq.distops.dso.InstMapper;
import com.dyrnq.distops.model.GlobalConfig;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;

@Component
@Slf4j
public class RegistrySuperGen {
    @Inject
    private GlobalConfigMapper globalConfigMapper;
    @Inject
    private InstMapper instMapper;
    @Inject
    private InstService instService;


    @Init
    public void registry() {

        if (!globalConfigMapper.existsByName(Constants.YAML_CONFIG)) {
            GlobalConfig globalConfig = new GlobalConfig();
            globalConfig.setId(10000L);
            globalConfig.setName(Constants.YAML_CONFIG);
            globalConfig.setValue(ResourceUtil.readUtf8Str("templates/registry/config.yml.tpl"));
            globalConfigMapper.insert(globalConfig, true);
        }
        if (!globalConfigMapper.existsByName(Constants.INI_CONFIG)) {
            GlobalConfig globalConfig = new GlobalConfig();
            globalConfig.setId(10001L);
            globalConfig.setName(Constants.INI_CONFIG);
            globalConfig.setValue(ResourceUtil.readUtf8Str("templates/supervisor/registry.ini.tpl"));
            globalConfigMapper.insert(globalConfig, true);
        }

        String config_yaml_template = globalConfigMapper.findByName(Constants.YAML_CONFIG).getValue();
        String registry_supervisor_template = globalConfigMapper.findByName(Constants.INI_CONFIG).getValue();
        log.debug(config_yaml_template);
        log.debug(registry_supervisor_template);

        instMapper.selectList(null).forEach(inst -> {
            if (inst.getEnabled() != null && inst.getEnabled() == 1) {
                instService.enable(inst, config_yaml_template, registry_supervisor_template);
            }
        });
    }
}
