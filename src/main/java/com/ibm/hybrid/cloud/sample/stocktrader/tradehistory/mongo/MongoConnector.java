/*
       Copyright 2018, 2019 IBM Corp All Rights Reserved
       Copyright 2024-2025 Kyndryl All Rights Reserved
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

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.client.Quote;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.client.StockQuoteClient;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest.*;
import com.mongodb.*;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.java.Log;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Log
@ApplicationScoped
public class MongoConnector {

    public static final String TRADE_COLLECTION_NAME = "test_collection";
    public static MongoDatabase database;
    public static MongoClient mongoClient;

    // Override Stock Quote Client URL if secret is configured to provide URL
    static {
        String mpUrlPropName = StockQuoteClient.class.getName() + "/mp-rest/url";
        String urlFromEnv = System.getenv("STOCK_QUOTE_URL");
        if ((urlFromEnv != null) && !urlFromEnv.isEmpty()) {
            log.info("Using Stock Quote URL from config map: " + urlFromEnv);
            System.setProperty(mpUrlPropName, urlFromEnv);
        } else {
            log.info("Stock Quote URL not found from env var from config map, so defaulting to value in jvm.options: " + System.getProperty(mpUrlPropName));
        }
    }

    private char[] MONGO_PASSWORD;
    private String MONGO_AUTH_DB;
    private String MONGO_USER;
    private String MONGO_IP;
    private int MONGO_PORT;
    private String MONGO_DATABASE;
    private String MONGO_CONNECTION_STRING;
    private MongoCollection<Document> tradesCollection;
    @Inject
    @RestClient
    private StockQuoteClient stockQuoteClient;

    public MongoConnector() {
        initializeProperties();
        try {
            if ((MONGO_CONNECTION_STRING == null) || MONGO_CONNECTION_STRING.equals("<your Mongo connection string>")) {
                log.info("Using traditional constructor for MongoClient");

                if (MONGO_IP == null || MONGO_PORT == 0 || MONGO_USER == null || MONGO_AUTH_DB == null || MONGO_PASSWORD == null || MONGO_DATABASE == null) {
                    log.warning("One or more mongo properties cannot be found or were not set.");
                    throw new NullPointerException("One or more mongo properties cannot be found or were not set.");
                }

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
                log.info("Using MongoClientURI constructor for MongoClient");

                MongoClientURI uri = new MongoClientURI(MONGO_CONNECTION_STRING);
                mongoClient = new MongoClient(uri);
            }

            try {
                log.fine(mongoClient.getClusterDescription().getShortDescription());
            } catch (Exception e) {
                log.warning("Exception initializing MongoClient: " + e.getMessage());
                mongoClient.close();
                throw e;
            }

            // Include the codec registry to deserialize Documents and fields into POJOs
            CodecRegistry pojoCodecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            database = mongoClient.getDatabase(MONGO_DATABASE)
                .withCodecRegistry(pojoCodecRegistry);
            log.info("Successfully initialized MongoClient");
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

    public MongoConnector(MongoClient mClient, String mongoDatabase, String mongoCollection) {
        mongoClient = mClient;
        database = mongoClient.getDatabase(mongoDatabase);
        database.createCollection(mongoCollection);
        tradesCollection = database.getCollection(mongoCollection);
    }

    private void initializeProperties() {
        //Probably should use mpConfig here instead of raw System.getenv....
        try {
            MONGO_PASSWORD = System.getenv("MONGO_PASSWORD").toCharArray();
        } catch (NullPointerException npe) {
            log.fine("MONGO_PASSWORD not set");
        }
        MONGO_AUTH_DB = System.getenv("MONGO_AUTH_DB");
        MONGO_USER = System.getenv("MONGO_USER");
        MONGO_IP = System.getenv("MONGO_IP");
        try {
            MONGO_PORT = Integer.parseInt(System.getenv("MONGO_PORT"));
        } catch (NumberFormatException nfe) {
            log.fine("MONGO_PORT not set");
        }
        MONGO_DATABASE = System.getenv("MONGO_DATABASE");
        MONGO_CONNECTION_STRING = System.getenv("MONGO_CONNECTION_STRING");
    }

    //{ "owner":"John", "symbol":"IBM", "shares":3, "price":120, "when":"now", "commission":0  } 
    public void insertStockPurchase(StockPurchase sp, String topic) {
        //Only add to DB if it's a valid Symbol 
        if (sp.price() > 0) {
            Document doc = new Document("topic", "stocktrader")
                .append("id", sp.id())
                .append("owner", sp.owner())
                .append("symbol", sp.symbol())
                .append("shares", sp.shares())
                .append("price", sp.price())
                .append("notional", sp.price() * sp.shares())
                .append("when", sp.when())
                .append("commission", sp.commission());
            tradesCollection.insertOne(doc);
        }
    }

    public Transactions getTrades(String ownerName) {
        MongoCollection<Transaction> tradesCollection = database.getCollection(TRADE_COLLECTION_NAME, Transaction.class);

        List<Transaction> trades = new ArrayList<Transaction>();
        tradesCollection.find(eq("owner", ownerName)).into(trades);
        trades.forEach(trade -> log.finest(trade.toString()));

        var t = new Transactions(trades);
        return t;
    }

    public Transactions getTradesForSymbol(String ownerName, String symbol) {
        MongoCollection<Transaction> tradesCollection = database.getCollection(TRADE_COLLECTION_NAME, Transaction.class);

        List<Transaction> trades = new ArrayList<Transaction>();
        tradesCollection.find(and(eq("owner", ownerName), eq("symbol", symbol))).into(trades);
        trades.forEach(trade -> log.finest(trade.toString()));

        var t = new Transactions(trades);
        return t;
    }

    private MongoIterable<Document> getSharesCount(String ownerName, String symbol) {
        AggregateIterable<Document> docs = tradesCollection.aggregate(Arrays.asList(
            match(and(eq("owner", ownerName), eq("symbol", symbol))),
            group("$symbol", sum("value", "$shares"))
        ));
        return docs;
    }

    public Share getSymbolShares(String ownerName, String symbol) {
        MongoIterable<Document> docs = getSharesCount(ownerName, symbol);
        var result = new Share(symbol, docs.first().getInteger("value"));
        //JSONObject result = docsToJsonObject(docs, "shares");
        return result;
    }

    public Shares getPortfolioShares(String ownerName) {
        var docs = tradesCollection.aggregate(Arrays.asList(
            match(eq("owner", ownerName)),
            group("$symbol", sum("value", "$shares"))
        )).into(new ArrayList<>());
        var result = docs.stream().map(d ->
                new Share(d.getString("_id"), d.getInteger("value")))
            .toList();
        var s = new Shares(result);
        return s;
    }

    public MongoIterable<Document> getStocksNotional(String ownerName) {
        AggregateIterable<Document> docs = tradesCollection.aggregate(Arrays.asList(
            match(eq("owner", ownerName)),
            group("$symbol", sum("value", "$notional"))
        ));

        return docs;
    }

    public Double getTotalNotional(String ownerName) {
        Document result = tradesCollection.aggregate(Arrays.asList(
            match(eq("owner", ownerName)),
            group(null, sum("notional", "$notional"))
        )).first();
        log.fine("Here are the results of the total notional calculation: " + result);
        if (result == null) {
            return Double.valueOf(0);
        } else {
            return result.getDouble("notional");
        }
    }

    public Double getCommissionTotal(String ownerName) {
        Document result = tradesCollection.aggregate(Arrays.asList(
            match(eq("owner", ownerName)),
            group(null, sum("commission", "$commission"))
        )).first();
        log.fine("Here are the results of the total commission calculation: " + result);
        if (result == null) {
            return Double.valueOf(0);
        } else {
            return result.getDouble("commission");
        }
    }

    @Deprecated
    public JSONObject getSymbolNotional(String ownerName, String symbol) {
        AggregateIterable<Document> docs = tradesCollection.aggregate(Arrays.asList(
            match(and(eq("owner", ownerName), eq("symbol", symbol))),
            group("$symbol", sum("value", "$notional"))
        ));
        JSONObject result = docsToJsonObject(docs, "notional");
        return result;
    }

    /**
     * @param ownerName
     * @return JSONObject containing array of equities
     */
    public List<ShareEquity> getPortfolioEquity(String ownerName) {
        // getPortfolioShares, iterate through and use StockQuote to get current price
        // to calculate equity per Symbol

        List<ShareEquity> equities = new ArrayList<>();

        var portfolioShares = getPortfolioShares(ownerName);
        for (Share item : portfolioShares.shares()) {
            log.fine("portfolio item: " + item.toString());

            String symbol = item.symbol();
            Integer shares = item.shares();
            Double equity = getSymbolEquity(shares, symbol);

            var se = new ShareEquity(symbol, equity);

            equities.add(se);
        }
        return equities;
    }

    private Double getSymbolPrice(String symbol) {
        Quote quote = new Quote();
        try {
            quote = stockQuoteClient.getStockQuote(symbol);
        } catch (Exception e) {
            log.severe("Error in " + this.getClass().getName());
            log.severe(e.toString());
        }
        Double price = quote.getPrice();
        return price;
    }

    private Double getSymbolEquity(Integer shares, String symbol) {
        Double price = getSymbolPrice(symbol);
        Double equity = price * shares.doubleValue();
        return equity;
    }

    public Double getSymbolEquity(String owner, String symbol) {
        Document doc = getSharesCount(owner, symbol).first(); //getSymbolShares(owner, symbol).get("shares");
        Integer shares = doc.getInteger("value");
        return getSymbolEquity(shares, symbol);
    }

    /**
     * @param ownerName
     * @return total value of equity (no symbol breakdown)
     */
    public Double getTotalEquity(String ownerName) {
        List<ShareEquity> equities = getPortfolioEquity(ownerName);
        var totalEquity = equities.stream().mapToDouble(ShareEquity::equity).sum();
        return totalEquity;
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
        Double roi = (profits / notional) * 100;
        if (roi.isNaN()) {
            return "None";
        } else {
            return String.format("%.2f", roi);
        }
    }

    public Transaction getLatestBuy() {
        MongoCollection<Transaction> tradesCollection = database.getCollection(TRADE_COLLECTION_NAME, Transaction.class);

        return tradesCollection.find()
            .sort(descending("when"))
            .limit(1)
            .first();
    }

    @Deprecated
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
