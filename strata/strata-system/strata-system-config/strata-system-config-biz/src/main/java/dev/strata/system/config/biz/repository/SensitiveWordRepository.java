// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.repository;

import dev.strata.system.config.api.entity.SensitiveWord;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Long> {

    List<SensitiveWord> findAllByOrderByIdAsc();

    @Query(
            "select sensitiveWord from SensitiveWord sensitiveWord where sensitiveWord.word not like concat(:prefix, '%')")
    Page<SensitiveWord> findByWordNotStartingWith(@Param("prefix") String prefix, Pageable pageable);

    Page<SensitiveWord> findByWordStartingWith(String prefix, Pageable pageable);

    boolean existsByWord(String word);
}
