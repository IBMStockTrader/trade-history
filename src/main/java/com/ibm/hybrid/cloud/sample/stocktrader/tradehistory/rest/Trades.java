package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.json.JSONObject;

import io.swagger.annotations.ApiOperation;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.MongoConnector;

@Path("/")
public class Trades {

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
    @ApiOperation(value = "Get trades",
        notes = "Get an array of owner's transactions",
        response = JSONObject.class,
        responseContainer = "JSONObject")
    public String getTradesByOwner(@PathParam("owner") String ownerName) {
        MongoConnector mConnector = new MongoConnector();
        return mConnector.getTrades(ownerName).toString();
    }

    @Path("/trades/{owner}/{symbol}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getROI(@PathParam("owner") String ownerName, @PathParam("symbol") String symbol) {
        MongoConnector mConnector = new MongoConnector();

        return mConnector.getTradesForSymbol(ownerName, symbol).toString();
    }

    @Path("/shares/{owner}/{symbol}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getCurrentShares(@PathParam("owner") String ownerName, @PathParam("symbol") String symbol) {
        MongoConnector mConnector = new MongoConnector();
        return mConnector.getSymbolShares(ownerName, symbol).toString();
    }

    @Path("/shares/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getPortfolioShares(@PathParam("owner") String ownerName) {
        MongoConnector mConnector = new MongoConnector();
        return mConnector.getPortfolioSharesJSON(ownerName).toString();
    }

    @Path("/equity/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getEquity(@PathParam("owner") String ownerName, @Context HttpServletRequest request) {
        MongoConnector mConnector = new MongoConnector();
        return mConnector.getPortfolioEquity(ownerName, request).toString();
    }

    @Path("/equity/{owner}/{symbol}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getSymbolEquity(@PathParam("owner") String ownerName, @PathParam("symbol") String symbol, @Context HttpServletRequest request) {
        MongoConnector mConnector = new MongoConnector();
        String jwt = request.getHeader("Authorization");

        return mConnector.getSymbolEquity(jwt, ownerName, symbol).toString();
    }

    @Path("/notional/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getNotional(@PathParam("owner") String ownerName, @QueryParam("currentValue") Double portfolioValue) {
        MongoConnector mConnector = new MongoConnector();
        return mConnector.getTotalNotional(ownerName).toString();
    }

    @Path("/returns/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getReturns(@PathParam("owner") String ownerName, @QueryParam("currentValue") Double portfolioValue) {
        MongoConnector mConnector = new MongoConnector();
        return mConnector.getROI(ownerName, portfolioValue).toString();
    }
}
