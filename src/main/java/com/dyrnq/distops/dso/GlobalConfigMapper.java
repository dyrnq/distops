package com.dyrnq.distops.dso;

import com.dyrnq.distops.model.*;
import com.dyrnq.distops.model.GlobalConfig;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.wood.DbContext;
import org.noear.wood.mapper.BaseMapperWrap;

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

    // Customize BEGIN
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
    // Customize END
}
