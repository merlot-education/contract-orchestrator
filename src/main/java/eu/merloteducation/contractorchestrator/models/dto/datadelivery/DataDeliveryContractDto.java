package eu.merloteducation.contractorchestrator.models.dto.datadelivery;

import eu.merloteducation.contractorchestrator.models.dto.ContractDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataDeliveryContractDto extends ContractDto {
    private DataDeliveryContractDetailsDto details;
    private DataDeliveryContractNegotiationDto negotiation;
    private DataDeliveryContractProvisioningDto provisioning;
}
