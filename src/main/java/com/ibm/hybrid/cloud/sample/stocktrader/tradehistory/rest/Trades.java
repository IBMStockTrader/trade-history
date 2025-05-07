/*
       Copyright 2018-2021 IBM Corp All Rights Reserved
       Copyright 2022-2024 Kyndryl, All Rights Reserved
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

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.client.Quote;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.MongoConnector;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;

import org.bson.Document;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.json.JSONObject;

import io.swagger.annotations.Api;

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
@RequestScoped
public class Trades {

    @Inject
    private MongoConnector mConnector;

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
    @WithSpan
    public String latestBuy() {
        JSONObject json = new JSONObject();
        MongoClient mClient = MongoConnector.mongoClient;
        
        long dbSize = mClient.getDatabase("test").getCollection("test_collection").countDocuments();
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
    @WithSpan
    public String getTradesByOwner(@Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName) {
    
        String trades = "Unknown";

        if (mConnector!=null) trades = mConnector.getTrades(ownerName).toString();

        return trades;
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
    @WithSpan
    public String getROI(
        @Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName, 
        @Parameter(description="Symbol name", required = true) @PathParam("symbol") String symbol) {

        String roi = "Unknown";

        if (mConnector!=null) roi = mConnector.getTradesForSymbol(ownerName, symbol).toString();

        return roi;
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
    @WithSpan
    public String getCurrentShares(
        @Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName, 
        @Parameter(description="Symbol name", required = false) @PathParam("symbol") String symbol) {

        String shares = "Unknown";

        if (mConnector!=null) shares = mConnector.getSymbolShares(ownerName, symbol).toString();

        return shares;
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
    @WithSpan
    public String getPortfolioShares(@Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName) {

        String shares = "Unknown";

        if (mConnector!=null) shares = mConnector.getPortfolioSharesJSON(ownerName).toString();

        return shares;
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
    @WithSpan
    public String getNotional(
        @Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName) {

        String notional = "Unknown";

        if (mConnector!=null) notional = mConnector.getTotalNotional(ownerName).toString();

        return notional;
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
    @WithSpan
    public String getReturns(
        @Parameter(description="Owner name", required = true) @PathParam("owner") String ownerName, 
        @Parameter(description="Current portfolio value", required = true) @QueryParam("currentValue") Double portfolioValue) {

        String roi = "Unknown";

        if (mConnector!=null) roi = mConnector.getROI(ownerName, portfolioValue).toString();

        return roi;
    }
}
