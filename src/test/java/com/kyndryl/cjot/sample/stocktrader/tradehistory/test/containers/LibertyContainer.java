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

package com.kyndryl.cjot.sample.stocktrader.tradehistory.test.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class LibertyContainer extends GenericContainer<LibertyContainer> {

    public LibertyContainer(ImageFromDockerfile image, int httpPort, int httpsPort) {

        super(image);
        addExposedPorts(httpPort, httpsPort);
        
        // set environment variables
        // Tell MP to use the test profile at META-INF/microprofile-config-test.properties
        withEnv("MP_CONFIG_PROFILE", "test");

        // wait for smarter planet message by default
        waitingFor(Wait.forLogMessage("^.*CWWKF0011I.*$", 1));

    }

    public String getBaseURL() throws IllegalStateException {
        return "http://" + getHost() + ":" + getFirstMappedPort();
    }
}