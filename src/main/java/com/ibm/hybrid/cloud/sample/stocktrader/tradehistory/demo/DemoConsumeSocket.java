package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.demo;

import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.net.MalformedURLException;


import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.log4j.Logger;

import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

 import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.MongoConnector;
 import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.StockPurchase;
 import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.kafka.Consumer;


@ServerEndpoint(value = "/democonsume", encoders = { DemoMessageEncoder.class })
public class DemoConsumeSocket {

    private final String BOOTSTRAP_SERVER_ENV_KEY = "BOOTSTRAP_SERVER";
    private final String TOPIC_ENV_KEY = "TOPIC";

    private Session currentSession = null;
    private Consumer consumer = null;

    private MessageController messageController = null;

    private Logger logger = Logger.getLogger(DemoConsumeSocket.class);

    private static MongoConnector MONGO_CONNECTOR;

    @Resource
    ManagedExecutorService managedExecutorService;

    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        logger.debug(String.format("Socket opened with id %s", session.getId()));
        currentSession = session;
        String bootstrapServerAddress = System.getenv(BOOTSTRAP_SERVER_ENV_KEY).replace("\"", "");
        String topic = System.getenv(TOPIC_ENV_KEY).replace("\"", "");
        try {
            consumer = new Consumer(bootstrapServerAddress, topic);
        } catch (InstantiationException e) {
            onError(e);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try (JsonReader reader = Json.createReader(new StringReader(message))) {
            JsonObject jsonMessage = reader.readObject();
            String action = jsonMessage.getString("action");
            logger.debug(String.format("Message received from session %s with action %s", session.getId(), action));
            switch (action) {
            case "start":
                if (messageController == null) {
                    logger.debug("Starting message consumption");
                    messageController = new MessageController();
                    messageController.start();
                } else {
                    logger.debug("Resuming message consumption");
                    messageController.resume();
                }
                break;
            case "stop":
                logger.debug("Pausing message consumption");
                messageController.pause();
                break;
            default:
                logger.warn("Received message with unknown action, expected 'start' or 'stop'.");
                break;
            }
        } catch (JsonException | IllegalStateException e) {
            onError(e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.debug("Closed websocket");
        if (messageController != null) {
            logger.debug("Stopping message controller");
            messageController.stop();
        }
        logger.info(String.format("Consumer and client connection for session %s closed.", session.getId()));
    }

    @OnError
    public void onError(Throwable throwable) {
        logger.error(throwable);
        try {
            CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                    throwable.getMessage());
            currentSession.close(closeReason);
        } catch (IOException e) {
            logger.error(e);
        }
    }
    
    private class MessageController {
        MessageSender sender = new MessageSender();
        KafkaConsumer consumer = new KafkaConsumer();
        BlockingQueue<DemoConsumedMessage> queue = new LinkedBlockingQueue<>();
        
        void start() {
            if(MONGO_CONNECTOR != null){
                sender.messageQueue = queue;
                consumer.messageQueue = queue;
                Thread thread = new Thread(sender);
                thread.start();
                thread = new Thread(consumer);
                thread.start();
            }
            else {
                logger.debug("Mongo not initialized properly");
            }
        }
        
        void pause() {
            sender.sendMessages = false;
        }
        
        void resume() {
            sender.sendMessages = true;
        }
        
        void stop() {
            consumer.exit = true;
            sender.exit = true;
        }
    }

    private class MessageSender implements Runnable {
        volatile boolean exit = false;
        volatile boolean sendMessages = true;
        BlockingQueue<DemoConsumedMessage> messageQueue;

        @Override
        public void run() {
            try {
                while (!exit) {
                    int messagesSent = 0;
                    while (!exit && sendMessages) {
                        logger.debug("Sending / waiting for messages, queue depth : " + messageQueue.size());
                        try {
                            DemoConsumedMessage message = messageQueue.poll(1, TimeUnit.SECONDS);
                            if (message != null) {
                                logger.debug(String.format("Updating session %s with new message %s",
                                        currentSession.getId(), message.encode()));
                                currentSession.getBasicRemote().sendObject(message);
                                messagesSent++;
                            }
                        } catch (IOException | EncodeException e) {
                            onError(e);
                        } 
                    }
                    sendMessages = false;
                    Thread.sleep(1000);
                    logger.debug(String.format("Paused consumer for session %s.", currentSession.getId()));
                }
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }

    }

    private class KafkaConsumer implements Runnable {
        volatile boolean exit = false;
        BlockingQueue<DemoConsumedMessage> messageQueue;
        private static final String filepath="consumed.json";

        @Override
        public void run() {
            try{
                MongoConnector MONGO_CONNECTOR = new MongoConnector();
                while (!exit) {
                    logger.debug("Consuming messages from Kafka");
                    ConsumerRecords<String, String> records = consumer.consume();
                    logger.debug("Processing records");
                    for (ConsumerRecord<String, String> record : records) {
                        DemoConsumedMessage message = new DemoConsumedMessage(record.topic(), record.partition(),
                                record.offset(), record.value(), record.timestamp());
                        
                        StockPurchase sp = new StockPurchase(message.getValue());
                        MONGO_CONNECTOR.insertStockPurchase(sp, message);
                        try {
                            logger.debug(String.format("Consumed message %s",message.encode()));
                            while (!exit && !messageQueue.offer(message, 1, TimeUnit.SECONDS));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
            catch(NullPointerException e){
                logger.debug(e.getMessage());
                MONGO_CONNECTOR = null;
            }  
            catch(IllegalArgumentException e) {
                logger.debug(e.getMessage());
                MONGO_CONNECTOR = null;
            }
            catch(MalformedURLException e){
                logger.debug(e.getMessage());
                MONGO_CONNECTOR = null;
            }
            catch(Exception e){
                logger.debug(e.getMessage());
                MONGO_CONNECTOR = null;
            }
            logger.debug("Closing consumer");
            consumer.shutdown();
            logger.debug("Consumer closed");
        }

    }
}