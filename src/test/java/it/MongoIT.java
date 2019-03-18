package it;

import static org.junit.Assert.assertTrue;

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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.BeforeClass;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import javax.servlet.http.HttpServletRequest;
import javax.inject.Inject;
import javax.inject.Provider;

public class MongoIT extends EndpointHelper {

    @Inject
    @ConfigProperty(name = "test.mongo.database")
    private static String TEST_MONGO_DATABASE;

    @Inject
    @ConfigProperty(name = "test.mongo.collection")
    private static String TEST_MONGO_COLLECTION;


    @Inject
    @ConfigProperty(name = "test.mongo.port")
    private static int TEST_MONGO_PORT;


    public static MongoConnector mConnector;

    @Test
    public void mongoTest(){
        assertTrue(true);
    }


    // @BeforeClass
    // public static void initializeMockMongoDB(){
    //     System.out.println("test.mongo.database" + TEST_MONGO_DATABASE);
    //     ServerAddress sa = new ServerAddress("localhost", TEST_MONGO_PORT);
    //     MongoClient mongoClient = new MongoClient(sa);
    //     mConnector = new MongoConnector(mongoClient, TEST_MONGO_DATABASE, TEST_MONGO_COLLECTION);
    // }
}