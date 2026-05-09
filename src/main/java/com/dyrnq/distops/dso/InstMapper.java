package com.dyrnq.distops.dso;

import org.noear.wood.DbContext;
import org.noear.wood.mapper.BaseMapperWrap;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import com.dyrnq.distops.model.Inst;
import com.dyrnq.distops.model.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Component
@Slf4j
public class InstMapper extends BaseMapperWrap<Inst> {

    public InstMapper() {
        super(null, Inst.class, Inst.TABLE_NAME);
    }

@Inject
DbContext dbContext;
public DbContext db() {
  return this.dbContext;
}



//Customize BEGIN
public Inst findByName(String name) {
        return this.selectItem(c -> {
            c.whereEq(Inst.NAME, name);
        });
    }

    public List<Inst> findByIdList(List<Long> idList) {
        return this.selectList(c -> {
            c.whereIn(Inst.ID, idList);
        });
    }
    public void updatePid(Long id, Long pid){
        if(pid!=null){
            try {
                db().table(Inst.TABLE_NAME).set(Inst.PID, pid).whereTrue().and().beginEq(Inst.ID, id).end().update();
            } catch (java.sql.SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void updateEnabled(Long id, int en){
        try {
            db().table(Inst.TABLE_NAME).set(Inst.ENABLED, en).whereTrue().and().beginEq(Inst.ID, id).end().update();
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }

    }
    
    /**
     * Update instance key pair information
     */
    public void updateKeyPair(Inst inst){
        try {
            db().table(Inst.TABLE_NAME)
                .set(Inst.AUTH_PRIVATE_KEY, inst.getAuthPrivateKey())
                .set(Inst.AUTH_PUBLIC_KEY, inst.getAuthPublicKey())
                .set(Inst.AUTH_JWKS_JSON, inst.getAuthJwksJson())
                .set(Inst.AUTH_KEY_TYPE, inst.getAuthKeyType())
                .set(Inst.AUTH_KEY_ALG, inst.getAuthKeyAlg())
                .whereTrue().and().beginEq(Inst.ID, inst.getId()).end().update();
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }
//Customize END
}
