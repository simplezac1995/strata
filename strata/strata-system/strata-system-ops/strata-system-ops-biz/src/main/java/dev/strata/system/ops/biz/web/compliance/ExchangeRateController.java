// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web.compliance;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.compliance.ExchangeRateDto;
import dev.strata.system.api.request.compliance.ExchangeRateCreateRequest;
import dev.strata.system.api.request.compliance.ExchangeRateUpdateRequest;
import dev.strata.system.ops.biz.service.compliance.ExchangeRateAppService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Admin exchange-rate management; public player read. */
@Validated
@RestController
public class ExchangeRateController {

    private final ExchangeRateAppService exchangeRateAppService;

    public ExchangeRateController(ExchangeRateAppService exchangeRateAppService) {
        this.exchangeRateAppService = exchangeRateAppService;
    }

    @GetMapping("/api/v1/admin/exchange-rates")
    @PreAuthorize(Permissions.COMPLIANCE_RATE_LIST)
    public Result<List<ExchangeRateDto>> list() {
        return Result.ok(exchangeRateAppService.list());
    }

    @PostMapping("/api/v1/admin/exchange-rates")
    @PreAuthorize(Permissions.COMPLIANCE_RATE_CREATE)
    public Result<ExchangeRateDto> create(@Valid @RequestBody ExchangeRateCreateRequest request) {
        return Result.ok(exchangeRateAppService.create(request));
    }

    @PutMapping("/api/v1/admin/exchange-rates/{id}")
    @PreAuthorize(Permissions.COMPLIANCE_RATE_UPDATE)
    public Result<ExchangeRateDto> update(
            @PathVariable Long id, @Valid @RequestBody ExchangeRateUpdateRequest request) {
        return Result.ok(exchangeRateAppService.update(id, request));
    }

    @GetMapping("/api/v1/player/exchange-rates")
    public Result<List<ExchangeRateDto>> playerList() {
        return Result.ok(exchangeRateAppService.list());
    }
}
