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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SCIM 2.0 Group resource mapped onto framework RBAC roles + user-role links. */
@Service
public class ScimGroups {

    private static final String GROUP_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Group";
    private static final String LIST_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:ListResponse";
    private static final Pattern EQ = Pattern.compile("(?i)^\\s*(displayName)\\s+eq\\s+\"?([^\"]*)\"?\\s*$");

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final ScimProperties properties;

    public ScimGroups(
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            UserRepository userRepository,
            ScimProperties properties) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(String filter, int startIndex, int count) {
        List<Role> roles = roleRepository.findByDeletedFalseOrderByIdAsc();
        List<Map<String, Object>> resources = new ArrayList<>();
        for (Role role : roles) {
            resources.add(toResource(role));
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
        return toResource(find(id));
    }

    @Transactional
    public Map<String, Object> create(JsonNode body) {
        String displayName = text(body, "displayName");
        if (displayName == null || displayName.isBlank()) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "displayName is required", "invalidValue");
        }
        Role role = new Role();
        role.setCode(uniqueCode(displayName));
        role.setName(displayName.length() > 64 ? displayName.substring(0, 64) : displayName);
        role.setEnabled(true);
        Role saved = roleRepository.save(role);
        syncMembers(saved, memberIds(body.path("members")));
        return toResource(saved);
    }

    @Transactional
    public Map<String, Object> replace(String id, JsonNode body) {
        Role role = find(id);
        String displayName = text(body, "displayName");
        if (displayName != null && !displayName.isBlank()) {
            role.setName(displayName.length() > 64 ? displayName.substring(0, 64) : displayName);
        }
        roleRepository.save(role);
        syncMembers(role, memberIds(body.path("members")));
        return toResource(role);
    }

    @Transactional
    public Map<String, Object> patch(String id, JsonNode body) {
        Role role = find(id);
        for (JsonNode operation : body.path("Operations")) {
            String op = operation.path("op").asText("").toLowerCase();
            String path = operation.path("path").asText("");
            JsonNode value = operation.path("value");
            if (path.equalsIgnoreCase("displayName") || path.isBlank()) {
                String displayName = value.isTextual() ? value.asText() : text(value, "displayName");
                if (displayName != null && !displayName.isBlank()) {
                    role.setName(displayName.length() > 64 ? displayName.substring(0, 64) : displayName);
                }
            } else if (path.equalsIgnoreCase("members")) {
                Set<Long> current = memberIdsOfRole(role.getId());
                if ("add".equals(op)) {
                    current.addAll(memberIds(value));
                } else if ("remove".equals(op)) {
                    current.removeAll(memberIds(value));
                } else {
                    current = new LinkedHashSet<>(memberIds(value));
                }
                syncMembers(role, current);
            }
        }
        roleRepository.save(role);
        return toResource(role);
    }

    @Transactional
    public void delete(String id) {
        Role role = find(id);
        role.setDeleted(true);
        role.setEnabled(false);
        roleRepository.save(role);
        userRoleRepository.deleteByRoleId(role.getId());
    }

    private void syncMembers(Role role, Set<Long> userIds) {
        for (Long userId : userIds) {
            userRepository
                    .findById(userId)
                    .filter(user -> !user.isDeleted())
                    .orElseThrow(() -> new ScimException(HttpStatus.BAD_REQUEST, "Unknown member: " + userId));
        }
        userRoleRepository.deleteByRoleId(role.getId());
        for (Long userId : userIds) {
            UserRole link = new UserRole();
            link.setUserId(userId);
            link.setRoleId(role.getId());
            userRoleRepository.save(link);
        }
    }

    private Set<Long> memberIdsOfRole(Long roleId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (UserRole link : userRoleRepository.findByRoleId(roleId)) {
            ids.add(link.getUserId());
        }
        return ids;
    }

    private Set<Long> memberIds(JsonNode members) {
        Set<Long> ids = new LinkedHashSet<>();
        if (members.isArray()) {
            for (JsonNode member : members) {
                JsonNode value = member.isObject() ? member.path("value") : member;
                try {
                    ids.add(Long.parseLong(value.asText()));
                } catch (NumberFormatException exception) {
                    throw new ScimException(HttpStatus.BAD_REQUEST, "Invalid member value", "invalidValue");
                }
            }
        }
        return ids;
    }

    private String uniqueCode(String displayName) {
        String slug = displayName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "group";
        }
        String base = "scim-" + (slug.length() > 48 ? slug.substring(0, 48) : slug);
        String code = base;
        int suffix = 1;
        while (roleRepository.findByCodeAndDeletedFalse(code).isPresent()) {
            code = base + "-" + suffix++;
        }
        return code;
    }

    private Role find(String id) {
        long roleId;
        try {
            roleId = Long.parseLong(id);
        } catch (NumberFormatException exception) {
            throw new ScimException(HttpStatus.NOT_FOUND, "Group not found", null);
        }
        return roleRepository
                .findById(roleId)
                .filter(role -> !role.isDeleted())
                .orElseThrow(() -> new ScimException(HttpStatus.NOT_FOUND, "Group not found", null));
    }

    private Map<String, Object> toResource(Role role) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("schemas", List.of(GROUP_SCHEMA));
        resource.put("id", String.valueOf(role.getId()));
        resource.put("displayName", role.getName());
        resource.put("externalId", String.valueOf(role.getId()));
        List<Map<String, Object>> members = new ArrayList<>();
        Set<Long> userIds = memberIdsOfRole(role.getId());
        Map<Long, User> users = new LinkedHashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            users.put(user.getId(), user);
        }
        for (Long userId : userIds) {
            User user = users.get(userId);
            if (user != null) {
                members.add(Map.of("value", String.valueOf(user.getId()), "display", user.getUsername()));
            }
        }
        resource.put("members", members);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("resourceType", "Group");
        meta.put("version", ScimVersion.of(role.getId(), role.getName(), memberIdsOfRole(role.getId())));
        meta.put("location", "/scim/v2/Groups/" + role.getId());
        resource.put("meta", meta);
        return resource;
    }

    private static String text(JsonNode body, String field) {
        JsonNode value = body.path(field);
        return value.isTextual() ? value.asText() : null;
    }
}
