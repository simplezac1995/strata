// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.PageResult;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.dto.SignInConfigDto;
import dev.strata.system.api.dto.SignInRecordDto;
import dev.strata.system.api.request.SignInConfigUpsertRequest;
import dev.strata.system.ops.api.entity.SignInConfig;
import dev.strata.system.ops.api.entity.SignInRecord;
import dev.strata.system.ops.biz.mapper.SignInConfigMapper;
import dev.strata.system.ops.biz.mapper.SignInRecordMapper;
import dev.strata.system.ops.biz.repository.SignInConfigRepository;
import dev.strata.system.ops.biz.repository.SignInRecordRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SignInAppServiceTest {

    @Mock
    private SignInConfigRepository signInConfigRepository;

    @Mock
    private SignInRecordRepository signInRecordRepository;

    @Mock
    private SignInConfigMapper signInConfigMapper;

    @Mock
    private SignInRecordMapper signInRecordMapper;

    private SignInAppService service;

    @BeforeEach
    void setUp() {
        service = new SignInAppService(
                signInConfigRepository, signInRecordRepository, signInConfigMapper, signInRecordMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(long userId) {
        UsernamePasswordAuthenticationToken authentication = Mockito.mock(UsernamePasswordAuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(new JwtPrincipal(userId, "alice"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void getConfig_MapsAll() {
        when(signInConfigRepository.findAllByOrderByDayAsc()).thenReturn(List.of(new SignInConfig()));
        when(signInConfigMapper.toDto(any(SignInConfig.class)))
                .thenReturn(new SignInConfigDto("1", 1, 100L, BigDecimal.ZERO));

        assertThat(service.getConfig()).hasSize(1);
    }

    @Test
    void replaceConfig_DeletesAndSaves() {
        SignInConfigUpsertRequest request =
                new SignInConfigUpsertRequest(List.of(new SignInConfigUpsertRequest.Item(1, 100L, BigDecimal.ZERO)));
        when(signInConfigMapper.toEntity(any(SignInConfigUpsertRequest.Item.class)))
                .thenReturn(new SignInConfig());
        when(signInConfigRepository.findAllByOrderByDayAsc()).thenReturn(List.of(new SignInConfig()));
        when(signInConfigMapper.toDto(any(SignInConfig.class)))
                .thenReturn(new SignInConfigDto("1", 1, 100L, BigDecimal.ZERO));

        service.replaceConfig(request);

        verify(signInConfigRepository).deleteAll();
        verify(signInConfigRepository).saveAll(any());
    }

    @Test
    void records_NullUserId_Throws() {
        assertThatThrownBy(() -> service.records(null, 0, 10))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void records_Paged() {
        when(signInRecordRepository.findByUserIdOrderBySignDateDescIdDesc(
                        org.mockito.ArgumentMatchers.eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new SignInRecord())));
        when(signInRecordMapper.toDto(any(SignInRecord.class)))
                .thenReturn(new SignInRecordDto("1", "7", null, 1, 0L, BigDecimal.ZERO, null));

        PageResult<SignInRecordDto> result = service.records(7L, 0, 10);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void checkin_AlreadySigned_Throws() {
        authenticate(7L);
        when(signInRecordRepository.existsByUserIdAndSignDate(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.checkin())
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void checkin_NotAuthenticated_Throws() {
        SecurityContextHolder.getContext().setAuthentication(null);

        assertThatThrownBy(() -> service.checkin())
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void checkin_WithConfig() {
        authenticate(7L);
        SignInConfig config = new SignInConfig();
        config.setDay(1);
        config.setRewardPoints(100L);
        config.setRewardMinor(BigDecimal.TEN);
        when(signInRecordRepository.existsByUserIdAndSignDate(any(), any())).thenReturn(false);
        when(signInRecordRepository.countByUserId(7L)).thenReturn(0L);
        when(signInConfigRepository.findByDay(1)).thenReturn(config);
        when(signInRecordRepository.save(any(SignInRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(signInRecordMapper.toDto(any(SignInRecord.class)))
                .thenReturn(new SignInRecordDto("1", "7", null, 1, 100L, BigDecimal.TEN, null));

        SignInRecordDto result = service.checkin();

        assertThat(result.dayNumber()).isEqualTo(1);
        verify(signInRecordRepository).save(any(SignInRecord.class));
    }

    @Test
    void checkin_NoConfig_ZeroReward() {
        authenticate(7L);
        when(signInRecordRepository.existsByUserIdAndSignDate(any(), any())).thenReturn(false);
        when(signInRecordRepository.countByUserId(7L)).thenReturn(2L);
        when(signInConfigRepository.findByDay(3)).thenReturn(null);
        when(signInRecordRepository.save(any(SignInRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(signInRecordMapper.toDto(any(SignInRecord.class)))
                .thenReturn(new SignInRecordDto("1", "7", null, 3, 0L, BigDecimal.ZERO, null));

        SignInRecordDto result = service.checkin();

        assertThat(result.dayNumber()).isEqualTo(3);
        assertThat(result.rewardPoints()).isZero();
    }
}
