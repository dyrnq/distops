package com.dyrnq.distops.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dyrnq.distops.HomeDir;
import com.dyrnq.distops.dso.ArtifactMapper;
import com.dyrnq.distops.dso.InstMapper;
import com.dyrnq.distops.dso.ManifestMapper;
import com.dyrnq.distops.dso.RepoMapper;
import com.dyrnq.distops.model.Artifact;
import com.dyrnq.distops.model.Inst;
import com.dyrnq.distops.model.Manifest;
import com.dyrnq.distops.model.Repo;
import com.dyrnq.utils.IDUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.noear.solon.annotation.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Controller
@Mapping("event")
@Slf4j
public class EventController {

    @Inject
    HomeDir homeDir;

    @Inject
    InstMapper instMapper;

    @Inject
    RepoMapper repoMapper;

    @Inject
    ManifestMapper manifestMapper;

    @Inject
    ArtifactMapper artifactMapper;

    @Mapping("/{instName}")
    public String event(@Body String o, @Path("instName") String instName) throws IOException {
        Long id = IDUtils.getLongID();
        File file = new File(StringUtils.joinWith(File.separator, homeDir.getTmpAbsolutePath(), "event", instName, "event_" + id + ".json"));
        log.info(file.getAbsolutePath());
        FileUtils.forceMkdirParent(file);
        FileUtil.appendUtf8String(o, file);

        // Parse and process the event data
        processEvent(o, instName);

        return "200";
    }

    @Mapping("")
    public String event(@Body String o) throws IOException {
        Long id = IDUtils.getLongID();
        File file = new File(StringUtils.joinWith(File.separator, homeDir.getTmpAbsolutePath(), "event", "event_" + id + ".json"));
        log.info(file.getAbsolutePath());
        FileUtils.forceMkdirParent(file);
        FileUtil.appendUtf8String(o, file);
        return "200";
    }

