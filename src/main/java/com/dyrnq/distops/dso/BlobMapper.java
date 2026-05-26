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

    public java.util.List<Blob> listByInstId(Long instId) {
        try {
            return dbContext.table(Blob.TABLE_NAME)
                    .whereEq(Blob.INST_ID, instId)
                    .selectList("*", Blob.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Paginated listing for large datasets.
     * @param lastId last id from previous page, or 0 for first page
     */
    public java.util.List<Blob> listByInstIdPaged(Long instId, long lastId, int pageSize) {
        try {
            return dbContext.table(Blob.TABLE_NAME)
                    .whereEq(Blob.INST_ID, instId)
                    .andGt(Blob.ID, lastId)
                    .orderBy(Blob.ID + " ASC")
                    .limit(pageSize)
                    .selectList("*", Blob.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public java.util.List<Blob> listAll() {
        try {
            return dbContext.table(Blob.TABLE_NAME)
                    .selectList("*", Blob.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Blob selectById(Long id) {
        try {
            return dbContext.table(Blob.TABLE_NAME)
                    .whereEq(Blob.ID, id)
                    .selectItem("*", Blob.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteById(Long id) {
        try {
            return dbContext.table(Blob.TABLE_NAME)
                    .whereEq(Blob.ID, id)
                    .delete() > 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
