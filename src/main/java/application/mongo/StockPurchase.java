package application.mongo;

import java.io.Serializable;
import javax.json.Json;

import org.json.JSONException;
import org.json.JSONObject;

import jdk.nashorn.internal.runtime.JSONFunctions;

import org.bson.Document;

public class StockPurchase implements Serializable{

    private String owner;
    private String symbol;
    private int shares;
    private int price;
    private String when;
    private int comission;

    /*
org.json.JSONException: JSONObject["symbol"] not found.
[INFO] [err] 	at org.json.JSONObject.get(JSONObject.java:566)
[INFO] [err] 	at org.json.JSONObject.getString(JSONObject.java:851)
[INFO] [err] 	at application.mongo.StockPurchase.<init>(StockPurchase.java:26)
[INFO] [err] 	at application.demo.DemoConsumeSocket$KafkaConsumer.run(DemoConsumeSocket.java:200)
[INFO] [err] 	at java.lang.Thread.run(Thread.java:748)

    */
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