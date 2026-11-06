// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect that automatically logs security audit events for methods annotated with {@link AuditLog}.
 *
 * <p>Records the following information for each audited operation:
 * <ul>
 *   <li>Timestamp (ISO 8601)</li>
 *   <li>User identity (username, user ID)</li>
 *   <li>Action and description</li>
 *   <li>Method parameters (optional)</li>
 *   <li>Return value (optional)</li>
 *   <li>IP address and user agent</li>
 *   <li>Execution duration</li>
 *   <li>Success/failure status</li>
 * </ul>
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(auditLog)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        AuditEvent event = buildEvent(joinPoint, auditLog);

        try {
            Object result = joinPoint.proceed();
            event.setStatus("SUCCESS");
            if (auditLog.logReturnValue() && result != null) {
                event.setReturnValue(safeSerialize(result));
            }
            return result;
        } catch (Exception e) {
            event.setStatus("FAILURE");
            event.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            event.setDurationMs(System.currentTimeMillis() - startTime);
            event.setTimestamp(Instant.now().toString());
            writeAuditLog(event);
        }
    }

    private AuditEvent buildEvent(ProceedingJoinPoint joinPoint, AuditLog auditLog) {
        AuditEvent event = new AuditEvent();
        event.setAction(auditLog.action());
        event.setDescription(auditLog.description());
        event.setSensitive(auditLog.sensitive());
        event.setCategory(auditLog.category());

        // User identity
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            event.setUsername(authentication.getName());
        }

        // Request details
        HttpServletRequest request = getRequest();
        if (request != null) {
            event.setIpAddress(getClientIp(request));
            event.setUserAgent(request.getHeader("User-Agent"));
            event.setRequestUri(request.getRequestURI());
            event.setHttpMethod(request.getMethod());
        }

        // Method details
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        event.setClassName(signature.getDeclaringTypeName());
        event.setMethodName(signature.getName());

        // Parameters
        if (auditLog.logParameters()) {
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            if (paramNames != null && args != null) {
                Map<String, Object> params = new HashMap<>();
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    params.put(paramNames[i], safeSerialize(args[i]));
                }
                event.setParameters(params);
            }
        }

        return event;
    }

    private void writeAuditLog(AuditEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            auditLogger.info(json);
        } catch (Exception e) {
            // Fallback: log as plain text
            auditLogger.info(
                    "AUDIT: action={}, user={}, status={}, duration={}ms",
                    event.getAction(),
                    event.getUsername(),
                    event.getStatus(),
                    event.getDurationMs());
        }
    }

    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private Object safeSerialize(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(obj, Object.class);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    /**
     * Internal audit event data structure.
     */
    public static class AuditEvent {
        private String timestamp;
        private String username;
        private String action;
        private String description;
        private boolean sensitive;
        private String category;
        private String status;
        private String errorMessage;
        private long durationMs;
        private String ipAddress;
        private String userAgent;
        private String requestUri;
        private String httpMethod;
        private String className;
        private String methodName;
        private Map<String, Object> parameters;
        private Object returnValue;

        // Getters and setters
        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isSensitive() {
            return sensitive;
        }

        public void setSensitive(boolean sensitive) {
            this.sensitive = sensitive;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public String getRequestUri() {
            return requestUri;
        }

        public void setRequestUri(String requestUri) {
            this.requestUri = requestUri;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        public Object getReturnValue() {
            return returnValue;
        }

        public void setReturnValue(Object returnValue) {
            this.returnValue = returnValue;
        }
    }
}
