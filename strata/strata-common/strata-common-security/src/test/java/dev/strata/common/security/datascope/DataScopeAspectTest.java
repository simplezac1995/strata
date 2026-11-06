// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.datascope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DataScopeAspect 数据范围切面")
class DataScopeAspectTest {

    private DataScopeProvider provider;
    private DataScopeAspect aspect;

    @BeforeEach
    void setUp() {
        provider = mock(DataScopeProvider.class);
        aspect = new DataScopeAspect(provider);
        lenient().when(provider.getCurrentUserId()).thenReturn(1L);
    }

    @AfterEach
    void clearContext() {
        DataScopeContext.clear();
    }

    /** Fixture with a method-level @DataScope and one unannotated method. */
    static class ScopedTarget {
        @DataScope(deptField = "deptId", userField = "creatorId")
        public Object scoped() {
            return "ok";
        }

        public Object unscoped() {
            return "ok";
        }
    }

    /** Fixture carrying a class-level @DataScope. */
    @DataScope(deptField = "deptId", userField = "creatorId")
    static class ClassScopedTarget {
        public Object method() {
            return "ok";
        }
    }

    private ProceedingJoinPoint joinPointFor(Object target, String methodName) throws Exception {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = target.getClass().getMethod(methodName);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getTarget()).thenReturn(target);
        when(signature.getMethod()).thenReturn(method);
        return pjp;
    }

    private AtomicReference<DataScopeContext> captureContextDuringProceed(ProceedingJoinPoint pjp) throws Throwable {
        AtomicReference<DataScopeContext> seen = new AtomicReference<>();
        when(pjp.proceed()).thenAnswer(invocation -> {
            seen.set(DataScopeContext.get());
            return "ok";
        });
        return seen;
    }

    @Test
    @DisplayName("无 @DataScope 注解时直接放行，不解析数据范围")
    void proceedsWithoutAnnotation() throws Throwable {
        ScopedTarget target = new ScopedTarget();
        ProceedingJoinPoint pjp = joinPointFor(target, "unscoped");
        AtomicReference<DataScopeContext> seen = captureContextDuringProceed(pjp);

        Object result = aspect.around(pjp);

        assertThat(result).isEqualTo("ok");
        assertThat(seen.get()).isNull();
        verify(provider, never()).getCurrentUserId();
    }

    @Test
    @DisplayName("未认证（无用户 id）时放行且不设置过滤上下文")
    void proceedsWhenUnauthenticated() throws Throwable {
        when(provider.getCurrentUserId()).thenReturn(null);
        ProceedingJoinPoint pjp = joinPointFor(new ScopedTarget(), "scoped");
        AtomicReference<DataScopeContext> seen = captureContextDuringProceed(pjp);

        aspect.around(pjp);

        assertThat(seen.get()).isNull();
        verify(provider, never()).resolveScope(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("ALL 范围在执行期间设置全部数据上下文")
    void appliesAllScope() throws Throwable {
        when(provider.resolveScope(1L)).thenReturn(DataScopeType.ALL);
        ProceedingJoinPoint pjp = joinPointFor(new ScopedTarget(), "scoped");
        AtomicReference<DataScopeContext> seen = captureContextDuringProceed(pjp);

        aspect.around(pjp);

        assertThat(seen.get()).isNotNull();
        assertThat(seen.get().isAllData()).isTrue();
        assertThat(DataScopeContext.get()).isNull();
    }

    @Test
    @DisplayName("SELF_ONLY 范围携带本人 id 与用户字段")
    void appliesSelfScope() throws Throwable {
        when(provider.resolveScope(1L)).thenReturn(DataScopeType.SELF_ONLY);
        ProceedingJoinPoint pjp = joinPointFor(new ScopedTarget(), "scoped");
        AtomicReference<DataScopeContext> seen = captureContextDuringProceed(pjp);

        aspect.around(pjp);

        assertThat(seen.get().isSelfFiltered()).isTrue();
        assertThat(seen.get().getUserId()).isEqualTo(1L);
        assertThat(seen.get().getUserField()).isEqualTo("creatorId");
        assertThat(DataScopeContext.get()).isNull();
    }

    @Test
    @DisplayName("DEPT_ONLY 解析单个部门")
    void appliesDeptOnlyScope() throws Throwable {
        when(provider.resolveScope(1L)).thenReturn(DataScopeType.DEPT_ONLY);
        when(provider.resolveDeptId(1L)).thenReturn(8L);
        ProceedingJoinPoint pjp = joinPointFor(new ScopedTarget(), "scoped");
        AtomicReference<DataScopeContext> seen = captureContextDuringProceed(pjp);

        aspect.around(pjp);

        assertThat(seen.get().isDeptFiltered()).isTrue();
        assertThat(seen.get().getDeptIds()).containsExactly(8L);
        assertThat(DataScopeContext.get()).isNull();
    }

    @Test
    @DisplayName("DEPT_AND_CHILDREN 解析部门及子部门集合")
    void appliesDeptAndChildrenScope() throws Throwable {
        when(provider.resolveScope(1L)).thenReturn(DataScopeType.DEPT_AND_CHILDREN);
        when(provider.resolveDeptId(1L)).thenReturn(5L);
        when(provider.resolveDeptAndChildren(5L)).thenReturn(Set.of(5L, 6L, 7L));
        ProceedingJoinPoint pjp = joinPointFor(new ScopedTarget(), "scoped");
        AtomicReference<DataScopeContext> seen = captureContextDuringProceed(pjp);

        aspect.around(pjp);

        assertThat(seen.get().isDeptFiltered()).isTrue();
        assertThat(seen.get().getDeptIds()).containsExactlyInAnyOrder(5L, 6L, 7L);
        assertThat(DataScopeContext.get()).isNull();
    }

    @Test
    @DisplayName("CUSTOM 范围使用自定义部门集合")
    void appliesCustomScope() throws Throwable {
        when(provider.resolveScope(1L)).thenReturn(DataScopeType.CUSTOM);
        when(provider.resolveCustomDeptIds(1L)).thenReturn(Set.of(9L, 10L));
        ProceedingJoinPoint pjp = joinPointFor(new ScopedTarget(), "scoped");
        AtomicReference<DataScopeContext> seen = captureContextDuringProceed(pjp);

        aspect.around(pjp);

        assertThat(seen.get().isDeptFiltered()).isTrue();
        assertThat(seen.get().getDeptIds()).containsExactlyInAnyOrder(9L, 10L);
        assertThat(DataScopeContext.get()).isNull();
    }

    @Test
    @DisplayName("业务方法抛异常时上下文仍在 finally 中清理")
    void clearsContextWhenProceedThrows() throws Throwable {
        when(provider.resolveScope(1L)).thenReturn(DataScopeType.SELF_ONLY);
        ProceedingJoinPoint pjp = joinPointFor(new ScopedTarget(), "scoped");
        when(pjp.proceed()).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> aspect.around(pjp)).isInstanceOf(RuntimeException.class);
        assertThat(DataScopeContext.get()).isNull();
    }

    @Test
    @DisplayName("类级 @DataScope 注解同样生效（方法无注解时回退到类）")
    void fallsBackToClassLevelAnnotation() throws Throwable {
        when(provider.resolveScope(1L)).thenReturn(DataScopeType.ALL);
        ProceedingJoinPoint pjp = joinPointFor(new ClassScopedTarget(), "method");
        AtomicReference<DataScopeContext> seen = captureContextDuringProceed(pjp);

        aspect.around(pjp);

        assertThat(seen.get()).isNotNull();
        assertThat(seen.get().isAllData()).isTrue();
    }
}
