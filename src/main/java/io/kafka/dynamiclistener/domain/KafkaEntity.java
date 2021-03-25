package io.kafka.dynamiclistener.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "kafaka")
public class KafkaEntity {
    @Id
    @GeneratedValue
    private UUID id;

    private String topic;

    private Boolean isStart;
}
