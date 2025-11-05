/*
       Copyright 2025 Kyndryl, All Rights Reserved
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

package com.kyndryl.cjot.sample.stocktrader.tradehistory.test;

import com.kyndryl.cjot.stocktrader.clients.tradehistory.TradeHistoryClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipse.yasson.JsonBindingProvider;

import java.net.URI;

@ApplicationScoped
@RegisterProvider(JsonBindingProvider.class)
public class RestClientProducers {
    @Produces
    @RestClient
    public TradeHistoryClient tradeHistoryClient() {
        return RestClientBuilder.newBuilder()
            .baseUri(URI.create(System.getProperty("tradehistory-api/mp-rest/url",
                "http://localhost:9080"))) // or read from MP Config, see below
            .build(TradeHistoryClient.class);
    }
}
