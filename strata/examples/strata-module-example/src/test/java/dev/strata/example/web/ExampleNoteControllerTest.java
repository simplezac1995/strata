// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.Result;
import dev.strata.example.api.ExampleNote;
import dev.strata.example.api.ExampleNoteRequest;
import dev.strata.example.service.ExampleNoteService;
import org.junit.jupiter.api.Test;

/** The web layer is thin enough to test by calling it: binding/validation are Spring's responsibility. */
class ExampleNoteControllerTest {

    private final ExampleNoteService notes = new ExampleNoteService();
    private final ExampleNoteController controller = new ExampleNoteController(notes);

    @Test
    void create_returns_the_wrapped_note() {
        Result<ExampleNote> created = controller.create(new ExampleNoteRequest("hello", "world"));

        assertThat(created.code()).isZero();
        assertThat(created.data()).isNotNull();
        assertThat(created.data().title()).isEqualTo("hello");
        assertThat(controller.list().data()).hasSize(1);
    }

    @Test
    void get_throws_the_typed_not_found_error() {
        assertThatThrownBy(() -> controller.get("7"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Example note");
    }
}
