package eu.merloteducation.contractorchestrator.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrganisationConnectorExtension {

    private String id;

    private String orgaId;

    private String connectorId;

    private String connectorEndpoint;

    private String connectorAccessToken;

    private List<String> bucketNames;
}

