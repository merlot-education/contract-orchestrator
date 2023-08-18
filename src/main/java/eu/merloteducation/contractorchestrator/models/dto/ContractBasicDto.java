package eu.merloteducation.contractorchestrator.models.dto;

import eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator.ServiceOfferingDetails;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
public class ContractBasicDto {
    private String id;
    private OffsetDateTime creationDate;

    private ServiceOfferingDetails offering;

    private String providerId;

    private String providerLegalName;

    private String consumerId;

    private String consumerLegalName;

    private String state;
}
