package com.dyrnq.distops;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpUtil;
import com.dyrnq.utils.PathUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.h2.engine.Constants;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

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
            String h2Path = StringUtils.endsWith(homeAbsolutePath, File.separator)
                    ? homeAbsolutePath + "h2"
                    : homeAbsolutePath + File.separator + "h2";
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

                if (Constants.VERSION_MAJOR == 2
                        && Constants.VERSION_MINOR > 1
                        && H2FormatVersionChecker.isVer2(h2DbPath)) {
                    // 2.1.214 ---> 2.2.224
                    // 脚本升级
                    upgradeH2(h2Path, h2DbPath, defaultDbName, "2.1.214", "2.2.224", true);

                } else if (Constants.VERSION_MAJOR == 2
                        && Constants.VERSION_MINOR <= 1
                        && H2FormatVersionChecker.isVer3(h2DbPath)) {
                    // 2.2.224 ---> 2.1.214
                    // 脚本降级
                    upgradeH2(h2Path, h2DbPath, defaultDbName, "2.2.224", "2.1.214", false);
                }
            }

            HikariConfig dbConfig = new HikariConfig();
            dbConfig.setJdbcUrl("jdbc:h2:" + h2Path + File.separator + defaultDbName
                    + ";DB_CLOSE_DELAY=1000;DB_CLOSE_ON_EXIT=FALSE");
            dbConfig.setUsername("sa");
            dbConfig.setPassword("");
            dbConfig.setMaximumPoolSize(1);
            dbConfig.setDriverClassName(org.h2.Driver.class.getName());
            ds = new HikariDataSource(dbConfig);
            migrationPath = "classpath:db/migration/h2";
        } else if (ReUtil.isMatch("(?i)sqlite", databaseType)) {
            String sqlitePath = StringUtils.endsWith(homeAbsolutePath, File.separator)
                    ? homeAbsolutePath + "sqlite"
                    : homeAbsolutePath + File.separator + "sqlite";
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
        // 判断mysql版本，如果是5.多版本则跳过flayway
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DatabaseMetaData meta = conn.getMetaData();
            if (ReUtil.isMatch("(?i).*mysql.*", meta.getDriverName())
                    && ReUtil.isMatch("^(?i)5\\..*", meta.getDatabaseProductVersion())) {
                flaywaySkipMysql5 = true;
            }
            if (meta instanceof DatabaseMetaData) {}

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
                        .dataSource(ds.getJdbcUrl(), ds.getUsername(), ds.getPassword())
                        .load();
                flyway.migrate();
            }
        }

        return ds;
    }

    /**
     * Run the H2 schema upgrade / downgrade by shelling out to a paired
     * version of the H2 driver. The previous implementation called
     * {@code System.exit} on failure, which kills the entire JVM; this
     * version throws so Solon can surface a clean startup error and the
     * caller (e.g. systemd, s6) can decide whether to restart.
     */
    private void upgradeH2(
            String h2Path,
            String h2DbPath,
            String defaultDbName,
            String fromVersion,
            String toVersion,
            boolean upgrading) {
        String fromJar = StringUtils.joinWith(File.separator, h2Path, "h2-" + fromVersion + ".jar");
        String toJar = StringUtils.joinWith(File.separator, h2Path, "h2-" + toVersion + ".jar");
        try {
            HttpUtil.downloadFile(
                    "http://mirrors.cloud.tencent.com/nexus/repository/maven-public/com/h2database/h2/" + fromVersion
                            + "/h2-" + fromVersion + ".jar",
                    new File(fromJar),
                    60000);
            HttpUtil.downloadFile(
                    "http://mirrors.cloud.tencent.com/nexus/repository/maven-public/com/h2database/h2/" + toVersion
                            + "/h2-" + toVersion + ".jar",
                    new File(toJar),
                    60000);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to download H2 jars for schema migration", e);
        }

        String oldDbName = "old";
        FileUtil.copy(
                new File(h2DbPath), new File(h2DbPath.replace(defaultDbName + ".mv.db", oldDbName + ".mv.db")), true);

        // Use ProcessBuilder so the command line is constructed as a discrete
        // argv and cannot be reinterpreted by a shell, even though the inputs
        // here are all server-side constants.
        String urlFrom = "jdbc:h2:" + h2Path + File.separator + oldDbName;
        String urlTo = "jdbc:h2:" + h2Path + File.separator + defaultDbName;
        String script = h2Path + File.separator + "backup";
        String[] fromCmd = upgrading
                ? new String[] {
                    "java", "-cp", fromJar, "org.h2.tools.Script", "-url", urlFrom, "-user", "sa", "-script", script
                }
                : new String[] {
                    "java", "-cp", toJar, "org.h2.tools.Script", "-url", urlFrom, "-user", "sa", "-script", script
                };
        String[] toCmd = upgrading
                ? new String[] {
                    "java", "-cp", toJar, "org.h2.tools.RunScript", "-url", urlTo, "-user", "sa", "-script", script
                }
                : new String[] {
                    "java", "-cp", fromJar, "org.h2.tools.RunScript", "-url", urlTo, "-user", "sa", "-script", script
                };

        int rc1 = runH2Step(fromCmd);
        if (rc1 != 0) {
            throw new IllegalStateException(
                    "H2 script step failed (rc=" + rc1 + "): " + java.util.Arrays.toString(fromCmd));
        }
        FileUtil.del(new File(h2DbPath));
        int rc2 = runH2Step(toCmd);
        if (rc2 != 0) {
            // Roll back: the .mv.db file was deleted between steps.
            FileUtil.copy(
                    new File(h2DbPath.replace(defaultDbName + ".mv.db", oldDbName + ".mv.db")),
                    new File(h2DbPath),
                    true);
            throw new IllegalStateException(
                    "H2 runscript step failed (rc=" + rc2 + "): " + java.util.Arrays.toString(toCmd));
        }
    }

    private static int runH2Step(String[] cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            while (p.isAlive()) {
                ThreadUtil.safeSleep(200);
            }
            if (p.exitValue() != 0) {
                String err = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                log.error("H2 step failed: {} (rc={})\n{}", java.util.Arrays.toString(cmd), p.exitValue(), err);
            }
            return p.exitValue();
        } catch (Exception e) {
            log.error("H2 step exception: {}: {}", java.util.Arrays.toString(cmd), e.getMessage());
            return -1;
        }
    }

    //	public void setDataSource(DataSource dataSource) {
    //		this.dataSource = dataSource;
    //	}

}
