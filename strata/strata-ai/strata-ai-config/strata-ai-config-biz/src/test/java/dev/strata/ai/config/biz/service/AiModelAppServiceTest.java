// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.request.AiModelCreateRequest;
import dev.strata.ai.api.request.AiModelUpdateRequest;
import dev.strata.ai.config.api.dto.AiModelDto;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.biz.mapper.AiModelMapper;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
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

@ExtendWith(MockitoExtension.class)
class AiModelAppServiceTest {

    @Mock
    private AiModelRepository aiModelRepository;

    @Mock
    private AiModelMapper aiModelMapper;

    @InjectMocks
    private AiModelAppService aiModelAppService;

    private AiModel testModel;
    private AiModelDto testModelDto;

    @BeforeEach
    void setUp() {
        testModel = new AiModel();
        testModel.setId(1L);
        testModel.setProviderId(1L);
        testModel.setCode("gpt-4");
        testModel.setName("GPT-4");
        testModel.setType("chat");
        testModel.setEnabled(true);

        testModelDto = new AiModelDto("1", "1", "gpt-4", "GPT-4", "chat", true);
    }

    @Nested
    class CreateModelTests {

        @Test
        void createModel_Success() {
            AiModelCreateRequest request = new AiModelCreateRequest(1L, "gpt-4", "GPT-4", "chat", true);

            when(aiModelMapper.toEntity(request)).thenReturn(testModel);
            when(aiModelRepository.save(any(AiModel.class))).thenReturn(testModel);
            when(aiModelMapper.toDto(any(AiModel.class))).thenReturn(testModelDto);

            AiModelDto result = aiModelAppService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.code()).isEqualTo("gpt-4");
            verify(aiModelRepository).save(any(AiModel.class));
        }
    }

    @Nested
    class UpdateModelTests {

        @Test
        void updateModel_Success() {
            AiModelUpdateRequest request = new AiModelUpdateRequest("GPT-4 Updated", "chat", true);

            when(aiModelRepository.findById(1L)).thenReturn(Optional.of(testModel));
            when(aiModelRepository.save(any(AiModel.class))).thenReturn(testModel);
            when(aiModelMapper.toDto(any(AiModel.class))).thenReturn(testModelDto);

            AiModelDto result = aiModelAppService.update(1L, request);

            assertThat(result).isNotNull();
            verify(aiModelMapper).update(any(AiModelUpdateRequest.class), any(AiModel.class));
            verify(aiModelRepository).save(any(AiModel.class));
        }

        @Test
        void updateModel_NotFound_ThrowsException() {
            AiModelUpdateRequest request = new AiModelUpdateRequest("Updated", "chat", true);

            when(aiModelRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiModelAppService.update(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    class DeleteModelTests {

        @Test
        void deleteModel_Success() {
            when(aiModelRepository.findById(1L)).thenReturn(Optional.of(testModel));

            aiModelAppService.delete(1L);

            verify(aiModelRepository).delete(testModel);
        }

        @Test
        void deleteModel_NotFound_ThrowsException() {
            when(aiModelRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiModelAppService.delete(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    class GetModelTests {

        @Test
        void getModel_Success() {
            when(aiModelRepository.findById(1L)).thenReturn(Optional.of(testModel));
            when(aiModelMapper.toDto(any(AiModel.class))).thenReturn(testModelDto);

            AiModelDto result = aiModelAppService.get(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("1");
            assertThat(result.code()).isEqualTo("gpt-4");
        }

        @Test
        void getModel_NotFound_ThrowsException() {
            when(aiModelRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiModelAppService.get(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    class ListModelsTests {

        @Test
        void listModels_Success() {
            AiModel model2 = new AiModel();
            model2.setId(2L);
            model2.setCode("gpt-3.5");
            model2.setName("GPT-3.5");
            model2.setEnabled(true);

            AiModelDto dto2 = new AiModelDto("2", "1", "gpt-3.5", "GPT-3.5", "chat", true);

            when(aiModelRepository.findAllByOrderByIdAsc()).thenReturn(List.of(testModel, model2));
            when(aiModelMapper.toDto(any(AiModel.class))).thenReturn(testModelDto, dto2);

            List<AiModelDto> result = aiModelAppService.list();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).code()).isEqualTo("gpt-4");
            assertThat(result.get(1).code()).isEqualTo("gpt-3.5");
        }
    }

    @Nested
    class PageModelsTests {

        @Test
        void pageModels_Success() {
            Page<AiModel> modelPage = new PageImpl<>(List.of(testModel));

            when(aiModelRepository.findAll(any(Pageable.class))).thenReturn(modelPage);
            when(aiModelMapper.toDto(any(AiModel.class))).thenReturn(testModelDto);

            PageResult<AiModelDto> result = aiModelAppService.page(0, 10);

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }
    }
}
