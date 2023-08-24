package eu.merloteducation.contractorchestrator.models.dto.saas;

import eu.merloteducation.contractorchestrator.models.dto.ContractDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaasContractDto extends ContractDto {
    private SaasContractDetailsDto details;
    private SaasContractNegotiationDto negotiation;
    private SaasContractProvisioningDto provisioning;
}
