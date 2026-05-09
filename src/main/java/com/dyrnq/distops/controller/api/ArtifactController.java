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

            Act1<MapperWhereQ> condition = mapperWhereQ -> {
                mapperWhereQ.whereTrue();
                if (filterInstId != null) {
                    mapperWhereQ.and().beginEq(ArtifactManifestView.INST_ID, filterInstId).end();
                }
                if (StrUtil.isNotBlank(artQuery.getRepoName())) {
                    mapperWhereQ.and().beginLk(ArtifactManifestView.FULL_NAME, "%" + artQuery.getRepoName() + "%").end();
                }
                if (StrUtil.isNotBlank(artQuery.getTagName())) {
                    mapperWhereQ.and()
                            .beginLk(ArtifactManifestView.TAG_NAME, "%" + artQuery.getTagName() + "%")
                            .orLk(ArtifactManifestView.DIGEST, "%" + artQuery.getTagName() + "%")
                            .end();
                }
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
