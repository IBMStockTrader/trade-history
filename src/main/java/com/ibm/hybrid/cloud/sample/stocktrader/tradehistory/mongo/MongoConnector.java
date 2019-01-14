package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.MongoCredential;
import com.mongodb.client.model.Filters;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Set;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.demo.DemoConsumedMessage;


public class MongoConnector {
    public static MongoDatabase database;
    public static MongoClient mongoClient;
    public static final String TRADE_DATABASE = "test_collection";

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
        MongoCollection<Document> collection = database.getCollection(TRADE_DATABASE);
           Document doc = new Document("topic", dcm.getTopic())
                .append("id", sp.getId())
                .append("owner", sp.getOwner())
                .append("symbol", sp.getSymbol())
                .append("shares", sp.getShares())
                .append("price", sp.getPrice())
                .append("when", sp.getWhen())
                .append("comission", sp.getCommission());
            collection.insertOne(doc);
    }

    /*public void retrieveMostRecentDoc(){
        return database.getCollection().find().skip(database.getCollection().count() - 1);
    }*/

    public JSONObject getTrades(String ownerName) {
        MongoCollection<Document> tradesCollection = database.getCollection(TRADE_DATABASE);

        FindIterable<Document> docs = tradesCollection.find(Filters.eq("owner", ownerName));
        return docsToJsonObject(docs);
    }

    public JSONObject getTradesForSymbol(String ownerName, String symbol) {
        MongoCollection<Document> tradesCollection = database.getCollection(TRADE_DATABASE);

        FindIterable<Document> docs = tradesCollection.find(Filters.and(Filters.eq("owner", ownerName), Filters.eq("symbol", symbol)));
        return docsToJsonObject(docs);
    }

    private JSONObject docsToJsonObject(FindIterable<Document> docs) {
        JSONArray jsonArray = new JSONArray();
        JSONObject json = new JSONObject();
        for (Document doc : docs) {
            JSONObject obj = new JSONObject();

            Set<String> keys = doc.keySet();
            for (String key : keys) {
                obj.put(key, doc.get(key).toString());
            }

            jsonArray.put(obj);
        }

        json.put("transactions", jsonArray);
        return json;
    }

    //StockPurchase purchase = new StockPurchase(tradeID, owner, symbol, shares, price, when, commission);
    
}