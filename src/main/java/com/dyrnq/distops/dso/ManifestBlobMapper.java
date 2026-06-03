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

    public ManifestBlob findByManifestIdAndBlobId(Long manifestId, Long blobId) {
        try {
            return dbContext
                    .table(ManifestBlob.TABLE_NAME)
                    .whereEq("manifest_id", manifestId)
                    .andEq("blob_id", blobId)
                    .selectItem("*", ManifestBlob.class);
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public java.util.List<Long> findBlobIdsByManifestId(Long manifestId) {
        try {
            return dbContext
                    .table(ManifestBlob.TABLE_NAME)
                    .whereEq("manifest_id", manifestId)
                    .selectList("*", ManifestBlob.class)
                    .stream()
                    .map(mb -> mb.getBlobId())
                    .collect(java.util.stream.Collectors.toList());
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public java.util.List<Long> findAllReferencedBlobIds() {
        try {
            return dbContext.table(ManifestBlob.TABLE_NAME).selectList("*", ManifestBlob.class).stream()
                    .map(mb -> mb.getBlobId())
                    .collect(java.util.stream.Collectors.toList());
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteByManifestId(Long manifestId) {
        try {
            return dbContext
                            .table(ManifestBlob.TABLE_NAME)
                            .whereEq("manifest_id", manifestId)
                            .delete()
                    > 0;
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
