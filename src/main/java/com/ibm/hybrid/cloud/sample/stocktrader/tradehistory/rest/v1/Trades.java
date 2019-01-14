package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import static com.mongodb.client.model.Filters.eq;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.MongoConnector;

@Path("/")
public class Trades {

    @Path("/example")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response example() {
        List<String> list = new ArrayList<>();
        //return a simple list of strings
        list.add("Congratulations, your application is up and running");
        return Response.ok(list.toString()).build();
    }

    //com.ibm.hybrid.cloud.sample.stocktrader.tradehistory
    //{ "owner":"John", "symbol":"IBM", "shares":3, "price":120, "when":"now", "comission":0  } 
    // URL: /tradeHistory/com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest/v1/trade
    @Path("/trade")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject getTrades() {
        //TODO: com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest call to get data from Mongo API
        JSONObject json = new JSONObject();
        json.put("id", "testObject");
        return json;
    }

    @Path("/latestBuy")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject latestBuy() {
        MongoConnector mConnector = new MongoConnector();
        MongoClient mClient = mConnector.mongoClient;
        //TODO: com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest call to get data from Mongo API
        JSONObject json = new JSONObject();
        long dbSize = mClient.getDatabase("test").getCollection("test_collection").count();
        int approxDbSize = Math.toIntExact(dbSize);

        FindIterable<Document> docs = mClient.getDatabase("test").getCollection("test_collection").find().skip(approxDbSize - 1);
        for (Document doc : docs) {
            json.put("trade", doc.toJson());
        }

        return json;
    }

    @Path("/totalTrades")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject totalTrades() {
        JSONObject json = new JSONObject();

        return json;
    }

    @Path("/trades/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject getTrades(@PathParam("owner") String ownerName) {
        MongoConnector mConnector = new MongoConnector();
        MongoClient mClient = mConnector.mongoClient;
        JSONArray jsonArray = new JSONArray();
        JSONObject json = new JSONObject();
        long dbSize = mClient.getDatabase("test").getCollection("test_collection").count();
        int approxDbSize = Math.toIntExact(dbSize);

        FindIterable<Document> docs = mClient.getDatabase("test").getCollection("test_collection").find(eq("owner", ownerName));
        for (Document doc : docs) {
            JSONObject obj = new JSONObject();

            Set<String> keys = doc.keySet();
            for (String key : keys) {
                obj.put(key, doc.get(key).toString());
            }

            jsonArray.add(obj);
        }
        json.put("transactions", jsonArray);
        return json;
    }
}
