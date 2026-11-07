// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.repository;

import dev.strata.system.communication.api.entity.MessageChannel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageChannelRepository extends JpaRepository<MessageChannel, Long> {

    List<MessageChannel> findAllByOrderByIdAsc();

    Optional<MessageChannel> findByChannelType(String channelType);
}
