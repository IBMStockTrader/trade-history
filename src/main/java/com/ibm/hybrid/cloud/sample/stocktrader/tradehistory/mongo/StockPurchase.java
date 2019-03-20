/*
       Copyright 2018, 2019 IBM Corp All Rights Reserved
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
    private double commission;

    public StockPurchase(String json) {
        JSONObject jObject = new JSONObject(json);
        this.id = jObject.getString("id");
        this.owner = jObject.getString("owner");
        this.symbol = jObject.getString("symbol");
        this.shares = jObject.getInt("shares");
        this.price = jObject.getDouble("price");
        this.when = jObject.getString("when");
        this.commission = jObject.getDouble("commission");
    }

    public StockPurchase(String id, String owner, String symbol, int shares, 
        double price, String when, double commission)  {
        this.id = id;
        this.owner = owner;
        this.symbol = symbol;
        this.shares = shares;
        this.price = price;
        this.when = when;
        this.commission = commission;
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