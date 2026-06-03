package com.dyrnq.distops.registry.auth.service;

import com.dyrnq.distops.dso.AccountMapper;
import com.dyrnq.distops.model.Account;
import com.dyrnq.distops.registry.auth.model.AclConfig;
import com.dyrnq.utils.BcryptUtils;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * Authentication Service for Docker Registry
 * Uses database (account table) for user authentication and ACL management
 */
@Slf4j
@Component
public class AuthService {

    private static final Pattern GLOB_PATTERN = Pattern.compile("[?*\\[\\]]");
    private static final Pattern REGEX_PATTERN = Pattern.compile("^/(.+)/$");

    @Inject
    private AccountMapper accountMapper;

    /**
     * Authenticate user from database
     *
     * @param username Username
     * @param password Password (plain text)
     * @return true if authentication successful, false otherwise
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        // Authenticate from database
        Account account = accountMapper.selectByInstIdAndUsernameAndEnabled(1L, username, 1);
        if (account == null || account.getId() == null) {
            log.debug("User not found or disabled: {}", username);
            return false;
        }

        boolean matches = BcryptUtils.checkPw(password, account.getHashpw());

        if (matches) {
            log.debug("Authentication successful for user: {}", username);
        } else {
            log.debug("Authentication failed for user: {}", username);
        }
        return matches;
    }

    /**
     * Get authorized actions for user based on ACL rules from database
     *
     * @param username         Username
     * @param resourceType     Resource type (repository, registry, namespace)
     * @param resourceName     Resource name (e.g., repository name)
     * @param requestedActions Requested actions (pull, push, delete, *)
     * @return Set of authorized actions
     */
    public Set<String> getAuthorizedActions(
            String username, String resourceType, String resourceName, Set<String> requestedActions, String clientIp) {
        // Get account from database
        Account account = accountMapper.selectByInstIdAndUsernameAndEnabled(1L, username, 1);
        if (account == null || account.getId() == null) {
            log.debug("Account not found: {}", username);
            return Collections.emptySet();
        }

        // Parse and apply ACL rules from account
        if (account.getAcl() != null && !account.getAcl().trim().isEmpty()) {
            List<AclConfig.AclRule> aclRules = parseAcl(account.getAcl());
            if (!aclRules.isEmpty()) {
                Set<String> authorizedActions =
                        matchAclRules(aclRules, username, resourceType, resourceName, requestedActions, clientIp);
                if (!authorizedActions.isEmpty()) {
                    log.debug("Authorized actions for user {}: {}", username, authorizedActions);
                    return authorizedActions;
                }
            }
        }

        // No ACL rules defined - deny by default
        log.debug("No ACL rules matched for user {}: {}/{}", username, resourceType, resourceName);
        return Collections.emptySet();
    }

