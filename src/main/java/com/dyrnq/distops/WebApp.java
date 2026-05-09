package com.dyrnq.distops;

import cn.hutool.system.SystemUtil;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.noear.snack4.ONode;
import org.noear.solon.Solon;
import org.noear.solon.annotation.SolonMain;
import org.noear.solon.scheduling.annotation.EnableScheduling;
import org.noear.solon.view.freemarker.FreemarkerRender;
import org.noear.wood.WoodConfig;

import java.util.Set;


@Slf4j
@SolonMain
@EnableScheduling
public class WebApp {

    public static void main(String[] args) {
        Solon.start(WebApp.class, args, app -> {
            Set<String> allNodes = Solon.cfg().stringPropertyNames();
            for (String entry : allNodes) {
                String envName1 = StringUtils.upperCase(Strings.CS.replace(entry, "-", "").replace(".", "_"));
                String envName2 = StringUtils.upperCase(Strings.CS.replace(entry, "-", "_").replace(".", "_"));
                String envName3 = StringUtils.upperCase(Strings.CS.replace(entry.replaceAll("(?<!^)(?=[A-Z])", "_"), "-", "_").replace(".", "_"));

                String getValue = SystemUtil.get(envName1, true);
                if (getValue != null) {
                    Solon.cfg().setProperty(entry, getValue);
                }
                getValue = SystemUtil.get(envName2, true);
                if (getValue != null) {
                    Solon.cfg().setProperty(entry, getValue);
                }
                getValue = SystemUtil.get(envName3, true);
                if (getValue != null) {
                    Solon.cfg().setProperty(entry, getValue);
                }
            }

            app.context().getBeanAsync(FreemarkerRender.class, e -> {
                Configuration cfg = e.getProvider();
                try {
                    //cfg.setClassicCompatible(false);
                    //cfg.setStrictSyntaxMode(false);
                    cfg.setSetting(Configuration.NUMBER_FORMAT_KEY, "0.##");
                    cfg.setSetting(Configuration.DEFAULT_ENCODING_KEY, "UTF-8");
                    cfg.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY, "0");
                    cfg.setSetting(Configuration.CACHE_STORAGE_KEY, "strong:20, soft:250");
                    // rethrow,debug,html_debug,ignore;
                    if (Solon.cfg().isDebugMode()) {
                        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
                    } else {
                        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
                    }
                } catch (TemplateException ex) {
                    log.error(ex.getMessage(), ex);
                }

            });
            app.filter((c, chain) -> {
                String path = c.path();
                while (path.contains("//")) {
                    path = path.replace("//", "/");
                }
                c.pathNew(path);
                chain.doFilter(c);
            });


            WoodConfig.isUsingValueExpression = false;
            if (Solon.cfg().isDebugMode()) {
                //执行后打印下sql
                WoodConfig.onExecuteAft(cmd -> {
                    System.out.println(cmd.text + "\r\n" + ONode.serialize(cmd.paramMap()));
                });

                WoodConfig.onException((cmd, err) -> {
                    System.out.println(cmd.text + "\r\n" + ONode.serialize(cmd.paramMap()));
                });
            }

            log.info("version: {} , build_date: {} ", Constants.VERSION, Constants.BUILD_DATETIME);

        });
    }


}
