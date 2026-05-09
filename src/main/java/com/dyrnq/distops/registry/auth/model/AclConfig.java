package com.dyrnq.distops.registry.auth.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * ACL (Access Control List) Configuration
 * Used for fine-grained access control in Docker Registry
 */
@Data
public class AclConfig {
    
    /**
     * List of ACL rules
     */
    private List<AclRule> rules;
    
    /**
     * ACL Rule definition
     */
    @Data
    public static class AclRule {
        
        /**
         * Match conditions
         */
        private Match match;
        
        /**
         * Allowed actions (pull, push, delete, *)
         */
        private List<String> actions;
        
        /**
         * Rule comment/description
         */
        private String comment;
        
        /**
         * Match condition definition
         */
        @Data
        public static class Match {
            
            /**
             * Resource type: repository, registry, namespace
             */
            private String type;
            
            /**
             * Resource name pattern (supports ${account} variable and glob patterns)
             * Examples:
             * - "${account}/*" - User's own repositories
             * - "library/*" - Library repositories
             * - "*" - All repositories
             */
            private String name;
            
            /**
             * Account/User pattern (optional, for backwards compatibility)
             */
            private String account;
            
            /**
             * Resource labels (optional)
             */
            private Map<String, String> labels;
            
            /**
             * IP address/CIDR pattern (optional)
             */
            private String ip;
            
            /**
             * Service name (optional)
             */
            private String service;
        }
    }
}
