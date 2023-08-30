package eu.merloteducation.contractorchestrator.models.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ContractDetailsDto {

    private String id;

    private String creationDate;

    private String providerId;

    private String providerLegalName;

    private String consumerId;

    private String consumerLegalName;

    private String state;

    private String providerSignerUser;

    private String providerSignature;

    private String consumerSignerUser;

    private String consumerSignature;
}
