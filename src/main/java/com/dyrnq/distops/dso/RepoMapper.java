package com.dyrnq.distops.dso;

import org.noear.wood.DbContext;
import org.noear.wood.mapper.BaseMapperWrap;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import com.dyrnq.distops.model.Repo;
import com.dyrnq.distops.model.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Component
@Slf4j
public class RepoMapper extends BaseMapperWrap<Repo> {

    public RepoMapper() {
        super(null, Repo.class, Repo.TABLE_NAME);
    }

@Inject
DbContext dbContext;
public DbContext db() {
  return this.dbContext;
}



//Customize BEGIN
/**
 * Find repo by inst_id and repo_name
 */
public Repo findByInstIdAndRepoName(Long instId, String repoName) {
    return this.selectItem(c -> {
        c.whereEq(Repo.INST_ID, instId).andEq(Repo.REPO_NAME, repoName);
    });
}
//Customize END
}
