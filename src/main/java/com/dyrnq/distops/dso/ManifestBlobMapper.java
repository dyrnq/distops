package com.dyrnq.distops.dso;

import com.dyrnq.distops.model.ManifestBlob;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.wood.DbContext;

@Component
public class ManifestBlobMapper {
    @Inject
    DbContext dbContext;

    public void insert(ManifestBlob mb, boolean replace) {
        try {
            dbContext.table(ManifestBlob.TABLE_NAME).setEntity(mb).insert();
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
