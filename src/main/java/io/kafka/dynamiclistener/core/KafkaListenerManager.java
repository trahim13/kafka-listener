package io.kafka.dynamiclistener.core;

import io.kafka.dynamiclistener.domain.KafkaEntity;
import io.kafka.dynamiclistener.repository.KafkaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class KafkaListenerManager {

    private final KafkaRepository kafkaRepository;
    private final KafkaListenerContainerFactory kafkaListenerContainerFactory;
    private final Map<String, MessageListenerContainer> registeredTopicMap;
    private final Object lock;


    public KafkaListenerManager(
            final KafkaListenerContainerFactory kafkaListenerContainerFactory,
            final KafkaRepository kafkaRepository
    ) {
        Assert.notNull(kafkaListenerContainerFactory, "kafkaListenerContainerFactory must be not null.");

        this.kafkaRepository = kafkaRepository;
        this.kafkaListenerContainerFactory = kafkaListenerContainerFactory;
        this.registeredTopicMap = new ConcurrentHashMap<>();
        this.lock = new Object();
    }


    public void init(final Supplier<MessageListener> messageListenerSupplier) {
        List<KafkaEntity> kafkaEntities = this.kafkaRepository.findAll();
        Set<String> topics = kafkaEntities
                .stream()
                .filter(KafkaEntity::getIsStart)
                .map(KafkaEntity::getTopic)
                .collect(Collectors.toSet());

        register(() -> topics, messageListenerSupplier);
    }

    /**
     * Kafka listener registration at runtime.
     **/
    public void register(final Supplier<Set<String>> topicSupplier, final Supplier<MessageListener> messageListenerSupplier) {
        Assert.notNull(topicSupplier, "topicSupplier must be not null.");
        Assert.notNull(messageListenerSupplier, "messageListenerSupplier must be not null.");

        synchronized (lock) {
            final Set<String> registeredTopics = this.getRegisteredTopics();
            final Set<String> topics = topicSupplier.get();

            if (topics.isEmpty()) {
                return;
            }

            topics.stream()
                    .filter(topic -> !registeredTopics.contains(topic))
                    .forEach(topic -> this.doRegister(topic, messageListenerSupplier.get()));
        }
    }

    /**
     * Kafka listener de-registration at runtime.
     **/
    public void deRegister(final Supplier<Set<String>> topicSupplier) {
        Assert.notNull(topicSupplier, "topicSupplier must be not null.");

        synchronized (lock) {
            final Set<String> registeredTopics = getRegisteredTopics();
            final Set<String> topics = topicSupplier.get();

            if (topics.isEmpty()) {
                return;
            }

            topics.stream()
                    .filter(registeredTopics::contains)
                    .forEach(this::doDeregister);
        }
    }

    /**
     * Kafka listener start all at runtime
     **/
    public void startAll() {
        synchronized (lock) {
            final Collection<MessageListenerContainer> registeredMessageListenerContainers = getRegisteredMessageListenerContainers();
            registeredMessageListenerContainers.forEach(container -> {
                if (container.isRunning()) {
                    return;
                }
                container.start();
            });
        }
    }

    /**
     * Kafka listener stop all at runtime
     **/
    public void stopAll() {
        synchronized (lock) {
            final Collection<MessageListenerContainer> registeredMessageListenerContainers = getRegisteredMessageListenerContainers();
            registeredMessageListenerContainers.forEach(container -> {
                if (!container.isRunning()) {
                    return;
                }
                container.stop();
            });

            List<KafkaEntity> all = kafkaRepository.findAll();
            all.forEach(k -> k.setIsStart(Boolean.FALSE));
            kafkaRepository.saveAll(all);
        }
    }

    /**
     * Kafka listener start at runtime
     **/
    public void start(String topic, MessageListener messageListener) {
        synchronized (lock) {
            doRegister(topic, messageListener);
        }
    }

    /**
     * Kafka listener stop at runtime
     **/
    public void stop(String topic) {
        synchronized (lock) {
            doDeregister(topic);
        }
    }

    public Map<String, MessageListenerContainer> getRegisteredTopicMap() {
        return Collections.unmodifiableMap(registeredTopicMap);
    }


    private void doRegister(final String topic, final MessageListener messageListener) {
        Assert.hasLength(topic, "topic must be not empty.");
        Assert.notNull(messageListener, "messageListener must be not null.");

        /**
         * KafkaListenerContainerFactory есть два вида, подробно тут https://docs.spring.io/spring-kafka/reference/html/#message-listener-container
         */
        final ConcurrentMessageListenerContainer messageListenerContainer = (ConcurrentMessageListenerContainer) kafkaListenerContainerFactory.createContainer(topic);

        /**
         * используется для именования потоков контейнера
         * если не задано то будет использоваться {@link ConcurrentMessageListenerContainer}
         * в методе doStart -> container.setBeanName((beanName != null ? beanName : "consumer") + "-" + i);
         * это значит, что для разных {@link ConcurrentMessageListenerContainer} у нас будет одно имя потока consumer-0-C-1, но на само деле потоки разные
         * в consumer-0-C-1 - единица на конце, это номер потока конкретного {@link ConcurrentMessageListenerContainer}, по-умолчанию значение канкаренси 1
         */
        messageListenerContainer.setBeanName(topic); // как вариант сюда писать топик name, но длинные названия не влазят полностью
        messageListenerContainer.setupMessageListener(messageListener);
        messageListenerContainer.getContainerProperties().setMissingTopicsFatal(false); //не падать если топик отсутствует
        messageListenerContainer.getContainerProperties().setGroupId(topic + "_GROUP"); // имя группы слушателя
//        messageListenerContainer.getContainerProperties().setClientId(topic + "_id"); // по-умолчанию consumer-1, consumer-2 и так далее


        messageListenerContainer.start();

        registeredTopicMap.put(topic, messageListenerContainer);

        while (true) {
            boolean running = messageListenerContainer.isRunning();
            if (running) break;
        }

        KafkaEntity byTopic = kafkaRepository.findByTopic(topic);
        if (byTopic == null) {
            byTopic = new KafkaEntity();
            byTopic.setTopic(topic);
        }
        byTopic.setIsStart(Boolean.TRUE);
        kafkaRepository.save(byTopic);

    }

    // обратить внимание на замковую модель
    private void doDeregister(final String topic) {
        Assert.hasLength(topic, "topic must be not empty.");

        final MessageListenerContainer messageListenerContainer = registeredTopicMap.get(topic);
        if (messageListenerContainer != null) {
            messageListenerContainer.stop();

            while (true) {
                boolean running = messageListenerContainer.isRunning();
                if (!running) break;
            }

            registeredTopicMap.remove(topic);

        }
        KafkaEntity byTopic = kafkaRepository.findByTopic(topic);
        byTopic.setIsStart(Boolean.FALSE);
        kafkaRepository.save(byTopic);

    }

    private Set<String> getRegisteredTopics() {
        return registeredTopicMap.keySet();
    }

    private Collection<MessageListenerContainer> getRegisteredMessageListenerContainers() {
        return registeredTopicMap.values();
    }

}