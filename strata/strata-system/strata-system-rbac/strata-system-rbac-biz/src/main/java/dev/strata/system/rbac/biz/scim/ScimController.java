// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SCIM 2.0 provisioning endpoints (RFC 7644) for identity providers such as Okta, Entra ID or OneLogin.
 *
 * <p>Machine-to-machine: permit-listed in the security chain and authenticated solely by the bearer token checked in
 * {@link ScimAuthFilter}. Supported: Users/Groups CRUD, PATCH, ETag/If-Match, complex filters, bulk up to 100
 * operations. Not supported: extension schemas and sorting.
 */
@RestController
@RequestMapping(path = "/scim/v2", produces = "application/scim+json")
@ConditionalOnProperty(prefix = "strata.scim", name = "enabled", havingValue = "true")
public class ScimController {

    private static final String ERROR_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";
    private static final String BULK_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:BulkResponse";
    private static final int BULK_LIMIT = 100;
    private static final Pattern RESOURCE_PATH = Pattern.compile("^/(Users|Groups)/([^/]+)$");

    private final ScimUsers users;
    private final ScimGroups groups;
    private final ObjectMapper json = new ObjectMapper();

    public ScimController(ScimUsers users, ScimGroups groups) {
        this.users = users;
        this.groups = groups;
    }

