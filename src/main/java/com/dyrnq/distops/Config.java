package com.dyrnq.distops;

import cn.hutool.json.JSONUtil;
import com.dyrnq.utils.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.i18n.LocaleResolver;
import org.noear.solon.i18n.impl.LocaleResolverCookie;

import java.io.File;

@Configuration
@Slf4j
public class Config {
    @Inject("${solon.app.name}")
    String projectName;
    @Inject("${project.home:}")
    private String home;
    @Inject("${server.session.state.jwt.name:${jwt.name:}}")
    private String jwtName;

    // typed=true，表示默认数据源。@Db 可不带名字注入
//    @Bean(value = "db1" ,typed = true)
//    public DataSource db1(@Inject("${test.db1}") HikariDataSource ds) throws Exception{
//        Flyway flyway = Flyway.configure()
//                .baselineOnMigrate(true)
//                .cleanDisabled(true)
//                .dataSource(ds.getJdbcUrl(), ds.getUsername(), ds.getPassword()).load();
//        flyway.migrate();
//
//        return ds;
//    }
    @Bean
    public LocaleResolver localInit() {
        return new LocaleResolverCookie();
    }

    @Bean(value = "homeDir", typed = true)
    public HomeDir getHomeDir() {
        String homeAbsolutePath = PathUtils.homeAbsolutePath(home, projectName);
        String tmpAbsolutePath = StringUtils.joinWith(File.separator, homeAbsolutePath, "tmp");
        HomeDir homeDir = new HomeDir();
        homeDir.setHomeAbsolutePath(homeAbsolutePath);
        homeDir.setTmpAbsolutePath(tmpAbsolutePath);
        try {
            FileUtils.forceMkdir(new File(tmpAbsolutePath));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        log.info("config***********homeDir={}", JSONUtil.toJsonStr(homeDir));
        return homeDir;
    }

    @Bean(value = "cfgExtractor", typed = true)
    public CfgExtractor getCfgExtractor() {
        String tokenCookieName = StringUtils.isNotBlank(jwtName) ? jwtName : CookieName.NAME_TOKEN;
        log.info("config***********tokenCookieName={}", tokenCookieName);
        return new CfgExtractor(tokenCookieName);
    }

}