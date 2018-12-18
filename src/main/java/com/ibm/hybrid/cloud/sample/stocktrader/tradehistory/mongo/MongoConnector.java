package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.MongoCredential;

import org.bson.Document;
import java.util.Arrays;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.demo.DemoConsumedMessage;


public class MongoConnector {
    public static MongoDatabase database;
    public static MongoClient mongoClient;

    //public void initialize(String MONGO_URL) {
    public static void initialize(MongoCredential credential, ServerAddress sa, String collection) { 
        mongoClient = new MongoClient(sa, Arrays.asList(credential));
        database = mongoClient.getDatabase( collection );
    }

    /*public void insertFile (DemoConsumedMessage dcm) {
        MongoCollection<Document> collection = database.getCollection("test_collection");
           Document doc = new Document("topic", dcm.getTopic())
                .append("partition", dcm.getPartition())
                .append("offset", dcm.getOffset())
                .append("value", dcm.getValue())
                .append("timestamp", dcm.getTimestamp());
            collection.insertOne(doc);
    }*/

    //{ "owner":"John", "symbol":"IBM", "shares":3, "price":120, "when":"now", "comission":0  } 
    public void insertStockPurchase(StockPurchase sp, DemoConsumedMessage dcm) {
        MongoCollection<Document> collection = database.getCollection("test_collection");
           Document doc = new Document("topic", dcm.getTopic())
                .append("owner", sp.getOwner())
                .append("symbol", sp.getSymbol())
                .append("shares", sp.getShares())
                .append("price", sp.getPrice())
                .append("when", sp.getWhen())
                .append("comission", sp.getComission());
            collection.insertOne(doc);
    }

    /*public void retrieveMostRecentDoc(){
        return database.getCollection().find().skip(database.getCollection().count() - 1);
    }*/

    //StockPurchase purchase = new StockPurchase(tradeID, owner, symbol, shares, price, when, commission);
    
}