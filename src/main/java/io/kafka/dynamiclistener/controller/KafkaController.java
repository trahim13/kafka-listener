package io.kafka.dynamiclistener.controller;

import io.kafka.dynamiclistener.service.KafkaService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.Set;

@Validated
@RequestMapping("/kafka-service")
@RestController()
public class KafkaController {

    private final KafkaService kafkaService;

    KafkaController(final KafkaService kafkaService) {
        this.kafkaService = kafkaService;
    }

    @GetMapping("/start/{topic}")
    public void start(@PathVariable String topic) {
        kafkaService.start(topic);
    }

    @GetMapping("/stop/{topic}")
    public void stop(@PathVariable String topic) {
        kafkaService.stop(topic);
    }

    @GetMapping("/start-all")
    public void startAll() {
        kafkaService.startAll();
    }

    @GetMapping("/stop-all")
    public void stopAll() {
        kafkaService.stopAll();
    }

    @PostMapping("/register")
    public void register(@RequestBody @Valid Payload payload) {
        kafkaService.registerListener(payload.getTopics());
    }

    @PostMapping("/de-register")
    public void deRegister(@RequestBody @Valid Payload payload) {
        kafkaService.deRegisterListener(payload.getTopics());
    }

    private static class Payload {
        @NotEmpty
        @Size(min = 1)
        private Set<@Valid @NotEmpty String> topics;

        private Payload() {
        }

        public Payload(final Set<String> topics) {
            this.topics = topics;
        }

        public Set<String> getTopics() {
            return topics;
        }
    }
}