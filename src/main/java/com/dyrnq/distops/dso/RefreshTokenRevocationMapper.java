package com.dyrnq.distops.dso;

import com.dyrnq.distops.model.RefreshTokenRevocation;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.wood.DbContext;
import org.noear.wood.mapper.BaseMapperWrap;

@Component
@Slf4j
public class RefreshTokenRevocationMapper extends BaseMapperWrap<RefreshTokenRevocation> {

    public RefreshTokenRevocationMapper() {
        super(null, RefreshTokenRevocation.class, RefreshTokenRevocation.TABLE_NAME);
    }

    @Inject
    DbContext dbContext;

    public DbContext db() {
        return this.dbContext;
    }

    public boolean existsByUsernameAndInstId(String username, Long instId) {
        try {
            return db().table(RefreshTokenRevocation.TABLE_NAME)
                    .whereEq("username", username)
                    .andEq("inst_id", instId)
                    .selectExists();
        } catch (Exception e) {
            log.warn("Failed to check revocation for user {}: {}", username, e.getMessage());
            return false;
        }
    }

    public boolean existsByJtiAndInstId(String jti, Long instId) {
        try {
            return db().table(RefreshTokenRevocation.TABLE_NAME)
                    .whereEq("jti", jti)
                    .andEq("inst_id", instId)
                    .selectExists();
        } catch (Exception e) {
            log.warn("Failed to check JTI revocation for {}: {}", jti, e.getMessage());
            return false;
        }
    }
}
