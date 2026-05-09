package com.dyrnq.distops;

import cn.hutool.core.lang.Dict;
import cn.hutool.json.JSONUtil;
import cn.hutool.setting.yaml.YamlUtil;
import com.dyrnq.distops.registry.RegistryProxy;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class YamlTest {
    public static void main(String[] args) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);


        Representer representer = new Representer(options) {
            @Override
            protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
                if (!this.classTags.containsKey(javaBean.getClass())) {
                    this.addClassTag(javaBean.getClass(), Tag.MAP);
                }
                return super.representJavaBean(properties, javaBean);
            }
        };

        Yaml yaml = new Yaml(representer, options);

        Dict dict = YamlUtil.load(new FileReader(new File("config.yaml")));
        System.out.println(JSONUtil.toJsonPrettyStr(dict));
        Dict dict2 = YamlUtil.load(new FileReader(new File("config_patch.yaml")));

        dict.putAll(dict2);
        YamlUtil.dump(dict, new OutputStreamWriter(System.out));

        // 加载 1.yaml 文件
        FileInputStream fis1 = new FileInputStream("config.yaml");
        Map<String, Object> map1 = yaml.load(fis1);
        fis1.close();

        // 加载 2.yaml 文件
        FileInputStream fis2 = new FileInputStream("config_patch.yaml");
        Map<String, Object> map2 = yaml.load(fis2);
        fis2.close();

        // 合并两个 YAML 文件
        Map<String, Object> mergedMap = new HashMap<>(map1);
        mergedMap.putAll(map2);

        // 输出合并后的 YAML 文件
        FileOutputStream fos = new FileOutputStream("output.yaml");
        Writer writer = new OutputStreamWriter(System.out);
        yaml.dump(mergedMap, writer);
        fos.close();


        RegistryProxy registryProxy = new RegistryProxy();
        registryProxy.setTtl("168h");
        Map<String, Object> proxy = new LinkedHashMap<>();
        proxy.put("proxy", registryProxy);

        String yamlString = yaml.dumpAsMap(proxy);
        System.out.println(yamlString);
    }
}
