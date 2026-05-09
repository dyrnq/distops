package com.dyrnq.distops.controller.api;

import com.dyrnq.distops.Constants;
import com.dyrnq.distops.controller.ApiController;
import com.dyrnq.distops.dso.GlobalConfigMapper;
import com.dyrnq.distops.model.GlobalConfig;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Result;

import java.util.LinkedHashMap;
import java.util.Map;

@Mapping("api/globalConfig")
@Controller
@Slf4j
public class GlobalConfigController extends ApiController {

    @Inject
    GlobalConfigMapper globalConfigMapper;

    @Mapping("getTemplates")
    public Result getTemplates() {
        try {
            GlobalConfig yamlCfg = globalConfigMapper.findByName(Constants.YAML_CONFIG);
            GlobalConfig iniCfg = globalConfigMapper.findByName(Constants.INI_CONFIG);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("yaml", yamlCfg != null ? yamlCfg.getValue() : "");
            data.put("ini", iniCfg != null ? iniCfg.getValue() : "");
            return Result.succeed(data);
        } catch (Exception e) {
            log.error("Failed to get templates", e);
            return Result.failure(e.getMessage());
        }
    }

    @Mapping("saveTemplate")
    public Result saveTemplate(String name, String value) {
        try {
            GlobalConfig cfg = globalConfigMapper.findByName(name);
            if (cfg == null) {
                return Result.failure("Template not found: " + name);
            }
            cfg.setValue(value);
            globalConfigMapper.updateById(cfg, true);
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to save template", e);
            return Result.failure(e.getMessage());
        }
    }
}
