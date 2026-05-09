package com.dyrnq.distops;

import cn.hutool.core.io.resource.ClassPathResource;
import com.dyrnq.utils.VersionUtils;
import org.apache.commons.io.IOUtils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.bean.LifecycleBean;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;

@Component
public class ShowBanner implements LifecycleBean {
    @Inject("${solon.app.name}")
    String projectName;

    @Override
    public void start() throws Throwable {
        BufferedReader reader = null;
        try {
            ClassPathResource resource = new ClassPathResource("banner.txt");
            reader = resource.getReader(StandardCharsets.UTF_8);
            String str;
            StringBuilder stringBuilder = new StringBuilder();
            // 使用readLine() 比较方便的读取一行
            while (null != (str = reader.readLine())) {
                stringBuilder.append(str + "\n");
            }
            reader.close();// 关闭流
            stringBuilder.append(projectName + " " + VersionUtils.getVersion() + "\n");
            IOUtils.write(stringBuilder.toString(), System.out);
        } catch (Exception e) {
        } finally {
            IOUtils.close(reader);
        }
    }
}
