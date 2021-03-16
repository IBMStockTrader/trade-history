package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.kafka;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class MPReactiveConsumer {
    
    MPReactiveConsumer() {

    }

    @Incoming("stocktrader")
    public void consume(String record) {
        System.out.println("[INCOMING MESSAGE]: " + record);
    }

    public boolean isHealthy() {
        return true;
    }

}
