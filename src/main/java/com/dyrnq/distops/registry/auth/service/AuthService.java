package com.dyrnq.distops.registry.auth.service;


import com.dyrnq.distops.dso.AccountMapper;
import com.dyrnq.distops.model.Account;
import com.dyrnq.distops.registry.auth.model.AclConfig;
import com.dyrnq.utils.BcryptUtils;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public Set<String> getAuthorizedActions(String username, String resourceType,
                                            String resourceName, Set<String> requestedActions) {
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
                Set<String> authorizedActions = matchAclRules(aclRules, username, resourceType,
                        resourceName, requestedActions);
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
    private Set<String> matchAclRules(List<AclConfig.AclRule> rules,
                                      String username, String resourceType, String resourceName,
                                      Set<String> requestedActions) {
        Set<String> authorizedActions = new HashSet<>();

        for (AclConfig.AclRule rule : rules) {
            if (matchesRule(rule, username, resourceType, resourceName)) {
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
    private boolean matchesRule(AclConfig.AclRule rule, String username, String resourceType, String resourceName) {
        AclConfig.AclRule.Match match = rule.getMatch();
        if (match == null) {
            return false;
        }

        // Match resource type
        if (match.getType() != null && !match.getType().equals(resourceType)) {
            return false;
        }

        // Match account/username
        if (match.getAccount() != null && !matchValue(match.getAccount(), username)) {
            return false;
        }

        // Match resource name (with variable expansion)
        if (match.getName() != null) {
            String expandedName = expandVariables(match.getName(), username);
            if (!matchValue(expandedName, resourceName)) {
                return false;
            }
        }

        // Match IP (optional - not implemented yet)
        if (match.getIp() != null) {
            // TODO: Implement IP matching
            log.warn("IP matching not yet implemented");
        }

        // Match service (optional)
        if (match.getService() != null) {
            // TODO: Implement service matching
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
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
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
}
