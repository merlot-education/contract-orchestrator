# MERLOT Contract Orchestrator
The Contract Orchestrator is a microservice in the MERLOT marketplace
which handles all contracting-related functionality.

This service allows for instantiation and management of contracts that are based upon service offerings provided by the
[Serviceoffering Orchestrator](https://github.com/merlot-education/serviceoffering-orchestrator).
It further provides mechanisms to provision a data transfer for data-centric service offerings by orchestrating
a communication with [EDC Connectors](https://github.com/eclipse-edc/Connector/tree/v0.4.1).


## Development

To start development for the MERLOT marketplace, please refer to [this document](https://github.com/merlot-education/.github/blob/main/Docs/DevEnv.md)
to set up a local WSL development environment of all relevant services.
This is by far the easiest way to get everything up and running locally.

## Structure

```
├── src/main/java/eu/merloteducation/contractorchestrator
│   ├── auth            # authorization checks
│   ├── config          # configuration-related components
│   ├── controller      # external REST API controllers
│   ├── models          # internal data models of contract-related data
│   ├── repositories    # DAOs for accessing the stored data
│   ├── security        # configuration for route-based authentication
│   ├── service         # internal services for processing data from the controller layer
```

REST API related models such as the DTOs can be found at [models-lib](https://github.com/merlot-education/models-lib/tree/main)
which is shared amongst the microservices.

## Dependencies
- A properly set-up keycloak instance (quay.io/keycloak/keycloak:20.0.5)
- [Organisations Orchestrator](https://github.com/merlot-education/organisations-orchestrator)
- [Serviceoffering Orchestrator](https://github.com/merlot-education/serviceoffering-orchestrator)
- rabbitmq (rabbitmq:3-management)

## Build

To build this microservice you need to provide a GitHub read-only token in order to be able to fetch maven packages from
GitHub. You can create this token at https://github.com/settings/tokens with at least the scope "read:packages".
Then set up your ~/.m2/settings.xml file as follows:

    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">

        <servers>
            <server>
                <id>github</id>
                <username>REPLACEME_GITHUB_USER</username>
                <!-- Public token with `read:packages` scope -->
                <password>REPLACEME_GITHUB_TOKEN</password>
            </server>
        </servers>
    </settings>

Afterward you can build the service with

    mvn clean package

## Run

    export S3LIBRARY_ACCESSKEY="somekey"
    export S3LIBRARY_SECRET="somesecret"
    export S3LIBRARY_SERVICEENDPOINT="someendpoint"
    java -jar target/contract-orchestrator-X.Y.Z.jar

The S3LIBRARY_* correspond to IONOS S3 bucket secrets to store generated contract PDF artefacts and attachment uploads.

Replace the X.Y.Z with the respective version of the service.

## Deploy (Docker)

This microservice can be deployed as part of the full MERLOT docker stack at
[localdeployment](https://github.com/merlot-education/localdeployment).

## Deploy (Helm)
TODO