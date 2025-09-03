#       Copyright 2017-2021 IBM Corp All Rights Reserved
#       Copyright 2022-2024 Kyndryl, All Rights Reserved

#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at

#       http://www.apache.org/licenses/LICENSE-2.0

#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

# FROM maven:3.6-jdk-11-slim AS build
# COPY . /usr/
# RUN mvn -f /usr/pom.xml clean package

# FROM websphere-liberty:microProfile3
FROM openliberty/open-liberty:25.0.0.6-full-java21-openj9-ubi-minimal

USER root

COPY --chown=1001:0 src/main/liberty/config /config/

# This script will add the requested XML snippets to enable Liberty features and grow image to be fit-for-purpose using featureUtility. 
# Only available in 'kernel-slim'. The 'full' tag already includes all features for convenience.
# RUN features.sh

# COPY --from=build --chown=1001:0 /usr/target/tradehistory-1.0-SNAPSHOT.war /config/apps/trade-history.war
COPY --chown=1001:0 target/tradehistory-1.0-SNAPSHOT.war /config/apps/trade-history.war

# COPY --chown=1001:0 /target/liberty/wlp/usr/servers/defaultServer /config/
# COPY --chown=1001:0 /target/liberty/wlp/usr/servers/defaultServer/resources/security/certs.jks output/resources/security/

USER 1001

RUN configure.sh