    /**
     * Process the event JSON and insert/update repo and repo_tag tables
     */
    private void processEvent(String json, String instName) {
        try {
            JSONObject jsonObject = JSONUtil.parseObj(json);
            JSONArray events = jsonObject.getJSONArray("events");
            if (events == null || events.isEmpty()) {
                return;
            }

            // Get inst by name
            Inst inst = instMapper.findByName(instName);
            if (inst == null) {
                log.warn("Instance not found: {}", instName);
                return;
            }

            Long instId = inst.getId();

            for (int i = 0; i < events.size(); i++) {
                JSONObject event = events.getJSONObject(i);
                String action = event.getStr("action");

                // Only process pull and push actions
                if (!"pull".equals(action) && !"push".equals(action)) {
                    continue;
                }

                JSONObject target = event.getJSONObject("target");
                if (target == null) {
                    continue;
                }

                String repository = target.getStr("repository");
                String digest = target.getStr("digest");
                String tag = target.getStr("tag");
                Long size = target.getLong("size");
                String mediaType = target.getStr("mediaType");

                if (StringUtils.isBlank(repository) || StringUtils.isBlank(digest)) {
                    continue;
                }

                // Parse timestamp
                LocalDateTime lastPushed = null;
                String timestamp = event.getStr("timestamp");
                if (StringUtils.isNotBlank(timestamp)) {
                    try {
                        // Handle ISO 8601 format with timezone offset: 2026-02-27T10:40:57.779182297+08:00
                        String ts = timestamp;
                        if (ts.contains("+") || ts.contains("-")) {
                            // Remove timezone offset for parsing
                            int plusIndex = ts.indexOf('+');
                            int minusIndex = ts.lastIndexOf('-');
                            if (plusIndex > 0) {
                                ts = ts.substring(0, plusIndex);
                            } else if (minusIndex > 10) { // Avoid matching date part
                                ts = ts.substring(0, minusIndex);
                            }
                        }
                        ts = ts.replace("Z", "");
                        lastPushed = LocalDateTime.parse(ts, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } catch (Exception e) {
                        log.warn("Failed to parse timestamp: {}", timestamp, e);
                    }
                }

                // Check if this is a manifest list/index (multi-arch image)
                boolean isManifestList = mediaType != null && (
                        mediaType.contains("application/vnd.docker.distribution.manifest.list") ||
                        mediaType.contains("application/vnd.oci.image.index")
                );

                // Process repository (create or update)
                Repo repo = repoMapper.findByInstIdAndRepoName(instId, repository);
                if (repo == null || repo.getId() == null) {
                    repo = new Repo();
                    repo.setId(IDUtils.getLongID());
                    repo.setInstId(instId);
                    repo.setRepoName(repository);

                    repo.setArtifactCount(0);
                    repoMapper.insert(repo, true);
                }

                // Process main manifest and tag - using new manifest/tag tables
                // Process manifest - create or update manifest record
                Manifest manifest = manifestMapper.findByInstIdAndDigest(instId, digest);
                if (manifest == null || manifest.getId() == null) {
                    // New manifest - create record
                    manifest = new Manifest();
                    manifest.setId(IDUtils.getLongID());
                    manifest.setInstId(instId);
                    manifest.setRepoId(repo.getId());
                    manifest.setDigest(digest);
                    manifest.setMediaType(mediaType);
                    manifest.setSize(size);
                    manifest.setLastPushed(lastPushed);
                    manifestMapper.insert(manifest, true);
                    log.info("Inserted manifest: repo={}, digest={}", repository, digest);
                } else {
                    // Existing manifest - update info
                    manifest.setSize(size);
                    manifest.setLastPushed(lastPushed);
                    manifestMapper.updateById(manifest, true);
                }

                // Process artifact - create or update artifact record
                if (StringUtils.isNotBlank(tag)) {
                    Artifact artifactRecord = artifactMapper.findByInstIdAndRepoIdAndTagName(instId, repo.getId(), tag);
                    if (artifactRecord == null || artifactRecord.getId() == null) {
                        // New artifact - create record
                        artifactRecord = new Artifact();
                        artifactRecord.setId(IDUtils.getLongID());
                        artifactRecord.setInstId(instId);
                        artifactRecord.setRepoId(repo.getId());
                        artifactRecord.setRepoName(repository); // 冗余字段
                        artifactRecord.setManifestId(manifest.getId());
                        artifactRecord.setTagName(tag);
                        artifactRecord.setFullName(repository + (tag != null ? ":" + tag : ""));
                        if ("push".equals(action)) {
                            artifactRecord.setLastPushed(lastPushed);
                        } else if ("pull".equals(action)) {
                            artifactRecord.setLastPulled(lastPushed);
                        }
                        artifactMapper.insert(artifactRecord, true);
                        log.info("Inserted artifact: repo={}, tag={}, manifestId={}", repository, tag, manifest.getId());
                    } else {
                        // Existing artifact - update manifest reference
                        artifactRecord.setManifestId(manifest.getId());
                        if ("push".equals(action)) {
                            artifactRecord.setLastPushed(lastPushed);
                        } else if ("pull".equals(action)) {
                            artifactRecord.setLastPulled(lastPushed);
                        }
                        artifactMapper.updateById(artifactRecord, true);
                    }

                    // Update artifact_count: count distinct non-null tag names for this repo
                    Integer newTagCount = artifactMapper.countDistinctArtifactsByRepoId(repo.getId());
                    if (!Objects.equals(repo.getArtifactCount(), newTagCount)) {
                        repo.setArtifactCount(newTagCount);
                        repoMapper.updateById(repo, true);
                        log.info("Updated artifact_count for repo={}: count={}", repository, newTagCount);
                    }
                }

                // Process multi-arch references if this is a manifest list
                if (isManifestList) {
                    processReferences(target, instId, repo.getId(), manifest.getDigest(), lastPushed);
                } else {
                    // For single-arch images, extract os/arch from target
                    JSONObject platform = target.getJSONObject("platform");
                    if (platform != null) {
                        String osArch = platform.getStr("architecture");
                        String os = platform.getStr("os");
                        String variant = platform.getStr("variant");

                        // Update manifest with os/arch info
                        if (StringUtils.isNotBlank(osArch)) {
                            manifest.setOsArch(osArch);
                        }
                        if (StringUtils.isNotBlank(os)) {
                            manifest.setOs(os);
                        }
                        if (StringUtils.isNotBlank(variant)) {
                            manifest.setVariant(variant);
                        }
                        manifestMapper.updateById(manifest, true);
                    }
                }

                log.info("Processed event: action={}, repository={}, digest={}, tag={}, isManifestList={}",
                        action, repository, digest, tag, isManifestList);
            }
        } catch (Exception e) {
            log.error("Failed to process event: {}", json, e);
        }
    }

    /**
     * Process references for multi-arch manifest lists
     */
    private void processReferences(JSONObject target, Long instId, Long repoId, String parentDigest, LocalDateTime lastPushed) {
        log.info("Processing references for manifest list: parentDigest={}", parentDigest);
        JSONArray references = target.getJSONArray("references");
        if (references == null || references.isEmpty()) {
            log.info("No references found in manifest list");
            return;
        }

        log.info("Found {} references in manifest list", references.size());

        for (int i = 0; i < references.size(); i++) {
            JSONObject ref = references.getJSONObject(i);
            String refDigest = ref.getStr("digest");
            Long refSize = ref.getLong("size");
            String refMediaType = ref.getStr("mediaType");

            if (StringUtils.isBlank(refDigest)) {
                continue;
            }

            // Skip attestation manifests
            JSONObject annotations = ref.getJSONObject("annotations");
            if (annotations != null) {
                String refType = annotations.getStr("vnd.docker.reference.type");
                if ("attestation-manifest".equals(refType)) {
                    log.info("Skipping attestation manifest: {}", refDigest);
                    continue;
                }
            }

            // Extract platform info
            String osArch = null;
            String os = null;
            String variant = null;
            LocalDateTime created = null;

            JSONObject platform = ref.getJSONObject("platform");
            if (platform != null) {
                osArch = platform.getStr("architecture");
                os = platform.getStr("os");
                variant = platform.getStr("variant");
            }

            // Extract created time from annotations
            if (annotations != null) {
                String createdStr = annotations.getStr("org.opencontainers.image.created");
                if (StringUtils.isNotBlank(createdStr)) {
                    try {
                        created = LocalDateTime.parse(createdStr.replace("Z", ""), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } catch (Exception e) {
                        log.warn("Failed to parse created time: {}", createdStr, e);
                    }
                }
            }

            // Check if this reference already exists
            Manifest existingManifest = manifestMapper.findByInstIdAndDigest(instId, refDigest);
            if (existingManifest == null || existingManifest.getId() == null) {
                // Insert new reference
                Manifest refManifest = new Manifest();
                refManifest.setId(IDUtils.getLongID());
                refManifest.setInstId(instId);
                refManifest.setRepoId(repoId);
                refManifest.setDigest(refDigest);
                refManifest.setSize(refSize);
                refManifest.setMediaType(refMediaType);
                refManifest.setOsArch(osArch);
                refManifest.setOs(os);
                refManifest.setVariant(variant);
                refManifest.setCreated(created);
                refManifest.setLastPushed(lastPushed);
                refManifest.setParentDigest(parentDigest);
                refManifest.setPushCount(1);
                manifestMapper.insert(refManifest, true);
                log.info("Inserted reference manifest: digest={}, arch={}, os={}", refDigest, osArch, os);
            } else {
                // Update existing reference
                existingManifest.setSize(refSize);
                existingManifest.setMediaType(refMediaType);
                if (osArch != null) existingManifest.setOsArch(osArch);
                if (os != null) existingManifest.setOs(os);
                if (variant != null) existingManifest.setVariant(variant);
                if (created != null) existingManifest.setCreated(created);
                existingManifest.setLastPushed(lastPushed);
                existingManifest.setParentDigest(parentDigest);
                manifestMapper.updateById(existingManifest, true);
                log.info("Updated reference manifest: digest={}, arch={}, os={}", refDigest, osArch, os);
            }
        }
    }
}
