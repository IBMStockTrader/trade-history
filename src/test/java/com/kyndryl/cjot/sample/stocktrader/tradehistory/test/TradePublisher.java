package com.kyndryl.cjot.sample.stocktrader.tradehistory.test;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.StockPurchase;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Helper to publish StockPurchase records to Kafka for testing.
 * Uses the Kafka client library directly rather than MicroProfile Reactive Messaging.
 * This is a simple synchronous publisher, not intended for production use.
 */
public class TradePublisher {
    private static final Jsonb JSONB = JsonbBuilder.create();

    /**
     * Create topic if it doesn't already exist. Safe to call repeatedly.
     */
    public static void ensureTopic(String bootstrapServers, String topic, int partitions, short replication)
        throws Exception {
        Properties adminProps = new Properties();
        adminProps.put("bootstrap.servers", bootstrapServers);
        try (Admin admin = Admin.create(adminProps)) {
            try {
                admin.createTopics(List.of(new NewTopic(topic, partitions, replication))).all().get();
            } catch (ExecutionException ee) {
                if (!(ee.getCause() instanceof TopicExistsException)) {
                    throw ee;
                }
            }
        }
    }

    /**
     * Publish a StockPurchase as JSON to the given Kafka topic (sync send).
     */
    public static RecordMetadata sendStockPurchase(String bootstrapServers, String topic,
                                                    com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.StockPurchase purchase)
        throws Exception {
        // Make sure topic is there (1 partition, RF=1 for Testcontainers)
        ensureTopic(bootstrapServers, topic, 1, (short) 1);

        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        // sensible reliability for tests
        props.put("acks", "all");
        props.put("retries", 3);
//        props.put("linger.ms", 5);
//        props.put("delivery.timeout.ms", 30000);
        props.put("enable.idempotence", "true");
        
        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            String value = JSONB.toJson(purchase);
            var rec = new ProducerRecord<>(topic, purchase.id(), value);
//            ProducerRecord<String, StockPurchase> rec = new ProducerRecord<>(topic, UUID.randomUUID().toString(), purchase);
            RecordMetadata md = producer.send(rec).get();
            producer.flush();
            return md;
        }
    }

    /**
     * Consume ONE StockPurchase from the topic (from earliest), optionally filtering by key.
     * Returns the parsed POJO, or throws TimeoutException if not seen before the deadline.
     */
    public static StockPurchase awaitStockPurchase(String bootstrapServers,
                                                   String topic,
                                                   String expectedKey,
                                                   Duration timeout) throws Exception {
        ensureTopic(bootstrapServers, topic, 1, (short) 1);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // fresh group each time so no old commits interfere
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "stocktrader-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // CRITICAL: start from the beginning for a brand-new group
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // keep reads simple/predictable for tests
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_uncommitted"); // fine since producer isn't transactional
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

        long deadline = System.nanoTime() + timeout.toNanos();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));

            while (System.nanoTime() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));
                for (ConsumerRecord<String, String> rec : records) {
                    if (expectedKey == null || expectedKey.equals(rec.key())) {
                        return JSONB.fromJson(rec.value(), StockPurchase.class);
                    }
                }
            }
        }

        throw new TimeoutException("Timed out waiting for StockPurchase on topic '" + topic +
            "' with key=" + expectedKey);
    }

    /** Convenience: publish then wait for the same record to appear (round-trip). */
    public static StockPurchase publishAndVerify(String bootstrapServers,
                                                 String topic,
                                                 StockPurchase purchase,
                                                 Duration timeout) throws Exception {
        sendStockPurchase(bootstrapServers, topic, purchase);
        return awaitStockPurchase(bootstrapServers, topic, purchase.id(), timeout);
    }
}
