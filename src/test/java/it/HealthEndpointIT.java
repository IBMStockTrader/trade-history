package it;

import static org.junit.Assert.assertTrue;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.junit.Test;

public class HealthEndpointIT extends EndpointHelper{

    @Test
    public void testHealthEndpoint() throws Exception {
        testEndpoint("health", "{\"checks\":[{\"data\":{},\"name\":\"Consumer\",\"state\":\"UP\"}],\"outcome\":\"UP\"}");
    }
}
