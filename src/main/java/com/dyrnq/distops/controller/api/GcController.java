package com.dyrnq.distops.controller.api;

import com.dyrnq.distops.controller.ApiController;
import com.dyrnq.distops.service.GcService;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Result;

@Controller
@Slf4j
public class GcController extends ApiController {

    @Inject
    GcService gcService;

    /**
     * POST /api/gc/mark
     * Mark unreferenced blobs as 'orphan' (files kept for content-addressable reuse)
     * Body: { "instId": 1 }
     */
    @Mapping("/api/gc/mark")
    public Result mark() {
        try {
            Long instId = parseInstId();
            GcService.GcResult result = gcService.mark(instId);
            return Result.succeed(result);
        } catch (Exception e) {
            log.error("GC mark failed", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * POST /api/gc/purge
     * Actually delete orphan blob files and DB records.
     * Body: { "instId": 1 }
     */
    @Mapping("/api/gc/purge")
    public Result purge() {
        try {
            Long instId = parseInstId();
            GcService.GcResult result = gcService.purge(instId);
            return Result.succeed(result);
        } catch (Exception e) {
            log.error("GC purge failed", e);
            return Result.failure(e.getMessage());
        }
    }

    private Long parseInstId() {
        try {
            String body = org.noear.solon.core.handle.Context.current().body();
            if (body != null && !body.isBlank()) {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(body);
            if (json.containsKey("instId") && json.get("instId") != null) {
                return json.getLong("instId");
            }
        }
        return null; // all insts
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
