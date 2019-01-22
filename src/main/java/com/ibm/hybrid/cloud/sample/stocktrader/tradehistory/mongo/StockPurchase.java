package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo;

import java.io.Serializable;
import javax.json.Json;

import org.json.JSONObject;

public class StockPurchase implements Serializable{

    private String id;
    private String owner;
    private String symbol;
    private int shares;
    private double price;
    private String when;
    private int commission;

    public StockPurchase(String json) {
        JSONObject jObject = new JSONObject(json);
        this.id = jObject.getString("id");
        this.owner = jObject.getString("owner");
        this.symbol = jObject.getString("symbol");
        this.shares = jObject.getInt("shares");
        this.price = jObject.getDouble("price");
        this.when = jObject.getString("when");
        this.commission = jObject.getInt("commission");
    }

    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getShares() {
        return shares;
    }

    public double getPrice() {
        return price;
    }

    public String getWhen() {
        return when;
    }

    public double getCommission() {
        return commission;
    }

    @Override
    public String toString() {
        return new StringBuffer().append(encode()).toString();
    }

    public String encode() {
        return Json.createObjectBuilder()
            .add("id", id)
            .add("owner", owner)
            .add("symbol", symbol)
            .add("shares", shares)
            .add("price", price)
            .add("when", when)
            .add("commission", commission)
            .build().toString();
    }
}