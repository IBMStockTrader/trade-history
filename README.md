# trade-history
Microservice that keeps a detailed history of all stock trades

### Configuration
Before building, make sure you setup your environment variables in your server.env file. 
```
TOPIC="..."
BOOTSTRAP_SERVER="..."
CONSUMER_API_KEY="..."

MONGO_PASSWORD=...
MONGO_USER=...
MONGO_DATABASE=...
MONGO_IP=...
MONGO_PORT=...
MONGO_COLLECTION=...
```

### Build and run

Run this from the project directory root: 

```bash
mvn clean install
```

To run the project from the target directory:

```bash
mvn liberty:run-server
```

The application should be accessible from http://localhost:9080

### Docker

Build the docker container from the root directory: 

```bash
docker build -t trade_history .
```

To run the docker containter locally:

```bash
docker run -p 9080:9080 trade_history
```

The application should be accessible from http://localhost:9080

### Deploy

After building the docker container locally, use the helm chart from the charts directory to deploy:
```bash
helm package chart/tradehistory/ --debug
helm install tradehistory-1.0.0.tgz --name tradehistory-release
```
