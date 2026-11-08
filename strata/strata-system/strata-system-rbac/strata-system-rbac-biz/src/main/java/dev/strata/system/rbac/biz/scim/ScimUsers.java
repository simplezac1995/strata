// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import com.fasterxml.jackson.databind.JsonNode;
import dev.strata.system.rbac.api.entity.Role;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.api.entity.UserRole;
import dev.strata.system.rbac.biz.repository.RoleRepository;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SCIM 2.0 User resource mapping onto {@code sys_user} + RBAC roles. */
@Service
public class ScimUsers {

    private static final String USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";
    private static final String LIST_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:ListResponse";
    private static final Pattern EQ =
            Pattern.compile("(?i)^\\s*(userName|emails\\.value|active)\\s+eq\\s+\"?([^\"]*)\"?\\s*$");

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ScimProperties properties;

    public ScimUsers(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            ScimProperties properties) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(String filter, int startIndex, int count) {
        List<User> users = userRepository.findByDeletedFalseOrderByIdAsc();
        Map<Long, Role> roles = roleMap();
        Set<Long> ids = new LinkedHashSet<>();
        for (User user : users) {
            ids.add(user.getId());
        }
        Map<Long, List<Long>> links = linksByUser(ids);
        List<Map<String, Object>> resources = new ArrayList<>();
        for (User user : users) {
            resources.add(toResource(user, roles, links.getOrDefault(user.getId(), List.of())));
        }
        if (filter != null && !filter.isBlank()) {
            var predicate = ScimFilter.parse(filter);
            resources = resources.stream().filter(predicate).collect(java.util.stream.Collectors.toList());
        }
        int total = resources.size();
        int pageSize = Math.min(count <= 0 ? properties.getMaxResults() : count, properties.getMaxResults());
        int from = Math.min(Math.max(startIndex - 1, 0), total);
        int to = Math.min(from + pageSize, total);
        List<Map<String, Object>> page = new ArrayList<>(resources.subList(from, to));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schemas", List.of(LIST_SCHEMA));
        response.put("totalResults", total);
        response.put("startIndex", from + 1);
        response.put("itemsPerPage", page.size());
        response.put("Resources", page);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(String id) {
        User user = find(id);
        return toResource(user, roleMap(), linksByUser(Set.of(user.getId())).getOrDefault(user.getId(), List.of()));
    }

    @Transactional
    public Map<String, Object> create(JsonNode body) {
        String username = text(body, "userName");
        if (username == null || username.isBlank()) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "userName is required", "invalidValue");
        }
        if (userRepository.existsByUsernameAndDeletedFalse(username)) {
            throw new ScimException(HttpStatus.CONFLICT, "userName already exists", "uniqueness");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        applyProfile(user, body);
        user.setEmailVerified(true);
        user.setEnabled(body.path("active").asBoolean(true));
        User saved = userRepository.save(user);
        return toResource(saved, roleMap(), List.of());
    }

    @Transactional
    public Map<String, Object> replace(String id, JsonNode body) {
        User user = find(id);
        String username = text(body, "userName");
        if (username != null && !username.equals(user.getUsername())) {
            if (userRepository.existsByUsernameAndDeletedFalse(username)) {
                throw new ScimException(HttpStatus.CONFLICT, "userName already exists", "uniqueness");
            }
            user.setUsername(username);
        }
        applyProfile(user, body);
        user.setEnabled(body.path("active").asBoolean(user.isEnabled()));
        User saved = userRepository.save(user);
        return toResource(saved, roleMap(), linksByUser(Set.of(saved.getId())).getOrDefault(saved.getId(), List.of()));
    }

    @Transactional
    public Map<String, Object> patch(String id, JsonNode body) {
        User user = find(id);
        JsonNode operations = body.path("Operations");
        for (JsonNode operation : operations) {
            String op = operation.path("op").asText("").toLowerCase();
            String path = operation.path("path").asText("");
            JsonNode value = operation.path("value");
            if (path.equalsIgnoreCase("userName") || path.isBlank()) {
                String username = value.isTextual() ? value.asText() : text(value, "userName");
                if (username != null && !username.isBlank() && !username.equals(user.getUsername())) {
                    if (userRepository.existsByUsernameAndDeletedFalse(username)) {
                        throw new ScimException(HttpStatus.CONFLICT, "userName already exists", "uniqueness");
                    }
                    user.setUsername(username);
                }
            } else if (path.equalsIgnoreCase("active")) {
                boolean active = value.isBoolean() ? value.asBoolean() : Boolean.parseBoolean(value.asText());
                user.setEnabled("remove".equals(op) ? false : active);
            } else if (path.startsWith("name.")) {
                String field = path.substring("name.".length());
                String nickname = user.getNickname();
                String valueText = value.isTextual() ? value.asText() : text(value, "formatted");
                if (field.equalsIgnoreCase("formatted")) {
                    nickname = valueText;
                } else if (nickname == null || nickname.isBlank()) {
                    nickname = valueText;
                }
                user.setNickname(nickname);
            } else if (path.equalsIgnoreCase("emails")) {
                String email = firstEmail(value);
                if (email != null) {
                    user.setEmail(email);
                }
            } else if (path.equalsIgnoreCase("password")) {
                String password = value.isTextual() ? value.asText() : text(value, "password");
                if (password != null && !password.isBlank()) {
                    user.setPasswordHash(passwordEncoder.encode(password));
                }
            }
        }
        User saved = userRepository.save(user);
        return toResource(saved, roleMap(), linksByUser(Set.of(saved.getId())).getOrDefault(saved.getId(), List.of()));
    }

