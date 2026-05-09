package com.dyrnq.distops.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dyrnq.distops.CfgExtractor;
import com.dyrnq.distops.CookieName;
import com.dyrnq.utils.VersionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;
import org.noear.solon.i18n.I18nUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AppFilter implements Filter {
    @Inject("${solon.app.name}")
    String projectName;

    @Inject
    CfgExtractor cfgExtractor;

    static String getCtxStr(Context context) {
        String httpHost = context.header("X-Forwarded-Host");
        String realPort = context.header("X-Forwarded-Port");
        String host = context.header("Host");

        String ctx = "//";
        if (StrUtil.isNotEmpty(httpHost)) {
            ctx += httpHost;
        } else if (StrUtil.isNotEmpty(host)) {
            ctx += host;
            if (!host.contains(":") && StrUtil.isNotEmpty(realPort)) {
                ctx += ":" + realPort;
            }
        } else {
            host = context.url().split("/")[2];
            ctx += host;
            if (!host.contains(":") && StrUtil.isNotEmpty(realPort)) {
                ctx += ":" + realPort;
            }
        }
        return ctx;
    }

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        Map<String, String> cookName = new HashMap<>();
        cookName.put("token", cfgExtractor.tokenCookieName());
        cookName.put("instId", CookieName.NAME_INSTID);
        ctx.attrSet("projectName", projectName);
        ctx.attrSet("cookName", JSONUtil.toJsonStr(cookName));
        ctx.attrSet("cfg", "{ \"pageLimit\":10, \"pageLimits\":[10,20,50,100,1000], \"aceMode\": \"yaml\" }");
        ctx.attrSet("ctx", getCtxStr(ctx));
        ctx.attrSet("currentVersion", VersionUtils.getVersion());
        ctx.attrSet("gitRevision", VersionUtils.getGitRevision());
        ctx.attrSet("jsrandom", VersionUtils.getVersion() + "." + System.currentTimeMillis());
        ctx.attrSet("cookieMap", ctx.cookieMap());
        try {
            String ctxDisplayLanguage = I18nUtil.getLocaleResolver().getLocale(ctx).getDisplayLanguage();
            if (Strings.CI.equals(ctxDisplayLanguage, "Chinese") || Strings.CI.equals(ctxDisplayLanguage, "中文")) {
                ctx.attrSet("langType", "简体中文");
            } else {
                ctx.attrSet("langType", "English");
            }
        } catch (Exception e) {
            ctx.attrSet("langType", "简体中文");
        }
        chain.doFilter(ctx);
    }

}
