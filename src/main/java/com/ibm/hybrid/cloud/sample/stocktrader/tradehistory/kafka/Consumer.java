/*
       Copyright 2018, 2019 IBM Corp All Rights Reserved
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.kafka;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class Consumer {

    private final String APP_NAME = "trade-history";
    private final long POLL_DURATION = 1000;

    @Inject
    @ConfigProperty(name = "BOOTSTRAP_SERVER")
    private String BOOTSTRAP_SERVER_ENV_KEY;

    @Inject
    @ConfigProperty(name = "TOPIC")
    private String TOPIC_ENV_KEY;

    @Inject
    @ConfigProperty(name = "CONSUMER_API_KEY")
    private String API_KEY;

    @Inject
    @ConfigProperty(name = "KAFKA_KEYSTORE", defaultValue = "resources/security/certs.jks")
    private String KEYSTORE;

    @Inject
    @ConfigProperty(name = "KAFKA_USER", defaultValue = "token")
    private String USERNAME;

    @Inject
    @ConfigProperty(name = "CONSUMER_GROUP_ID", defaultValue = APP_NAME)
    private String CONSUMER_GROUP_ID;

    private KafkaConsumer<String, String> kafkaConsumer=null;

    private Logger logger = Logger.getLogger(Consumer.class);

    public Consumer(){

    }

    @PostConstruct
    private void init() {
        BasicConfigurator.configure();
        String bootstrapServerAddress = BOOTSTRAP_SERVER_ENV_KEY.replace("\"", "");
        String topic = TOPIC_ENV_KEY.replace("\"", "");

        kafkaConsumer = createConsumer(bootstrapServerAddress);
        kafkaConsumer.subscribe(Arrays.asList(topic));
    }

    private KafkaConsumer<String, String> createConsumer(String brokerList) {
        Properties properties = new Properties();
        //common Kafka configs
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        properties.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + USERNAME + "\" password=\"" + API_KEY + "\";");
        properties.put(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
        properties.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG, "TLSv1.2");
        properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "HTTPS");
        //Kafka consumer configs
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> kafkaConsumer = null;

        try {
            kafkaConsumer = new KafkaConsumer<>(properties);
        } catch (KafkaException kafkaError) {
            logger.error("Error creating kafka consumer.", kafkaError);
            throw kafkaError;
        }
        
        return kafkaConsumer;
    }

    public ConsumerRecords<String, String> consume() {
        ConsumerRecords<String, String> records = kafkaConsumer.poll(POLL_DURATION);
        return records;
    }

    public boolean isHealthy() {
        return kafkaConsumer != null;
    }
    
    public void shutdown() {
        kafkaConsumer.close();
        logger.info(String.format("Closed consumer: %s", CONSUMER_GROUP_ID));
    }
}