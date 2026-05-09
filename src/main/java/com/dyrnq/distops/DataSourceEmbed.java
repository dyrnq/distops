package com.dyrnq.distops;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.http.HttpUtil;
import com.dyrnq.utils.PathUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.h2.engine.Constants;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@Configuration
@Slf4j
public class DataSourceEmbed {
    @Inject("${spring.database.type:}")
    String databaseType;
    @Inject("${spring.datasource.url}")
    String url;
    @Inject("${spring.datasource.username}")
    String username;
    @Inject("${spring.datasource.password}")
    String password;
    @Inject("${solon.app.name}")
    private String projectName;

    @Inject("${project.home:}")
    private String home;

    @Inject("${spring.flyway.enabled:true}")
    private boolean flyway;

    // typed=true，表示默认数据源。@Db 可不带名字注入
    @Bean(value = "db1", typed = true)
    public DataSource getDataSource() {

        String homeAbsolutePath = PathUtils.homeAbsolutePath(home, projectName);


        HikariDataSource ds = null;
        String migrationPath = null;
        if (StringUtils.isBlank(databaseType) || ReUtil.isMatch("(?i)h2", databaseType)) {
            String h2Path = StringUtils.endsWith(homeAbsolutePath, File.separator) ? homeAbsolutePath + "h2" : homeAbsolutePath + File.separator + "h2";
            try {
                FileUtils.forceMkdir(new File(h2Path));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            String defaultDbName = "h2";
            String h2DbPath = StringUtils.joinWith(File.separator, h2Path, defaultDbName + ".mv.db");
            if (FileUtil.exist(h2DbPath)) {
                // Unsupported database file version or invalid file header in file
                // 判断h2 format版本
                // Caused by: org.h2.mvstore.MVStoreException: The write format 2 is smaller than the supported format 3

                String oldDbName = "old";

                if (Constants.VERSION_MAJOR == 2 && Constants.VERSION_MINOR > 1 && H2FormatVersionChecker.isVer2(h2DbPath)) {
                    // 2.1.214 ---> 2.2.224
                    //脚本升级


                    String jar_2_1_214 = StringUtils.joinWith(File.separator, h2Path, "h2-2.1.214.jar");
                    String jar_2_2_224 = StringUtils.joinWith(File.separator, h2Path, "h2-2.2.224.jar");

                    HttpUtil.downloadFile("http://mirrors.cloud.tencent.com/nexus/repository/maven-public/com/h2database/h2/2.1.214/h2-2.1.214.jar", new File(jar_2_1_214), 60000);
                    HttpUtil.downloadFile("http://mirrors.cloud.tencent.com/nexus/repository/maven-public/com/h2database/h2/2.2.224/h2-2.2.224.jar", new File(jar_2_2_224), 60000);

                    FileUtil.copy(new File(h2DbPath), new File(h2DbPath.replace(defaultDbName + ".mv.db", oldDbName + ".mv.db")), true);
                    String cmd1 = "java -cp " + jar_2_1_214 + " org.h2.tools.Script -url jdbc:h2:" + h2Path + File.separator + oldDbName + " -user sa -script " + h2Path + File.separator + "backup";
                    String cmd2 = "java -cp " + jar_2_2_224 + " org.h2.tools.RunScript -url jdbc:h2:" + h2Path + File.separator + defaultDbName + " -user sa -script " + h2Path + File.separator + "backup";
                    Process process = RuntimeUtil.exec(cmd1);
                    while (process.isAlive()) {
                        ThreadUtil.safeSleep(200);
                    }
                    if (process.exitValue() != 0) {
                        log.error(RuntimeUtil.getErrorResult(process));
                        System.exit(process.exitValue());
                    }
                    FileUtil.del(new File(h2DbPath));
                    process = RuntimeUtil.exec(cmd2);
                    while (process.isAlive()) {
                        ThreadUtil.safeSleep(200);
                    }
                    if (process.exitValue() != 0) {
                        //如果脚本执行失败得回滚数据
                        FileUtil.copy(new File(h2DbPath.replace(defaultDbName + ".mv.db", oldDbName + ".mv.db")), new File(h2DbPath), true);
                        log.error(RuntimeUtil.getErrorResult(process));
                        System.exit(process.exitValue());
                    }


                } else if (Constants.VERSION_MAJOR == 2 && Constants.VERSION_MINOR <= 1 && H2FormatVersionChecker.isVer3(h2DbPath)) {
                    // 2.2.224 ---> 2.1.214
                    //脚本降级


                    String jar_2_1_214 = StringUtils.joinWith(File.separator, h2Path, "h2-2.1.214.jar");
                    String jar_2_2_224 = StringUtils.joinWith(File.separator, h2Path, "h2-2.2.224.jar");
                    HttpUtil.downloadFile("http://mirrors.cloud.tencent.com/nexus/repository/maven-public/com/h2database/h2/2.1.214/h2-2.1.214.jar", new File(jar_2_1_214), 60000);
                    HttpUtil.downloadFile("http://mirrors.cloud.tencent.com/nexus/repository/maven-public/com/h2database/h2/2.2.224/h2-2.2.224.jar", new File(jar_2_2_224), 60000);

                    FileUtil.copy(new File(h2DbPath), new File(h2DbPath.replace(defaultDbName + ".mv.db", oldDbName + ".mv.db")), true);
                    String cmd1 = "java -cp " + jar_2_2_224 + " org.h2.tools.Script -url jdbc:h2:" + h2Path + File.separator + oldDbName + " -user sa -script " + h2Path + File.separator + "backup";
                    String cmd2 = "java -cp " + jar_2_1_214 + " org.h2.tools.RunScript -url jdbc:h2:" + h2Path + File.separator + defaultDbName + " -user sa -script " + h2Path + File.separator + "backup";
                    Process process = RuntimeUtil.exec(cmd1);
                    while (process.isAlive()) {
                        ThreadUtil.safeSleep(200);
                    }
                    if (process.exitValue() != 0) {
                        log.error(RuntimeUtil.getErrorResult(process));
                        System.exit(process.exitValue());
                    }
                    FileUtil.del(new File(h2DbPath));
                    process = RuntimeUtil.exec(cmd2);
                    while (process.isAlive()) {
                        ThreadUtil.safeSleep(200);
                    }
                    if (process.exitValue() != 0) {
                        //如果脚本执行失败得回滚数据
                        FileUtil.copy(new File(h2DbPath.replace(defaultDbName + ".mv.db", oldDbName + ".mv.db")), new File(h2DbPath), true);
                        log.error(RuntimeUtil.getErrorResult(process));
                        System.exit(process.exitValue());
                    }

                }

            }


            HikariConfig dbConfig = new HikariConfig();
            dbConfig.setJdbcUrl("jdbc:h2:" + h2Path + File.separator + defaultDbName + ";DB_CLOSE_DELAY=1000;DB_CLOSE_ON_EXIT=FALSE");
            dbConfig.setUsername("sa");
            dbConfig.setPassword("");
            dbConfig.setMaximumPoolSize(1);
            dbConfig.setDriverClassName(org.h2.Driver.class.getName());
            ds = new HikariDataSource(dbConfig);
            migrationPath = "classpath:db/migration/h2";
        } else if (ReUtil.isMatch("(?i)sqlite", databaseType)) {
            String sqlitePath = StringUtils.endsWith(homeAbsolutePath, File.separator) ? homeAbsolutePath + "sqlite" : homeAbsolutePath + File.separator + "sqlite";
            try {
                FileUtils.forceMkdir(new File(sqlitePath));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            HikariConfig dbConfig = new HikariConfig();
            dbConfig.setJdbcUrl("jdbc:sqlite:" + sqlitePath + File.separator + "data.db");
            dbConfig.setUsername(username);
            dbConfig.setPassword(password);
            dbConfig.setMaximumPoolSize(1);
            dbConfig.setDriverClassName(org.sqlite.JDBC.class.getName());
            ds = new HikariDataSource(dbConfig);
            migrationPath = "classpath:db/migration/sqlite";
        } else if (ReUtil.isMatch("(?i)my(sql)?", databaseType)) {
            HikariConfig dbConfig = new HikariConfig();
            dbConfig.setJdbcUrl(url);
            dbConfig.setUsername(username);
            dbConfig.setPassword(password);
            dbConfig.setMaximumPoolSize(1);
            dbConfig.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getName());
            ds = new HikariDataSource(dbConfig);
            migrationPath = "classpath:db/migration/mysql";
        } else if (ReUtil.isMatch("(?i)postgres(ql)?|pg(sql)?", databaseType)) {
            HikariConfig dbConfig = new HikariConfig();
            dbConfig.setJdbcUrl(url);
            dbConfig.setUsername(username);
            dbConfig.setPassword(password);
            dbConfig.setMaximumPoolSize(1);
            dbConfig.setDriverClassName(org.postgresql.Driver.class.getName());
            ds = new HikariDataSource(dbConfig);
            migrationPath = "classpath:db/migration/postgresql";
        }
        boolean flaywaySkipMysql5 = false;
        //判断mysql版本，如果是5.多版本则跳过flayway
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DatabaseMetaData meta = conn.getMetaData();
            if (ReUtil.isMatch("(?i).*mysql.*", meta.getDriverName()) && ReUtil.isMatch("^(?i)5\\..*", meta.getDatabaseProductVersion())) {
                flaywaySkipMysql5 = true;
            }
            if (meta instanceof DatabaseMetaData) {

            }
        } catch (SQLException e) {

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }

        }
        log.info("flyway {}, will {}.", flyway ? "enabled" : "disabled", flyway ? "migrate" : "skip migrate");
        if (flyway) {
            if (!flaywaySkipMysql5) {
                Flyway flyway = Flyway.configure()
                        .locations(migrationPath)
                        .baselineOnMigrate(true)
                        .cleanDisabled(true)
                        .mixed(true)
                        .dataSource(ds.getJdbcUrl(), ds.getUsername(), ds.getPassword()).load();
                flyway.migrate();
            }
        }

        return ds;
    }


//	public void setDataSource(DataSource dataSource) {
//		this.dataSource = dataSource;
//	}

}
