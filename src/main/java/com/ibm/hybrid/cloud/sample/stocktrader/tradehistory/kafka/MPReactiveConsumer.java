package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.kafka;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.MongoConnector;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.StockPurchase;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class MPReactiveConsumer {
    
    @Inject
    private MongoConnector mConnector;
    private static String topic = null;

    static {
        topic = System.getenv("KAFKA_TOPIC");
        if ((topic != null) && !topic.isEmpty()) {
		} else {
            System.out.println("Using default topic of 'stocktrader'");
            topic = "stocktrader";
		}
    }

    MPReactiveConsumer() {}

    @Incoming("stocktrader")
    // Acknowledgement annotation for high-throughput as mentioned here: https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/2.4/kafka/kafka.html
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public void consume(String record) {
        StockPurchase sp = new StockPurchase(record);
        try {
            mConnector.insertStockPurchase(sp, topic);
            System.out.println("Inserted stockpurchase " + record);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isHealthy() {
        return true;
    }

}
