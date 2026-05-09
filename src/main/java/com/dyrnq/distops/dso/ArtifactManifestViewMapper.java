package com.dyrnq.distops.dso;

import org.noear.wood.DbContext;
import org.noear.wood.mapper.BaseMapperWrap;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import com.dyrnq.distops.model.ArtifactManifestView;
import com.dyrnq.distops.model.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Component
@Slf4j
public class ArtifactManifestViewMapper extends BaseMapperWrap<ArtifactManifestView> {

    public ArtifactManifestViewMapper() {
        super(null, ArtifactManifestView.class, ArtifactManifestView.TABLE_NAME);
    }

@Inject
DbContext dbContext;
public DbContext db() {
  return this.dbContext;
}



//Customize BEGIN

//Customize END
}
