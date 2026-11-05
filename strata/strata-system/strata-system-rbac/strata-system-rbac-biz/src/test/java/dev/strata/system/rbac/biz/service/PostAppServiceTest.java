// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.PostDto;
import dev.strata.system.api.request.PostCreateRequest;
import dev.strata.system.api.request.PostUpdateRequest;
import dev.strata.system.rbac.api.entity.Post;
import dev.strata.system.rbac.biz.mapper.PostMapper;
import dev.strata.system.rbac.biz.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostAppService 单元测试")
class PostAppServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostAppService postAppService;

    private Post post(long id, String code) {
        Post p = new Post();
        p.setId(id);
        p.setCode(code);
        p.setName("name-" + code);
        p.setSortOrder(1);
        p.setEnabled(true);
        return p;
    }

    private PostDto dto() {
        return new PostDto("1", "admin", "Administrator", 1, true);
    }

    @Nested
    @DisplayName("list / page - 查询")
    class QueryTests {

        @Test
        @DisplayName("list 按 sortOrder/id 返回全部岗位")
        void list_MapsAll() {
            when(postRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(post(1L, "admin")));
            when(postMapper.toDto(any(Post.class))).thenReturn(dto());

            List<PostDto> result = postAppService.list();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("page 分页查询")
        void page_ReturnsPaged() {
            when(postRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(post(1L, "admin"))));
            when(postMapper.toDto(any(Post.class))).thenReturn(dto());

            PageResult<PostDto> result = postAppService.page(0, 10);

            assertThat(result.content()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("create - 创建岗位")
    class CreateTests {

        @Test
        @DisplayName("code 未占用时创建成功")
        void create_CodeFree_Success() {
            PostCreateRequest request = new PostCreateRequest("dev", "Developer", 2, true);
            Post entity = post(0L, "dev");
            when(postRepository.existsByCode("dev")).thenReturn(false);
            when(postMapper.toEntity(request)).thenReturn(entity);
            when(postRepository.save(entity)).thenReturn(entity);
            when(postMapper.toDto(entity)).thenReturn(dto());

            PostDto result = postAppService.create(request);

            assertThat(result.code()).isEqualTo("admin");
            verify(postRepository).save(entity);
        }

        @Test
        @DisplayName("code 已占用时抛出 CONFLICT")
        void create_DuplicateCode_Throws() {
            PostCreateRequest request = new PostCreateRequest("admin", "Admin", 1, true);
            when(postRepository.existsByCode("admin")).thenReturn(true);

            assertThatThrownBy(() -> postAppService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));

            verify(postRepository, never()).save(any(Post.class));
        }
    }

    @Nested
    @DisplayName("update - 更新岗位")
    class UpdateTests {

        @Test
        @DisplayName("code 未变更时跳过唯一性检查直接更新")
        void update_SameCode_SkipsUniquenessCheck() {
            Post existing = post(1L, "admin");
            PostUpdateRequest request = new PostUpdateRequest("admin", "Admin Renamed", 2, true);
            when(postRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(postRepository.save(existing)).thenReturn(existing);
            when(postMapper.toDto(existing)).thenReturn(dto());

            postAppService.update(1L, request);

            verify(postRepository, never()).existsByCode(any());
            verify(postRepository).save(existing);
        }

        @Test
        @DisplayName("code 变更且新 code 已占用时抛出 CONFLICT")
        void update_NewCodeTaken_Throws() {
            Post existing = post(1L, "old");
            PostUpdateRequest request = new PostUpdateRequest("taken", "X", 1, true);
            when(postRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(postRepository.existsByCode("taken")).thenReturn(true);

            assertThatThrownBy(() -> postAppService.update(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));

            verify(postRepository, never()).save(any(Post.class));
        }

        @Test
        @DisplayName("code 变更且新 code 未占用时更新成功")
        void update_NewCodeFree_Success() {
            Post existing = post(1L, "old");
            PostUpdateRequest request = new PostUpdateRequest("fresh", "Fresh", 1, true);
            when(postRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(postRepository.existsByCode("fresh")).thenReturn(false);
            when(postRepository.save(existing)).thenReturn(existing);
            when(postMapper.toDto(existing)).thenReturn(dto());

            postAppService.update(1L, request);

            verify(postRepository).save(existing);
        }

        @Test
        @DisplayName("更新不存在的岗位抛出 NOT_FOUND")
        void update_NotFound_Throws() {
            PostUpdateRequest request = new PostUpdateRequest("x", "X", 1, true);
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postAppService.update(99L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("delete - 删除岗位")
    class DeleteTests {

        @Test
        @DisplayName("删除成功")
        void delete_Success() {
            when(postRepository.findById(1L)).thenReturn(Optional.of(post(1L, "admin")));

            postAppService.delete(1L);

            verify(postRepository).delete(any(Post.class));
        }

        @Test
        @DisplayName("删除不存在的岗位抛出 NOT_FOUND")
        void delete_NotFound_Throws() {
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postAppService.delete(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(postRepository, never()).delete(any(Post.class));
        }
    }
}
