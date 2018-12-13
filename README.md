# trade-history
Microservice that keeps a detailed history of all stock trades

### Deploy

Use WebSphere Liberty helm chart to deploy Trade History microservice:
```bash
helm repo add ibm-charts https://raw.githubusercontent.com/IBM/charts/master/repo/stable/
helm install ibm-charts/ibm-websphere-liberty -f <VALUES_YAML> -n <RELEASE_NAME> --tls
```

In practice this means you'll run something like:
```bash
helm repo add ibm-charts https://raw.githubusercontent.com/IBM/charts/master/repo/stable/
helm install ibm-charts/ibm-websphere-liberty -f manifests/trade-history-values.yaml -n trade-history --namespace stock-trader --tls
```
