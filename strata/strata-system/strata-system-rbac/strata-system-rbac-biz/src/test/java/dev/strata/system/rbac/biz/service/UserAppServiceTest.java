// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.security.tenant.TenantProperties;
import dev.strata.system.api.dto.UserDto;
import dev.strata.system.api.request.UserCreateRequest;
import dev.strata.system.api.request.UserUpdateRequest;
import dev.strata.system.rbac.api.entity.User;
import dev.strata.system.rbac.biz.mapper.UserMapper;
import dev.strata.system.rbac.biz.metrics.RbacMetrics;
import dev.strata.system.rbac.biz.repository.UserRepository;
import dev.strata.system.rbac.biz.repository.UserRoleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "varargs"})
class UserAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TenantProperties tenantProperties;

    @Mock
    private RbacMetrics rbacMetrics;

    @InjectMocks
    private UserAppService userAppService;

    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setNickname("Test User");
        testUser.setEmail("test@example.com");
        testUser.setEnabled(true);
        testUser.setDeleted(false);
        testUser.setPasswordHash("encoded_password");

        testUserDto = new UserDto("1", "testuser", "Test User", "test@example.com", true, List.of("1"), null);
    }

    @Nested
    class CreateUserTests {

        @Test
        void createUser_Success() {
            UserCreateRequest request = new UserCreateRequest("newuser", "password123", "New User", true, List.of("1"));

            when(userRepository.existsByUsernameAndDeletedFalse("newuser")).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(testUser);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_new_password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
            when(userMapper.toDto(any(User.class), anyList())).thenReturn(testUserDto);

            UserDto result = userAppService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.username()).isEqualTo("testuser");
            verify(userRepository).save(any(User.class));
            verify(userRoleRepository).deleteByUserId(1L);
            verify(userRoleRepository).saveAll(anyList());
            verify(rbacMetrics).recordUserCreated();
        }

        @Test
        void createUser_UsernameExists_ThrowsException() {
            UserCreateRequest request =
                    new UserCreateRequest("existinguser", "password123", "Existing User", true, List.of());

            when(userRepository.existsByUsernameAndDeletedFalse("existinguser")).thenReturn(true);

            assertThatThrownBy(() -> userAppService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });

            verify(userRepository, never()).save(any(User.class));
            verify(rbacMetrics, never()).recordUserCreated();
        }

        @Test
        void createUser_PasswordIsEncoded() {
            UserCreateRequest request = new UserCreateRequest("newuser", "plainpassword", "New User", true, List.of());

            when(userRepository.existsByUsernameAndDeletedFalse("newuser")).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(testUser);
            when(passwordEncoder.encode("plainpassword")).thenReturn("encrypted_password_hash");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
            when(userMapper.toDto(any(User.class), anyList())).thenReturn(testUserDto);

            userAppService.create(request);

            verify(passwordEncoder).encode("plainpassword");
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    class UpdateUserTests {

        @Test
        void updateUser_Success() {
            UserUpdateRequest request = new UserUpdateRequest("Updated User", true, null, List.of("2"));

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
            when(userMapper.toDto(any(User.class), anyList())).thenReturn(testUserDto);

            UserDto result = userAppService.update(1L, request);

            assertThat(result).isNotNull();
            verify(userMapper).update(eq(request), any(User.class));
            verify(userRepository).save(any(User.class));
            verify(userRoleRepository).deleteByUserId(1L);
            verify(userRoleRepository).saveAll(anyList());
            verify(rbacMetrics).recordUserUpdated();
        }

        @Test
        void updateUser_NotFound_ThrowsException() {
            UserUpdateRequest request = new UserUpdateRequest("Updated User", true, null, List.of());

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userAppService.update(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void updateUser_WithNewPassword_EncodesPassword() {
            UserUpdateRequest request = new UserUpdateRequest("Updated User", true, "newpassword", List.of());

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newpassword")).thenReturn("new_encoded_password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
            when(userMapper.toDto(any(User.class), anyList())).thenReturn(testUserDto);

            userAppService.update(1L, request);

            verify(passwordEncoder).encode("newpassword");
        }

        @Test
        void updateUser_WithoutPassword_DoesNotChangePassword() {
            UserUpdateRequest request = new UserUpdateRequest("Updated User", true, null, List.of());

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
            when(userMapper.toDto(any(User.class), anyList())).thenReturn(testUserDto);

            userAppService.update(1L, request);

            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    @Nested
    class DeleteUserTests {

        @Test
        void deleteUser_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userAppService.delete(1L);

            assertThat(testUser.isDeleted()).isTrue();
            assertThat(testUser.isEnabled()).isFalse();
            verify(userRepository).save(testUser);
            verify(rbacMetrics).recordUserDeleted();
        }

        @Test
        void deleteUser_NotFound_ThrowsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userAppService.delete(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void deleteBatch_Success() {
            User user2 = new User();
            user2.setId(2L);
            user2.setUsername("user2");
            user2.setEnabled(true);
            user2.setDeleted(false);
            // P2-21 + tenant isolation: the ids are resolved through the tenant-scoped spec first.
            when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(testUser, user2));
            when(userRepository.softDeleteByIds(anyList())).thenReturn(2);

            userAppService.deleteBatch(List.of(1L, 2L));

            verify(userRepository).findAll(any(Specification.class));
            verify(userRepository).softDeleteByIds(List.of(1L, 2L));
            verify(userRepository, never()).findById(anyLong());
            verify(userRepository, never()).save(any(User.class));
            verify(rbacMetrics).recordUserDeleted();
        }

        @Test
        void deleteBatch_ContainsInvisibleId_ThrowsNotFound() {
            // only one of the two ids is visible in the current tenant
            when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(testUser));

            assertThatThrownBy(() -> userAppService.deleteBatch(List.of(1L, 999L)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
            verify(userRepository, never()).softDeleteByIds(anyList());
            verify(rbacMetrics, never()).recordUserDeleted();
        }

        @Test
        void deleteBatch_EmptyList_DoesNothing() {
            userAppService.deleteBatch(List.of());

            verify(userRepository, never()).findById(anyLong());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    class AnonymizeUserTests {

        @Test
        void anonymize_Success() {
            testUser.setNickname("Real Name");
            testUser.setPhone("13800000000");
            testUser.setAvatar("https://cdn.example/me.png");
            testUser.setEmail("real@example.com");
            testUser.setResetToken("raw-reset-token");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userAppService.anonymize(1L);

            assertThat(testUser.getUsername()).isEqualTo("deleted_1");
            assertThat(testUser.getEmail()).isEqualTo("deleted_1@anonymized.local");
            assertThat(testUser.getNickname()).isNull();
            assertThat(testUser.getPhone()).isNull();
            assertThat(testUser.getAvatar()).isNull();
            assertThat(testUser.getResetToken()).isNull();
            assertThat(testUser.getTotpSecret()).isNull();
            assertThat(testUser.isTotpEnabled()).isFalse();
            assertThat(testUser.isEnabled()).isFalse();
            assertThat(testUser.isDeleted()).isTrue();
            // row kept (not physically removed) so transaction history FK references stay intact
            verify(userRepository).save(testUser);
            verify(userRepository, never()).delete(any(User.class));
            verify(rbacMetrics).recordUserDeleted();
        }

        @Test
        void anonymize_NotFound_ThrowsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userAppService.anonymize(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(userRepository, never()).save(any(User.class));
            verify(rbacMetrics, never()).recordUserDeleted();
        }

        @Test
        void anonymize_AlreadyDeleted_ThrowsNotFound() {
            testUser.setDeleted(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> userAppService.anonymize(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    class GetUserTests {

        @Test
        void getUser_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());
            when(userMapper.toDto(any(User.class), anyList())).thenReturn(testUserDto);

            UserDto result = userAppService.get(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("1");
            assertThat(result.username()).isEqualTo("testuser");
        }

        @Test
        void getUser_NotFound_ThrowsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userAppService.get(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }

        @Test
        void getUser_DeletedUser_ThrowsException() {
            testUser.setDeleted(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> userAppService.get(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    class ListUsersTests {

        @Test
        void listUsers_Success() {
            User user2 = new User();
            user2.setId(2L);
            user2.setUsername("user2");
            user2.setEnabled(true);
            user2.setDeleted(false);

            UserDto dto2 = new UserDto("2", "user2", "User 2", "user2@example.com", true, List.of(), null);

            when(userRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                    .thenReturn(List.of(testUser, user2));
            when(userRoleRepository.findByUserIdIn(anyList())).thenReturn(List.of());
            when(userMapper.toDto(eq(testUser), anyList())).thenReturn(testUserDto);
            when(userMapper.toDto(eq(user2), anyList())).thenReturn(dto2);

            List<UserDto> result = userAppService.list();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).username()).isEqualTo("testuser");
            assertThat(result.get(1).username()).isEqualTo("user2");
        }

        @Test
        void listUsers_Empty_ReturnsEmptyList() {
            when(userRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                    .thenReturn(List.of());

            List<UserDto> result = userAppService.list();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class PageUsersTests {

        @Test
        void pageUsers_Success() {
            Page<User> userPage = new PageImpl<>(List.of(testUser));

            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(userPage);
            when(userRoleRepository.findByUserIdIn(anyList())).thenReturn(List.of());
            when(userMapper.toDto(any(User.class), anyList())).thenReturn(testUserDto);

            PageResult<UserDto> result = userAppService.page("test", 0, 10, "id", "asc");

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        void pageUsers_NullKeyword_Success() {
            Page<User> userPage = new PageImpl<>(List.of(testUser));

            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(userPage);
            when(userRoleRepository.findByUserIdIn(anyList())).thenReturn(List.of());
            when(userMapper.toDto(any(User.class), anyList())).thenReturn(testUserDto);

            PageResult<UserDto> result = userAppService.page(null, 0, 10, "id", "asc");

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
        }
    }
}
