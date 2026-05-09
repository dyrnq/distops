package com.dyrnq.distops.controller.api;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import com.dyrnq.distops.controller.ApiController;
import com.dyrnq.distops.controller.PageResult;
import com.dyrnq.distops.dso.InstMapper;
import com.dyrnq.distops.model.Inst;
import com.dyrnq.distops.service.InstService;
import com.dyrnq.distops.service.dto.ConfigVo;
import com.dyrnq.distops.service.dto.InstQuery;
import com.dyrnq.utils.IDUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Result;
import org.noear.solon.validation.annotation.Numeric;
import org.noear.solon.validation.annotation.Valid;
import org.noear.wood.IPage;
import org.noear.wood.MapperWhereQ;
import org.noear.wood.ext.Act1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapping("api/inst")
@Controller
@Valid
@Slf4j
public class InstController extends ApiController {

    @Inject
    InstMapper instMapper;

    @Inject
    InstService instService;

    @Mapping("")
    public PageResult query(Context ctx, int page, int limit, InstQuery query) {
        try {

            Act1<MapperWhereQ> condition = mapperWhereQ -> {
                mapperWhereQ.whereTrue();
                if (StrUtil.isNotBlank(query.getInstName())) {
                    mapperWhereQ.and().beginLk("name", "%" + query.getInstName() + "%").end();
                }
            };

            int start = PageUtil.getStart(page - 1, limit);
            IPage<Inst> p = instMapper.selectPage(start, limit, condition);
            return PageResult.succeed(p.getList(), p.getTotal());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return PageResult.failure(e.getMessage());
        }
    }

    @Mapping("add")
    @Numeric("id")
    public Result add(Context ctx, Inst inst) {
        try {

            boolean exists = instMapper.exists(c -> {
                c.whereEq(Inst.NAME, inst.getName());
            });
            if (exists) {
                return Result.failure(String.format("%s已存在!请不要重复增加!", inst.getName()));
            }

            exists = instMapper.exists(c -> {
                c.whereEq(Inst.PORT, inst.getPort());
            });
            if (exists) {
                return Result.failure(String.format("端口%s已存在!请不要重复增加!", inst.getPort()));
            }

            Long id = IDUtils.getLongID();
            if (ObjectUtil.isNull(inst.getId())) {
                inst.setId(id);
            }
            if (Strings.CI.equals("none", inst.getAuth())) {
                inst.setAuth(null);
            }
            instMapper.insert(inst, true);
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.failure(e.getMessage());
        }
    }

    @Mapping("del")
    public Result del(Context ctx, long... id) {
        try {
            for (long i : id) {
                instMapper.deleteById(i);
            }
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.failure(e.getMessage());
        }
    }


