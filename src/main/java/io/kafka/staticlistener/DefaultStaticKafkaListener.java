package io.kafka.staticlistener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

@Slf4j
//@Component
//@RequiredArgsConstructor
public class DefaultStaticKafkaListener {


//    private final KafkaListenerEndpointRegistry registry;

    @KafkaListener(
            topics = {"out-topic-55"},
            groupId = "out-topic-55",
            containerFactory = "kafkaListenerContainerFactory",
            clientIdPrefix = "out-topic-client"
    )
    public void receive1(@Payload String msg,
                         @Headers MessageHeaders headers,
                         Acknowledgment acknowledgment) {

        log.info("receive1 " + msg);


    }

    @KafkaListener(
            topics = {"src-topic-55"},
            groupId = "src-topic-55",
            containerFactory = "kafkaListenerContainerFactory",
            clientIdPrefix = "src-topic-client"
    )
    public void receive2(@Payload String msg,
                         @Headers MessageHeaders headers,
                         Acknowledgment acknowledgment) {
        log.info("receive2 " + msg);


    }

    @KafkaListener(
            topics = {"src-topic-55"},
            groupId = "src-topic-540",
            containerFactory = "kafkaListenerContainerFactory",
            clientIdPrefix = "src-topic-client-23"
    )
    public void receive3(@Payload String msg,
                         @Headers MessageHeaders headers,
                         Acknowledgment acknowledgment) {
        log.info("receive3 " + msg);


    }
}
