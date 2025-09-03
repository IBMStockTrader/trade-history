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

package com.kyndryl.cjot.stocktrader.clients.tradehistory;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest.Share;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest.Shares;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest.Transaction;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.rest.Transactions;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@ApplicationPath("/")
@Path("/")
@ApplicationScoped
@RegisterRestClient(configKey = "tradehistory-api")
public interface TradeHistoryClient {

    @GET
    @Path("/returns/{owner}")
    @Produces(MediaType.TEXT_PLAIN)
    @WithSpan(kind = SpanKind.CLIENT, value = "TradeHistoryClient.getReturns")
    public Double getReturns(@PathParam("owner") String ownerName, @QueryParam("currentValue") Double portfolioValue);

    @GET
    @Path("/notional/{owner}")
    @Produces(MediaType.APPLICATION_JSON)
    @WithSpan(kind = SpanKind.CLIENT, value = "TradeHistoryClient.getNotional")
    public Double getNotional(@PathParam("owner") String ownerName);

    @Path("/shares/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @WithSpan
    public Shares getPortfolioShares(@PathParam("owner") String ownerName);

    @Path("/shares/{owner}/{symbol}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @WithSpan
    public Share getCurrentShares(@PathParam("owner") String ownerName, @PathParam("symbol") String symbol);

    @Path("/trades/{owner}/{symbol}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @WithSpan
    public Transactions getTradesByOwnerAndSymbol(@PathParam("owner") String ownerName, @PathParam("symbol") String symbol);

    @Path("/trades/{owner}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @WithSpan
    public Transactions getTradesByOwner(@PathParam("owner") String ownerName);

    @Path("/latestBuy")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @WithSpan
    public Transaction latestBuy();

}