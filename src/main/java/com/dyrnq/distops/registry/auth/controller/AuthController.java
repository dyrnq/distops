package com.dyrnq.distops.registry.auth.controller;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.dyrnq.distops.registry.auth.model.AuthRequest;
import com.dyrnq.distops.registry.auth.model.JWTPayload;
import com.dyrnq.distops.registry.auth.model.TokenResponse;
import com.dyrnq.distops.registry.auth.service.AuthService;
import com.dyrnq.distops.registry.auth.service.ITokenService;
import com.dyrnq.distops.registry.auth.service.impl.ECTokenServiceImpl;
import com.dyrnq.distops.registry.auth.service.impl.HMACTokenServiceImpl;
import com.dyrnq.distops.registry.auth.service.impl.RSATokenServiceImpl;
import com.dyrnq.distops.dso.InstMapper;
import com.dyrnq.distops.model.Inst;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.Context;
import org.noear.solon.serialization.snack4.Snack4StringSerializer;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Controller
public class AuthController {
    private static final Pattern SCOPE_PATTERN = Pattern.compile("([a-z0-9]+)(\\([a-z0-9]+\\))?");
    private static final String DEFAULT_ISSUER = "docker-auth-server";

    @Inject
    AuthService authService;

    @Inject
    private InstMapper instMapper;

    @Mapping("/auth")
    public TokenResponse auth(Context ctx,
                              @Param(required = false) String account,
                              @Param(required = false) String service,
                              @Param(required = false) List<String> scope,
                              @Header(value = "Authorization", required = false) String authorization) {
        Inst inst = instMapper.selectById(1L);
        return authenticate(ctx, inst, account, service, scope, authorization);
    }

    @Mapping("/auth/token/?")
    public TokenResponse authToken(Context ctx,
                                   @Param(required = false) String account,
                                   @Param(required = false) String service,
                                   @Param(required = false) List<String> scope,
                                   @Header(value = "Authorization", required = false) String authorization
    ) {
        Inst inst = instMapper.selectById(1L);
        return authenticate(ctx, inst, account, service, scope, authorization);
    }

    @Mapping("/auth/{instName}")
    public TokenResponse authByInst(Context ctx,
                                    @Path("instName") String instName,
                                    @Param(required = false) String account,
                                    @Param(required = false) String service,
                                    @Param(required = false) List<String> scope,
                                    @Header(value = "Authorization", required = false) String authorization) {
        Inst inst = instMapper.findByName(instName);
        if (inst == null) {
            log.warn("Instance not found: {}", instName);
            ctx.status(404);
            return null;
        }
        return authenticate(ctx, inst, account, service, scope, authorization);
    }

    @Mapping("/auth/{instName}/token/?")
    public TokenResponse authTokenByInst(Context ctx,
                                         @Path("instName") String instName,
                                         @Param(required = false) String account,
                                         @Param(required = false) String service,
                                         @Param(required = false) List<String> scope,
                                         @Header(value = "Authorization", required = false) String authorization) {
        Inst inst = instMapper.findByName(instName);
        if (inst == null) {
            log.warn("Instance not found: {}", instName);
            ctx.status(404);
            return null;
        }
        return authenticate(ctx, inst, account, service, scope, authorization);
    }


    public TokenResponse authenticate(Context ctx,
                                      Inst inst,
                                      String account,
                                      String service,
                                      List<String> scope,
                                      String authorization) {
        log.info("url: {}, method: {}, headers: {}, paramMap: {} ", ctx.url(), ctx.method(),
                JSONUtil.toJsonStr(ctx.headerMap()),
                ctx.paramMap()
        );


        try {
            byte[] bytes = ctx.bodyAsBytes();
            log.info("body: {}", ObjUtil.toString(bytes));
        } catch (Exception e) {
            log.error(e.getMessage());
        }


        try {
            AuthRequest authRequest = parseRequest(account, service, scope, authorization, ctx);

            log.info("Auth request: user={}, account={}, service={}, scopes={}", authRequest.getUser(), authRequest.getAccount(), authRequest.getService(), authRequest.getScopes());

            if (!authService.authenticate(authRequest.getUser(), authRequest.getPassword())) {
                log.warn("Authentication failed for user: {}", authRequest.getUser());
                String issuer = (inst != null && inst.getAuthIssuer() != null)
                        ? inst.getAuthIssuer()
                        : DEFAULT_ISSUER;
                ctx.headerAdd("WWW-Authenticate", "Basic realm=\"" + issuer + "\"");
                ctx.status(401);
                return null;
            }

            List<JWTPayload.ResourceAccess> accessList = authorizeScopes(authRequest);

            String auth = inst.getAuthKeyType();

            ITokenService tokenService = null;
            switch (auth) {
                case "EC":
                    tokenService = new ECTokenServiceImpl(inst);
                    break;
                case "RSA":
                    tokenService = new RSATokenServiceImpl(inst);
                    break;
                case "HMAC":
                    tokenService = new HMACTokenServiceImpl(inst);
                    break;
                default:
                    log.warn(auth);
            }
            if (tokenService == null) {
                log.error("tokenService not found");
                throw new RuntimeException("tokenService not found");
            }

            String token = tokenService.createToken(
                    authRequest.getAccount(),
                    authRequest.getService(),
                    accessList
            );

            String issuedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

            TokenResponse response = TokenResponse.builder()
                    .accessToken(token)
                    .token(token)
                    .expiresIn(36000)
                    .issuedAt(issuedAt)
                    .build();

            log.info("Token issued for user: {}, service: {}, scopes: {}, keyType: {}", authRequest.getAccount(), authRequest.getService(), authRequest.getScopes(), tokenService.getClass().getName());
            log.debug(JSONUtil.toJsonPrettyStr(ctx.headerNamesOfResponse()));
            log.info("response: {} ", Snack4StringSerializer.getDefault().serialize(response));
            return response;

        } catch (IllegalArgumentException e) {
            log.warn("Bad request: {}", e.getMessage());
            ctx.status(400);
            return null;
        } catch (Exception e) {
            log.error("Authentication error", e);
            ctx.status(500);
            return null;
        }
    }


