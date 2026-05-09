package com.dyrnq.distops.dso;

import com.dyrnq.distops.model.Account;
import com.dyrnq.utils.BCryptPasswordEncoder;
import com.dyrnq.utils.BcryptUtils;
import com.dyrnq.utils.IDUtils;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

@SolonTest(value = com.dyrnq.distops.WebApp.class, enableHttp = false, args = "-server.port=61935")
public class AccountMapperTest {

    @Inject
    AccountMapper accountMapper;


    @Test
    public void testInitData() {
        for (int i = 0; i < 1000; i++) {
            Account e = new Account();
            e.setId(IDUtils.getLongID());
            e.setInstId(1L);
            e.setUsername("user" + i);
            e.setPassword("user" + i);
            e.setHashpw(BcryptUtils.hashPw(e.getPassword()));
            accountMapper.insert(e, true);
        }
    }

    @Test
    public void testHashpw() {
        String password = "test";
        System.out.println(BcryptUtils.hashPw(password));
    }

    @Test
    public void testEncrypt() {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(12);
        String password = "test";
        System.out.println(bCryptPasswordEncoder.encode(password));
    }
}
