package com.dyrnq.distops.service;

import com.dyrnq.distops.HomeDir;
import com.dyrnq.distops.dso.BlobMapper;
import com.dyrnq.distops.dso.InstMapper;
import com.dyrnq.distops.dso.ManifestBlobMapper;
import com.dyrnq.distops.dso.RepoMapper;
import com.dyrnq.distops.model.Blob;
import com.dyrnq.distops.model.Inst;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

@Component
@Slf4j
public class GcService {

    @Inject
    HomeDir homeDir;

    @Inject
    BlobMapper blobMapper;

    @Inject
    ManifestBlobMapper manifestBlobMapper;

    @Inject
    InstMapper instMapper;

    @Inject
    RepoMapper repoMapper;

    private static final int PAGE_SIZE = 1000;

    public static class GcResult {
        public long orphanSize;
        public int orphanBlobs;
        public int activeBlobs;
        public String error;
    }

    /**
     * Mark blobs: referenced → active, not referenced → orphan. Files untouched.
     */
    public GcResult mark(Long instId) {
        GcResult result = new GcResult();
        List<Inst> insts = resolveInsts(instId);
        if (insts == null) {
            result.error = "Inst not found: " + instId;
            return result;
        }

        Set<Long> referenced = new HashSet<>(manifestBlobMapper.findAllReferencedBlobIds());

        for (Inst inst : insts) {
            long lastId = 0;
            while (true) {
                List<Blob> page = blobMapper.listByInstIdPaged(inst.getId(), lastId, PAGE_SIZE);
                if (page.isEmpty()) break;

                for (Blob blob : page) {
                    String newStatus = referenced.contains(blob.getId()) ? "active" : "orphan";
                    if (!newStatus.equals(blob.getStatus())) {
                        blob.setStatus(newStatus);
                        blobMapper.updateById(blob);
                    }
                    if ("orphan".equals(newStatus)) {
                        result.orphanBlobs++;
                        result.orphanSize += (blob.getSize() != null ? blob.getSize() : 0);
                    } else {
                        result.activeBlobs++;
                    }
                }
                lastId = page.get(page.size() - 1).getId();
            }
        }

        log.info(
                "GC mark: {} active, {} orphan ({})",
                result.activeBlobs,
                result.orphanBlobs,
                formatSize(result.orphanSize));
        return result;
    }

    /**
     * Purge orphan blobs: delete files + DB records.
     */
    public GcResult purge(Long instId) {
        GcResult result = new GcResult();
        List<Inst> insts = resolveInsts(instId);
        if (insts == null) {
            result.error = "Inst not found: " + instId;
            return result;
        }

        for (Inst inst : insts) {
            long lastId = 0;
            while (true) {
                List<Blob> page = blobMapper.listByInstIdPaged(inst.getId(), lastId, PAGE_SIZE);
                if (page.isEmpty()) break;
                List<Blob> orphans = page.stream()
                        .filter(b -> "orphan".equals(b.getStatus()))
                        .collect(Collectors.toList());

                for (Blob b : orphans) {
                    try {
                        if (deleteBlobFile(inst, b.getDigest())) {
                            deleteAllManifestRevisions(inst, b.getDigest());
                            blobMapper.deleteById(b.getId());
                            result.orphanBlobs++;
                            result.orphanSize += (b.getSize() != null ? b.getSize() : 0);
                        }
                    } catch (Exception e) {
                        log.warn("GC purge: failed for {}: {}", b.getDigest(), e.getMessage());
                    }
                }
                lastId = page.get(page.size() - 1).getId();
            }
        }

        log.info("GC purge done: {} blobs, {} freed", result.orphanBlobs, formatSize(result.orphanSize));
        return result;
    }

    // ──── Helpers ────

    private List<Inst> resolveInsts(Long instId) {
        if (instId != null) {
            Inst inst = instMapper.selectById(instId);
            if (inst == null) return null;
            return Collections.singletonList(inst);
        }
        return instMapper.selectList(c -> {});
    }

    private boolean deleteBlobFile(Inst inst, String digest) {
        String hex = digest.replace("sha256:", "");
        if (hex.length() < 4) return false;
        String path = StringUtils.joinWith(
                File.separator,
                homeDir.getHomeAbsolutePath(),
                "registry",
                inst.getName(),
                "data",
                "docker",
                "registry",
                "v2",
                "blobs",
                "sha256",
                hex.substring(0, 2),
                hex,
                "data");
        File f = new File(path);
        if (!f.exists()) return false;
        boolean ok = FileUtils.deleteQuietly(f);
        if (ok) cleanEmptyParents(f);
        return ok;
    }

    private void cleanEmptyParents(File file) {
        File parent = file.getParentFile();
        if (parent != null && parent.isDirectory() && isEmptyDir(parent)) {
            FileUtils.deleteQuietly(parent);
            File grand = parent.getParentFile();
            if (grand != null && grand.isDirectory() && isEmptyDir(grand)) {
                FileUtils.deleteQuietly(grand);
            }
        }
    }

    private boolean isEmptyDir(File dir) {
        String[] list = dir.list();
        return list == null || list.length == 0;
    }

    private void deleteAllManifestRevisions(Inst inst, String digest) {
        String reposPath = StringUtils.joinWith(
                File.separator,
                homeDir.getHomeAbsolutePath(),
                "registry",
                inst.getName(),
                "data",
                "docker",
                "registry",
                "v2",
                "repositories");
        File reposDir = new File(reposPath);
        if (!reposDir.exists()) return;
        File[] repoDirs = reposDir.listFiles();
        if (repoDirs == null) return;
        String hex = digest.replace("sha256:", "");
        for (File repoDir : repoDirs) {
            if (repoDir.getName().startsWith("_")) continue;
            File rev = new File(new File(new File(new File(repoDir, "_manifests"), "revisions"), "sha256"), hex);
            if (rev.exists()) FileUtils.deleteQuietly(rev);
            File tagsDir = new File(repoDir, "_manifests/tags");
            if (tagsDir.exists()) {
                File[] tagDirs = tagsDir.listFiles();
                if (tagDirs != null) {
                    for (File td : tagDirs) {
                        for (String link : new String[] {"current", "index"}) {
                            File lf = new File(td, link);
                            if (lf.exists()) {
                                try {
                                    String target = FileUtils.readFileToString(
                                                    lf, java.nio.charset.StandardCharsets.UTF_8)
                                            .trim();
                                    if (target.contains(hex)) {
                                        FileUtils.deleteQuietly(td);
                                        break;
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double v = bytes / 1024.0;
        if (v < 1024) return String.format("%.1f KB", v);
        v /= 1024;
        if (v < 1024) return String.format("%.1f MB", v);
        v /= 1024;
        return String.format("%.1f GB", v);
    }
}
