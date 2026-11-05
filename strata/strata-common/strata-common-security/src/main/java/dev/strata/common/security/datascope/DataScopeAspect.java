// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.datascope;

import java.lang.reflect.Method;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * AOP aspect that resolves and applies data scope for methods annotated with {@link DataScope}.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Find {@link DataScope} annotation on method or class</li>
 *   <li>Resolve current user's data scope via {@link DataScopeProvider}</li>
 *   <li>Store scope in {@link DataScopeContext}</li>
 *   <li>Proceed with method execution</li>
 *   <li>Clear context after execution</li>
 * </ol>
 *
 * <p>The actual data filtering is done by repositories/queries that read from
 * {@link DataScopeContext}.
 */
@Aspect
public class DataScopeAspect {

    private static final Logger log = LoggerFactory.getLogger(DataScopeAspect.class);

    private final DataScopeProvider dataScopeProvider;

    public DataScopeAspect(DataScopeProvider dataScopeProvider) {
        this.dataScopeProvider = dataScopeProvider;
    }

    @Around("@annotation(dev.strata.common.security.datascope.DataScope) || "
            + "@within(dev.strata.common.security.datascope.DataScope)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // Find @DataScope annotation (method-level takes precedence over class-level)
        DataScope dataScope = AnnotatedElementUtils.findMergedAnnotation(method, DataScope.class);
        if (dataScope == null) {
            dataScope = AnnotatedElementUtils.findMergedAnnotation(targetClass, DataScope.class);
        }

        if (dataScope == null) {
            return joinPoint.proceed();
        }

        Long userId = dataScopeProvider.getCurrentUserId();
        if (userId == null) {
            // Not authenticated - proceed without data scope filtering
            log.debug(
                    "No authenticated user, skipping data scope for {}.{}",
                    targetClass.getSimpleName(),
                    method.getName());
            return joinPoint.proceed();
        }

        try {
            DataScopeContext context = resolveContext(userId, dataScope);
            DataScopeContext.set(context);

            log.debug(
                    "Data scope applied: user={}, scope={}, method={}.{}",
                    userId,
                    context.getScopeType(),
                    targetClass.getSimpleName(),
                    method.getName());

            return joinPoint.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }

    private DataScopeContext resolveContext(Long userId, DataScope dataScope) {
        String deptField = dataScope.deptField();
        String userField = dataScope.userField();

        DataScopeType scopeType = dataScopeProvider.resolveScope(userId);

        return switch (scopeType) {
            case ALL -> DataScopeContext.allData(userId, deptField, userField);
            case SELF_ONLY -> DataScopeContext.selfOnly(userId, deptField, userField);
            case DEPT_ONLY -> {
                Long deptId = dataScopeProvider.resolveDeptId(userId);
                yield DataScopeContext.deptScoped(scopeType, userId, deptId, Set.of(deptId), deptField, userField);
            }
            case DEPT_AND_CHILDREN -> {
                Long deptId = dataScopeProvider.resolveDeptId(userId);
                Set<Long> deptIds = dataScopeProvider.resolveDeptAndChildren(deptId);
                yield DataScopeContext.deptScoped(scopeType, userId, deptId, deptIds, deptField, userField);
            }
            case CUSTOM -> {
                Long deptId = dataScopeProvider.resolveDeptId(userId);
                Set<Long> deptIds = dataScopeProvider.resolveCustomDeptIds(userId);
                yield DataScopeContext.deptScoped(scopeType, userId, deptId, deptIds, deptField, userField);
            }
        };
    }
}
