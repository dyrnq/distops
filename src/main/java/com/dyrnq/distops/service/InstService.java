package com.dyrnq.distops.service;

import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dyrnq.distops.Constants;
import com.dyrnq.distops.HomeDir;
import com.dyrnq.distops.dso.AccountMapper;
import com.dyrnq.distops.dso.GlobalConfigMapper;
import com.dyrnq.distops.dso.InstMapper;
import com.dyrnq.distops.model.Account;
import com.dyrnq.distops.model.Inst;
import com.dyrnq.distops.registry.RegistryProxy;
import com.dyrnq.distops.registry.auth.KeyPairInfo;
import com.dyrnq.distops.registry.auth.KeyPairManager;
import com.dyrnq.distops.service.dto.ConfigVo;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.NonNull;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

@Component
@Slf4j
public class InstService {
    @Inject
    InstMapper instMapper;

    @Inject
    AccountMapper accountMapper;
    @Inject
    HomeDir homeDir;
    @Inject
    private GlobalConfigMapper globalConfigMapper;
    @Inject
    private KeyPairManager keyPairManager;

    private static @NonNull Yaml getSnakeyaml(DumperOptions options) {
        Representer representer = new Representer(options) {
            @Override
            protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
                if (!this.classTags.containsKey(javaBean.getClass())) {
                    this.addClassTag(javaBean.getClass(), Tag.MAP);
                }
                return super.representJavaBean(properties, javaBean);
            }
        };

