package com.dyrnq.distops.dso;

import org.noear.wood.DbContext;
import org.noear.wood.mapper.BaseMapperWrap;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import com.dyrnq.distops.model.Account;
import com.dyrnq.distops.model.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Component
@Slf4j
public class AccountMapper extends BaseMapperWrap<Account> {

    public AccountMapper() {
        super(null, Account.class, Account.TABLE_NAME);
    }

@Inject
DbContext dbContext;
public DbContext db() {
  return this.dbContext;
}



//Customize BEGIN
/**
 * 根据用户名查询账户（默认查询启用的，instId=1）
 */

/**
 * 根据用户名、实例 ID 和启用状态查询账户
 */
public Account selectByInstIdAndUsernameAndEnabled(Long instId, String username, int enabled) {
    return this.selectItem(c->{
        c.whereEq(Account.USERNAME, username)
                .andEq(Account.INST_ID,instId)
                .andEq(Account.ENABLED, enabled);
    });
}







/**
 * 根据实例 ID 查询启用的账户
 */
public List<Account> selectByInstIdAndEnabled(Long instId, int enabled) {
    return this.selectList(c->{
                c.whereEq(Account.INST_ID,instId)
                .andEq(Account.ENABLED, enabled);
    });
}
//Customize END
}
