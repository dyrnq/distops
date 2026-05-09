package com.dyrnq.distops.service;

import cn.hutool.core.codec.Base64;
import com.dyrnq.distops.dso.UserMapper;
import com.dyrnq.distops.model.User;
import com.dyrnq.utils.BCryptPasswordEncoder;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.i18n.I18nUtil;


@Component
@Slf4j
public class BusinessLogic {
    @Inject
    UserMapper userMapper;

    public void changePass(String id, String base64Pass) {
        String pass = Base64.decodeStr(Base64.decodeStr(base64Pass));
        User user = new User();
        user.setId(id);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        user.setPass(encoder.encode(pass));
        userMapper.updateById(user, true);
    }

    public User findByName(String name) {
        return userMapper.findByName(name);
    }

    public User login(String base64Name, String base64Pass) {
        String name = Base64.decodeStr(Base64.decodeStr(base64Name));
        String pass = Base64.decodeStr(Base64.decodeStr(base64Pass));
        User user = userMapper.findByName(name);
        if (user != null) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
            if (encoder.matches(pass, user.getPass())) {
                return user;
            } else {
                throw new RuntimeException(I18nUtil.getMessage("loginStr.backError2"));
            }
        } else {
            throw new RuntimeException(I18nUtil.getMessage("loginStr.backError5"));
        }

    }

}
