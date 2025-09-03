//package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.kafka;
//
//import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.StockPurchase;
//import jakarta.json.bind.Jsonb;
//import jakarta.json.bind.JsonbBuilder;
//import lombok.extern.java.Log;
//import org.apache.kafka.clients.admin.Admin;
//import org.apache.kafka.clients.admin.NewTopic;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.consumer.KafkaConsumer;
//import org.apache.kafka.clients.producer.KafkaProducer;
//import org.apache.kafka.clients.producer.Producer;
//import org.apache.kafka.clients.producer.ProducerRecord;
//import org.apache.kafka.clients.producer.RecordMetadata;
//import org.apache.kafka.common.errors.TopicExistsException;
//import org.apache.kafka.common.serialization.StringDeserializer;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.time.OffsetDateTime;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Properties;
//import java.util.UUID;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeoutException;
//
//@Log
//class CustomKafkaListener implements Runnable {
//    private final String topic;
//    private final KafkaConsumer<String, String> consumer;
//
//    private final String bootstrapServers = "kafka:19092";
//
//    private static final Jsonb JSONB = JsonbBuilder.create();
//
//    public CustomKafkaListener(String topic) {
//        this(topic, defaultKafkaConsumer("kafka.dns.podman:19092"));
//    }
//
//    static KafkaConsumer<String, String> defaultKafkaConsumer(String boostrapServers) {
//        Properties props = new Properties();
//        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServers);
//        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test_group_id");
//        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        // keep reads simple/predictable for tests
//        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
//        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_uncommitted"); // fine since producer isn't transactional
//        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");
//        return new KafkaConsumer<>(props);
//    }
//
//    CustomKafkaListener(String topic, KafkaConsumer<String, String> consumer) {
//        this.topic = topic;
//        this.consumer = consumer;
//        ensureTopic(1,(short)1);
//    }
//
//    public void ensureTopic(int partitions, short replication) {
//        Properties adminProps = new Properties();
//        adminProps.put("bootstrap.servers", this.bootstrapServers);
//        adminProps.put("request.timeout.ms", "10000");
//        adminProps.put("default.api.timeout.ms", "10000");
//        adminProps.put("client.dns.lookup", "use_all_dns_ips"); // helpful with Podman
//        
//        try (Admin admin = Admin.create(adminProps)) {
//            try {
//                var nodes = admin.describeCluster().nodes().get(10, java.util.concurrent.TimeUnit.SECONDS);
//                nodes.forEach(n -> log.info("Broker endpoint from metadata: " + n.host() + ":" + n.port()));
//                var ctrl = admin.describeCluster().controller().get(10, java.util.concurrent.TimeUnit.SECONDS);
//                log.info("Controller endpoint from metadata: " + ctrl.host() + ":" + ctrl.port());
//                
//                log.info("BU: Creating topic: " + topic);
//                admin.createTopics(List.of(new NewTopic("stocktrader", partitions, replication))).all().get();
//                log.info("BU: topic created");
//            } catch (ExecutionException | InterruptedException ee) {
//                log.severe(ee.toString());
//                if (!(ee.getCause() instanceof TopicExistsException)) {
//                    log.severe(ee.toString());
//                }
//            } catch (TimeoutException e) {
//                log.severe("BU: Timeout waiting for topic creation: " + e.toString());
//            }
//        }
//    }
//
//    @Override
//    public void run() {
//        log.info("Listening to topic: " + topic);
//        sendStockPurchase();
//        consumer.subscribe(Arrays.asList(topic));
//        while (true) {
//            consumer.poll(Duration.ofMillis(100))
//                .forEach(record -> {
//                    log.info("BU: Received: " + record.value().toString());
//                });
//        }
//    }
//
//    public RecordMetadata sendStockPurchase() {
//        Properties props = new Properties();
//        props.put("bootstrap.servers", bootstrapServers);
//        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
//        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
//        // sensible reliability for tests
//        props.put("acks", "all");
//        props.put("retries", 3);
////        props.put("linger.ms", 5);
////        props.put("delivery.timeout.ms", 30000);
//        props.put("enable.idempotence", "true");
//
//        var purchase = new StockPurchase(
//            UUID.randomUUID().toString(), "Tim", "KD", 140, 45.0f, Instant.from(OffsetDateTime.now()), 5.99f);
//
//        log.info("BU: Sending: " + purchase);
//        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
//            String value = JSONB.toJson(purchase);
//            var rec = new ProducerRecord<>(topic, purchase.id(), value);
//            log.info("BU: Record: " + rec.toString());
//            RecordMetadata md = producer.send(rec).get();
//            log.info("BU: flushing");
//            producer.flush();
//            log.info("BU: Sent: " + md.toString());
//            return md;
//        } catch (Exception e) {
//            log.severe(e.toString());
//            return null;
//        }
//    }
//
//}
