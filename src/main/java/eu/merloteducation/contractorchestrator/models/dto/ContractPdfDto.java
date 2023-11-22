package eu.merloteducation.contractorchestrator.models.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ContractPdfDto {
    private String contractId;
    private String contractCreationDate;
    private String contractRuntime;
    private String contractDataTransferCount; //data delivery
    private String contractUserCount; //saas
    private List<Map<String, String>> contractTnc; //Merlot + Provider TnC (link + hash)
    private List<String> contractAttachmentFilenames;

    private String serviceId;
    private String serviceName;
    private String serviceType;
    private String serviceDescription;
    private String serviceExampleCosts;
    private String serviceDataAccessType;//data delivery
    private String serviceDataTransferType; //data delivery
    private String serviceHardwareRequirements;//saas

    private String providerLegalName;
    private Map<String, String> providerLegalAddress;
    private String providerSignerUser;
    private String providerSignature;
    private String providerSignatureTimestamp;

    private String consumerLegalName;
    private Map<String, String> consumerLegalAddress;
    private String consumerSignerUser;
    private String consumerSignature;
    private String consumerSignatureTimestamp;
}
