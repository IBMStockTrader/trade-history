/*
   Copyright 2019-2021 IBM Corp All Rights Reserved
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
package it;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MapReduceIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.MongoCredential;
import com.mongodb.client.model.Filters;
import com.mongodb.MongoSocketException;

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.MongoConnector;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.StockPurchase;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.json.JSONObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class MongoIT extends EndpointHelper {

    private static String TEST_MONGO_DATABASE = System.getProperty("test.mongo.database");
    private static String TEST_MONGO_COLLECTION = System.getProperty("test.mongo.collection");
    private static int TEST_MONGO_PORT = Integer.parseInt(System.getProperty("test.mongo.port"));
    private static String TEST_MONGO_HOST = System.getProperty("test.mongo.host");


    private static MongoClient mongoClient;
    public static MongoConnector mConnector;

    @Test
    public void getTradesTest(){
        StockPurchase sp = new StockPurchase("User", "User", "IBM", 1, 1.00, "2008-01-01 12:00:01.01", 1.00);
        mConnector.insertStockPurchase(sp, "getTradesTest");

        JSONObject mongoResponse = mConnector.getTrades("User");
        JSONObject transaction = mongoResponse.getJSONArray("transactions").getJSONObject(0);
                
        assertEquals(transaction.getString("owner"), "User");
        assertEquals(transaction.getString("shares"), "1");
        assertEquals(transaction.getString("symbol"), "IBM");
        assertEquals(transaction.getString("notional"), "1.0");
        assertEquals(transaction.getString("price"), "1.0");
        assertEquals(transaction.getString("topic"), "getTradesTest");
        assertEquals(transaction.getString("commission"), "1.0");
        assertEquals(transaction.getString("id"), "User");
        assertEquals(transaction.getString("when"), "2008-01-01 12:00:01.01");
    }

    @Test
    public void getROITest(){
        StockPurchase sp = new StockPurchase("UserROI", "UserROI", "IBM", 10, 2.00, "2008-01-01 12:00:01.01", 0.00);
        mConnector.insertStockPurchase(sp, "getROITest");

        assertEquals( "150.00", mConnector.getROI("UserROI",50.00 ) );
    }


    @BeforeClass
    public static void initializeMockMongoDB(){
        ServerAddress sa = new ServerAddress(TEST_MONGO_HOST, TEST_MONGO_PORT);
        mongoClient = new MongoClient(sa);
        mConnector = new MongoConnector(mongoClient, TEST_MONGO_DATABASE, TEST_MONGO_COLLECTION);
     }

     @AfterClass
     public static void closeMongoDB() {
        mongoClient.close();
     }
}