    private AuthRequest parseRequest(String account, String service, List<String> scopes, String authorization, Context request) {

        AuthRequest.AuthRequestBuilder builder = AuthRequest.builder();

        String user = null;
        String password = null;

        String key = "Basic ";
        if (authorization != null && authorization.startsWith(key)) {
            String decoded = new String(Base64.getDecoder().decode(authorization.substring(key.length())));
            int colonIndex = decoded.indexOf(':');
            if (colonIndex > 0) {
                user = decoded.substring(0, colonIndex);
                password = decoded.substring(colonIndex + 1);
            }
        }


        if (user == null && request.method().equals("POST")) {
            user = request.param("username");
            password = request.param("password");
        }

        builder.user(user);
        builder.password(password);

        builder.account(account != null ? account : user);
        builder.service(service);

        String remoteAddr = request.remoteIp();
        String realIp = request.header("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            remoteAddr = realIp.split(",")[0].trim();
        }
        builder.remoteAddr(remoteAddr);

        List<AuthRequest.Scope> parsedScopes = new ArrayList<>();
        if (scopes != null && !scopes.isEmpty()) {
            // Merge scopes that were split by the framework (e.g., "repo:name:pull" and "push" should be "repo:name:pull,push")
            StringBuilder mergedScope = new StringBuilder();
            for (String scopeValue : scopes) {
                if (scopeValue == null || scopeValue.isEmpty()) {
                    continue;
                }
                // Check if this looks like a detached action (pull, push, delete, *)
                if (scopeValue.equals("pull") || scopeValue.equals("push") || scopeValue.equals("*") || scopeValue.equals("delete")) {
                    // Append to previous scope
                    if (!mergedScope.isEmpty()) {
                        mergedScope.append(",").append(scopeValue);
                    } else {
                        // No previous scope, treat as standalone (will be ignored by parseScopes)
                        mergedScope.append(scopeValue);
                    }
                } else {
                    // This is a new full scope, parse previous if exists
                    if (!mergedScope.isEmpty()) {
                        parsedScopes.addAll(parseScopes(mergedScope.toString()));
                    }
                    mergedScope = new StringBuilder(scopeValue);
                }
            }
            // Parse the last merged scope
            if (!mergedScope.isEmpty()) {
                parsedScopes.addAll(parseScopes(mergedScope.toString()));
            }
        }
        builder.scopes(parsedScopes);

        return builder.build();
    }

