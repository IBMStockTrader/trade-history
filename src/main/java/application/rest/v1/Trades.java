package application.rest.v1;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ibm.json.java.JSONObject;

import application.mongo.MongoConnector;

@Path("/v1")
public class Trades {

    @Path("/example")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response example() {
        List<String> list = new ArrayList<>();
        //return a simple list of strings
        list.add("Congratulations, your application is up and running");
        return Response.ok(list.toString()).build();
    }

    // URL: /tradeHistory/rest/v1/trade
    @Path("/trade")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject getTrades() {
        //TODO: rest call to get data from Mongo API
        JSONObject json = new JSONObject();
        json.put("id", "testObject");
        return json;
    }

    @Path("/latestBuy")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject latestBuy() {
        MongoConnector mc = new MongoConnector();
        //TODO: rest call to get data from Mongo API
        JSONObject json = new JSONObject();
        json.put("id", mc.retrieveMostRecentDoc());
        return json;
    }*/

    //TODO: add a path that takes date start and end parameters, and filter to trades within the timeframe
        // cloudant rest api should have a way to do this filtering
}