    @GetMapping("/ServiceProviderConfig")
    public Map<String, Object> serviceProviderConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"));
        config.put("patch", Map.of("supported", true));
        config.put("bulk", Map.of("supported", true, "maxOperations", BULK_LIMIT, "maxPayloadSize", 1048576));
        config.put("filter", Map.of("supported", true, "maxResults", 200));
        config.put("etag", Map.of("supported", true));
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
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        return versioned(users.create(asJson(body)), HttpStatus.CREATED);
    }

    @GetMapping("/Users/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String id) {
        return versioned(users.get(id), HttpStatus.OK);
    }

    @PutMapping("/Users/{id}")
    public ResponseEntity<Map<String, Object>> replaceUser(
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody Map<String, Object> body) {
        requireCurrentVersion(users.get(id), ifMatch);
        return versioned(users.replace(id, asJson(body)), HttpStatus.OK);
    }

    @PatchMapping("/Users/{id}")
    public ResponseEntity<Map<String, Object>> patchUser(
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody Map<String, Object> body) {
        requireCurrentVersion(users.get(id), ifMatch);
        return versioned(users.patch(id, asJson(body)), HttpStatus.OK);
    }

    @DeleteMapping("/Users/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable String id, @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        requireCurrentVersion(users.get(id), ifMatch);
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
    public ResponseEntity<Map<String, Object>> createGroup(@RequestBody Map<String, Object> body) {
        return versioned(groups.create(asJson(body)), HttpStatus.CREATED);
    }

    @GetMapping("/Groups/{id}")
    public ResponseEntity<Map<String, Object>> getGroup(@PathVariable String id) {
        return versioned(groups.get(id), HttpStatus.OK);
    }

    @PutMapping("/Groups/{id}")
    public ResponseEntity<Map<String, Object>> replaceGroup(
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody Map<String, Object> body) {
        requireCurrentVersion(groups.get(id), ifMatch);
        return versioned(groups.replace(id, asJson(body)), HttpStatus.OK);
    }

    @PatchMapping("/Groups/{id}")
    public ResponseEntity<Map<String, Object>> patchGroup(
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody Map<String, Object> body) {
        requireCurrentVersion(groups.get(id), ifMatch);
        return versioned(groups.patch(id, asJson(body)), HttpStatus.OK);
    }

    @DeleteMapping("/Groups/{id}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable String id, @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        requireCurrentVersion(groups.get(id), ifMatch);
        groups.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Bulk operations: each entry is dispatched through the same services; failures are reported per operation. */
    @PostMapping("/Bulk")
    public Map<String, Object> bulk(@RequestBody Map<String, Object> body) {
        JsonNode operations = asJson(body).path("Operations");
        if (!operations.isArray()) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "Operations must be an array", "invalidValue");
        }
        if (operations.size() > BULK_LIMIT) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "Too many bulk operations", "tooMany");
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (JsonNode operation : operations) {
            String method = operation.path("method").asText("").toUpperCase();
            String bulkId = operation.path("bulkId").asText("");
            String path = operation.path("path").asText("");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("method", method);
            result.put("bulkId", bulkId);
            try {
                BulkResult outcome = dispatch(method, path, operation.path("data"));
                result.put("status", String.valueOf(outcome.status()));
                if (outcome.response() != null) {
                    result.put("response", outcome.response());
                }
            } catch (ScimException exception) {
                result.put("status", String.valueOf(exception.status().value()));
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("schemas", List.of(ERROR_SCHEMA));
                error.put("status", String.valueOf(exception.status().value()));
                error.put("detail", exception.getMessage());
                if (exception.scimType() != null) {
                    error.put("scimType", exception.scimType());
                }
                result.put("response", error);
            }
            results.add(result);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schemas", List.of(BULK_SCHEMA));
        response.put("Operations", results);
        return response;
    }

    private BulkResult dispatch(String method, String rawPath, JsonNode data) {
        String path = rawPath == null ? "" : rawPath.split("\\?")[0];
        Matcher matcher = RESOURCE_PATH.matcher(path);
        boolean collection;
        String resource;
        String id;
        if (path.equals("/Users") || path.equals("/Groups")) {
            collection = true;
            resource = path.substring(1);
            id = null;
        } else if (matcher.matches()) {
            collection = false;
            resource = matcher.group(1);
            id = matcher.group(2);
        } else {
            throw new ScimException(HttpStatus.BAD_REQUEST, "Unsupported bulk path: " + path, "invalidValue");
        }
        return switch (method) {
            case "GET" -> {
                if (collection) {
                    yield new BulkResult(
                            HttpStatus.OK.value(),
                            "Users".equals(resource) ? users.list(query(path), 1, 0) : groups.list(query(path), 1, 0));
                }
                yield new BulkResult(HttpStatus.OK.value(), "Users".equals(resource) ? users.get(id) : groups.get(id));
            }
            case "POST" ->
                new BulkResult(
                        HttpStatus.CREATED.value(),
                        "Users".equals(resource) ? users.create(require(data)) : groups.create(require(data)));
            case "PUT" ->
                new BulkResult(
                        HttpStatus.OK.value(),
                        "Users".equals(resource)
                                ? users.replace(id, require(data))
                                : groups.replace(id, require(data)));
            case "PATCH" ->
                new BulkResult(
                        HttpStatus.OK.value(),
                        "Users".equals(resource) ? users.patch(id, require(data)) : groups.patch(id, require(data)));
            case "DELETE" -> {
                if ("Users".equals(resource)) {
                    users.delete(id);
                } else {
                    groups.delete(id);
                }
                yield new BulkResult(HttpStatus.NO_CONTENT.value(), null);
            }
            default ->
                throw new ScimException(HttpStatus.BAD_REQUEST, "Unsupported bulk method: " + method, "invalidValue");
        };
    }

    private static JsonNode require(JsonNode data) {
        if (data == null || data.isMissingNode() || data.isNull()) {
            throw new ScimException(HttpStatus.BAD_REQUEST, "Bulk operation requires data", "invalidValue");
        }
        return data;
    }

    private static String query(String path) {
        int index = path.indexOf('?');
        if (index < 0) {
            return "";
        }
        for (String pair : path.substring(index + 1).split("&")) {
            if (pair.startsWith("filter=")) {
                return URLDecoder.decode(pair.substring("filter=".length()), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private ResponseEntity<Map<String, Object>> versioned(Map<String, Object> resource, HttpStatus status) {
        return ResponseEntity.status(status).eTag(version(resource)).body(resource);
    }

    private void requireCurrentVersion(Map<String, Object> resource, String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank() || "*".equals(ifMatch.trim())) {
            return;
        }
        if (!normalizeTag(ifMatch).equals(normalizeTag(version(resource)))) {
            throw new ScimException(HttpStatus.PRECONDITION_FAILED, "If-Match does not match the current version");
        }
    }

    private static String normalizeTag(String tag) {
        String value = tag.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        if (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String version(Map<String, Object> resource) {
        Object meta = resource.get("meta");
        return meta instanceof Map<?, ?> map ? String.valueOf(map.get("version")) : "";
    }

    /** Jackson 3 (Spring MVC) deserializes into a Map; the SCIM services use Jackson 2 JsonNode, so bridge here. */
    private JsonNode asJson(Map<String, Object> body) {
        return json.valueToTree(body);
    }

    private record BulkResult(int status, Map<String, Object> response) {}

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
