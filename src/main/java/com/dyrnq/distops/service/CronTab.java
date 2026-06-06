package com.dyrnq.distops.service;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.dyrnq.distops.dso.InstMapper;
import com.dyrnq.distops.model.Inst;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.scheduling.annotation.Scheduled;

@Component
@Slf4j
public class CronTab {

    @Inject
    InstMapper instMapper;

    /**
     * Run a supervisorctl subcommand and return its stdout, or an empty string
     * on failure. We shell out via ProcessBuilder (no string interpolation,
     * no shell) so a malicious inst.name cannot escape. The previous version
     * called {@code RuntimeUtil.exec("supervisorctl status " + svcName)} which
     * ran the string through {@code /bin/sh -c}.
     */
    public String exec(String... cmd) {
        try {
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            while (process.isAlive()) {
                ThreadUtil.safeSleep(200);
            }
            if (process.exitValue() == 0) {
                String re =
                        new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                log.debug("cmd={}, result={}", java.util.Arrays.toString(cmd), re);
                return re;
            }
        } catch (Exception e) {
            log.warn("cmd {} failed: {}", java.util.Arrays.toString(cmd), e.getMessage());
        }
        return "";
    }

    @Scheduled(fixedRate = 1000 * 3)
    public void run() {
        instMapper
                .selectList(c -> {
                    c.whereEq(Inst.ENABLED, 1);
                })
                .forEach(x -> {
                    try {
                        // Validate the instance name before letting it into a
                        // process argument, otherwise an old DB row with a
                        // weird name could end up as a shell token.
                        if (!isValidInstName(x.getName())) {
                            log.warn("Skipping cron for instance with unsafe name: {}", x.getName());
                            return;
                        }
                        String svcName = "registry-" + x.getName();
                        String status = exec("supervisorctl", "status", svcName);
                        if (StrUtil.containsIgnoreCase(status, "RUNNING")) {
                            String pidResult = exec("supervisorctl", "pid", svcName);
                            Long pid = Long.valueOf(StrUtil.trim(pidResult).replace("\"", ""));
                            instMapper.updatePid(x.getId(), pid);
                        } else {
                            instMapper.updatePid(x.getId(), 0L);
                        }

                    } catch (Exception e) {
                        // log.error(e.getMessage());
                    }
                });
    }

    private static boolean isValidInstName(String name) {
        if (name == null || name.isEmpty() || name.length() > 64) {
            return false;
        }
        if (".".equals(name) || "..".equals(name)) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.')) {
                return false;
            }
        }
        return true;
    }
}
