# trade-history
Microservice that keeps a detailed history of all stock trades

### Configuration
If you wish to build locally,  setup up your environment variables in your server.env file. 
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

If you plan to deploy your application, you can configure the same properties using Kube Secrets defined ad `mondodbenv` and `kafka`. 

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


### ICP Setup and Deployment

Prerequisites:

1. Install [Cloudctl ClI](https://www.ibm.com/support/knowledgecenter/en/SSBS6K_3.1.0/manage_cluster/install_cli.html)
2. Install [Kubectl ClI](https://www.ibm.com/support/knowledgecenter/SSBS6K_3.1.1/manage_cluster/cfc_cli.html)

Steps:

1. Login to your ICP instance using cloudctl from the machine you wish to deploy from.  
2. Configure your client and set your Kubectl context by copying and pasting the configure client 
3. Follow the Kubernetes General Deployment Instructions to deploy the application. 

### Kubernetes General Deployment

After building the docker container locally, use the helm chart from the charts directory to deploy:
```bash
helm package chart/tradehistory/ --debug
helm install tradehistory-1.0.0.tgz --name tradehistory-release
```