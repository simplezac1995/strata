// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.web;

import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.Result;
import dev.strata.example.api.ExampleNote;
import dev.strata.example.api.ExampleNoteRequest;
import dev.strata.example.service.ExampleNoteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP surface of the example module: parameter binding, validation and {@link Result} wrapping only.
 *
 * <p>Notice what is absent: no repository, no entity, no transaction. A feature module keeps all persistence behind
 * its service so the host application can depend on {@code api} alone.
 */
@RestController
@RequestMapping("/api/v1/examples/notes")
public class ExampleNoteController {

    private final ExampleNoteService notes;

    public ExampleNoteController(ExampleNoteService notes) {
        this.notes = notes;
    }

    @PostMapping
    public Result<ExampleNote> create(@RequestBody @Valid ExampleNoteRequest request) {
        return Result.ok(notes.create(request.title(), request.body()));
    }

    @GetMapping
    public Result<List<ExampleNote>> list() {
        return Result.ok(notes.findAll());
    }

    @GetMapping("/{id}")
    public Result<ExampleNote> get(@PathVariable String id) {
        return Result.ok(notes.findById(id).orElseThrow(() -> new NotFoundException("Example note", id)));
    }
}