    @Transactional
    public void delete(String id) {
        User user = find(id);
        user.setDeleted(true);
        user.setEnabled(false);
        userRepository.save(user);
        userRoleRepository.deleteByUserId(user.getId());
    }

    private User find(String id) {
        long userId;
        try {
            userId = Long.parseLong(id);
        } catch (NumberFormatException exception) {
            throw new ScimException(HttpStatus.NOT_FOUND, "User not found", null);
        }
        return userRepository
                .findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new ScimException(HttpStatus.NOT_FOUND, "User not found", null));
    }

    private void applyProfile(User user, JsonNode body) {
        JsonNode name = body.path("name");
        String formatted = name.isObject() ? name.path("formatted").asText("") : "";
        String given = name.isObject() ? name.path("givenName").asText("") : "";
        String family = name.isObject() ? name.path("familyName").asText("") : "";
        String nickname = !formatted.isBlank() ? formatted : (given + " " + family).trim();
        if (!nickname.isBlank()) {
            user.setNickname(nickname.length() > 64 ? nickname.substring(0, 64) : nickname);
        }
        String email = firstEmail(body.path("emails"));
        if (email != null) {
            user.setEmail(email.toLowerCase());
        }
    }

    private static String firstEmail(JsonNode emails) {
        if (emails.isArray()) {
            for (JsonNode email : emails) {
                String value = email.path("value").asText("");
                if (!value.isBlank()) {
                    return value.trim();
                }
            }
        } else if (emails.isTextual()) {
            return emails.asText().trim();
        }
        return null;
    }

    private static String text(JsonNode body, String field) {
        JsonNode value = body.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private Map<Long, Role> roleMap() {
        Map<Long, Role> roles = new HashMap<>();
        for (Role role : roleRepository.findByDeletedFalseOrderByIdAsc()) {
            roles.put(role.getId(), role);
        }
        return roles;
    }

    private Map<Long, List<Long>> linksByUser(Collection<Long> userIds) {
        Map<Long, List<Long>> links = new HashMap<>();
        for (UserRole link : userRoleRepository.findByUserIdIn(userIds)) {
            links.computeIfAbsent(link.getUserId(), key -> new ArrayList<>()).add(link.getRoleId());
        }
        return links;
    }

    private List<Map<String, Object>> toResources(List<User> users) {
        Map<Long, Role> roles = roleMap();
        Set<Long> ids = new LinkedHashSet<>();
        for (User user : users) {
            ids.add(user.getId());
        }
        Map<Long, List<Long>> links = linksByUser(ids);
        List<Map<String, Object>> resources = new ArrayList<>();
        for (User user : users) {
            resources.add(toResource(user, roles, links.getOrDefault(user.getId(), List.of())));
        }
        return resources;
    }

    private Map<String, Object> toResource(User user, Map<Long, Role> roles, List<Long> roleIds) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("schemas", List.of(USER_SCHEMA));
        resource.put("id", String.valueOf(user.getId()));
        resource.put("userName", user.getUsername());
        resource.put("externalId", String.valueOf(user.getId()));
        Map<String, Object> name = new LinkedHashMap<>();
        name.put("formatted", user.getNickname());
        resource.put("name", name);
        if (user.getEmail() != null) {
            resource.put("emails", List.of(Map.of("value", user.getEmail(), "primary", true, "type", "work")));
        }
        resource.put("active", user.isEnabled());
        List<Map<String, Object>> groups = new ArrayList<>();
        for (Long roleId : roleIds) {
            Role role = roles.get(roleId);
            if (role != null) {
                groups.add(Map.of("value", String.valueOf(role.getId()), "display", role.getName()));
            }
        }
        resource.put("groups", groups);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("resourceType", "User");
        meta.put(
                "version",
                ScimVersion.of(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.isEnabled(),
                        user.getNickname(),
                        roleIds));
        meta.put("location", "/scim/v2/Users/" + user.getId());
        resource.put("meta", meta);
        return resource;
    }
}
