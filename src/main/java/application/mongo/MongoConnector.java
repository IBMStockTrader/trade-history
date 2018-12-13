package application.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import org.bson.Document;
import java.util.Arrays;
import com.mongodb.Block;

import com.mongodb.client.MongoCursor;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.DB;

import static com.mongodb.client.model.Filters.*;
import com.mongodb.client.result.DeleteResult;
import static com.mongodb.client.model.Updates.*;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.List;

import application.demo.DemoConsumedMessage;

public class MongoConnector {
    public static MongoDatabase database;
    public static MongoClient mongoClient;

    //public void initialize(String MONGO_URL) {
    public static void initialize() { 
        char[] password = {'S','w','U','N','o','y','W','w','I','7'};
        MongoCredential credential = MongoCredential.createCredential("mongo", "admin", password);
        ServerAddress sa = new ServerAddress("9.42.17.249",30282);

        mongoClient = new MongoClient(sa, Arrays.asList(credential));
        database = mongoClient.getDatabase( "test" );

        System.out.println("\n::::No errors here: " + database.getName());
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