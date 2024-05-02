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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;

import org.junit.Test;

public class HealthEndpointIT extends EndpointHelper{

    @Test
    public void testHealthEndpoint() throws Exception {
        testEndpoint("health", "{\"checks\":[{\"data\":{\"kafka\":\"available\"},\"name\":\"Consumer\",\"state\":\"UP\"}],\"outcome\":\"UP\"}");
    }
}
