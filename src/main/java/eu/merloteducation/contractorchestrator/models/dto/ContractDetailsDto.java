package eu.merloteducation.contractorchestrator.models.dto;

import com.fasterxml.jackson.databind.JsonNode;
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

    private JsonNode providerLegalAddress;

    private String consumerId;

    private String consumerLegalName;

    private JsonNode consumerLegalAddress;

    private String state;

    private List<ContractTncDto> termsAndConditions;

    private String consumerSignerUserName;

    private String consumerSignature;

    private String consumerSignatureDate;

    private String providerSignerUserName;

    private String providerSignature;

    private String providerSignatureDate;
}
