// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.frontier;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.system.api.request.FrontierErrorRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FrontierErrorAppServiceTest {

    private final FrontierErrorAppService service = new FrontierErrorAppService();

    private static FrontierErrorRequest sample() {
        return new FrontierErrorRequest(
                "boom", "Error: boom\n  at app.js", "http://app/", "u-1", "agent", "error", Instant.now(), null);
    }

    @Test
    void withinLimit_Accepted() {
        boolean accepted = service.report(sample(), "10.0.0.1");
        assertThat(accepted).isTrue();
    }

    @Test
    void overLimit_RateLimited() {
        String ip = "10.0.0.2";
        boolean lastAccepted = false;
        for (int i = 0; i < FrontierErrorAppService.MAX_PER_WINDOW; i++) {
            lastAccepted = service.report(sample(), ip);
        }
        assertThat(lastAccepted).isTrue();

        boolean dropped = service.report(sample(), ip);
        assertThat(dropped).isFalse();
    }

    @Test
    void differentIp_Independent() {
        String ipA = "10.0.0.3";
        String ipB = "10.0.0.4";
        for (int i = 0; i < FrontierErrorAppService.MAX_PER_WINDOW; i++) {
            service.report(sample(), ipA);
        }
        assertThat(service.report(sample(), ipA)).isFalse();
        assertThat(service.report(sample(), ipB)).isTrue();
    }

    @Test
    void nullRequest_DoesNotThrow() {
        assertThat(service.report(null, "10.0.0.5")).isTrue();
    }
}