    @Mapping("stop")
    public Result stop(Context ctx, long... id) {
        try {
            List<Long> idList = new ArrayList<>();
            for (long i : id) {
                idList.add(i);
            }
            List<Inst> selectedInst = instMapper.findByIdList(idList);
            for (Inst inst : selectedInst) {
                try {
                    String svcName = "registry-" + inst.getName();
                    RuntimeUtil.exec("supervisorctl stop " + svcName);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.failure(e.getMessage());
        }
    }

    @Mapping("start")
    public Result start(Context ctx, long... id) {
        try {
            List<Long> idList = new ArrayList<>();
            for (long i : id) {
                idList.add(i);
            }
            List<Inst> selectedInst = instMapper.findByIdList(idList);
            for (Inst inst : selectedInst) {
                try {
                    String svcName = "registry-" + inst.getName();
                    RuntimeUtil.exec("supervisorctl start " + svcName);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.failure(e.getMessage());
        }
    }

    @Mapping("get")
    public Result get(Context ctx, long id) {
        try {
            Inst inst = instMapper.selectById(id);
            return Result.succeed(inst);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.failure(e.getMessage());
        }
    }

    @Mapping("update")
    public Result update(Context ctx, Inst inst) {
        try {

            boolean exists = instMapper.exists(c -> {
                c.whereEq(Inst.NAME, inst.getName()).andNeq(Inst.ID, inst.getId());
            });
            if (exists) {
                return Result.failure(String.format("%s已存在!请不要重复增加!", inst.getName()));
            }

            exists = instMapper.exists(c -> {
                c.whereEq(Inst.PORT, inst.getPort()).andNeq(Inst.ID, inst.getId());
            });
            if (exists) {
                return Result.failure(String.format("端口%s已存在!请不要重复增加!", inst.getPort()));
            }

            instMapper.updateById(inst, true);

            if (inst.getAuth() == null || Strings.CI.equals("none", inst.getAuth())) {
                instMapper.db().table("inst").usingNull(true)
                        .set("auth", null)
                        .whereTrue().and().beginEq("id", inst.getId()).end().update();
            }

            return Result.succeed("ok");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.failure(e.getMessage());
        }
    }


    @Mapping("/config")
    public Result getConfig(Context ctx, long id) {
        try {
            Map<String, ConfigVo> map = this.instService.getInstConfig(instMapper.selectById(id));
            return Result.succeed(map);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.failure(e.getMessage());
        }
    }

    @Mapping("/restart")
    public Result restart(Context ctx, long... id) {
        try {
            List<Long> idList = new ArrayList<>();
            for (long i : id) {
                idList.add(i);
            }
            List<Inst> selectedInst = instMapper.findByIdList(idList);
            for (Inst inst : selectedInst) {
                try {
                    instService.enable(inst);
                    String svcName = "registry-" + inst.getName();
                    RuntimeUtil.exec("supervisorctl restart " + svcName);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }

            return Result.succeed("ok");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.failure(e.getMessage());
        }
    }

    @Mapping("/disable")
    public Result disable(Context ctx, long... id) {
        try {
            List<Long> list = CollectionUtil.newArrayList();
            for (long i : id) {
                list.add(i);
            }
            List<Inst> selectedInst = instMapper.findByIdList(list);
            for (Inst inst : selectedInst) {
                try {
                    instService.disable(inst);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Run registry garbage collection to clean up orphaned blobs.
     */
    @Mapping("/gc")
    public Result gc(Context ctx, long... id) {
        try {
            List<Long> list = CollectionUtil.newArrayList();
            for (long i : id) {
                list.add(i);
            }
            List<Inst> selectedInst = instMapper.findByIdList(list);
            StringBuilder result = new StringBuilder();
            for (Inst inst : selectedInst) {
                try {
                    instService.runGarbageCollection(inst);
                    result.append(inst.getName()).append(" GC completed. ");
                } catch (Exception e) {
                    log.error("GC failed for instance {}", inst.getName(), e);
                    result.append(inst.getName()).append(" GC failed: ").append(e.getMessage()).append("; ");
                }
            }
            return Result.succeed(result.toString().trim());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.failure(e.getMessage());
        }
    }


    /**
     * Generate key pair for instance
     *
     * @param ctx       Context
     * @param instId    Instance ID
     * @param keyType   Key type: EC, RSA, or HMAC
     * @param algorithm Key algorithm: ES256, RS256, HS256, etc.
     * @return Result
     */
    @Mapping("/keypair")
    public Result generateKeypair(Context ctx, Long instId, String keyType, String algorithm) {
        try {
            if (instId == null) {
                instId = 1L; // Default to first instance
            }
            Inst inst = instMapper.selectById(instId);
            if (inst == null) {
                return Result.failure("Instance not found");
            }

            // Set default algorithm if not specified
            if (algorithm == null || algorithm.isEmpty()) {
                if ("HMAC".equalsIgnoreCase(keyType)) {
                    algorithm = "HS256";
                } else if ("RSA".equalsIgnoreCase(keyType)) {
                    algorithm = "RS256";
                } else {
                    algorithm = "ES256";
                }
            }

            instService.generateKeyPairForInst(inst, keyType, algorithm);
            // Reload inst from database to get updated JWKS
            inst = instMapper.selectById(instId);
            instService.updateJwksFile(inst);
            log.info("Generated {} key pair for instance {} with algorithm {}", keyType, inst.getName(), algorithm);

            return Result.succeed("Key pair generated successfully");
        } catch (Exception e) {
            log.error("Failed to generate key pair", e);
            return Result.failure(e.getMessage());
        }
    }

    @Mapping("/enable")
    public Result enable(Context ctx, long... id) {
        try {
            List<Long> list = CollectionUtil.newArrayList();
            for (long i : id) {
                list.add(i);
            }
            List<Inst> selectedInst = instMapper.findByIdList(list);
            for (Inst inst : selectedInst) {
                try {
                    instService.enable(inst);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.failure(e.getMessage());
        }
    }

    //
//    @Mapping("/reset")
//    public Result reset(Context ctx, long... id) {
//        try {
//            List<Long> list = CollectionUtil.newArrayList();
//            for (long i : id) {
//                list.add(i);
//            }
//            instMapper.db().table("inst").usingNull(true)
//                    .set("current_job_id", null)
//                    .set("final_status", null)
//                    .set("_lock", null).whereTrue().and().beginIn("id", list).end().update();
//            return Result.succeed("ok");
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            return Result.failure(e.getMessage());
//        }
//    }
//
//    @Mapping("/batchAdd")
//    public Result batchAdd(Context ctx, String data) {
//        try {
//            List<String> list = IOUtils.readLines(data);
//            int success = 0;
//            int skip = 0;
//            for (String str : list) {
//                if (StringUtils.isBlank(str)) {
//                    continue;
//                }
//                String[] k = StringUtils.split(str, ",");
//                Inst inst = new Inst();
//                if (k.length > 2) {
//                    inst.setId(IDUtils.getLongID());
//                    inst.setName(k[0]);
//                    inst.setUrl(k[1]);
//                    if (ReUtil.isMatch("^(1|yes|ok)$", k[2])) {
//                        inst.setAutoJob(1);
//                    }
//                } else if (k.length == 2) {
//                    inst.setId(IDUtils.getLongID());
//                    inst.setName(k[0]);
//                    inst.setUrl(k[1]);
//                    inst.setAutoJob(1);
//                } else if (k.length == 1) {
//                    inst.setId(IDUtils.getLongID());
//                    inst.setUrl(k[0]);
//                    inst.setAutoJob(1);
//                }
//                long count = instMapper.db().table("inst").whereTrue().and().beginEq("url", inst.getUrl()).end().selectCount();
//                if (count > 0) {
//                    log.debug("{}已存在! skip", inst.getUrl());
//                    skip++;
//                } else {
//                    instMapper.insert(inst, true);
//                    success++;
//                }
//
//            }
//            String skipStr = "";
//            if (skip > 0) {
//                skipStr = String.format(", 跳过%s条重复数据", skip);
//            }
//            return Result.succeed(String.format("成功添加%s条%s", success, skipStr));
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            return Result.failure(e.getMessage());
//        }
//    }
}

