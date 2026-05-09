package com.dyrnq.distops.dso;

import org.noear.wood.DbContext;
import org.noear.wood.mapper.BaseMapperWrap;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import com.dyrnq.distops.model.User;
import com.dyrnq.distops.model.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Component
@Slf4j
public class UserMapper extends BaseMapperWrap<User> {

    public UserMapper() {
        super(null, User.class, User.TABLE_NAME);
    }

@Inject
DbContext dbContext;
public DbContext db() {
  return this.dbContext;
}



//Customize BEGIN
public User findByName(String name) {
        return this.selectItem(c -> {
            c.whereEq("name", name);
        });
    }
//Customize END
}
