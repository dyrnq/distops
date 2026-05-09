package com.dyrnq.distops;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.wood.DbContext;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class DbConfig {


    @Bean(autoInject = true)
    public DbContext dbContext(DataSource dataSource) {
        log.debug(JSONUtil.toJsonPrettyStr(dataSource));
        return new DbContext(dataSource);
    }
}