    private List<AuthRequest.Scope> parseScopes(String scopeValue) {
        List<AuthRequest.Scope> scopes = new ArrayList<>();

        if (scopeValue == null || scopeValue.isEmpty()) {
            return scopes;
        }

        log.info("Parsing scope value: {}", scopeValue);

        // Split by space to handle multiple scopes
        for (String scopeStr : scopeValue.split(" ")) {
            if (scopeStr == null || scopeStr.isEmpty()) {
                continue;
            }

            log.info("Parsing individual scope: {}", scopeStr);

            // Handle detached actions (e.g., "push" alone) - merge with previous scope
            if (scopeStr.equals("pull") || scopeStr.equals("push") || scopeStr.equals("*") || scopeStr.equals("delete")) {
                if (!scopes.isEmpty()) {
                    AuthRequest.Scope previousScope = scopes.get(scopes.size() - 1);
                    List<String> newActions = new ArrayList<>(previousScope.getActions());
                    if (!newActions.contains(scopeStr)) {
                        newActions.add(scopeStr);
                        previousScope.setActions(newActions);
                        log.info("Merged detached action '{}' into previous scope: {}", scopeStr, previousScope);
                    }
                } else {
                    log.warn("Detached action '{}' without previous scope, ignoring", scopeStr);
                }
                continue;
            }

            // Handle scope with comma-separated actions (e.g., "repository:name:pull,push")
            if (scopeStr.contains(":")) {
                String[] parts = scopeStr.split(":");
                if (parts.length >= 3) {
                    String type = parts[0];
                    String name = parts[1];
                    String actionsStr = parts[2];
                    List<String> actions = Arrays.asList(actionsStr.split(","));

                    log.info("Parsed scope: type={}, name={}, actions={}", type, name, actions);

                    scopes.add(AuthRequest.Scope.builder()
                            .type(type)
                            .name(name)
                            .actions(actions)
                            .build());
                    continue;
                }
            }

            // Fallback to original parsing logic
            String[] parts = scopeStr.split(":");
            if (parts.length < 2 || parts.length > 4) {
                log.warn("Invalid scope format: {}, skipping", scopeStr);
                continue;
            }

            Matcher matcher = SCOPE_PATTERN.matcher(parts[0]);
            if (!matcher.matches()) {
                log.warn("Invalid scope type: {}, skipping", parts[0]);
                continue;
            }

            String type = matcher.group(1);
            String classType = matcher.group(2);
            if (classType != null) {
                classType = classType.replaceAll("[()]", "");
            }

            String name;
            List<String> actions;

            if (parts.length == 3) {
                name = parts[1];
                actions = Arrays.asList(parts[2].split(","));
            } else if (parts.length == 4) {
                name = parts[1] + ":" + parts[2];
                actions = Arrays.asList(parts[3].split(","));
            } else {
                name = parts[1];
                actions = Collections.emptyList();
            }

            scopes.add(AuthRequest.Scope.builder()
                    .type(type)
                    .classType(classType)
                    .name(name)
                    .actions(actions)
                    .build());
        }

        return scopes;
    }

    private List<JWTPayload.ResourceAccess> authorizeScopes(AuthRequest authRequest) {
        List<JWTPayload.ResourceAccess> accessList = new ArrayList<>();

        // If no scopes requested, give admin full access to all resources
        if (authRequest.getScopes() == null || authRequest.getScopes().isEmpty()) {
            if ("admin".equals(authRequest.getAccount())) {
                // Admin gets full access to everything
                accessList.add(JWTPayload.ResourceAccess.builder()
                        .type("registry")
                        .name("catalog")
                        .actions(List.of("*"))
                        .build());
            }
            return accessList;
        }

        for (AuthRequest.Scope scope : authRequest.getScopes()) {
            Set<String> requestedActions = new HashSet<>(scope.getActions());
            Set<String> authorizedActions = authService.getAuthorizedActions(
                    authRequest.getAccount(),
                    scope.getType(),
                    scope.getName(),
                    requestedActions
            );

            log.info("Scope: type={}, name={}, requestedActions={}, authorizedActions={}",
                    scope.getType(), scope.getName(), requestedActions, authorizedActions);

            // Create a mutable set for authorized actions
            Set<String> mutableAuthorizedActions = new HashSet<>(authorizedActions);

            // For admin user, grant all requested actions plus any related actions
            if ("admin".equals(authRequest.getAccount())) {
                log.info("Admin user detected, granting additional actions");
                if (requestedActions.contains("push")) {
                    mutableAuthorizedActions.addAll(Arrays.asList("pull", "push", "*"));
                    log.info("Added pull, push, * for push action");
                }
                if (requestedActions.contains("pull")) {
                    mutableAuthorizedActions.add("pull");
                    log.info("Added pull for pull action");
                }
                if (requestedActions.contains("*")) {
                    mutableAuthorizedActions.addAll(Arrays.asList("pull", "push", "delete", "*"));
                    log.info("Added all actions for * action");
                }
                if (requestedActions.contains("delete")) {
                    mutableAuthorizedActions.add("delete");
                    log.info("Added delete for delete action");
                }
                log.info("Final authorized actions for admin: {}", mutableAuthorizedActions);
            }

            List<String> sortedActions = new ArrayList<>(mutableAuthorizedActions);
            Collections.sort(sortedActions);

            accessList.add(JWTPayload.ResourceAccess.builder()
                    .type(scope.getType())
                    .name(scope.getName())
                    .actions(sortedActions)
                    .build());
        }

        return accessList;
    }
}
