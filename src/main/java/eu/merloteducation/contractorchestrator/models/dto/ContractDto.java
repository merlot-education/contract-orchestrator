package eu.merloteducation.contractorchestrator.models.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import eu.merloteducation.contractorchestrator.models.dto.cooperation.CooperationContractDto;
import eu.merloteducation.contractorchestrator.models.dto.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.contractorchestrator.models.dto.saas.SaasContractDto;
import eu.merloteducation.contractorchestrator.models.serviceofferingorchestrator.ServiceOfferingDetails;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DataDeliveryContractDto.class, name = "DataDeliveryContractTemplate"),
        @JsonSubTypes.Type(value = SaasContractDto.class, name = "SaasContractTemplate"),
        @JsonSubTypes.Type(value = CooperationContractDto.class, name = "CooperationContractTemplate")
})
public class ContractDto {
    private String type;
    private ContractDetailsDto details;
    private ContractNegotiationDto negotiation;
    private ContractProvisioningDto provisioning;
    private ServiceOfferingDetails offering;
}
