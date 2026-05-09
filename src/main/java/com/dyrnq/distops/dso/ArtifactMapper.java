package com.dyrnq.distops.dso;

import org.noear.wood.DbContext;
import org.noear.wood.mapper.BaseMapperWrap;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import com.dyrnq.distops.model.Artifact;
import com.dyrnq.distops.model.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Component
@Slf4j
public class ArtifactMapper extends BaseMapperWrap<Artifact> {

    public ArtifactMapper() {
        super(null, Artifact.class, Artifact.TABLE_NAME);
    }

@Inject
DbContext dbContext;
public DbContext db() {
  return this.dbContext;
}



//Customize BEGIN
/**
 * Find artifact by inst_id, repo_id and tag_name
 */
public Artifact findByInstIdAndRepoIdAndTagName(Long instId, Long repoId, String tagName) {
    return this.selectItem(c -> {
        c.whereEq(Artifact.INST_ID, instId)
            .andEq(Artifact.REPO_ID, repoId)
            .andEq(Artifact.TAG_NAME, tagName);
    });
}

/**
 * Find artifacts by repo_id
 */
public List<Artifact> findByRepoId(Long repoId) {
    return this.selectList(c -> {
        c.whereEq(Artifact.REPO_ID, repoId);
    });
}

/**
 * Count distinct artifacts by repo_id
 */
public Integer countDistinctArtifactsByRepoId(Long repoId) {
    try {
        long count = db().sql("SELECT COUNT(DISTINCT tag_name) FROM artifact WHERE repo_id = ? AND tag_name IS NOT NULL", repoId)
            .getCount();
        return (int) count;
    } catch (Exception e) {
        log.warn("Failed to count distinct artifacts", e);
        return 0;
    }
}
//Customize END
}
