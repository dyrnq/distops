package com.dyrnq.distops.model;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import org.noear.wood.annotation.Column;
import org.noear.wood.annotation.PrimaryKey;
import org.noear.wood.annotation.Table;

/** JTI blacklist entry for revoked refresh tokens. */
@Table("refresh_token_revocation")
@Data
public class RefreshTokenRevocation implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "refresh_token_revocation";

    @PrimaryKey
    @Column("jti")
    public String jti;

    @PrimaryKey
    @Column("inst_id")
    public Long instId;

    @Column("username")
    public String username;

    @Column("revoked_at")
    public Date revokedAt;

    @Column("expires_at")
    public Date expiresAt;
}
