package eu.merloteducation.contractorchestrator.models.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
public class ContractBasicDto {
    private String id;
    private OffsetDateTime creationDate;

    private String offeringId;

    private String offeringName;

    private String providerId;

    private String providerLegalName;

    private String consumerId;

    private String consumerLegalName;

    private String state;
}
