package com.dyrnq.distops.service;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
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


    public String exec(String cmd) {
        Process process = RuntimeUtil.exec(cmd);
        while (process.isAlive()) {
            ThreadUtil.safeSleep(200);
        }
        if (process.exitValue() == 0) {
            String re = RuntimeUtil.getResult(process);
            log.debug("cmd={}, result={}", cmd, re);
            return re;
        }
        return "";
    }

    @Scheduled(fixedRate = 1000 * 3)
    public void run() {
        instMapper.selectList(c -> {
            c.whereEq(Inst.ENABLED, 1);
        }).forEach(x -> {
            try {
                String svcName = "registry-" + x.getName();
                String cmd = "supervisorctl status " + svcName;
                String status = exec(cmd);
                if (StrUtil.containsIgnoreCase(status, "RUNNING")) {
                    cmd = "supervisorctl pid " + svcName;
                    String pidResult = exec(cmd);
                    Long pid = Long.valueOf(StrUtil.trim(pidResult).replace("\"", ""));
                    instMapper.updatePid(x.getId(), pid);
                } else {
                    instMapper.updatePid(x.getId(), 0L);
                }

            } catch (Exception e) {
                //log.error(e.getMessage());
            }
        });
    }
}
