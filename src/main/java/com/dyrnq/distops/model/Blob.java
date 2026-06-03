package com.dyrnq.distops.model;

import java.io.Serializable;
import lombok.Data;
import org.noear.wood.annotation.Column;
import org.noear.wood.annotation.PrimaryKey;
import org.noear.wood.annotation.Table;

@Table("blob")
@Data
public class Blob implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "blob";

    @PrimaryKey
    @Column("id")
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(
            using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    public Long id;

    @Column("inst_id")
    public Long instId;

    @Column("digest")
    public String digest;

    @Column("size")
    public Long size;

    @Column("media_type")
    public String mediaType;

    @Column("created")
    public java.time.LocalDateTime created;

    @Column("status")
    public String status;

    public static final String ID = "id";
    public static final String INST_ID = "inst_id";
    public static final String DIGEST = "digest";
    public static final String SIZE = "size";
    public static final String MEDIA_TYPE = "media_type";
    public static final String CREATED = "created";
    public static final String STATUS = "status";
}
