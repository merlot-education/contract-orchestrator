package eu.merloteducation.contractorchestrator.models.dto.saas;

import eu.merloteducation.contractorchestrator.models.dto.ContractNegotiationDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaasContractNegotiationDto extends ContractNegotiationDto {
    private String userCountSelection;
}
