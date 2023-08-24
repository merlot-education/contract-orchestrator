package eu.merloteducation.contractorchestrator.models.dto.cooperation;

import eu.merloteducation.contractorchestrator.models.dto.ContractDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CooperationContractDto extends ContractDto {
    private CooperationContractDetailsDto details;
    private CooperationContractNegotiationDto negotiation;
    private CooperationContractProvisioningDto provisioning;
}
