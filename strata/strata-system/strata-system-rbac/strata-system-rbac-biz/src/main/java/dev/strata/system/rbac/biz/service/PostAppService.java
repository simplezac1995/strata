// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import dev.strata.system.api.dto.PostDto;
import dev.strata.system.api.request.PostCreateRequest;
import dev.strata.system.api.request.PostUpdateRequest;
import dev.strata.system.rbac.api.entity.Post;
import dev.strata.system.rbac.biz.mapper.PostMapper;
import dev.strata.system.rbac.biz.repository.PostRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostAppService {

    private static final Logger log = LoggerFactory.getLogger(PostAppService.class);

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    public PostAppService(PostRepository postRepository, PostMapper postMapper) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
    }

    @Transactional(readOnly = true)
    public List<PostDto> list() {
        return postRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(postMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<PostDto> page(int page, int size) {
        return PageResult.from(postRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "sortOrder", "id")))
                .map(postMapper::toDto));
    }

    @Transactional
    @OperationLog(module = "system:post", action = "create")
    public PostDto create(PostCreateRequest request) {
        requireCodeFree(request.code());
        Post post = postMapper.toEntity(request);
        postRepository.save(post);
        return postMapper.toDto(post);
    }

    @Transactional
    @OperationLog(module = "system:post", action = "update")
    public PostDto update(Long id, PostUpdateRequest request) {
        Post post = require(id);
        if (!post.getCode().equals(request.code()) && postRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Post code already exists");
        }
        postMapper.update(request, post);
        postRepository.save(post);
        return postMapper.toDto(post);
    }

    @Transactional
    @OperationLog(module = "system:post", action = "delete")
    public void delete(Long id) {
        postRepository.delete(require(id));
    }

    private void requireCodeFree(String code) {
        if (postRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Post code already exists");
        }
    }

    private Post require(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new NotFoundException("Post not found"));
    }
}
