package com.dyrnq.distops.controller;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class TestController {
    @Mapping("test/?")
    public String test(@Param List<String> scope) {
        log.info("test");
        return JSONUtil.toJsonPrettyStr(scope);
    }
    @Mapping("hang/?")
    public Map<String,Object> hang() {
        try {
            long time = 1*1000*9;
            ThreadUtil.sleep(time);
        }catch (Exception ex){

        }
        Map map = new HashMap();
        map.put("hello", "world");
        return map;
    }
}
