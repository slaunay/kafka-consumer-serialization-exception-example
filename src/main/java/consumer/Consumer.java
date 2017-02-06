package consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Consumes from a Kafka topic records that contains Integer values.
 */
public class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    public static void main(String args[]) throws InterruptedException, IOException {
        if (args.length < 3) {
            System.out.println("usage java -jar path/to/jar <brokers> <topic> <consumer-group-id>");
            System.exit(1);
        }

        String brokers = args[0];
        String topic = args[1];
        String groupId = args[2];
        final CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean doStop = new AtomicBoolean();

        logger.info("Starting Kafka consumer with brokers: {}, topic: {} and group id: {}", brokers, topic, groupId);
        final Consumer consumer = new Consumer();

        // Manage the shutdown on the JVM gracefully
        Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {
            @Override
            public void run() {
                logger.info("Received INT signal, stopping processing...");
                try {
                    doStop.set(true);
                    // Wait for the consumer method to finish
                    latch.await();
                    logger.info("Kafka consumer done");
                } catch (InterruptedException e) {
                    logger.warn("Got interrupted", e);
                }
            }
        });

        Map<String, Object> consumerConfig = new LinkedHashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        // Turn off auto commit and use earliest so that we consume from the start of the stream every time
        consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Simulate fetching two records at a time using a small fetch size (supported in 0.10.1.+)
        consumerConfig.put("fetch.max.bytes", 100);

        try (KafkaConsumer<String, Integer> kafkaConsumer = new KafkaConsumer<>(consumerConfig, new StringDeserializer(), new IntegerDeserializer())) {
            kafkaConsumer.subscribe(Arrays.asList(topic));

            // Will run till the shutdown hook is called
            while (!doStop.get()) {
                try {
                    ConsumerRecords<String, Integer> records = kafkaConsumer.poll(1000);
                    if (!records.isEmpty()) {
                        logger.info("Got {} messages", records.count());
                        for (ConsumerRecord<String, Integer> record : records) {
                            logger.info("Message with partition: {}, offset: {}, key: {}, value: {}",
                                    record.partition(), record.offset(), record.key(), record.value());

                        }
                    } else {
                        logger.info("No messages to consume");
                    }
                } catch (SerializationException e) {
                    logger.warn("Failed polling some records", e);
                }

            }
        } finally {
            // Notify the shutdown hook that we are done
            latch.countDown();
        }
    }
}