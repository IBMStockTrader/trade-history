package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo;

import java.io.Serializable;
import javax.json.Json;

import org.json.JSONObject;

public class StockPurchase implements Serializable{

    private String owner;
    private String symbol;
    private int shares;
    private int price;
    private String when;
    private int comission;

    public StockPurchase(String json) {
        JSONObject jObject = new JSONObject(json);

        this.owner = jObject.getString("owner");
        this.symbol = jObject.getString("symbol");
        this.shares = jObject.getInt("shares");
        this.price = jObject.getInt("price");
        this.when = jObject.getString("when");
        this.comission = jObject.getInt("comission");
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

    public int getPrice() {
        return price;
    }

    public String getWhen() {
        return when;
    }

    public int getComission() {
        return comission;
    }

    @Override
    public String toString() {
        return new StringBuffer().append(encode()).toString();
    }

    public String encode() {
        return Json.createObjectBuilder()
            .add("owner", owner)
            .add("symbol", symbol)
            .add("shares", shares)
            .add("price", price)
            .add("when", when)
            .add("comission", comission)
            .build().toString();
    }
}