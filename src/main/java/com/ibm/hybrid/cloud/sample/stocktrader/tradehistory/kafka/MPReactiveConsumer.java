/*
       Copyright 2018-2021 IBM Corp All Rights Reserved
       Copyright 2022-2025 Kyndryl, All Rights Reserved
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

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.MongoConnector;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.StockPurchase;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import lombok.extern.java.Log;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@Log
@ApplicationScoped
public class MPReactiveConsumer {

    @Inject
    private MongoConnector mConnector;
    private static String topic = null;

    private static final Jsonb JSONB = JsonbBuilder.create();

    static {
        topic = System.getenv("MP_MESSAGING_INCOMING_STOCKTRADER_TOPIC");
        if ((topic == null) && topic.isEmpty()) {
            log.info("Using environment topic of '" + topic + "'");

        } else {
            topic = "stocktrader";
            log.info("Using default topic of '" + topic + "'");
        }
    }

    MPReactiveConsumer() {
    }

    @Incoming("stocktrader")
    // Acknowledgement annotation for high-throughput as mentioned here: https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/2.4/kafka/kafka.html
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    @WithSpan
    public void consume(String record) {
        StockPurchase spRecord = JSONB.fromJson(record, StockPurchase.class);
        log.info("Received stock purchase " + spRecord);
        try {
            mConnector.insertStockPurchase(spRecord, topic);
            log.info("Inserted stock purchase " + record);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isHealthy() {
        return true;
    }

}
