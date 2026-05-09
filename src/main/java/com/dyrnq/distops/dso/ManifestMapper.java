package com.dyrnq.distops.dso;

import org.noear.wood.DbContext;
import org.noear.wood.mapper.BaseMapperWrap;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import com.dyrnq.distops.model.Manifest;
import com.dyrnq.distops.model.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Component
@Slf4j
public class ManifestMapper extends BaseMapperWrap<Manifest> {

    public ManifestMapper() {
        super(null, Manifest.class, Manifest.TABLE_NAME);
    }

@Inject
DbContext dbContext;
public DbContext db() {
  return this.dbContext;
}



//Customize BEGIN
/**
 * Find manifest by inst_id and digest
 */
public Manifest findByInstIdAndDigest(Long instId, String digest) {
    return this.selectItem(c -> {
        c.whereEq(Manifest.INST_ID, instId).andEq(Manifest.DIGEST, digest);
    });
}

/**
 * Find child manifests by parent_digest
 */
public List<Manifest> findByParentDigest(Long instId, String parentDigest) {
    return this.selectList(c -> {
        c.whereEq(Manifest.INST_ID, instId).andEq(Manifest.PARENT_DIGEST, parentDigest);
    });
}

/**
 * Find or create manifest for reference
 */
public Manifest findOrCreateReference(Long instId, Long repoId, String digest, String parentDigest) {
    Manifest manifest = findByInstIdAndDigest(instId, digest);
    if (manifest == null || manifest.getId() == null) {
        manifest = new Manifest();
        manifest.setId(com.dyrnq.utils.IDUtils.getLongID());
        manifest.setInstId(instId);
        manifest.setRepoId(repoId);
        manifest.setDigest(digest);
        manifest.setParentDigest(parentDigest);
        manifest.setPushCount(1);
        insert(manifest, true);
    }
    return manifest;
}
//Customize END
}
