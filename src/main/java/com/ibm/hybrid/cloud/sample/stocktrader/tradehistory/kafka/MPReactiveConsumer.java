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

package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
        topic = System.getenv("MP_MESSAGING_INCOMING_STOCKTRADER_TOPIC");
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
