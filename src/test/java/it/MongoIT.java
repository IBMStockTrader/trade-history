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

import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.MongoConnector;
import com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.mongo.StockPurchase;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;

public class MongoIT extends EndpointHelper {

    private static String TEST_MONGO_DATABASE = System.getProperty("test.mongo.database");
    private static String TEST_MONGO_COLLECTION = System.getProperty("test.mongo.collection");
    private static int TEST_MONGO_PORT = Integer.parseInt(System.getProperty("test.mongo.port"));
    private static String TEST_MONGO_HOST = System.getProperty("test.mongo.host");


    private static MongoClient mongoClient;
    public static MongoConnector mConnector;

    @Test
    public void getTradesTest() {

        var fixed = Instant.from(OffsetDateTime.of(2025, 8, 28, 10, 45, 30, 0, ZoneOffset.UTC));
        StockPurchase sp = new StockPurchase("User", "User", "IBM", 1, 1.00, fixed, 1.00);
        mConnector.insertStockPurchase(sp, "getTradesTest");

        var mongoResponse = mConnector.getTrades("User");
        //JSONObject transaction = mongoResponse.getJSONArray("transactions").getJSONObject(0);
        var transaction = mongoResponse.transactions().get(0);

        assertEquals(transaction.owner(), "User");
        assertEquals(transaction.shares(), "1");
        assertEquals(transaction.symbol(), "IBM");
        assertEquals(transaction.notional(), "1.0");
        assertEquals(transaction.price(), "1.0");
//        assertEquals(transaction.topic(), "getTradesTest");
        assertEquals(transaction.commission(), "1.0");
        assertEquals(transaction._id(), "User");
        assertEquals(transaction.when(), fixed);
    }

    @Test
    public void getROITest() {
        var fixed = Instant.from(OffsetDateTime.of(2025, 8, 28, 10, 45, 30, 0, ZoneOffset.UTC));
        StockPurchase sp = new StockPurchase("UserROI", "UserROI", "IBM", 10, 2.00, fixed, 0.00);
        mConnector.insertStockPurchase(sp, "getROITest");

        assertEquals("150.00", mConnector.getROI("UserROI", 50.00));
    }


    @BeforeClass
    public static void initializeMockMongoDB() {
        ServerAddress sa = new ServerAddress(TEST_MONGO_HOST, TEST_MONGO_PORT);
        mongoClient = new MongoClient(sa);
        mConnector = new MongoConnector(mongoClient, TEST_MONGO_DATABASE, TEST_MONGO_COLLECTION);
    }

    @AfterClass
    public static void closeMongoDB() {
        mongoClient.close();
    }
}