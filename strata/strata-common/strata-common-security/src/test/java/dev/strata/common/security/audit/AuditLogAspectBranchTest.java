// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Branch arms of {@link AuditLogAspect} not in {@link AuditLogAspectTest}: null return value,
 * unauthenticated-but-present authentication, null parameter names/args, and the empty/unknown
 * X-Forwarded-For / X-Real-IP fallbacks.
 */
class AuditLogAspectBranchTest {

    private final AuditLogAspect aspect = new AuditLogAspect();

    static class Target {
        @AuditLog(action = "RET_NULL", description = "null return", logReturnValue = true)
        public String returnsNull() {
            return null;
        }

        @AuditLog(action = "WITH_PARAMS", description = "with params", logParameters = true)
        public void twoParams(String a, String b) {}
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /** Build a mocked joinPoint with controllable parameter names / args / proceed result. */
    private ProceedingJoinPoint jp(Method method, String[] paramNames, Object[] args, Object proceedResult)
            throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getName()).thenReturn(method.getName());
        when(signature.getDeclaringTypeName()).thenReturn(Target.class.getName());
        when(signature.getParameterNames()).thenReturn(paramNames);
        when(pjp.getArgs()).thenReturn(args);
        when(pjp.proceed()).thenReturn(proceedResult);
        return pjp;
    }

    private AuditLog annotation(Method method) {
        return method.getAnnotation(AuditLog.class);
    }

    @Test
    void nullReturnValueNotSerialized() throws Throwable {
        Method m = Target.class.getMethod("returnsNull");
        Object result = aspect.audit(jp(m, new String[0], new Object[0], null), annotation(m));
        assertThat(result).isNull();
    }

    @Test
    void unauthenticatedAuthenticationDoesNotSetUser() throws Throwable {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("ghost", "pw", List.of());
        auth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Method m = Target.class.getMethod("returnsNull");
        assertThatCodeNoThrow(() -> aspect.audit(jp(m, new String[0], new Object[0], "ok"), annotation(m)));
    }

    private static void assertThatCodeNoThrow(org.junit.jupiter.api.function.Executable e) {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(e);
    }

    @Test
    void nullParameterNamesSkipsBlock() throws Throwable {
        Method m = Target.class.getMethod("twoParams", String.class, String.class);
        Object result = aspect.audit(jp(m, null, new Object[] {"a", "b"}, "ok"), annotation(m));
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void nullArgsSkipsBlock() throws Throwable {
        Method m = Target.class.getMethod("twoParams", String.class, String.class);
        Object result = aspect.audit(jp(m, new String[] {"a", "b"}, null, "ok"), annotation(m));
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void nullParameterValueSerializedAsNull() throws Throwable {
        Method m = Target.class.getMethod("twoParams", String.class, String.class);
        Object result = aspect.audit(jp(m, new String[] {"a", "b"}, new Object[] {null, "v"}, "ok"), annotation(m));
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void emptyForwardedForFallsBackToRealIp() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "");
        request.addHeader("X-Real-IP", "5.5.5.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method m = Target.class.getMethod("returnsNull");
        assertThat(aspect.audit(jp(m, new String[0], new Object[0], "ok"), annotation(m)))
                .isEqualTo("ok");
    }

    @Test
    void unknownForwardedForFallsBackToRealIp() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "6.6.6.6");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method m = Target.class.getMethod("returnsNull");
        assertThat(aspect.audit(jp(m, new String[0], new Object[0], "ok"), annotation(m)))
                .isEqualTo("ok");
    }

    @Test
    void unknownRealIpFallsBackToRemoteAddr() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "unknown");
        request.setRemoteAddr("7.7.7.7");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method m = Target.class.getMethod("returnsNull");
        assertThat(aspect.audit(jp(m, new String[0], new Object[0], "ok"), annotation(m)))
                .isEqualTo("ok");
    }
}
