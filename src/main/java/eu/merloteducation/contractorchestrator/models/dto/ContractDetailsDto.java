package eu.merloteducation.contractorchestrator.models.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

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

    private List<ContractTncDto> termsAndConditions;
}