    /**
     * Parse ACL JSON string to list of rules
     *
     * @param aclJson ACL JSON string
     * @return List of ACL rules
     */
    private List<AclConfig.AclRule> parseAcl(String aclJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            AclConfig config = mapper.readValue(aclJson, AclConfig.class);
            return config != null ? config.getRules() : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to parse ACL JSON: {}", aclJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * Match ACL rules and return authorized actions
     *
     * @param rules            List of ACL rules
     * @param username         Username
     * @param resourceType     Resource type
     * @param resourceName     Resource name
     * @param requestedActions Requested actions
     * @return Set of authorized actions
     */
    private Set<String> matchAclRules(
            List<AclConfig.AclRule> rules,
            String username,
            String resourceType,
            String resourceName,
            Set<String> requestedActions,
            String clientIp) {
        Set<String> authorizedActions = new HashSet<>();

        for (AclConfig.AclRule rule : rules) {
            if (matchesRule(rule, username, resourceType, resourceName, clientIp)) {
                if (rule.getActions() == null || rule.getActions().isEmpty()) {
                    log.debug("ACL rule matched but no actions defined for user: {}", username);
                    return Collections.emptySet();
                }

                // Wildcard - grant all requested actions
                if (rule.getActions().contains("*")) {
                    log.debug("ACL rule matched with wildcard for user: {}", username);
                    return requestedActions;
                }

                // Grant matching actions
                for (String action : rule.getActions()) {
                    if (requestedActions.contains(action)) {
                        authorizedActions.add(action);
                    }
                }

                if (!authorizedActions.isEmpty()) {
                    log.debug("ACL rule matched for user {}: granted {}", username, authorizedActions);
                    return authorizedActions;
                }
            }
        }

        log.debug("No ACL rules matched for user {}: {}/{}", username, resourceType, resourceName);
        return Collections.emptySet();
    }

    /**
     * Check if an ACL rule matches the given context
     *
     * @param rule         ACL rule
     * @param username     Username
     * @param resourceType Resource type
     * @param resourceName Resource name
     * @return true if rule matches, false otherwise
     */
    private boolean matchesRule(
            AclConfig.AclRule rule, String username, String resourceType, String resourceName, String clientIp) {
        AclConfig.AclRule.Match match = rule.getMatch();
        if (match == null) {
            return false;
        }

        // type
        if (match.getType() != null && !match.getType().equals(resourceType)) {
            return false;
        }

        // account
        if (match.getAccount() != null && !matchValue(match.getAccount(), username)) {
            return false;
        }

        // name
        if (match.getName() != null) {
            String expandedName = expandVariables(match.getName(), username);
            if (!matchValue(expandedName, resourceName)) {
                return false;
            }
        }

        // ip (comma-separated, all must match)
        if (match.getIp() != null) {
            if (clientIp == null) {
                return false;
            }
            for (String ipPattern : match.getIp().split(",")) {
                String trimmed = ipPattern.trim();
                if (!trimmed.isEmpty() && !matchIp(trimmed, clientIp)) {
                    return false;
                }
            }
        }

        // service (optional - not implemented yet)
        if (match.getService() != null) {
            log.warn("Service matching not yet implemented");
        }

        return true;
    }

    /**
     * Match a value against a pattern (supports exact match, glob, and regex)
     *
     * @param pattern Pattern to match against
     * @param value   Value to match
     * @return true if match successful, false otherwise
     */
    private boolean matchValue(String pattern, String value) {
        if (value == null) {
            return false;
        }

        if (pattern == null || pattern.isEmpty()) {
            return value.isEmpty();
        }

        // Check for regex pattern (enclosed in /.../)
        Matcher regexMatcher = REGEX_PATTERN.matcher(pattern);
        if (regexMatcher.matches()) {
            String regex = regexMatcher.group(1);
            boolean matches = value.matches(regex);
            log.debug("Regex match: pattern={} value={} result={}", pattern, value, matches);
            return matches;
        }

        // Check for glob pattern (contains *, ?, or [])
        if (GLOB_PATTERN.matcher(pattern).find()) {
            boolean matches = globMatch(pattern, value);
            log.debug("Glob match: pattern={} value={} result={}", pattern, value, matches);
            return matches;
        }

        // Exact match
        boolean matches = pattern.equals(value);
        log.debug("Exact match: pattern={} value={} result={}", pattern, value, matches);
        return matches;
    }

    /**
     * Perform glob pattern matching
     *
     * @param pattern Glob pattern (*, ?, [])
     * @param text    Text to match
     * @return true if match successful, false otherwise
     */
    private boolean globMatch(String pattern, String text) {
        String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
        return text.matches(regex);
    }

    /**
     * Expand variables in pattern (e.g., ${account} -> username)
     *
     * @param pattern  Pattern with variables
     * @param username Username to substitute
     * @return Expanded pattern
     */
    private String expandVariables(String pattern, String username) {
        if (pattern == null) {
            return null;
        }

        if (pattern.contains("${account}")) {
            pattern = pattern.replace("${account}", username);
        }
        return pattern;
    }

    /**
     * Match client IP against a CIDR pattern or single IP
     * Supports: "192.168.1.0/24", "10.0.0.1", "0.0.0.0/0" (matches all)
     * Prefix "!" for deny: "!10.0.0.0/8" (returns false if IP matches)
     *
     * @param pattern  CIDR pattern or single IP (optionally prefixed with ! for deny)
     * @param clientIp Client IP address
     * @return true if IP matches the allow/deny rule
     */
    private boolean matchIp(String pattern, String clientIp) {
        boolean deny = false;
        String cidr = pattern;
        if (pattern.startsWith("!")) {
            deny = true;
            cidr = pattern.substring(1);
        }

        boolean matched;
        if (cidr.contains("/")) {
            matched = matchCidr(cidr, clientIp);
        } else {
            matched = cidr.equals(clientIp);
        }

        boolean result = deny ? !matched : matched;
        log.debug("IP match: pattern={} clientIp={} result={}", pattern, clientIp, result);
        return result;
    }

    /**
     * Check if client IP is within a CIDR range (supports IPv4 and IPv6)
     */
    private boolean matchCidr(String cidr, String ip) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) return false;
            int prefixLen = Integer.parseInt(parts[1]);

            byte[] cidrBytes = ipToBytes(parts[0]);
            byte[] ipBytes = ipToBytes(ip);
            if (cidrBytes.length != ipBytes.length) return false;

            int fullBytes = prefixLen / 8;
            int remainingBits = prefixLen % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (cidrBytes[i] != ipBytes[i]) return false;
            }
            if (remainingBits > 0 && fullBytes < cidrBytes.length) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                if ((cidrBytes[fullBytes] & mask) != (ipBytes[fullBytes] & mask)) return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Invalid CIDR or IP: cidr={} ip={}", cidr, ip, e);
            return false;
        }
    }

    /**
     * Convert IP string to byte array (supports IPv4 and IPv6)
     */
    private byte[] ipToBytes(String ip) throws java.net.UnknownHostException {
        java.net.InetAddress addr = java.net.InetAddress.getByName(ip);
        return addr.getAddress();
    }
}
