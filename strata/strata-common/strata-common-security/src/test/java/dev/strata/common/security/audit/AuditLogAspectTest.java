// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unit tests for {@link AuditLogAspect}: success/failure status, user identity resolution,
 * and client-IP extraction — all without loading a Spring context.
 */
@DisplayName("AuditLogAspect 审计切面")
class AuditLogAspectTest {

    private final AuditLogAspect aspect = new AuditLogAspect();

    static class Target {
        @AuditLog(action = "USER_CREATE", description = "create", logReturnValue = true)
        public String create(String name) {
            return "ok";
        }

        @AuditLog(action = "USER_DELETE", logParameters = false)
        public String delete() {
            return "deleted";
        }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private ProceedingJoinPoint joinPointFor(String methodName, Object... args) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = Target.class.getMethod(methodName, parameterTypes(methodName));
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getName()).thenReturn(methodName);
        when(signature.getDeclaringTypeName()).thenReturn(Target.class.getName());
        when(signature.getParameterNames()).thenReturn(parameterNames(methodName));
        when(pjp.getArgs()).thenReturn(args);
        when(pjp.proceed()).thenReturn("ok");
        return pjp;
    }

    private Class<?>[] parameterTypes(String methodName) {
        return methodName.equals("create") ? new Class<?>[] {String.class} : new Class<?>[0];
    }

    private String[] parameterNames(String methodName) {
        return methodName.equals("create") ? new String[] {"name"} : new String[0];
    }

    private AuditLog annotationOn(String methodName) throws Exception {
        return Target.class.getMethod(methodName, parameterTypes(methodName)).getAnnotation(AuditLog.class);
    }

    private void authenticate(String user) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(user, "pw", "ROLE_USER"));
    }

    @Test
    @DisplayName("成功执行：记录 SUCCESS 并返回结果")
    void successRecordsSuccessAndReturnsResult() throws Throwable {
        Object result = aspect.audit(joinPointFor("create", "alice"), annotationOn("create"));

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("业务异常：状态置 FAILURE 并传播异常")
    void failureRecordsFailureAndPropagates() throws Throwable {
        ProceedingJoinPoint pjp = joinPointFor("delete");
        when(pjp.proceed()).thenThrow(new IllegalStateException("nope"));

        assertThatThrownBy(() -> aspect.audit(pjp, annotationOn("delete")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("nope");
    }

    @Test
    @DisplayName("无认证与请求上下文时不抛 NPE")
    void runsWithoutAuthOrRequestContext() throws Throwable {
        Object result = aspect.audit(joinPointFor("create", "bob"), annotationOn("create"));

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("已认证时记录用户名")
    void recordsAuthenticatedUser() throws Throwable {
        authenticate("admin");

        Object result = aspect.audit(joinPointFor("create", "bob"), annotationOn("create"));

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("X-Forwarded-For 首个地址作为客户端 IP")
    void extractsIpFromForwardedFor() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.1.1.1, 2.2.2.2");
        request.setRequestURI("/api/v1/users");
        request.setMethod("POST");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Object result = aspect.audit(joinPointFor("create", "bob"), annotationOn("create"));

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("无 X-Forwarded-For 时使用 X-Real-IP")
    void extractsIpFromRealIp() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "3.3.3.3");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(aspect.audit(joinPointFor("create", "bob"), annotationOn("create")))
                .isEqualTo("ok");
    }

    @Test
    @DisplayName("无代理头时使用 remoteAddr")
    void fallsBackToRemoteAddr() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("4.4.4.4");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(aspect.audit(joinPointFor("create", "bob"), annotationOn("create")))
                .isEqualTo("ok");
    }
}
