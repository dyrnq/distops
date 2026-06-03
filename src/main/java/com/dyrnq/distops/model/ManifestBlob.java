package com.dyrnq.distops.model;

import java.io.Serializable;
import lombok.Data;
import org.noear.wood.annotation.Column;
import org.noear.wood.annotation.PrimaryKey;
import org.noear.wood.annotation.Table;

@Table("manifest_blob")
@Data
public class ManifestBlob implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "manifest_blob";

    @PrimaryKey
    @Column("id")
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(
            using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    public Long id;

    @Column("manifest_id")
    public Long manifestId;

    @Column("blob_id")
    public Long blobId;

    public static final String ID = "id";
    public static final String MANIFEST_ID = "manifest_id";
    public static final String BLOB_ID = "blob_id";
}
