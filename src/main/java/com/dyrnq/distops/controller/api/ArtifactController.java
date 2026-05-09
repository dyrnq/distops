package com.dyrnq.distops.controller.api;

import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import com.dyrnq.distops.controller.ApiController;
import com.dyrnq.distops.controller.PageResult;
import com.dyrnq.distops.dso.ArtifactManifestOciViewMapper;
import com.dyrnq.distops.dso.ArtifactManifestViewMapper;
import com.dyrnq.distops.dso.ArtifactMapper;
import com.dyrnq.distops.model.Artifact;
import com.dyrnq.distops.model.ArtifactManifestOciView;
import com.dyrnq.distops.model.ArtifactManifestView;
import com.dyrnq.distops.service.dto.ArtQuery;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Result;
import org.noear.wood.IPage;
import org.noear.wood.MapperWhereQ;
import org.noear.wood.ext.Act1;

import java.util.List;

@Mapping("api/artifact")
@Controller
@Slf4j
public class ArtifactController extends ApiController {

    @Inject
    ArtifactMapper artifactMapper;

    @Inject
    ArtifactManifestViewMapper artifactManifestViewMapper;

    @Inject
    ArtifactManifestOciViewMapper artifactManifestOciViewMapper;

    /**
     * Query artifacts with pagination
     */
    @Mapping("")
    public PageResult query(Context ctx, int page, int limit, ArtQuery artQuery) {
        try {
            int start = PageUtil.getStart(page - 1, limit);

            Act1<MapperWhereQ> condition = mapperWhereQ -> {
                mapperWhereQ.whereTrue();
//                if (StrUtil.isNotBlank(artQuery.getFullName())) {
//                    mapperWhereQ.and().beginLk(ArtifactManifestView.FULL_NAME, "%" + artQuery.getFullName() + "%").end();
//                }
                if (StrUtil.isNotBlank(artQuery.getRepoName())) {
                    mapperWhereQ.and().beginLk(ArtifactManifestView.FULL_NAME, "%" + artQuery.getRepoName() + "%").end();
                }
                if (StrUtil.isNotBlank(artQuery.getTagName())) {
                    mapperWhereQ.and()
                            .beginLk(ArtifactManifestView.TAG_NAME, "%" + artQuery.getTagName() + "%")
                            .orLk(ArtifactManifestView.DIGEST, "%" + artQuery.getTagName() + "%")
                            .end();
                }
//                if (StrUtil.isNotBlank(artQuery.getDigest())) {
//                    mapperWhereQ.and().beginLk(ArtifactManifestView.DIGEST, "%" + artQuery.getDigest() + "%").end();
//                }
            };

            IPage<ArtifactManifestView> p = artifactManifestViewMapper.selectPage(start, limit, condition);
            List<ArtifactManifestView> artifactList = p.getList();
            return PageResult.succeed(artifactList, p.getTotal());
        } catch (Exception e) {
            log.error("Failed to query artifacts", e);
            return PageResult.failure(e.getMessage());
        }
    }

    /**
     * Get artifact by ID
     */
    @Mapping("get")
    public Result get(Context ctx, Long id) {
        try {
            Artifact artifact = artifactMapper.selectById(id);
            if (artifact == null) {
                return Result.failure("Artifact not found");
            }
            return Result.succeed(artifact);
        } catch (Exception e) {
            log.error("Failed to get artifact", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Delete artifacts by IDs
     */
    @Mapping("del")
    public Result del(Context ctx, Long... id) {
        try {
            for (Long i : id) {
                artifactMapper.deleteById(i);
            }
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to delete artifacts", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Add new artifact
     */
    @Mapping("add")
    public Result add(Context ctx, Artifact artifact) {
        try {
            artifactMapper.insert(artifact, true);
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to add artifact", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Update artifact
     */
    @Mapping("update")
    public Result update(Context ctx, Artifact artifact) {
        try {
            artifactMapper.updateById(artifact, true);
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to update artifact", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Query artifacts by repository ID with pagination
     */
    @Mapping("byRepo")
    public PageResult queryByRepo(Context ctx, Long repoId, int page, int limit) {
        try {
            int start = PageUtil.getStart(page - 1, limit);
            IPage<Artifact> p = artifactMapper.selectPage(start, limit, c -> c.whereEq(Artifact.REPO_ID, repoId));
            List<Artifact> artifactList = p.getList();
            return PageResult.succeed(artifactList, p.getTotal());
        } catch (Exception e) {
            log.error("Failed to query artifacts by repo", e);
            return PageResult.failure(e.getMessage());
        }
    }

    /**
     * Query artifacts by manifest ID
     */
    @Mapping("byManifest")
    public Result queryByManifest(Context ctx, Long manifestId) {
        try {
            List<Artifact> artifactList = artifactMapper.selectList(c ->
                    c.whereEq(Artifact.MANIFEST_ID, manifestId)
            );
            return Result.succeed(artifactList);
        } catch (Exception e) {
            log.error("Failed to query artifacts by manifest", e);
            return Result.failure(e.getMessage());
        }
    }

    @Mapping("queryOciByManifest")
    public Result queryOciByManifest(Context ctx, Long manifestId) {
        try {
            List<ArtifactManifestOciView> artifactList = artifactManifestOciViewMapper.selectList(c -> {
                c.whereEq(ArtifactManifestOciView.MANIFEST_LIST_ID, manifestId);
            });
            return Result.succeed(artifactList);
        } catch (Exception e) {
            log.error("Failed to query OCI artifacts by manifest", e);
            return Result.failure(e.getMessage());
        }
    }


}
