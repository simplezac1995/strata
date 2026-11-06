// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.example.api.ExampleNote;
import org.junit.jupiter.api.Test;

/** Unit tests for the example service: no Spring context, no database, fast enough to keep in the build. */
class ExampleNoteServiceTest {

    private final ExampleNoteService notes = new ExampleNoteService();

    @Test
    void create_stores_the_note_and_find_by_id_returns_it() {
        ExampleNote created = notes.create("first", "body");

        assertThat(created.id()).isEqualTo("1");
        assertThat(notes.findById("1")).contains(created);
        assertThat(notes.findAll()).containsExactly(created);
    }

    @Test
    void find_by_unknown_or_malformed_id_is_empty_not_an_error() {
        assertThat(notes.findById("42")).isEmpty();
        assertThat(notes.findById("not-a-number")).isEmpty();
    }

    @Test
    void create_refuses_more_notes_than_the_example_capacity() {
        for (int index = 0; index < 100; index++) {
            notes.create("note-" + index, null);
        }

        assertThatThrownBy(() -> notes.create("one-too-many", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capacity");
    }
}
