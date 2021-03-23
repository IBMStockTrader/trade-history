package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.kafka;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.MongoConnector;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.StockPurchase;

import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class MPReactiveConsumer {
    
    private static MongoConnector mConnector = null;
    private static String topic = null;

    MPReactiveConsumer() {}

    public void init(){
        try {
            mConnector = new MongoConnector();
            topic = System.getenv("MP_MESSAGING_INCOMING_STOCKTRADER_TOPIC");
            if (topic == null || topic.isEmpty()) {
                topic = "stocktrader";
            }
        }
        catch( NullPointerException e) {
            System.out.println(e.getMessage());
        }
        catch(IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Incoming("stocktrader")
    public void consume(String record) {
        StockPurchase sp = new StockPurchase(record);
        try {
            if (mConnector == null) {
                init();
            }
            mConnector.insertStockPurchase(sp, topic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isHealthy() {
        return true;
    }

}
