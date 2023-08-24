package eu.merloteducation.contractorchestrator.models.dto.datadelivery;

import eu.merloteducation.contractorchestrator.models.dto.ContractNegotiationDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataDeliveryContractNegotiationDto extends ContractNegotiationDto {
    private String exchangeCountSelection;
}
