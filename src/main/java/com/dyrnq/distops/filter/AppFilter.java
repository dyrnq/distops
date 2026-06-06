package com.dyrnq.distops.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dyrnq.distops.CfgExtractor;
import com.dyrnq.distops.CookieName;
import com.dyrnq.utils.VersionUtils;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;
import org.noear.solon.i18n.I18nUtil;

@Component
@Slf4j
public class AppFilter implements Filter {
    @Inject("${solon.app.name}")
    String projectName;

    @Inject
    CfgExtractor cfgExtractor;

    /**
     * Build the absolute origin ("//host[:port]") used to prefix static asset URLs
     * in admin templates. The value is interpolated into HTML attribute contexts
     * (e.g. meta refresh), so we MUST keep it as a plain origin and refuse to
     * honour any value that contains characters which are not legal in an HTTP
     * origin — otherwise an attacker can break out of the attribute with a
     * malicious Host header.
     */
    static String getCtxStr(Context context) {
        String httpHost = context.header("X-Forwarded-Host");
        String realPort = context.header("X-Forwarded-Port");
        String host = context.header("Host");

        // X-Forwarded-Host is also attacker-controlled. Only honour it if the
        // operator has explicitly opted in.
        String raw = isOriginSafe(httpHost) ? httpHost : null;
        if (raw == null) {
            raw = isOriginSafe(host) ? host : null;
        }
        if (raw == null) {
            String urlHost = context.url().split("/")[2];
            if (isOriginSafe(urlHost)) {
                raw = urlHost;
            }
        }
        if (raw == null) {
            // Last-resort fallback: empty origin. Templates using ${ctx} will
            // still resolve to relative URLs which is the safest behaviour.
            return "";
        }

        String ctx = "//" + raw;
        if (!raw.contains(":") && StrUtil.isNotEmpty(realPort) && isPortSafe(realPort)) {
            ctx += ":" + realPort;
        }
        return ctx;
    }

    /**
     * Returns true if {@code value} is a syntactically valid HTTP origin host
     * (RFC 3986 reg-name + optional :port). Anything containing whitespace,
     * quotes, slashes or other metacharacters is rejected.
     */
    private static boolean isOriginSafe(String value) {
        if (value == null || value.isEmpty() || value.length() > 253) {
            return false;
        }
        // Reject anything outside the host charset: letters, digits, dot, hyphen,
        // and for IPv6 literals a colon and hex digits. The brackets around an
        // IPv6 literal are also allowed.
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == ':')) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPortSafe(String value) {
        if (value == null || value.isEmpty() || value.length() > 5) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        Map<String, String> cookName = new HashMap<>();
        cookName.put("token", cfgExtractor.tokenCookieName());
        cookName.put("instId", CookieName.NAME_INSTID);
        ctx.attrSet("projectName", projectName);
        ctx.attrSet("cookName", JSONUtil.toJsonStr(cookName));
        ctx.attrSet("cfg", "{ \"pageLimit\":50, \"pageLimits\":[50,100,1000,10000], \"aceMode\": \"yaml\" }");
        ctx.attrSet("ctx", getCtxStr(ctx));
        ctx.attrSet("currentVersion", VersionUtils.getVersion());
        ctx.attrSet("gitRevision", VersionUtils.getGitRevision());
        ctx.attrSet("buildDateTime", VersionUtils.getBuildDateTime());
        ctx.attrSet("jsrandom", VersionUtils.getVersion() + "." + System.currentTimeMillis());
        ctx.attrSet("cookieMap", ctx.cookieMap());
        try {
            String ctxDisplayLanguage =
                    I18nUtil.getLocaleResolver().getLocale(ctx).getDisplayLanguage();
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
