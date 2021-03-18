### Dependencies
For managing versions of Java/Groovy/Gradle, I like to use the sdk (https://sdkman.io) management tool.

Just run
```shell script
curl -s "https://get.sdkman.io" | bash
```
which installs the sdk tool. Follow the instruction to load the script in the current shell or open a new one and then install java/groovy/gradle:
```shell script
sdk install java 11.0.3.hs-adpt
sdk install groovy 3.0.7
sdk install gradle 6.8.3
```

### Run from Command Line

From the root directory, run `./gradlew bootRun`.

### Testing Endpoints

The application exposes the following endpoints:

1. `POST` to URI `/transactions` with JSON request body matching the spec.
```shell
curl --header "Content-Type: application/json" --request POST --data '{ "payer": "MILLER COORS", "points": 10000, "timestamp": "2020-11-01T14:00:00Z" }' http://localhost:8080/transactions
```

2. `POST` to URI `/points` with JSON request body matching the spec. 
```shell
curl --header "Content-Type: application/json" --request POST --data '{ "points": 1000 }' http://localhost:8080/points
```

3. `GET` to URI "/balances"
```shell
curl --header "Content-Type: application/json" --request GET http://localhost:8080/balances
```