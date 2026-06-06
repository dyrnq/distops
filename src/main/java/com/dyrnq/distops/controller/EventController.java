package com.dyrnq.distops.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dyrnq.distops.HomeDir;
import com.dyrnq.distops.dso.ArtifactMapper;
import com.dyrnq.distops.dso.BlobMapper;
import com.dyrnq.distops.dso.InstMapper;
import com.dyrnq.distops.dso.ManifestBlobMapper;
import com.dyrnq.distops.dso.ManifestMapper;
import com.dyrnq.distops.dso.RepoMapper;
import com.dyrnq.distops.model.Artifact;
import com.dyrnq.distops.model.Blob;
import com.dyrnq.distops.model.Inst;
import com.dyrnq.distops.model.Manifest;
import com.dyrnq.distops.model.ManifestBlob;
import com.dyrnq.distops.model.Repo;
import com.dyrnq.utils.IDUtils;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.noear.solon.annotation.*;

@Controller
@Mapping("event")
@Slf4j
public class EventController {

    private static final java.util.concurrent.ExecutorService EVENT_EXECUTOR =
            java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "event-proc");
                t.setDaemon(true);
                return t;
            });

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

    @Inject
    BlobMapper blobMapper;

    @Inject
    ManifestBlobMapper manifestBlobMapper;

    @Mapping("/{instName}")
    public String event(@Body String o, @Path("instName") String instName) throws IOException {
        // Validate instName: must look like a normal instance name. Anything containing
        // path separators or ".." is rejected so the path we build below cannot escape
        // the events directory.
        if (!isSafeInstName(instName)) {
            log.warn("Rejected event with unsafe instName: {}", instName);
            return "400";
        }

        Long id = IDUtils.getLongID();
        File file = new File(StringUtils.joinWith(
                File.separator, homeDir.getTmpAbsolutePath(), "event", instName, "event_" + id + ".json"));
        log.info(file.getAbsolutePath());
        FileUtils.forceMkdirParent(file);
        FileUtil.appendUtf8String(o, file);

        // Process event asynchronously so registry doesn't block waiting for us.
        // Distribution registry synchronously waits for the event endpoint's response;
        // heavy processing (DB writes, manifest parsing) must not hold this thread.
        final String eventJson = o;
        final String name = instName;
        EVENT_EXECUTOR.submit(() -> processEvent(eventJson, name));

        return "200";
    }

    private static boolean isSafeInstName(String name) {
        if (name == null || name.isEmpty() || name.length() > 128) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            // Allow only URL-safe characters that make sense in a registry instance name.
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.')) {
                return false;
            }
        }
        // Forbid "." and ".." outright even though the loop above allows dots.
        return !".".equals(name) && !"..".equals(name);
    }

    @Mapping("")
    public String event(@Body String o) throws IOException {
        Long id = IDUtils.getLongID();
        File file = new File(
                StringUtils.joinWith(File.separator, homeDir.getTmpAbsolutePath(), "event", "event_" + id + ".json"));
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

                // Only process push/mount actions (pull events are too frequent and
                // the heavy processing per event causes memory/thread contention)
                if (!"push".equals(action) && !"mount".equals(action)) {
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

                // Parse timestamp. Distribution emits ISO 8601 with optional
                // fractional seconds and a timezone offset. We strip the
                // offset because the column is a naive LocalDateTime.
                LocalDateTime lastPushed = null;
                String timestamp = event.getStr("timestamp");
                if (StringUtils.isNotBlank(timestamp)) {
                    lastPushed = parseIsoToLocalDateTime(timestamp);
                }

                // Check if this is a manifest list/index (multi-arch image)
                boolean isManifestList = mediaType != null
                        && (mediaType.contains("application/vnd.docker.distribution.manifest.list")
                                || mediaType.contains("application/vnd.oci.image.index"));

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

                // Determine if this event is for a blob (layer) or manifest
                boolean isLayer = false;
                if (mediaType != null) {
                    String mt = mediaType.toLowerCase();
                    isLayer = mt.contains("octet-stream")
                            || mt.contains("tar")
                            || mt.contains("gzip")
                            || mt.contains("layer")
                            || mt.contains("rootfs")
                            || mt.contains("container.image");
                }

                if (isLayer) {
                    // Store blob (layer) record
                    Blob blob = blobMapper.findByInstIdAndDigest(instId, digest);
                    if (blob == null || blob.getId() == null) {
                        blob = new Blob();
                        blob.setId(IDUtils.getLongID());
                        blob.setInstId(instId);
                        blob.setDigest(digest);
                        blob.setSize(size);
                        blob.setMediaType(mediaType);
                        blob.setCreated(lastPushed);
                        blob.setStatus("active");
                        blobMapper.insert(blob);
                        log.info("Inserted blob: repo={}, digest={}, size={}", repository, digest, size);
                    } else {
                        blob.setSize(size);
                        if (!"active".equals(blob.getStatus())) {
                            blob.setStatus("active");
                            log.info("Re-activated orphan blob: repo={}, digest={}", repository, digest);
                        }
                        blobMapper.updateById(blob);
                    }
                }

                // Manifest variable for non-layer events
                Manifest manifest = null;
                if (!isLayer) {
                    // Process manifest - create or update manifest record
                    manifest = manifestMapper.findByInstIdAndDigest(instId, digest);
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
                }

                // Process artifact, references, platform info (only for non-layer manifests)
                if (!isLayer) {
                    // Process artifact - create or update artifact record
                    if (StringUtils.isNotBlank(tag)) {
                        Artifact artifactRecord =
                                artifactMapper.findByInstIdAndRepoIdAndTagName(instId, repo.getId(), tag);
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
                            log.info(
                                    "Inserted artifact: repo={}, tag={}, manifestId={}",
                                    repository,
                                    tag,
                                    manifest.getId());
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

                        // Update artifact_count
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

                    // Fetch manifest body to compute compressed_size (sum of layer sizes)
                    if ("push".equals(action) && manifest != null && manifest.getId() != null) {
                        try {
                            fetchAndUpdateCompressedSize(inst, repository, manifest.getDigest(), manifest.getId());
                        } catch (Exception e) {
                            log.warn("Failed to compute compressed_size for {}: {}", digest, e.getMessage());
                        }
                    }
                }

                log.info(
                        "Processed event: action={}, repository={}, digest={}, tag={}, isManifestList={}",
                        action,
                        repository,
                        digest,
                        tag,
                        isManifestList);
            }
        } catch (Exception e) {
            log.error("Failed to process event: {}", json, e);
        }
    }

    /**
     * Parse an ISO 8601 timestamp into a {@link LocalDateTime}. The registry
     * emits values like {@code 2026-02-27T10:40:57.779182297+08:00} or
     * {@code 2026-02-27T10:40:57Z}. We previously hand-rolled offset stripping
     * which was fragile; here we use the standard library instead.
     */
    private static LocalDateTime parseIsoToLocalDateTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return null;
        }
        try {
            // Fast path: parse as OffsetDateTime/ZonedDateTime/Instant first.
            try {
                java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(timestamp);
                return odt.toLocalDateTime();
            } catch (Exception ignore) {
                // fall through
            }
            try {
                java.time.Instant inst = java.time.Instant.parse(timestamp);
                return inst.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            } catch (Exception ignore) {
                // fall through
            }
            // Naive local datetime (no offset suffix).
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}", timestamp, e);
            return null;
        }
    }

    /**
     * Process references for multi-arch manifest lists
     */
    private void processReferences(
            JSONObject target, Long instId, Long repoId, String parentDigest, LocalDateTime lastPushed) {
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
                    created = parseIsoToLocalDateTime(createdStr);
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

            // Compute compressed_size for each child manifest
            try {
                Inst inst = instMapper.selectById(instId);
                Repo repo = repoMapper.selectById(repoId);
                if (inst != null && repo != null) {
                    Long refManifestId = null;
                    if (existingManifest != null && existingManifest.getId() != null) {
                        refManifestId = existingManifest.getId();
                    }
                    if (refManifestId != null) {
                        fetchAndUpdateCompressedSize(inst, repo.getRepoName(), refDigest, refManifestId);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to compute compressed_size for child {}: {}", refDigest, e.getMessage());
            }
        }

        // Sum compressed_size from child manifests in DB
        long totalCompressedSize = 0;
        for (int i = 0; i < references.size(); i++) {
            JSONObject ref = references.getJSONObject(i);
            String refDigest = ref.getStr("digest");
            if (StringUtils.isNotBlank(refDigest)) {
                Manifest child = manifestMapper.findByInstIdAndDigest(instId, refDigest);
                if (child != null && child.getCompressedSize() != null) {
                    totalCompressedSize += child.getCompressedSize();
                }
            }
        }
        if (totalCompressedSize > 0) {
            Manifest parent = manifestMapper.findByInstIdAndDigest(instId, parentDigest);
            if (parent != null && parent.getId() != null) {
                parent.setCompressedSize(totalCompressedSize);
                manifestMapper.updateById(parent, true);
                log.info(
                        "Updated parent manifest list {} compressed_size: {} bytes (from {} children)",
                        parentDigest,
                        totalCompressedSize,
                        references.size());
            }
        }
    }

    /**
     * Hard cap on how much of a single blob we are willing to slurp into
     * memory. Real registry manifests are well under 1 MiB; the cap exists
     * so a malicious webhook cannot trick us into reading gigabytes.
     */
    private static final long MAX_BLOB_READ_BYTES = 4L * 1024 * 1024;

    /**
     * Fetch manifest body from registry and compute compressed_size (sum of config + layers size)
     */
    private void fetchAndUpdateCompressedSize(Inst inst, String repository, String digest, Long manifestId) {
        try {
            // Read manifest body from local registry storage filesystem (avoids auth issues)
            String instName = inst.getName();
            String blobBasePath = StringUtils.joinWith(
                    File.separator,
                    homeDir.getHomeAbsolutePath(),
                    "registry",
                    instName,
                    "data",
                    "docker",
                    "registry",
                    "v2",
                    "blobs",
                    "sha256");
            String digestHex = digest.replace("sha256:", "");
            String blobPath =
                    StringUtils.joinWith(File.separator, blobBasePath, digestHex.substring(0, 2), digestHex, "data");
            File blobFile = new File(blobPath);
            if (!blobFile.exists()) {
                log.warn("Manifest blob not found for {}: {}", digest, blobPath);
                return;
            }
            if (blobFile.length() > MAX_BLOB_READ_BYTES) {
                log.warn("Manifest blob too large to read: {} ({} bytes)", digest, blobFile.length());
                return;
            }
            String body =
                    org.apache.commons.io.FileUtils.readFileToString(blobFile, java.nio.charset.StandardCharsets.UTF_8);
            if (StringUtils.isBlank(body)) {
                return;
            }
            JSONObject manifestJson = JSONUtil.parseObj(body);
            long totalSize = 0;

            // OCI image manifest or Docker v2 manifest: config.size + sum(layers[].size)
            if (manifestJson.containsKey("config") && manifestJson.containsKey("layers")) {
                JSONObject config = manifestJson.getJSONObject("config");
                if (config != null) {
                    Long configSize = config.getLong("size");
                    totalSize += (configSize != null ? configSize : 0);
                }

                // Extract platform from config blob for Docker v2 manifests (distribution doesn't send platform in v2
                // events)
                Manifest existingForPlatform = manifestMapper.selectById(manifestId);
                if (existingForPlatform != null && StringUtils.isBlank(existingForPlatform.getOsArch())) {
                    try {
                        String configDigest = config != null ? config.getStr("digest") : null;
                        if (StringUtils.isNotBlank(configDigest)) {
                            String configHex = configDigest.replace("sha256:", "");
                            String configPath = StringUtils.joinWith(
                                    File.separator, blobBasePath, configHex.substring(0, 2), configHex, "data");
                            File configFile = new File(configPath);
                            if (configFile.exists()) {
                                if (configFile.length() > MAX_BLOB_READ_BYTES) {
                                    log.warn(
                                            "Config blob too large to read: {} ({} bytes)",
                                            configDigest,
                                            configFile.length());
                                } else {
                                    String configBody = org.apache.commons.io.FileUtils.readFileToString(
                                            configFile, java.nio.charset.StandardCharsets.UTF_8);
                                    if (StringUtils.isNotBlank(configBody)) {
                                        JSONObject configJson = JSONUtil.parseObj(configBody);
                                        String cfgOs = configJson.getStr("os");
                                        String cfgArch = configJson.getStr("architecture");
                                        String cfgVariant = configJson.getStr("variant");
                                        if (StringUtils.isNotBlank(cfgArch)) {
                                            existingForPlatform.setOsArch(cfgArch);
                                        }
                                        if (StringUtils.isNotBlank(cfgOs)) {
                                            existingForPlatform.setOs(cfgOs);
                                        }
                                        if (StringUtils.isNotBlank(cfgVariant)) {
                                            existingForPlatform.setVariant(cfgVariant);
                                        }
                                        if (StringUtils.isNotBlank(cfgArch) || StringUtils.isNotBlank(cfgOs)) {
                                            manifestMapper.updateById(existingForPlatform, true);
                                            log.info(
                                                    "Updated platform for manifest {}: {}/{}/{}",
                                                    digest,
                                                    cfgArch,
                                                    cfgOs,
                                                    cfgVariant);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to extract platform for {}: {}", digest, e.getMessage());
                    }
                }

                JSONArray layers = manifestJson.getJSONArray("layers");
                if (layers != null) {
                    for (int i = 0; i < layers.size(); i++) {
                        JSONObject layer = layers.getJSONObject(i);
                        Long layerSize = layer.getLong("size");
                        totalSize += (layerSize != null ? layerSize : 0);

                        // Build manifest_blob association
                        String layerDigest = layer.getStr("digest");
                        if (StringUtils.isNotBlank(layerDigest)) {
                            Blob layerBlob = blobMapper.findByInstIdAndDigest(inst.getId(), layerDigest);
                            if (layerBlob != null && layerBlob.getId() != null) {
                                ManifestBlob mb =
                                        manifestBlobMapper.findByManifestIdAndBlobId(manifestId, layerBlob.getId());
                                if (mb == null || mb.getId() == null) {
                                    mb = new ManifestBlob();
                                    mb.setId(IDUtils.getLongID());
                                    mb.setManifestId(manifestId);
                                    mb.setBlobId(layerBlob.getId());
                                    manifestBlobMapper.insert(mb, true);
                                }
                            }
                        }
                    }
                }
            }
            // OCI image index / Docker manifest list: sum of referenced manifests
            else if (manifestJson.containsKey("manifests")) {
                JSONArray manifests = manifestJson.getJSONArray("manifests");
                if (manifests != null) {
                    for (int i = 0; i < manifests.size(); i++) {
                        JSONObject m = manifests.getJSONObject(i);
                        Long mSize = m.getLong("size");
                        totalSize += (mSize != null ? mSize : 0);
                    }
                }
            }

            if (totalSize > 0) {
                Manifest existing = manifestMapper.selectById(manifestId);
                if (existing != null) {
                    existing.setCompressedSize(totalSize);
                    manifestMapper.updateById(existing, true);
                    log.info("Updated compressed_size for manifest {}: {} bytes", digest, totalSize);
                }
            }
        } catch (Exception e) {
            log.warn("Error computing compressed_size for {}: {}", digest, e.getMessage());
        }
    }
}
