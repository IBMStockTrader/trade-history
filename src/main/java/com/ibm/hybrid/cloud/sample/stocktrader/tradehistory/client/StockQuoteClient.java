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

package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.client;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/")
@Dependent
@RegisterRestClient
@RegisterClientHeaders //To enable JWT propagation
public interface StockQuoteClient {
	@GET
	@Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @WithSpan(kind = SpanKind.CLIENT, value="StockQuoteClient.getAllCachedQuotes")
//	public Quote[] getAllCachedQuotes(@HeaderParam("Authorization") String jwt);
    public List<Quote> getAllCachedQuotes();

	@GET
	@Path("/{symbol}")
    @Produces(MediaType.APPLICATION_JSON)
    @WithSpan(kind = SpanKind.CLIENT, value="StockQuoteClient.getStockQuote")
//	public Quote getStockQuote(@HeaderParam("Authorization") String jwt, @PathParam("symbol") String symbol);
    public Quote getStockQuote(@PathParam("symbol") String symbol);
}
