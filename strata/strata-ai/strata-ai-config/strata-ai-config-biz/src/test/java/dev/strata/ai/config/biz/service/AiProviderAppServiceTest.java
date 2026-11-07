// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.request.AiProviderCreateRequest;
import dev.strata.ai.api.request.AiProviderUpdateRequest;
import dev.strata.ai.config.api.dto.AiProviderDto;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.biz.mapper.AiProviderMapper;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
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
class AiProviderAppServiceTest {

    @Mock
    private AiProviderRepository aiProviderRepository;

    @Mock
    private AiProviderMapper aiProviderMapper;

    @InjectMocks
    private AiProviderAppService aiProviderAppService;

    private AiProvider testProvider;
    private AiProviderDto testProviderDto;

    @BeforeEach
    void setUp() {
        testProvider = new AiProvider();
        testProvider.setId(1L);
        testProvider.setCode("openai");
        testProvider.setName("OpenAI");
        testProvider.setType("openai");
        testProvider.setBaseUrl("https://api.openai.com/v1");
        testProvider.setApiKey("sk-test-key");
        testProvider.setEnabled(true);

        testProviderDto =
                new AiProviderDto("1", "openai", "OpenAI", "openai", "https://api.openai.com/v1", "sk-test-key", true);
    }

    @Nested
    class CreateProviderTests {

        @Test
        void createProvider_Success() {
            AiProviderCreateRequest request = new AiProviderCreateRequest(
                    "openai", "OpenAI", "openai", "https://api.openai.com/v1", "sk-test-key", true);

            when(aiProviderMapper.toEntity(request)).thenReturn(testProvider);
            when(aiProviderRepository.save(any(AiProvider.class))).thenReturn(testProvider);
            when(aiProviderMapper.toDto(any(AiProvider.class))).thenReturn(testProviderDto);

            AiProviderDto result = aiProviderAppService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.code()).isEqualTo("openai");
            verify(aiProviderRepository).save(any(AiProvider.class));
        }
    }

    @Nested
    class UpdateProviderTests {

        @Test
        void updateProvider_Success() {
            AiProviderUpdateRequest request = new AiProviderUpdateRequest(
                    "OpenAI Updated", "openai", "https://api.openai.com/v2", "sk-new-key", true);

            when(aiProviderRepository.findById(1L)).thenReturn(Optional.of(testProvider));
            when(aiProviderRepository.save(any(AiProvider.class))).thenReturn(testProvider);
            when(aiProviderMapper.toDto(any(AiProvider.class))).thenReturn(testProviderDto);

            AiProviderDto result = aiProviderAppService.update(1L, request);

            assertThat(result).isNotNull();
            verify(aiProviderMapper).update(any(AiProviderUpdateRequest.class), any(AiProvider.class));
            verify(aiProviderRepository).save(any(AiProvider.class));
        }

        @Test
        void updateProvider_NotFound_ThrowsException() {
            AiProviderUpdateRequest request = new AiProviderUpdateRequest("Updated", "openai", null, null, true);

            when(aiProviderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiProviderAppService.update(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    class DeleteProviderTests {

        @Test
        void deleteProvider_Success() {
            when(aiProviderRepository.findById(1L)).thenReturn(Optional.of(testProvider));

            aiProviderAppService.delete(1L);

            verify(aiProviderRepository).delete(testProvider);
        }

        @Test
        void deleteProvider_NotFound_ThrowsException() {
            when(aiProviderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiProviderAppService.delete(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    class GetProviderTests {

        @Test
        void getProvider_Success() {
            when(aiProviderRepository.findById(1L)).thenReturn(Optional.of(testProvider));
            when(aiProviderMapper.toDto(any(AiProvider.class))).thenReturn(testProviderDto);

            AiProviderDto result = aiProviderAppService.get(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("1");
            assertThat(result.code()).isEqualTo("openai");
        }

        @Test
        void getProvider_NotFound_ThrowsException() {
            when(aiProviderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiProviderAppService.get(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    class ListProvidersTests {

        @Test
        void listProviders_Success() {
            AiProvider provider2 = new AiProvider();
            provider2.setId(2L);
            provider2.setCode("anthropic");
            provider2.setName("Anthropic");
            provider2.setEnabled(true);

            AiProviderDto dto2 = new AiProviderDto(
                    "2", "anthropic", "Anthropic", "anthropic", "https://api.anthropic.com", "sk-ant-key", true);

            when(aiProviderRepository.findAll()).thenReturn(List.of(testProvider, provider2));
            when(aiProviderMapper.toDto(any(AiProvider.class))).thenReturn(testProviderDto, dto2);

            List<AiProviderDto> result = aiProviderAppService.list();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).code()).isEqualTo("openai");
            assertThat(result.get(1).code()).isEqualTo("anthropic");
        }
    }

    @Nested
    class PageProvidersTests {

        @Test
        void pageProviders_Success() {
            Page<AiProvider> providerPage = new PageImpl<>(List.of(testProvider));

            when(aiProviderRepository.findAll(any(Pageable.class))).thenReturn(providerPage);
            when(aiProviderMapper.toDto(any(AiProvider.class))).thenReturn(testProviderDto);

            PageResult<AiProviderDto> result = aiProviderAppService.page(0, 10);

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }
    }
}
