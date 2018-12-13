package application.demo;

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
        //db.createCollection("create_collection_test");

        System.out.println("\n::::No errors here: " + database.getName());
    }

    public void insertFile (DemoConsumedMessage dcm) {
        MongoCollection<Document> collection = database.getCollection("test_collection");
           Document doc = new Document("topic", dcm.getTopic())
                .append("partition", dcm.getPartition())
                .append("offset", dcm.getOffset())
                .append("value", dcm.getValue())
                .append("timestamp", dcm.getTimestamp());
            collection.insertOne(doc);
    }

    //StockPurchase purchase = new StockPurchase(tradeID, owner, symbol, shares, price, when, commission);

    /*
        .add("topic", topic)
        .add("partition", partition)
        .add("offset", offset)
        .add("value", value)
        .add("timestamp", timestamp)

    */

    /*
          {
            "name" : "MongoDB",
            "type" : "database",
            "count" : 1,
            "versions": [ "v3.2", "v3.0", "v2.6" ],
            "info" : { x : 203, y : 102 }
          }
    */

    /*
        MongoCollection<Document> collection = database.getCollection("test");
           Document doc = new Document("name", "MongoDB")
                .append("type", "database")
                .append("count", 1)
                .append("versions", Arrays.asList("v3.2", "v3.0", "v2.6"))
                .append("info", new Document("x", 203).append("y", 102));
            collection.insertOne(doc);
    */
}