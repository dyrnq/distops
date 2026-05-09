package com.dyrnq.distops.dso;

import org.noear.wood.DbContext;
import org.noear.wood.mapper.BaseMapperWrap;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import com.dyrnq.distops.model.GlobalConfig;
import com.dyrnq.distops.model.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Component
@Slf4j
public class GlobalConfigMapper extends BaseMapperWrap<GlobalConfig> {

    public GlobalConfigMapper() {
        super(null, GlobalConfig.class, GlobalConfig.TABLE_NAME);
    }

@Inject
DbContext dbContext;
public DbContext db() {
  return this.dbContext;
}



//Customize BEGIN
public GlobalConfig findByName(String name) {
        return this.selectItem(c -> {
            c.whereEq(GlobalConfig.NAME, name);
        });
    }

    public boolean existsByName(String name) {
        Long config = this.selectCount(c -> {
            c.whereEq(GlobalConfig.NAME, name);
        });
        return config != null && config > 0;
    }
//Customize END
}
