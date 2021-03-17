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