        return new Yaml(representer, options);
    }

    /**
     * Deep merge source map into target map recursively.
     * Nested maps are merged; all other values are replaced.
     */
    @SuppressWarnings("unchecked")
    private static void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object sourceValue = entry.getValue();
            Object targetValue = target.get(key);

            if (sourceValue instanceof Map && targetValue instanceof Map) {
                Map<String, Object> targetMap = (Map<String, Object>) targetValue;
                Map<String, Object> sourceMap = (Map<String, Object>) sourceValue;
                deepMerge(targetMap, sourceMap);
            } else {
                target.put(key, sourceValue);
            }
        }
    }

    /**
     * Auto-detect the local non-loopback IPv4 address.
     * Used as fallback when auth_realm is not explicitly configured.
     */
    private static String getLocalIp() {
        // try to find first non-loopback, non-link-local IPv4 address
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address
                        && !addr.isLoopbackAddress()
                        && !addr.isLinkLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignore) {
        }
        // last resort: try InetAddress.getLocalHost with strict loopback check
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            if (localHost instanceof java.net.Inet4Address
                && !localHost.isLoopbackAddress()
                && !localHost.isLinkLocalAddress()) {
                return localHost.getHostAddress();
            }
        } catch (Exception ignore) {
        }
        throw new RuntimeException("Cannot determine local non-loopback IP. Please configure auth_realm explicitly on the instance.");
    }

    /**
     * Write htpasswd file from Account table (preferred) or Htpasswd table (legacy)
     */
    public void writeHtpasswd(Inst inst) {
        String inst_name = inst.getName();
        File htpasswd_file = new File(StringUtils.joinWith(File.separator, homeDir.getHomeAbsolutePath(), "registry", inst_name, "config", "htpasswd"));
        try {
            FileUtils.forceMkdirParent(htpasswd_file);
        } catch (IOException e) {
            log.error("Failed to create htpasswd directory", e);
        }

        List<String> lines = new ArrayList<>();

        // Try to get accounts from Account table first
        List<Account> accountList = accountMapper.selectByInstIdAndEnabled(inst.getId(), 1);
        if (accountList != null && !accountList.isEmpty()) {
            accountList.forEach(account -> {
                String line = account.getUsername() + ":" + account.getHashpw();
                lines.add(line);
            });
            log.info("Written htpasswd file with {} accounts from Account table for instance {}", lines.size(), inst_name);
        } else {
            // Fallback to Htpasswd table for backward compatibility
            List<Account> htpasswdList = accountMapper.selectByInstIdAndEnabled(inst.getId(), 1);
            if (htpasswdList != null) {
                htpasswdList.forEach(htpasswd -> {
                    String line = htpasswd.getUsername() + ":" + htpasswd.getHashpw();
                    lines.add(line);
                });
                log.info("Written htpasswd file with {} users from Htpasswd table for instance {}", lines.size(), inst_name);
            }
        }

        try {
            FileUtils.writeStringToFile(htpasswd_file, "", Charset.defaultCharset(), false);
            if (!lines.isEmpty()) {
                FileUtils.writeLines(htpasswd_file, lines, true);
            }
        } catch (IOException e) {
            log.error("Failed to write htpasswd file", e);
        }
    }

    public void enable(Inst inst) {
        enable(inst, null, null);
    }

    public void enable(Inst inst, String config_yaml_template, String registry_supervisor_template) {

        if (StringUtils.isEmpty(config_yaml_template)) {
            config_yaml_template = globalConfigMapper.findByName(Constants.YAML_CONFIG).getValue();
        }
        if (StringUtils.isEmpty(registry_supervisor_template)) {
            registry_supervisor_template = globalConfigMapper.findByName(Constants.INI_CONFIG).getValue();
        }

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);


        String inst_name = inst.getName();


        File config_file = new File(StringUtils.joinWith(File.separator, homeDir.getHomeAbsolutePath(), "registry", inst_name, "config", "config.yml"));

        try {
            FileUtils.forceMkdirParent(config_file);
        } catch (IOException e) {
            log.error(e.getMessage());
        }


        Map<String, Object> data = new HashMap<>();
        data.put("port", String.valueOf(Solon.cfg().serverPort()));

        data.put("app_home", homeDir.getHomeAbsolutePath());

        // Sanitize env for template: do NOT modify inst object (prevents commas persisting to DB)
        if (StringUtils.isNotBlank(inst.getEnv())) {
            List<String> lines = IOUtils.readLines(inst.getEnv());

            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                String trim_line = StringUtils.trim(line);
                if (StringUtils.isNotBlank(trim_line) && !trim_line.startsWith("#")) {
                    sb.append(trim_line).append(",");
                }
            }
            String sanitizedEnv = sb.toString();
            inst.setEnv(sanitizedEnv);
        }
        data.put("inst", inst);


        // Compute realm URL: use explicit config, or auto-detect from local IP
        String realmUrl = inst.getAuthRealm();
        if (StringUtils.isBlank(realmUrl)) {
            realmUrl = "http://" + getLocalIp() + ":" + Solon.cfg().serverPort() + "/auth/" + inst.getName();
        } else if (!realmUrl.endsWith("/" + inst.getName())) {
            realmUrl = realmUrl + "/" + inst.getName();
        }
        data.put("realm_url", realmUrl);
        data.put("service_name", StringUtils.defaultIfBlank(inst.getAuthService(), "registry.docker.io"));
        data.put("issuer_name", StringUtils.defaultIfBlank(inst.getAuthIssuer(), "docker-auth-server"));

        try {
            FileUtils.forceMkdirParent(config_file);
            Template yaml = new Template("yaml", new StringReader(config_yaml_template), cfg);
            try (Writer out = new OutputStreamWriter(new FileOutputStream(config_file))) {
                yaml.process(data, out);
            }
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);


            Yaml snakeyaml = getSnakeyaml(options);
            Map<String, Object> proxy = null;
            Map<String, Object> extra = null;
            if (StringUtils.isNotEmpty(inst.getExtraYaml())) {
                try {
                    extra = snakeyaml.load(inst.getExtraYaml());
                } catch (Exception ignore) {

                }
            }

            if (StringUtils.isNotBlank(inst.getProxyRemoteurl())) {
                RegistryProxy registryProxy = new RegistryProxy();
                registryProxy.setTtl(StringUtils.isNotBlank(inst.getProxyTtl()) ? inst.getProxyTtl() : "168h");
                registryProxy.setRemoteurl(inst.getProxyRemoteurl());
                registryProxy.setUsername(inst.getProxyUsername());
                registryProxy.setPassword(inst.getProxyPassword());

                proxy = new LinkedHashMap<>();
                proxy.put("proxy", registryProxy);

            }

            if (proxy != null || extra != null) {
                Map<String, Object> mergedMap = new LinkedHashMap<>();
                Map<String, Object> org = snakeyaml.load(new FileInputStream(config_file));

                mergedMap.putAll(org);
                if (proxy != null) {
                    mergedMap.putAll(proxy);
                    mergedMap.remove("notifications");
                }
                if (extra != null) {
                    deepMerge(mergedMap, extra);
                }


                FileOutputStream fos = new FileOutputStream(config_file);
                Writer writer = new OutputStreamWriter(fos);
                snakeyaml.dump(mergedMap, writer);
                IOUtils.closeQuietly(writer);
            }


        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (TemplateException e) {
            log.error(e.getMessage());
        }


        if (Strings.CI.equals("token", inst.getAuth())) {
            if (StrUtil.isBlank(inst.getAuthJwksJson())) {
                generateKeyPairForInst(inst, "EC", "ES512");
            }
        }
        updateJwksFile(inst);

        writeHtpasswd(inst);

        File super_file = new File(StringUtils.joinWith(File.separator, homeDir.getHomeAbsolutePath(), "supervisor", "conf.d", "registry-" + inst_name + ".ini"));

        try {
            FileUtils.forceMkdirParent(super_file);
//            FileUtils.writeStringToFile(super_file, registry_supervisor, Charset.defaultCharset(), false);

            Template yaml = new Template("yaml", new StringReader(registry_supervisor_template), cfg);
            try (Writer out = new OutputStreamWriter(new FileOutputStream(super_file))) {
                yaml.process(data, out);
            }

        } catch (IOException | TemplateException e) {
            log.error(e.getMessage());
        }

        try {
            RuntimeUtil.exec("supervisorctl update");
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        instMapper.updateEnabled(inst.getId(), 1);
    }

    public void disable(Inst inst) {
        String inst_name = inst.getName();
        String svcName = "registry-" + inst_name;
        File super_file = new File(StringUtils.joinWith(File.separator, homeDir.getHomeAbsolutePath(), "supervisor", "conf.d", "registry-" + inst_name + ".ini"));
        try {
            RuntimeUtil.exec("supervisorctl stop " + svcName);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        try {
            FileUtils.deleteQuietly(super_file);
        } catch (Exception ignore) {
        }
        try {
            RuntimeUtil.exec("supervisorctl update");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        instMapper.updateEnabled(inst.getId(), 0);

    }

    /**
     * Generate key pair for an instance
     *
     * @param inst      The instance to generate key pair for
     * @param keyType   Key type (EC, RSA or HMAC)
     * @param algorithm Key algorithm (ES256, ES384, ES512, RS256, RS384, RS512, HS256, HS384, HS512)
     */
    public void generateKeyPairForInst(Inst inst, String keyType, String algorithm) {
        try {
            // Generate key pair
            KeyPairInfo keyPair;
            if ("RSA".equalsIgnoreCase(keyType)) {
                keyPair = keyPairManager.generateRSAKeyPair(algorithm != null ? algorithm : "RS256");
            } else if ("HMAC".equalsIgnoreCase(keyType)) {
                keyPair = keyPairManager.generateHMACKeyPair(algorithm != null ? algorithm : "HS256");
            } else {
                keyPair = keyPairManager.generateECKeyPair(algorithm != null ? algorithm : "ES256");
            }

            // Update instance with key information
            inst.setAuthPrivateKey(keyPair.getPrivateKeyPem());
            inst.setAuthPublicKey(keyPair.getPublicKeyPem());
            inst.setAuthJwksJson(keyPair.getJwksJson());
            inst.setAuthKeyType(keyPair.getKeyType());
            inst.setAuthKeyAlg(keyPair.getAlgorithm());

            instMapper.updateKeyPair(inst);

            // Write JWKS file
            writeJwksFile(inst, keyPair.getJwksJson());

            // Update Registry config with signing algorithm
//            updateRegistrySigningAlgorithms(inst, algorithm);

            log.info("Generated {} key pair for instance {} with algorithm {}",
                    keyPair.getKeyType(), inst.getName(), keyPair.getAlgorithm());

        } catch (Exception e) {
            log.error("Failed to generate key pair for instance {}", inst.getName(), e);
            throw new RuntimeException("Failed to generate key pair", e);
        }
    }

    /**
     * Update Registry config.yml with the specified signing algorithm
     */
//    private void updateRegistrySigningAlgorithms(Inst inst, String algorithm) {
//        File configFile = new File(StringUtils.joinWith(File.separator,
//                homeDir.getHomeAbsolutePath(), "registry", inst.getName(), "config", "config.yml"));
//        try {
//            if (configFile.exists()) {
//                String content = FileUtils.readFileToString(configFile, Charset.defaultCharset());
//
//                // Check if algorithm already exists in signingalgorithms
//                if (!content.contains(algorithm)) {
//                    // Add algorithm to signingalgorithms list
//                    content = content.replaceFirst("(\\s+- ES256)", "$1\n        - " + algorithm);
//                    FileUtils.writeStringToFile(configFile, content, Charset.defaultCharset());
//                    log.info("Added {} to Registry signing algorithms", algorithm);
//                }
//            }
//        } catch (IOException e) {
//            log.error("Failed to update Registry signing algorithms", e);
//        }
//    }

    /**
     * Write JWKS file for an instance
     */
    private void writeJwksFile(Inst inst, String jwksJson) {
        File jwksFile = new File(StringUtils.joinWith(File.separator,
                homeDir.getHomeAbsolutePath(), "registry", inst.getName(), "config", "jwks.json"));
        try {
            FileUtils.forceMkdirParent(jwksFile);
            FileUtils.writeStringToFile(jwksFile, jwksJson, Charset.defaultCharset());
            log.info("Written JWKS file to {}", jwksFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write JWKS file", e);
        }
    }

    /**
     * Update JWKS file for an instance from stored key information
     */
    public void updateJwksFile(Inst inst) {
        if (inst.getAuthJwksJson() != null) {
            writeJwksFile(inst, inst.getAuthJwksJson());
        }
    }

    public ConfigVo getRegistryConfig(Inst inst) {
        ConfigVo vo = new ConfigVo();
        String inst_name = inst.getName();
        File config_file = new File(StringUtils.joinWith(File.separator, homeDir.getHomeAbsolutePath(), "registry", inst_name, "config", "config.yml"));
        vo.setPath(config_file.getAbsolutePath());
        try {
            vo.setBody(IOUtils.toString(new FileReader(config_file)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return vo;
    }

    public ConfigVo getSupervisorConfig(Inst inst) {
        ConfigVo vo = new ConfigVo();
        String inst_name = inst.getName();
        File super_file = new File(StringUtils.joinWith(File.separator, homeDir.getHomeAbsolutePath(), "supervisor", "conf.d", "registry-" + inst_name + ".ini"));
        vo.setPath(super_file.getAbsolutePath());
        try {
            vo.setBody(IOUtils.toString(new FileReader(super_file)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return vo;
    }

    public ConfigVo getJwksFileConfig(Inst inst) {
        ConfigVo vo = new ConfigVo();
        String inst_name = inst.getName();
        File jwksFile = new File(StringUtils.joinWith(File.separator,
                homeDir.getHomeAbsolutePath(), "registry", inst.getName(), "config", "jwks.json"));
        vo.setPath(jwksFile.getAbsolutePath());
        try {
            vo.setBody(JSONUtil.toJsonPrettyStr(IOUtils.toString(new FileReader(jwksFile))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return vo;
    }

    public ConfigVo getHtpasswdConfig(Inst inst) {
        ConfigVo vo = new ConfigVo();
        String inst_name = inst.getName();
        File htpasswd_file = new File(StringUtils.joinWith(File.separator, homeDir.getHomeAbsolutePath(), "registry", inst_name, "config", "htpasswd"));
        vo.setPath(htpasswd_file.getAbsolutePath());
        try {
            vo.setBody(IOUtils.toString(new FileReader(htpasswd_file)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return vo;
    }

    public Map<String, ConfigVo> getInstConfig(Inst inst) {
        Map<String, ConfigVo> all = new LinkedHashMap<>();


        try {
            all.put("config.yaml", getRegistryConfig(inst));
        } catch (Exception ignore) {

        }
        try {
            all.put("supervisor", getSupervisorConfig(inst));
        } catch (Exception ignore) {

        }
        try {
            all.put("jwks.json", getJwksFileConfig(inst));
        } catch (Exception ignore) {

        }

        try {
            all.put("htpasswd", getHtpasswdConfig(inst));
        } catch (Exception ignore) {

        }
        return all;
    }

    /**
     * Run registry garbage collection to clean up orphaned blobs.
     */
    public void runGarbageCollection(Inst inst) {
        String instName = inst.getName();
        String configPath = StringUtils.joinWith(File.separator,
                homeDir.getHomeAbsolutePath(), "registry", instName, "config", "config.yml");
        String cmd = "registry garbage-collect " + configPath;
        log.info("Running GC for instance {}: {}", instName, cmd);
        String output = RuntimeUtil.execForStr(cmd);
        log.info("GC output for {}: {}", instName, output);
    }

}
