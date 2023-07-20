package eu.merloteducation.contractorchestrator.models;

import lombok.Getter;


@Getter
public class ConnectorDetailsRequest {
    String connectorId;
    String orgaId;

    public ConnectorDetailsRequest(String connectorId, String orgaId) {
        this.connectorId = connectorId;
        this.orgaId = orgaId;
    }
}
