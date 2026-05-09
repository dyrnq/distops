package com.dyrnq.distops.filter;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;
import org.noear.solon.i18n.I18nUtil;

import java.util.*;

@Component
@Slf4j
public class I18nFilter implements Filter {

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {


        Properties properties = I18nUtil.getMessageBundle().toProps();

        // js国际化
        Set<String> messageHeaders = new HashSet<>();
        List<Message> messages = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            Message message = new Message();
            message.setKey(key);
            message.setValue(properties.getProperty(key));
            messages.add(message);

            messageHeaders.add(key.split("\\.")[0]);
        }

        ctx.attrSet("messageHeaders", messageHeaders);
        ctx.attrSet("messages", messages);


        chain.doFilter(ctx);
    }

}
