package com.dyrnq.distops.controller.api;

import cn.hutool.core.util.PageUtil;
import com.dyrnq.distops.controller.ApiController;
import com.dyrnq.distops.controller.PageResult;
import com.dyrnq.distops.dso.RepoMapper;
import com.dyrnq.distops.model.Repo;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Result;
import org.noear.wood.IPage;

import java.util.List;

@Mapping("api/repo")
@Controller
@Slf4j
public class RepoController extends ApiController {

    @Inject
    RepoMapper repoMapper;

    /**
     * Query repositories with pagination
     */
    @Mapping("")
    public PageResult query(Context ctx, int page, int limit) {
        try {
            int start = PageUtil.getStart(page - 1, limit);
            IPage<Repo> p = repoMapper.selectPage(start, limit, null);
            List<Repo> repoList = p.getList();
            return PageResult.succeed(repoList, p.getTotal());
        } catch (Exception e) {
            log.error("Failed to query repos", e);
            return PageResult.failure(e.getMessage());
        }
    }

    /**
     * Get repository by ID
     */
    @Mapping("get")
    public Result get(Context ctx, Long id) {
        try {
            Repo repo = repoMapper.selectById(id);
            if (repo == null) {
                return Result.failure("Repository not found");
            }
            return Result.succeed(repo);
        } catch (Exception e) {
            log.error("Failed to get repository", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Delete repositories by IDs
     */
    @Mapping("del")
    public Result del(Context ctx, Long... id) {
        try {
            for (Long i : id) {
                repoMapper.deleteById(i);
            }
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to delete repositories", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Add new repository
     */
    @Mapping("add")
    public Result add(Context ctx, Repo repo) {
        try {
            repoMapper.insert(repo, true);
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to add repository", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Update repository
     */
    @Mapping("update")
    public Result update(Context ctx, Repo repo) {
        try {
            repoMapper.updateById(repo, true);
            return Result.succeed("ok");
        } catch (Exception e) {
            log.error("Failed to update repository", e);
            return Result.failure(e.getMessage());
        }
    }
}
