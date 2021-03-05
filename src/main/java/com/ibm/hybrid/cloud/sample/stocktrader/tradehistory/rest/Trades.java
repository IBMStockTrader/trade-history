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
package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.MongoConnector;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.bson.Document;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import org.json.JSONObject;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.client.Quote;

import io.swagger.annotations.Api;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.kafka.Consumer;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.demo.*;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;

@Path("/")
@Api( tags = {"trade-history"} )
@Produces("application/json")
@OpenAPIDefinition(
    info = @Info(
        title = "Trade History",
        version = "0.0",
        description = "TradeHistory API",
        contact = @Contact(url = "https://github.com/IBMStockTrader", name = "IBMStockTrader"),
        license = @License(name = "License", url = "https://github.com/IBMStockTrader/trade-history/blob/master/LICENSE")
        )
)
public class Trades {

    
    @Inject
    private Consumer consumer;

    private MessageController messageController = null;
    private static MongoConnector MONGO_CONNECTOR;
    public static MongoConnector mConnector;



    private class MessageController {
        KafkaConsumer consumer = new KafkaConsumer();
        BlockingQueue<DemoConsumedMessage> queue = new LinkedBlockingQueue<>();
        
        void start() {
            if(MONGO_CONNECTOR != null) {
                consumer.messageQueue = queue;
                Thread thread = new Thread(consumer);
                thread.start();
            }
            else {
                System.out.println("Mongo not initialized properly");
                stop();
            }
        }
        
        void pause() {
        }
        
        void resume() {
        }
        
        void stop() {
            consumer.exit = true;
        }
    }

    private class KafkaConsumer implements Runnable {
        volatile boolean exit = false;
        BlockingQueue<DemoConsumedMessage> messageQueue;

        public KafkaConsumer(){
            super();
            try {
                MONGO_CONNECTOR = new MongoConnector();
                mConnector = MONGO_CONNECTOR;
            }
            catch(NullPointerException e) {
                e.printStackTrace();
                MONGO_CONNECTOR = null;
            }  
            catch(IllegalArgumentException e) {
                e.printStackTrace();
                MONGO_CONNECTOR = null;
            }
            catch(Exception e) {
                e.printStackTrace();
                MONGO_CONNECTOR = null;
            }
        }
        @Override
        public void run() {
            while (!exit) {
                System.out.println("Consuming messages from Kafka");
                ConsumerRecords<String, String> records = consumer.consume();
                System.out.println("Processing records");
                for (ConsumerRecord<String, String> record : records) {
                    DemoConsumedMessage message = new DemoConsumedMessage(record.topic(), record.partition(),
                            record.offset(), record.value(), record.timestamp());
                    
                    StockPurchase sp = new StockPurchase(message.getValue());
                    MONGO_CONNECTOR.insertStockPurchase(sp, message.getTopic());
                    try {
                        System.out.println(String.format("Consumed message %s",message.encode()));
                        while (!exit && !messageQueue.offer(message, 1, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            System.out.println("Closing consumer");
            consumer.shutdown();
            System.out.println("Consumer closed");
        }

    }

    @PostConstruct
    public void initialize(){
        try {
            System.out.println("Starting message consumption");
            messageController = new MessageController();
            messageController.start();
        }
        catch( NullPointerException e) {
            System.out.println(e.getMessage());
        }
        catch(IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Path("/latestBuy")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "404",
            description = "The Mongo database cannot be found. ",
            content = @Content(
                        mediaType = "text/plain")),
        @APIResponse(
            responseCode = "200",
            description = "The latest trade has been retrieved successfully.",
            content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = Quote.class)))})
    @Operation(
        summary = "Shows the latest trade.",
        description = "Retrieve the latest record from the mongo database."
    )
    public String latestBuy() {
        JSONObject json = new JSONObject();
        MongoClient mClient = MongoConnector.mongoClient;
        
        long dbSize = mClient.getDatabase("test").getCollection("test_collection").count();
        int approxDbSize = Math.toIntExact(dbSize);

        FindIterable<Document> docs = mClient.getDatabase("test").getCollection("test_collection").find().skip(approxDbSize - 1);
        for (Document doc : docs) {
            json.put("trade", doc.toJson());
        }
        return json.getString("trade");
    }

    @Path("/trades/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "404",
            description = "The Mongo database cannot be found.",
            content = @Content(
                        mediaType = "text/plain")),
        @APIResponse(
            responseCode = "200",
            description = "The trades for the requested owner have been retrieved successfully.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Quote.class)))})
    @Operation(summary = "Get trade history of specified owner",
        description = "Get an array of owner's transactions")
    public String getTradesByOwner(@Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName) {
    
        return mConnector.getTrades(ownerName).toString();

    }

    @Path("/trades/{owner}/{symbol}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "404",
            description = "The Mongo database cannot be found.",
            content = @Content(
                        mediaType = "text/plain")),
        @APIResponse(
            responseCode = "200",
            description = "The ROI for the requested owner and symbol have been retrieved successfully.",
            content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = Quote.class)))})
    @Operation(summary = "Get trade histoiry of specified owner for the specified stock symbol",
        description = "Get an array of the owner's transactions for the specified stock symbol")
    public String getROI(
        @Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName, 
        @Parameter(description="Symbol name", required = true) @PathParam("symbol") String symbol) {

        return mConnector.getTradesForSymbol(ownerName, symbol).toString();

    }

    @Path("/shares/{owner}/{symbol}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "404",
            description = "The Mongo database cannot be found.",
            content = @Content(
                        mediaType = "text/plain")),
        @APIResponse(
            responseCode = "200",
            description = "The ROI for the requested owner and symbol have been retrieved successfully.",
            content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = Quote.class)))})
    @Operation(summary = "Get the number of shares owned by specified owner for a specified stock symbol.")
    public String getCurrentShares(
        @Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName, 
        @Parameter(description="Symbol name", required = false) @PathParam("symbol") String symbol) {

        return mConnector.getSymbolShares(ownerName, symbol).toString();

    }

    @Path("/shares/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "404",
            description = "The Mongo database cannot be found.",
            content = @Content(
                        mediaType = "text/plain")),
        @APIResponse(
            responseCode = "200",
            description = "The shares for the requested owner and symbol have been retrieved successfully.",
            content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = Quote.class)))})
    @Operation(summary = "Get the number of shares of all owned stock by specified owner.")
    public String getPortfolioShares(@Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName) {

        return mConnector.getPortfolioSharesJSON(ownerName).toString();

    }

    @Path("/notional/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "404",
            description = "The Mongo database cannot be found. ",
            content = @Content(
                        mediaType = "text/plain")),
        @APIResponse(
            responseCode = "200",
            description = "The notional for the requested owner and symbol have been retrieved successfully.",
            content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = Quote.class)))})
    public String getNotional(
        @Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName) {

        return mConnector.getTotalNotional(ownerName).toString();
        
    }

    @Path("/returns/{owner}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "404",
            description = "The Mongo database cannot be found.",
            content = @Content(
                        mediaType = "text/plain")),
        @APIResponse(
            responseCode = "200",
            description = "The ROI for the requested owner has been retrieved successfully.",
            content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = Quote.class)))})
    @Operation(summary = "Get the percentage return on portfolio for the specified owner, with passed in portfolio value.")
    public String getReturns(
        @Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName, 
        @Parameter(description="Current portfolio value", required = true) @QueryParam("currentValue") Double portfolioValue) {

        return mConnector.getROI(ownerName, portfolioValue).toString();

    }
}
