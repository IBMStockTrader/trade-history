/*
       Copyright 2025 Kyndryl, All Rights Reserved
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

package it;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.StockPurchase;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest.Share;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest.Transaction;
import com.kyndryl.cjot.sample.stocktrader.tradehistory.test.RestClientProducers;
import com.kyndryl.cjot.sample.stocktrader.tradehistory.test.TradePublisher;
import com.kyndryl.cjot.sample.stocktrader.tradehistory.test.containers.LibertyContainer;
import com.kyndryl.cjot.stocktrader.clients.tradehistory.TradeHistoryClient;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.inject.Inject;
import lombok.extern.java.Log;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.kafka.KafkaContainer;

import java.net.Socket;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Log
@EnableAutoWeld
@AddBeanClasses({RestClientProducers.class, TradeHistoryClient.class})
public class TradeHistoryServiceTest {

    public static final String TRADE_COLLECTION_NAME = "test_collection";
    public static final String MONGO_DATABASE = "tradeHistoryCollection";
    private static final int DB_PORT = 27017;
    static ImageFromDockerfile invImage = new ImageFromDockerfile("tradehistory-it:local", false)
        .withFileFromPath("Dockerfile", Paths.get("Dockerfile"))  // if at repo root
        .withFileFromPath("src/main/liberty/config", Paths.get("src/main/liberty/config"))
        .withFileFromPath("target/tradehistory-1.0-SNAPSHOT.war",
            Paths.get("target/tradehistory-1.0-SNAPSHOT.war"));
    private static Network network = Network.newNetwork();
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5")
        .withNetworkAliases("mongodb")
        .withNetwork(network)
        .waitingFor(Wait.forListeningPort());
    private static final KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0")
        .withNetworkAliases("kafka")
        .withListener("kafka:19092") // We need a second listener for some reason to make the liberty work.
        .withNetwork(network);
    private static LibertyContainer tradeHistoryContainer
        = new LibertyContainer(invImage, 9080, 9443)
        .withNetworkAliases("tradehistory")
        .withNetwork(network)
        .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()));

    @Inject
    @RestClient
    private TradeHistoryClient client;

    private static boolean isServiceRunning(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    public static void setup() throws Exception {
        log.info("Testing by using Testcontainers...");
        if (isServiceRunning("localhost", DB_PORT)) {
            throw new Exception(
                "MongoDB database is running locally. Stop it and retry.");
        } else {
            setupKafkaContainer();
            setupMongoDBContainer();
            setTradeHistoryContainer();
        }

        // override the mp-rest url to point to the test container
        System.setProperty(
            "tradehistory-api/mp-rest/url",
            "http://" + tradeHistoryContainer.getHost() + ":" + tradeHistoryContainer.getMappedPort(9080) + "/trade-history"
        );
    }

    private static void setupKafkaContainer() {
        // START KAFKA *BEFORE* CDI boots
        log.info("Starting Kafka container...");
        kafka.start();
        var kafkaServers = kafka.getBootstrapServers();
        log.info("Started Kafka container with bootstrap servers (mapped port for localhost): " + kafkaServers);
        log.info("Kafka host info: " + kafka.getHost());
        log.info("Network aliases" + kafka.getNetworkAliases().toString());
        log.info("Mapped ports:" + kafka.getMappedPort(9092).toString());

        log.info("Creating topic 'stocktrader'...");
        try {
            TradePublisher.ensureTopic(kafkaServers, "stocktrader", 1, (short) 1);
            log.info("Created topic 'stocktrader'");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static void setupMongoDBContainer() {
        log.info("Starting MongoDB container...");
        mongoDBContainer.start();
        var mongoDBConnectionString = "mongodb://localhost:" + mongoDBContainer.getMappedPort(27017);
        log.info(mongoDBConnectionString);
        log.info("Started MongoDB container");

        log.info("Populating MongoDB with test data...");

        CodecRegistry pojoCodecRegistry = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        MongoDatabase database;
        MongoClient mongoClient;
        mongoClient = new MongoClient(mongoDBConnectionString);
        database = mongoClient.getDatabase(MONGO_DATABASE).withCodecRegistry(pojoCodecRegistry);
        MongoCollection tradesCollection;
        try {
            tradesCollection = database.getCollection(TRADE_COLLECTION_NAME, Transaction.class);
        } catch (IllegalArgumentException e) {
            database.createCollection(TRADE_COLLECTION_NAME);
            tradesCollection = database.getCollection(TRADE_COLLECTION_NAME, Transaction.class);
        }

        var fixed = Instant.from(OffsetDateTime.of(2025, 8, 28, 10, 15, 30, 0, ZoneOffset.UTC));
        var fixed1 = Instant.from(OffsetDateTime.of(2025, 8, 28, 10, 20, 30, 0, ZoneOffset.UTC));
        var fixed2 = Instant.from(OffsetDateTime.of(2025, 8, 28, 10, 30, 30, 0, ZoneOffset.UTC));

        var t1 = new Transaction("Ryan", 2, "AAPL", 300.0f, 150.0f, fixed);
        var t2 = new Transaction("Ryan", 4, "KD", 150.0f, 15.0f, fixed1);
        var t3 = new Transaction("Karri", 5, "AMZN", 200.0f, 3.0f, fixed);
        var t4 = new Transaction("John", 7, "MSFT", 100.0f, 40.0f, fixed);
        var t5 = new Transaction("Ryan", 1, "KD", 150.0f, 15.0f, fixed2);
        var trades = tradesCollection.insertMany(Arrays.asList(t1, t2, t3, t4, t5));

        log.info("Inserted: " + trades);
    }

    private static void setTradeHistoryContainer() {
        log.info("Starting Trade History container...");

        tradeHistoryContainer
            // global connector settings (liberty-kafka)
            .withEnv("MP_CONFIG_PROFILE", "test") // activate test profile
            .withEnv("MP_MESSAGING_CONNECTOR_LIBERTY_KAFKA_BOOTSTRAP_SERVERS", "kafka:19092") // Use the mapped kafka port.
            .withEnv("MP_MESSAGING_CONNECTOR_LIBERTY_KAFKA_SECURITY_PROTOCOL", "PLAINTEXT")

            // channel-specific overrides for your incoming channel "stocktrader"
            .withEnv("MP_MESSAGING_INCOMING_STOCKTRADER_CONNECTOR", "liberty-kafka")
            .withEnv("MP_MESSAGING_INCOMING_STOCKTRADER_SECURITY_PROTOCOL", "PLAINTEXT")
            .withEnv("MP_MESSAGING_INCOMING_STOCKTRADER_TOPIC", "stocktrader")
            .withEnv("MP_MESSAGING_INCOMING_STOCKTRADER_GROUP_ID",
                "trade-history-" + UUID.randomUUID().toString())
            // Mongo settings
            .withEnv("MONGO_CONNECTION_STRING", "mongodb://mongodb:27017") //+ mongoDBContainer.getMappedPort(27017))
            .withEnv("MONGO_DATABASE", "tradeHistoryCollection")
            .start();
        log.info("Started Trade History container");
    }

    @AfterAll
    public static void tearDown() {
        tradeHistoryContainer.stop();
        kafka.stop();
        mongoDBContainer.stop();
        network.close();
    }

    @Test
    @Order(1)
    public void testGetTradesByOwner() {
        log.info("TEST: Get trades for Ryan");
        var tradesByRyan = client.getTradesByOwner("Ryan");
        assertEquals(3, tradesByRyan.transactions().size());
        assertThat(tradesByRyan.transactions())
            .hasSize(3)
            .extracting(Transaction::shares, Transaction::symbol)
            .contains(tuple(2, "AAPL"), tuple(4, "KD"), tuple(1, "KD"));
    }

    @Test
    @Order(2)
    public void testGetNotional() {
        log.info("TEST: Get notional for Ryan");
        var notional = client.getNotional("Ryan");
        assertEquals(1350.0f, notional);
    }

    @Test
    @Order(3)
    public void testGetCurrentShares() {
        log.info("TEST: Get current shares of KD for Ryan");
        var shares = client.getCurrentShares("Ryan", "KD");
        assertEquals(5, shares.shares());
    }

    @Test
    @Order(4)
    public void testGetPortfolioShares() {
        log.info("TEST: Get current portfolio shares for Ryan");
        var shares = client.getPortfolioShares("Ryan");
        assertEquals(2, shares.shares().size());
        assertThat(shares.shares())
            .hasSize(2)
            .extracting(Share::shares, Share::symbol)
            .contains(tuple(2, "AAPL"), tuple(5, "KD"));
    }

    @Test
    @Order(5)
    public void testGetTradesByOwnerAndSymbol() {
        log.info("TEST: Get current portfolio shares for Ryan & KD");
        var trades = client.getTradesByOwnerAndSymbol("Ryan", "KD");
        assertEquals(2, trades.transactions().size());
        assertThat(trades.transactions())
            .hasSize(2)
            .extracting(Transaction::shares, Transaction::symbol)
            .contains(tuple(4, "KD"), tuple(1, "KD"));
    }

    @Test
    @Order(6)
    public void testGetReturns() {
        log.info("TEST: Get returns for Ryan w/portfolio value 10000.0");
        var trades = client.getReturns("Ryan", 10000.0);
        assertEquals(627.41, trades);
    }

    @Test
    @Order(7)
    public void testLatestBuy() {
        log.info("TEST: Get latest buy");
        var buy = client.latestBuy();
        log.info("Latest buy: " + buy.toString());
        assertThat(buy)
            .isNotNull()
            .extracting(Transaction::owner, Transaction::shares, Transaction::symbol)
            .contains("Sally", 140, "IBM");
    }

    @Test
    @Order(8)
    public void testKafka() throws Exception {
        log.info("TEST: Kafka Integration");

        var fixed = Instant.from(OffsetDateTime.now());

        var purchase = new StockPurchase(
            UUID.randomUUID().toString(), "Sally", "IBM", 140, 45.0f, fixed, 5.99f);

        String bs = kafka.getBootstrapServers();      // from your Testcontainers Kafka
        String topic = "stocktrader";

        var md = TradePublisher.sendStockPurchase(bs, topic, purchase);
        log.info("Published to " + md.topic() + " partition " + md.partition() + " @ offset " + md.offset());
        log.info("Sleeping for consumption of message...");
        Thread.sleep(3000); // wait for it to be consumed
        log.info("Waking up and checking...");

//        var purchased = TradePublisher.awaitStockPurchase(bs, topic, null, java.time.Duration.ofSeconds(5));
//        log.info("Consumed: " + purchased.toString());
//        
//        // Prove we can publish to Kafka from Unit test and that we can process that message
//        var md1 = TradePublisher.publishAndVerify(bs, topic, purchase, Duration.ofSeconds(10));
//        log.info("Published and received: " + md1.toString());

        var sally = client.getTradesByOwner("Sally").transactions().getFirst();
        log.info("Trades for Sally: " + sally.toString());
        assertThat(sally)
            .isNotNull()
            .extracting(Transaction::owner, Transaction::shares, Transaction::symbol)
            .contains("Sally", 140, "IBM");
    }
}

