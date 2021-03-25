package io.kafka.dynamiclistener.repository;

import io.kafka.dynamiclistener.domain.KafkaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface KafkaRepository
        extends JpaRepository<KafkaEntity, UUID>, JpaSpecificationExecutor<KafkaEntity> {
    KafkaEntity findByTopic(String topic);


}
