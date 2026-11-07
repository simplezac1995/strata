// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.service;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.example.api.ExampleNote;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * The module's business logic. The store is in memory on purpose: an example should not drag a database (and Flyway
 * migration) into a copy-pasted module. Replace this class with a repository-backed service in a real feature module.
 */
@Service
public class ExampleNoteService {

    private final ConcurrentHashMap<Long, ExampleNote> notes = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    /**
     * Creates and stores a note.
     *
     * @throws BusinessException when the example capacity is reached
     */
    public ExampleNote create(String title, String body) {
        if (notes.size() >= 100) {
            throw new BusinessException(ErrorCode.CONFLICT, "example note capacity reached");
        }
        long id = sequence.incrementAndGet();
        ExampleNote note = new ExampleNote(Long.toString(id), title, body);
        notes.put(id, note);
        return note;
    }

    public Optional<ExampleNote> findById(String id) {
        try {
            return Optional.ofNullable(notes.get(Long.parseLong(id)));
        } catch (NumberFormatException notAnExampleId) {
            return Optional.empty();
        }
    }

    public List<ExampleNote> findAll() {
        return List.copyOf(notes.values());
    }
}
