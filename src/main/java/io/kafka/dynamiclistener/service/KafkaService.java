package io.kafka.dynamiclistener.service;

import io.kafka.dynamiclistener.core.KafkaListenerManager;
import io.kafka.dynamiclistener.repository.KafkaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.stereotype.Service;

import java.util.Set;


@Service
public class KafkaService {

    private static final Logger log = LoggerFactory.getLogger(KafkaService.class.getName());

    private final KafkaListenerManager kafkaListenerManager;

    KafkaService(
            final KafkaListenerContainerFactory kafkaListenerContainerFactory,
            final KafkaRepository kafkaRepository
    ) {
        this.kafkaListenerManager = new KafkaListenerManager(kafkaListenerContainerFactory, kafkaRepository);
        kafkaListenerManager.init(KafkaService::messageListener);
    }

    public void startAll() {
        kafkaListenerManager.startAll();
    }

    public void stopAll() {
        kafkaListenerManager.stopAll();
    }

    public void start(String topic) {
        kafkaListenerManager.start(topic, messageListener());
    }

    public void stop(String topic) {
        kafkaListenerManager.stop(topic);
    }

    public void registerListener(final Set<String> topic) {
        kafkaListenerManager.register(() -> topic, KafkaService::messageListener);
    }

    public void deRegisterListener(final Set<String> topic) {
        kafkaListenerManager.deRegister(() -> topic);
    }

    //могут быть различные реализации
    private static AcknowledgingMessageListener<String, String> messageListener() {
        return (consumerRecord, acknowledgment) -> {
            log.info("Consume message: " + consumerRecord.value());
//           acknowledgment.acknowledge();

        };

    }
}
