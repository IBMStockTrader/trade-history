package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.MongoConnector;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;

import org.bson.Document;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.json.JSONObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/")
@Api( tags = {"trade-history"} )
public class Trades {

    @Path("/latestBuy")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject latestBuy() {
        JSONObject json = new JSONObject();
        try{
            MongoConnector mConnector = new MongoConnector();
            MongoClient mClient = mConnector.mongoClient;
            long dbSize = mClient.getDatabase("test").getCollection("test_collection").count();
            int approxDbSize = Math.toIntExact(dbSize);

            FindIterable<Document> docs = mClient.getDatabase("test").getCollection("test_collection").find().skip(approxDbSize - 1);
            for (Document doc : docs) {
                json.put("trade", doc.toJson());
            }
        }   
        catch( NullPointerException e){
            System.out.println(e.getMessage());
        }
        catch(IllegalArgumentException e){
            System.out.println(e.getMessage());
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
    @Operation(summary = "Get trade history of specified owner",
        description = "Get an array of owner's transactions")
    public String getTradesByOwner(@Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName) {
        try{
            MongoConnector mConnector = new MongoConnector();
            return mConnector.getTrades(ownerName).toString();
        }
        catch( NullPointerException e){
            System.out.println(e.getMessage());
        }
        catch(IllegalArgumentException e){
            System.out.println(e.getMessage());
        }
        return null;
    }

    @Path("/trades/{owner}/{symbol}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get trade histoiry of specified owner for the specified stock symbol",
        description = "Get an array of the owner's transactions for the specified stock symbol")
    public String getROI(
        @Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName, 
        @Parameter(description="Symbol name", required = true) @PathParam("symbol") String symbol) {

        try{
            MongoConnector mConnector = new MongoConnector();
            return mConnector.getTradesForSymbol(ownerName, symbol).toString();
        }
        catch( NullPointerException e){
            System.out.println(e.getMessage());
        }
        catch(IllegalArgumentException e){
            System.out.println(e.getMessage());
        }
        return null;
    }

    @Path("/shares/{owner}/{symbol}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the number of shares owned by specified owner for a specified stock symbol.")
    public String getCurrentShares(
        @Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName, 
        @Parameter(description="Symbol name", required = false) @PathParam("symbol") String symbol) {
        try{
            MongoConnector mConnector = new MongoConnector();
            return mConnector.getSymbolShares(ownerName, symbol).toString();
        }
        catch( NullPointerException e){
            System.out.println(e.getMessage());
        }
        catch(IllegalArgumentException e){
            System.out.println(e.getMessage());
        }
        return null;
    }

    @Path("/shares/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the number of shares of all owned stock by specified owner.")
    public String getPortfolioShares(@Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName) {
        try{
            MongoConnector mConnector = new MongoConnector();
            return mConnector.getPortfolioSharesJSON(ownerName).toString();
        }
        catch( NullPointerException e){
            System.out.println(e.getMessage());
        }
        catch(IllegalArgumentException e){
            System.out.println(e.getMessage());
        }
        return null;
    }

    // @Path("/equity/{owner}")
    // @GET
    // @Produces(MediaType.APPLICATION_JSON)
    // public String getEquity(@PathParam("owner") String ownerName, @Context HttpServletRequest request) {
    //     MongoConnector mConnector = new MongoConnector();
    //     return mConnector.getPortfolioEquity(ownerName, request).toString();
    // }

    // @Path("/equity/{owner}/{symbol}")
    // @GET
    // @Produces(MediaType.APPLICATION_JSON)
    // public String getSymbolEquity(@PathParam("owner") String ownerName, @PathParam("symbol") String symbol, @Context HttpServletRequest request) {
    //     MongoConnector mConnector = new MongoConnector();
    //     String jwt = request.getHeader("Authorization");

    //     return mConnector.getSymbolEquity(jwt, ownerName, symbol).toString();
    // }

    @Path("/notional/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getNotional(
        @Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName) {
        
        try{
            MongoConnector mConnector = new MongoConnector();
            return mConnector.getTotalNotional(ownerName).toString();
        }
        catch( NullPointerException e){
            System.out.println(e.getMessage());
        }
        catch(IllegalArgumentException e){
            System.out.println(e.getMessage());
        }
        return null;
        
    }

    @Path("/returns/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the percentage return on portfolio for the specified owner, with passed in portfolio value.")
    public String getReturns(
        @Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName, 
        @Parameter(description="Current portfolio value", required = true) @QueryParam("currentValue") Double portfolioValue) {

        try{
            MongoConnector mConnector = new MongoConnector();
            return mConnector.getROI(ownerName, portfolioValue).toString();
        }
        catch( NullPointerException e){
            System.out.println(e.getMessage());
        }
        catch(IllegalArgumentException e){
            System.out.println(e.getMessage());
        }
        return null;
    }
}
