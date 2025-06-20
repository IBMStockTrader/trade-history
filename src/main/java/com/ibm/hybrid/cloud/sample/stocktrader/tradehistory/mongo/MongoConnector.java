/*
       Copyright 2018, 2019 IBM Corp All Rights Reserved
       Copyright 2024 Kyndryl All Rights Reserved
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
package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MapReduceIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.MongoCredential;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.client.Quote;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.client.StockQuoteClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MongoConnector {

    private char[] MONGO_PASSWORD;
    private String MONGO_AUTH_DB;
    private String MONGO_USER;
    private String MONGO_IP;
    private int MONGO_PORT;
    private String MONGO_DATABASE;
    private String MONGO_CONNECTION_STRING;

    public static MongoDatabase database;
    public static MongoClient mongoClient;
    private MongoCollection<Document> tradesCollection;
    public static final String TRADE_COLLECTION_NAME = "test_collection";

    private Logger logger = Logger.getLogger(MongoConnector.class.getName());

    @Inject
    @RestClient
    private StockQuoteClient stockQuoteClient;

    // Override Stock Quote Client URL if secret is configured to provide URL
    static {
        String mpUrlPropName = StockQuoteClient.class.getName() + "/mp-rest/url";
        String urlFromEnv = System.getenv("STOCK_QUOTE_URL");
        if ((urlFromEnv != null) && !urlFromEnv.isEmpty()) {
            System.out.println("Using Stock Quote URL from config map: " + urlFromEnv);
            System.setProperty(mpUrlPropName, urlFromEnv);
        } else {
            System.out.println("Stock Quote URL not found from env var from config map, so defaulting to value in jvm.options: " + System.getProperty(mpUrlPropName));
        }
    }

    public MongoConnector() {
        initializeProperties();
        try {
            if (MONGO_IP == null || MONGO_PORT == 0 || MONGO_USER == null || MONGO_AUTH_DB == null || MONGO_PASSWORD == null || MONGO_DATABASE == null) {
                logger.warning("One or more mongo properties cannot be found or were not set.");
                throw new NullPointerException("One or more mongo properties cannot be found or were not set.");
            }

            if ((MONGO_CONNECTION_STRING == null) || MONGO_CONNECTION_STRING.equals("<your Mongo connection string>")) {
                logger.info("Using traditional constructor for MongoClient");

                ArrayList<ServerAddress> seeds = new ArrayList<>();
                if (MONGO_IP.contains(":")) { //host and port (potentially a list thereof) are in a single env var, so parse them apart
                    for (String ipString : MONGO_IP.split(",")) {
                        String[] hostAndPort = ipString.split(":");
                        seeds.add(new ServerAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1])));
                    }
                } else { //host and port are in separate env vars
                    seeds.add(new ServerAddress(MONGO_IP, MONGO_PORT));
                }

                MongoCredential credential = MongoCredential.createCredential(MONGO_USER, MONGO_AUTH_DB, MONGO_PASSWORD);
                MongoClientOptions options = MongoClientOptions.builder().sslEnabled(true).build();
                mongoClient = new MongoClient(seeds, credential, options);
            } else {
                logger.info("Using MongoClientURI constructor for MongoClient");

                MongoClientURI uri = new MongoClientURI(MONGO_CONNECTION_STRING);
                mongoClient = new MongoClient(uri);
            }

            try {
                System.out.println(mongoClient.getClusterDescription().getShortDescription());
            } catch (Exception e) {
                logger.warning("Exception initializing MongoClient: "+e.getMessage());
                mongoClient.close();
                throw e;
            }
            database = mongoClient.getDatabase(MONGO_DATABASE);
            logger.info("Successfully initialized MongoClient");
        } catch (NullPointerException e) {
            throw e;
        }

        try {
            tradesCollection = database.getCollection(TRADE_COLLECTION_NAME);
        } catch (IllegalArgumentException e) {
            database.createCollection(TRADE_COLLECTION_NAME);
            tradesCollection = database.getCollection(TRADE_COLLECTION_NAME);
        }
    }

    private void initializeProperties() {
        //Probably should use mpConfig here instead of raw System.getenv....
        MONGO_PASSWORD = System.getenv("MONGO_PASSWORD").toCharArray();
        MONGO_AUTH_DB = System.getenv("MONGO_AUTH_DB");
        MONGO_USER = System.getenv("MONGO_USER");
        MONGO_IP = System.getenv("MONGO_IP");
        MONGO_PORT = Integer.parseInt(System.getenv("MONGO_PORT"));
        MONGO_DATABASE = System.getenv("MONGO_DATABASE");
        MONGO_CONNECTION_STRING = System.getenv("MONGO_CONNECTION_STRING");
    }

    public MongoConnector(MongoClient mClient, String mongoDatabase, String mongoCollection) {
        mongoClient = mClient;
        database = mongoClient.getDatabase(mongoDatabase);
        database.createCollection(mongoCollection);
        tradesCollection = database.getCollection(mongoCollection);
    }

    //{ "owner":"John", "symbol":"IBM", "shares":3, "price":120, "when":"now", "commission":0  } 
    public void insertStockPurchase(StockPurchase sp, String topic) {
        //Only add to DB if it's a valid Symbol 
        if (sp.getPrice() > 0) {
            Document doc = new Document("topic", topic)
                .append("id", sp.getId())
                .append("owner", sp.getOwner())
                .append("symbol", sp.getSymbol())
                .append("shares", sp.getShares())
                .append("price", sp.getPrice())
                .append("notional", sp.getPrice() * sp.getShares())
                .append("when", sp.getWhen())
                .append("commission", sp.getCommission());
            tradesCollection.insertOne(doc);
        }
    }

    public JSONObject getTrades(String ownerName) {
        // MongoCollection<Document> tradesCollection = database.getCollection(TRADE_DATABASE);

        FindIterable<Document> docs = this.tradesCollection.find(Filters.eq("owner", ownerName));
        return docsToJsonObject(docs, "transactions");
    }

    public JSONObject getTradesForSymbol(String ownerName, String symbol) {
        FindIterable<Document> docs = tradesCollection.find(Filters.and(Filters.eq("owner", ownerName), Filters.eq("symbol", symbol)));
        return docsToJsonObject(docs, "transactions");
    }

    private MongoIterable<Document> getSharesCount(String ownerName, String symbol) {
        // TODO replace this mapReduce with an aggregate like getTotalNotional
        MapReduceIterable<Document> docs = tradesCollection.mapReduce("function() { emit( this.symbol, this.shares); }",
                "function(key, values) { return Array.sum(values) }")
            .filter(Filters.and(Filters.eq("owner", ownerName), Filters.eq("symbol", symbol)));
        return docs;
    }

    public JSONObject getSymbolShares(String ownerName, String symbol) {
        MongoIterable<Document> docs = getSharesCount(ownerName, symbol);
        JSONObject result = docsToJsonObject(docs, "shares");
        return result;
    }

    private MongoIterable<Document> getPortfolioShares(String ownerName) {
        // TODO replace this mapReduce with an aggregate like getTotalNotional
        MapReduceIterable<Document> docs = tradesCollection.mapReduce("function() { emit( this.symbol, this.shares); }",
                "function(key, values) { return Array.sum(values) }")
            .filter(Filters.eq("owner", ownerName));
        return docs;
    }

    public JSONObject getPortfolioSharesJSON(String ownerName) {
        return docsToJsonObject(getPortfolioShares(ownerName), "shares");
    }

    public MapReduceIterable<Document> getStocksNotional(String ownerName) {
        // TODO replace this mapReduce with an aggregate like getTotalNotional
        MapReduceIterable<Document> docs = tradesCollection.mapReduce("function() { emit( this.symbol, this.notional); }",
                "function(key, values) { return Array.sum(values) }")
            .filter(Filters.eq("owner", ownerName));
        return docs;
    }

    public Double getTotalNotional(String ownerName) {
        Document result = tradesCollection.aggregate(Arrays.asList(
            Aggregates.match(Filters.eq("owner", ownerName)),
            Aggregates.group(null, Accumulators.sum("notional", "$notional"))
        )).first();
        logger.fine("Here are the results of the total notional calculation: " + result);
        if (result == null) {
            return Double.valueOf(0);
        } else {
            return result.getDouble("notional");
        }
    }

    public Double getCommissionTotal(String ownerName) {
        Document result = tradesCollection.aggregate(Arrays.asList(
            Aggregates.match(Filters.eq("owner", ownerName)),
            Aggregates.group(null, Accumulators.sum("commission", "$commission"))
        )).first();
        logger.fine("Here are the results of the total commission calculation: " + result);
        if (result == null) {
            return Double.valueOf(0);
        } else {
            return result.getDouble("commission");
        }
    }

    public JSONObject getSymbolNotional(String ownerName, String symbol) {
        // TODO replace this mapReduce with an aggregate like getTotalNotional
        MapReduceIterable<Document> docs = tradesCollection.mapReduce("function() { emit( this.symbol, this.notional); }",
                "function(key, values) { return Array.sum(values) }")
            .filter(Filters.and(Filters.eq("owner", ownerName), Filters.eq("symbol", symbol)));
        JSONObject result = docsToJsonObject(docs, "notional");
        return result;
    }

    /**
     * @param ownerName
     * @return JSONObject containing array of equities
     */
    public JSONObject getPortfolioEquity(String ownerName) {
        // getPortfolioShares, iterate through and use StockQuote to get current price
        // to calculate equity per Symbol

        JSONArray jsonArray = new JSONArray();

        MongoIterable<Document> portfolioShares = getPortfolioShares(ownerName);
        for (Document item : portfolioShares) {
            System.out.println("portfolio item: " + item.toString());

            String symbol = item.get("_id").toString();
            Double shares = Double.parseDouble(item.get("value").toString());
            Double equity = getSymbolEquity(shares, symbol);

            JSONObject obj = new JSONObject();
            obj.put("symbol", symbol);
            obj.put("equity", equity);

            jsonArray.put(obj);
        }
        JSONObject result = new JSONObject().put("portfolio", jsonArray);
        return result;
    }

    private Double getSymbolPrice(String symbol) {
        Quote quote = new Quote();
        try {
            quote = stockQuoteClient.getStockQuote(symbol);
        } catch (Exception e) {
            System.out.println("Error in " + this.getClass().getName());
            System.out.println(e);
        }
        Double price = quote.getPrice();
        return price;
    }

    private Double getSymbolEquity(Double shares, String symbol) {
        Double price = getSymbolPrice(symbol);
        Double equity = price * shares;
        return equity;
    }

    public Double getSymbolEquity(String owner, String symbol) {
        Document doc = getSharesCount(owner, symbol).first(); //getSymbolShares(owner, symbol).get("shares");
        Double shares = doc.getDouble("value");
        return getSymbolEquity(shares, symbol);
    }

    /**
     * @param ownerName
     * @return total value of equity (no symbol breakdown)
     */
    public JSONObject getTotalEquity(String ownerName) {
        //TODO: getPortfolioEquity and reduce value
        JSONArray portfolioEquity = getPortfolioEquity(ownerName).getJSONArray("portfolio");
        for (Object obj : portfolioEquity) {

        }

        JSONObject result = new JSONObject();
        return result;
    }

    /**
     * @param ownerName - String containing owner name
     * @param equity    - equity value passed in from portfolio, current value of portfolio
     * @return - String - percentage return
     */
    public String getROI(String ownerName, Double equity) {
        Double notional = getTotalNotional(ownerName);
        Double commissions = getCommissionTotal(ownerName);
        Double profits = equity - notional - commissions;
        Double roi = profits / notional * 100;
        if (roi.isNaN()) {
            return "None";
        } else {
            return String.format("%.2f", roi);
        }
    }

    private JSONObject docsToJsonObject(MongoIterable<Document> docs, String label) {
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

        json.put(label, jsonArray);
        return json;
    }
}
