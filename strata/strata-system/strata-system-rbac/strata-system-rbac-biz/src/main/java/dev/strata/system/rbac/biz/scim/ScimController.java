// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SCIM 2.0 provisioning endpoints (RFC 7644) for identity providers such as Keycloak, Okta or Entra ID.
 *
 * <p>Machine-to-machine: the endpoints are permit-listed in the security chain and authenticated solely by the
 * bearer token checked in {@link ScimAuthFilter}. Supported: Users and Groups CRUD, PATCH (add/replace/remove),
 * simple {@code eq} filters and pagination. Not supported: bulk operations, ETags/If-Match and complex filters.
 */
@RestController
@RequestMapping(path = "/scim/v2", produces = "application/scim+json")
@ConditionalOnProperty(prefix = "strata.scim", name = "enabled", havingValue = "true")
public class ScimController {

    private static final String ERROR_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";

    private final ScimUsers users;
    private final ScimGroups groups;

    public ScimController(ScimUsers users, ScimGroups groups) {
        this.users = users;
        this.groups = groups;
    }

    @GetMapping("/ServiceProviderConfig")
    public Map<String, Object> serviceProviderConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"));
        config.put("patch", Map.of("supported", true));
        config.put("filter", Map.of("supported", true, "maxResults", 200));
        config.put("changePassword", Map.of("supported", true));
        return config;
    }

    @GetMapping("/Users")
    public Map<String, Object> listUsers(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") int startIndex,
            @RequestParam(defaultValue = "0") int count) {
        return users.list(filter, startIndex, count);
    }

    @PostMapping("/Users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(users.create(body));
    }

    @GetMapping("/Users/{id}")
    public Map<String, Object> getUser(@PathVariable String id) {
        return users.get(id);
    }

    @PutMapping("/Users/{id}")
    public Map<String, Object> replaceUser(@PathVariable String id, @RequestBody JsonNode body) {
        return users.replace(id, body);
    }

    @PatchMapping("/Users/{id}")
    public Map<String, Object> patchUser(@PathVariable String id, @RequestBody JsonNode body) {
        return users.patch(id, body);
    }

    @DeleteMapping("/Users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        users.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/Groups")
    public Map<String, Object> listGroups(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") int startIndex,
            @RequestParam(defaultValue = "0") int count) {
        return groups.list(filter, startIndex, count);
    }

    @PostMapping("/Groups")
    public ResponseEntity<Map<String, Object>> createGroup(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groups.create(body));
    }

    @GetMapping("/Groups/{id}")
    public Map<String, Object> getGroup(@PathVariable String id) {
        return groups.get(id);
    }

    @PutMapping("/Groups/{id}")
    public Map<String, Object> replaceGroup(@PathVariable String id, @RequestBody JsonNode body) {
        return groups.replace(id, body);
    }

    @PatchMapping("/Groups/{id}")
    public Map<String, Object> patchGroup(@PathVariable String id, @RequestBody JsonNode body) {
        return groups.patch(id, body);
    }

    @DeleteMapping("/Groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String id) {
        groups.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ScimException.class)
    public ResponseEntity<Map<String, Object>> handleScimError(ScimException exception) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("schemas", List.of(ERROR_SCHEMA));
        error.put("status", String.valueOf(exception.status().value()));
        error.put("detail", exception.getMessage());
        if (exception.scimType() != null) {
            error.put("scimType", exception.scimType());
        }
        return ResponseEntity.status(exception.status()).body(error);
    }
}
