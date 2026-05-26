package com.dyrnq.distops.dso;

import com.dyrnq.distops.model.Blob;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.wood.DbContext;

@Component
public class BlobMapper {
    @Inject
    DbContext dbContext;

    public Blob findByInstIdAndDigest(Long instId, String digest) {
        try {
            return dbContext.table(Blob.TABLE_NAME)
                    .whereEq(Blob.INST_ID, instId)
                    .andEq(Blob.DIGEST, digest)
                    .selectItem("*", Blob.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void insert(Blob blob, boolean replace) {
        try {
            dbContext.table(Blob.TABLE_NAME).setEntity(blob).insert();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateById(Blob blob, boolean replace) {
        try {
            dbContext.table(Blob.TABLE_NAME).setEntity(blob).whereEq(Blob.ID, blob.getId()).update();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
