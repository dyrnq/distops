package com.dyrnq.distops.controller.api;

import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.dyrnq.distops.controller.ApiController;
import com.dyrnq.distops.controller.PageResult;
import com.dyrnq.distops.dso.*;
import com.dyrnq.distops.model.*;
import com.dyrnq.distops.service.dto.ArtQuery;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Result;
import org.noear.wood.IPage;

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

    @Inject
    ManifestMapper manifestMapper;

    @Inject
    RepoMapper repoMapper;

    @Inject
    InstMapper instMapper;

    /**
     * Query artifacts with pagination
     */
    @Mapping("")
    public PageResult query(Context ctx, int page, int limit, ArtQuery artQuery) {
        try {
            int start = PageUtil.getStart(page - 1, limit);

            // Resolve instName to instId if provided
            final Long filterInstId;
            if (artQuery != null && StrUtil.isNotBlank(artQuery.getInstName())) {
                com.dyrnq.distops.model.Inst inst = instMapper.findByName(artQuery.getInstName());
                if (inst != null) {
                    filterInstId = inst.getId();
                } else {
                    return PageResult.succeed(java.util.Collections.emptyList(), 0L);
                }
            } else {
                filterInstId = null;
            }

            StringBuilder sql = new StringBuilder(
                "select v.*, i.name as inst_name from artifact_manifest_view v, inst i where v.inst_id = i.id");
            StringBuilder countSql = new StringBuilder(
                "select count(*) from artifact_manifest_view v, inst i where v.inst_id = i.id");

            if (filterInstId != null) {
                String cond = " and v.inst_id = " + filterInstId;
                sql.append(cond);
                countSql.append(cond);
            }
            if (StrUtil.isNotBlank(artQuery.getRepoName())) {
                String cond = " and v.full_name like '%" + artQuery.getRepoName() + "%'";
                sql.append(cond);
                countSql.append(cond);
            }
            if (StrUtil.isNotBlank(artQuery.getTagName())) {
                String cond = " and (v.tag_name like '%" + artQuery.getTagName() + "%' or v.digest like '%" + artQuery.getTagName() + "%')";
                sql.append(cond);
                countSql.append(cond);
            }
            sql.append(" LIMIT ?,?");
            List<ArtifactManifestView> artifactList = instMapper.db().sql(sql.toString(), start, limit).getList(ArtifactManifestView.class);
            long count = instMapper.db().sql(countSql.toString()).getCount();
            return PageResult.succeed(artifactList, count);
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
     * Delete artifacts by IDs.
     * Also deletes the manifest from the registry so GC can clean up physical files.
     */
    @Mapping("del")
    public Result del(Context ctx, Long... id) {
        try {
            StringBuilder resultMsg = new StringBuilder();
            for (Long artifactId : id) {
                Artifact artifact = artifactMapper.selectById(artifactId);
                if (artifact == null) {
                    log.warn("Artifact not found: {}", artifactId);
                    continue;
                }

                Inst inst = instMapper.selectById(artifact.getInstId());
                Manifest manifest = manifestMapper.selectById(artifact.getManifestId());

                // Delete manifest from registry (marks blobs for GC)
                if (inst != null && manifest != null) {
                    deleteRegistryManifest(inst, artifact.getRepoName(), manifest.getDigest());
                    log.info("Deleted manifest from registry: {}/{} @ {}", inst.getName(), artifact.getFullName(), manifest.getDigest());

                    // If manifest is a multi-arch index, also delete child manifests
                    if (manifest.getDigest() != null) {
                        List<Manifest> children = manifestMapper.findByParentDigest(inst.getId(), manifest.getDigest());
                        if (children != null) {
                            for (Manifest child : children) {
                                deleteRegistryManifest(inst, artifact.getRepoName(), child.getDigest());
                                log.info("Deleted child manifest from registry: {}", child.getDigest());
                                manifestMapper.deleteById(child.getId());
                            }
                        }
                    }
                }

                // Delete artifact from database
                artifactMapper.deleteById(artifactId);

                // If no other artifacts reference this manifest, delete it too
                if (manifest != null) {
                    List<Artifact> remaining = artifactMapper.selectList(c ->
                            c.whereEq(Artifact.MANIFEST_ID, manifest.getId())
                    );
                    if (remaining == null || remaining.isEmpty()) {
                        manifestMapper.deleteById(manifest.getId());
                        log.info("Deleted orphaned manifest: {}", manifest.getDigest());
                    }
                }

                resultMsg.append(artifact.getFullName()).append(" deleted. ");
            }
            return Result.succeed(resultMsg.toString().trim());
        } catch (Exception e) {
            log.error("Failed to delete artifacts", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Call the registry's DELETE /v2/{name}/manifests/{reference} API
     * to remove a manifest and mark its blobs for garbage collection.
     */
    private void deleteRegistryManifest(Inst inst, String repoName, String digest) {
        int registryPort = inst.getPort() != null ? inst.getPort() : 5000;
        String url = "http://127.0.0.1:" + registryPort + "/v2/" + repoName + "/manifests/" + digest;
        try (HttpResponse response = HttpRequest.of(url)
                .method(Method.DELETE)
                .header("Accept", "application/vnd.docker.distribution.manifest.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json")
                .execute()) {
            if (!response.isOk() && response.getStatus() != 404) {
                log.warn("Registry delete returned {}: {}", response.getStatus(), response.body());
            }
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

    @Mapping("queryManifest")
    public Result queryManifest(Context ctx, Long manifestId) {
        try {
            // First try OCI manifest list (multi-arch)
            List<ArtifactManifestOciView> ociList = artifactManifestOciViewMapper.selectList(c -> {
                c.whereEq(ArtifactManifestOciView.MANIFEST_LIST_ID, manifestId);
            });
            if (ociList != null && !ociList.isEmpty()) {
                // Deduplicate by child digest: multiple tags can point to the same OCI index
                java.util.List<ArtifactManifestOciView> deduped = new java.util.ArrayList<>();
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (ArtifactManifestOciView oci : ociList) {
                    if (seen.add(oci.getChildDigest())) {
                        deduped.add(oci);
                    }
                }
                return Result.succeed(deduped);
            }

            // Not an OCI index, return single manifest detail
            Manifest manifest = manifestMapper.selectById(manifestId);
            if (manifest == null) {
                return Result.failure("Manifest not found");
            }
            return Result.succeed(manifest);
        } catch (Exception e) {
            log.error("Failed to query manifest", e);
            return Result.failure(e.getMessage());
        }
    }


}
