package com.dyrnq.distops.controller.api;

import cn.hutool.core.util.PageUtil;
import com.dyrnq.distops.controller.ApiController;
import com.dyrnq.distops.controller.PageResult;
import com.dyrnq.distops.dso.ManifestMapper;
import com.dyrnq.distops.model.Manifest;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Result;
import org.noear.wood.IPage;

import java.util.List;

@Mapping("api/manifest")
@Controller
@Slf4j
public class ManifestController extends ApiController {

    @Inject
    ManifestMapper manifestMapper;

    /**
     * Query manifests with pagination
     */
    @Mapping("")
    public PageResult query(Context ctx, int page, int limit) {
        try {
            int start = PageUtil.getStart(page - 1, limit);
            IPage<Manifest> p = manifestMapper.selectPage(start, limit, null);
            List<Manifest> manifestList = p.getList();
            return PageResult.succeed(manifestList, p.getTotal());
        } catch (Exception e) {
            log.error("Failed to query manifests", e);
            return PageResult.failure(e.getMessage());
        }
    }

    /**
     * Get manifest by ID
     */
    @Mapping("get")
    public Result get(Context ctx, Long id) {
        try {
            Manifest manifest = manifestMapper.selectById(id);
            if (manifest == null) {
                return Result.failure("Manifest not found");
            }
            return Result.succeed(manifest);
        } catch (Exception e) {
            log.error("Failed to get manifest", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Get manifest by digest
     */
    @Mapping("getByDigest")
    public Result getByDigest(Context ctx, String digest) {
        try {
            // Get inst_id from context or use default
            Long instId = ctx.param("instId") != null ? Long.valueOf(ctx.param("instId")) : 1L;
            Manifest manifest = manifestMapper.findByInstIdAndDigest(instId, digest);
            if (manifest == null) {
                return Result.failure("Manifest not found");
            }
            return Result.succeed(manifest);
        } catch (Exception e) {
            log.error("Failed to get manifest by digest", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Delete manifests by IDs
     */
    @Mapping("del")
    public Result del(Context ctx, Long... id) {
        try {
            for (Long i : id) {
                manifestMapper.deleteById(i);
            }
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to delete manifests", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Query child manifests by parent digest (for manifest lists)
     */
    @Mapping("children")
    public PageResult queryChildren(Context ctx, String parentDigest, int page, int limit) {
        try {
            int start = PageUtil.getStart(page - 1, limit);
            Long instId = ctx.param("instId") != null ? Long.valueOf(ctx.param("instId")) : 1L;

            IPage<Manifest> p = manifestMapper.selectPage(start, limit, c ->
                    c.whereEq(Manifest.INST_ID, instId)
                            .andEq(Manifest.PARENT_DIGEST, parentDigest)
            );
            List<Manifest> manifestList = p.getList();
            return PageResult.succeed(manifestList, p.getTotal());
        } catch (Exception e) {
            log.error("Failed to query child manifests", e);
            return PageResult.failure(e.getMessage());
        }
    }

    /**
     * Query manifests by repository ID with pagination
     */
    @Mapping("byRepo")
    public PageResult queryByRepo(Context ctx, Long repoId, int page, int limit) {
        try {
            int start = PageUtil.getStart(page - 1, limit);
            IPage<Manifest> p = manifestMapper.selectPage(start, limit, c ->
                    c.whereEq(Manifest.REPO_ID, repoId)
            );
            List<Manifest> manifestList = p.getList();
            return PageResult.succeed(manifestList, p.getTotal());
        } catch (Exception e) {
            log.error("Failed to query manifests by repo", e);
            return PageResult.failure(e.getMessage());
        }
    }

    /**
     * Query manifests by architecture
     */
    @Mapping("byArch")
    public PageResult queryByArch(Context ctx, String osArch, int page, int limit) {
        try {
            int start = PageUtil.getStart(page - 1, limit);
            IPage<Manifest> p = manifestMapper.selectPage(start, limit, c ->
                    c.whereEq(Manifest.OS_ARCH, osArch)
            );
            List<Manifest> manifestList = p.getList();
            return PageResult.succeed(manifestList, p.getTotal());
        } catch (Exception e) {
            log.error("Failed to query manifests by architecture", e);
            return PageResult.failure(e.getMessage());
        }
    }
}
