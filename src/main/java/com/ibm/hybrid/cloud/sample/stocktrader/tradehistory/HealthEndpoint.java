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
package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory;

import jakarta.inject.Inject;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.kafka.MPReactiveConsumer;

import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

@Readiness
public class HealthEndpoint implements HealthCheck {

    @Inject
    private MPReactiveConsumer kafka;

    @Override
    public HealthCheckResponse call() {
        //TODO: add checks for mongo, kafka, etc
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Consumer")
        .withData("kafka", kafka.isHealthy() ? "available" : "down");

        if (kafkaReady()) {
            return builder.up().build();
        }

        return builder.down().build();
    }

    private boolean kafkaReady() {
        return kafka != null && kafka.isHealthy();
    }

}