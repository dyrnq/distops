package com.dyrnq.distops.controller.api;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dyrnq.distops.controller.ApiController;
import com.dyrnq.distops.controller.PageResult;
import com.dyrnq.distops.dso.AccountMapper;
import com.dyrnq.distops.dso.InstMapper;
import com.dyrnq.distops.model.Account;
import com.dyrnq.distops.model.Inst;
import com.dyrnq.distops.service.InstService;
import com.dyrnq.distops.service.dto.AccountQuery;
import com.dyrnq.utils.BcryptUtils;
import com.dyrnq.utils.IDUtils;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Result;
import org.noear.solon.validation.annotation.Numeric;
import org.noear.wood.MapperWhereQ;
import org.noear.wood.ext.Act1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Account Management Controller
 * Provides REST API for managing Docker Registry user accounts
 */
@Mapping("api/account")
@Controller
@Slf4j
public class AccountController extends ApiController {

    @Inject
    AccountMapper accountMapper;

    @Inject
    InstMapper instMapper;

    @Inject
    InstService instService;

    /**
     * Query accounts with pagination
     */
    @Mapping("")
    public PageResult query(Context ctx, int page, int limit, AccountQuery query) {
        try {
            int start = PageUtil.getStart(page - 1, limit);
            StringBuilder sql = new StringBuilder("select a.*, b.name as inst_name from account as a, inst as b where a.inst_id = b.id");
            StringBuilder countSql = new StringBuilder("select count(*) from account as a, inst as b where a.inst_id = b.id");

            if (StrUtil.isNotBlank(query.getInstName())) {
                String like = " and b.name like '%" + query.getInstName() + "%'";
                sql.append(like);
                countSql.append(like);
            }
            sql.append(" LIMIT ?,?");
            List<Account> list = instMapper.db().sql(sql.toString(), start, limit).getList(Account.class);
            long count = instMapper.db().sql(countSql.toString()).getCount();
            return PageResult.succeed(list, count);
        } catch (Exception e) {
            log.error("Failed to query accounts", e);
            return PageResult.failure(e.getMessage());
        }
    }

    /**
     * Get account by ID
     */
    @Mapping("get")
    public Result get(Context ctx, long id) {
        try {
            Account account = accountMapper.selectById(id);
            if (account == null) {
                return Result.failure("Account not found");
            }
            // Don't return password hash
            account.setPassword(null);
            return Result.succeed(account);
        } catch (Exception e) {
            log.error("Failed to get account", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Add new account
     */
    @Mapping("add")
    @Numeric("id")
    public Result add(Context ctx, Account account) {
        try {
            if (StrUtil.isBlank(account.getUsername())) {
                return Result.failure("Username is required");
            }

            String hashPw = BcryptUtils.hashPw(account.getPassword());
            // Hash password
            account.setHashpw(hashPw);

            // Set default values
            Long id = IDUtils.getLongID();
            if (ObjectUtil.isNull(account.getId())) {
                account.setId(id);
            }
            if (ObjectUtil.isNull(account.getEnabled())) {
                account.setEnabled(1);
            }

            accountMapper.insert(account, true);

            // Update htpasswd file
            if (account.getInstId() != null) {
                Inst inst = instMapper.selectById(account.getInstId());
                if (inst != null) {
                    instService.writeHtpasswd(inst);
                }
            }

            log.info("Added account: {}", account.getUsername());
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to add account", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Update account
     */
    @Mapping("update")
    public Result update(Context ctx, Account account) {
        try {
            Account existing = accountMapper.selectById(account.getId());
            if (existing == null) {
                return Result.failure("Account not found");
            }

            // Update password if provided
            if (StrUtil.isNotBlank(account.getPassword())) {
                account.setHashpw(BcryptUtils.hashPw(account.getPassword()));
            } else {
                // Keep existing password hash
                account.setHashpw(null);
            }

            accountMapper.updateById(account, true);

            // Update htpasswd file
            if (existing.getInstId() != null) {
                Inst inst = instMapper.selectById(existing.getInstId());
                if (inst != null) {
                    instService.writeHtpasswd(inst);
                }
            }

            log.info("Updated account: {}", existing.getUsername());
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to update account", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Delete account(s)
     */
    @Mapping("del")
    public Result del(Context ctx, long... id) {
        try {
            for (long i : id) {
                Account account = accountMapper.selectById(i);
                if (account != null) {
                    Long instId = account.getInstId();
                    accountMapper.deleteById(i);
                    log.info("Deleted account: {}", account.getUsername());

                    // Update htpasswd file
                    if (instId != null) {
                        Inst inst = instMapper.selectById(instId);
                        if (inst != null) {
                            instService.writeHtpasswd(inst);
                        }
                    }
                }
            }
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to delete account", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Enable account
     */
    @Mapping("enable")
    public Result enable(Context ctx, long id) {
        try {
            Account account = accountMapper.selectById(id);
            if (account == null) {
                return Result.failure("Account not found");
            }

            account.setEnabled(1);
            accountMapper.updateById(account, true);

            // Update htpasswd file
            if (account.getInstId() != null) {
                Inst inst = instMapper.selectById(account.getInstId());
                if (inst != null) {
                    instService.writeHtpasswd(inst);
                }
            }

            log.info("Enabled account: {}", account.getUsername());
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to enable account", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Disable account
     */
    @Mapping("disable")
    public Result disable(Context ctx, long id) {
        try {
            Account account = accountMapper.selectById(id);
            if (account == null) {
                return Result.failure("Account not found");
            }

            account.setEnabled(0);
            accountMapper.updateById(account, true);

            // Update htpasswd file
            if (account.getInstId() != null) {
                Inst inst = instMapper.selectById(account.getInstId());
                if (inst != null) {
                    instService.writeHtpasswd(inst);
                }
            }

            log.info("Disabled account: {}", account.getUsername());
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to disable account", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Get account ACL
     */
    @Mapping("acl/get")
    public Result getAcl(Context ctx, long id) {
        try {
            Account account = accountMapper.selectById(id);
            if (account == null) {
                return Result.failure("Account not found");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", account.getId());
            result.put("username", account.getUsername());

            try{
                //pretty format JSON
                String acl = JSONUtil.toJsonPrettyStr(account.getAcl());
                result.put("acl", acl);
            } catch (Exception ignore) {
                result.put("acl", account.getAcl());
            }
            return Result.succeed(result);
        } catch (Exception e) {
            log.error("Failed to get account ACL", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Update account ACL
     */
    @Mapping("acl/update")
    public Result updateAcl(Context ctx, long id, String acl) {
        try {
            Account account = accountMapper.selectById(id);
            if (account == null) {
                return Result.failure("Account not found");
            }
            
            // Validate ACL JSON if provided
            if (acl != null && !acl.trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    mapper.readTree(acl); // Validate JSON
                } catch (Exception e) {
                    return Result.failure("Invalid ACL JSON format: " + e.getMessage());
                }
            }
            
            account.setAcl(acl);
            accountMapper.updateById(account, true);
            
            log.info("Updated ACL for account: {}", account.getUsername());
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to update account ACL", e);
            return Result.failure(e.getMessage());
        }
    }
}
