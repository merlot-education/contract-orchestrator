package eu.merloteducation.contractorchestrator.models.edc.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.merloteducation.contractorchestrator.models.edc.EdcConstants;
import eu.merloteducation.contractorchestrator.models.edc.asset.IonosS3DataAddress;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IonosS3TransferProcess extends TransferProcess {
    @JsonProperty(EdcConstants.EDC_PREFIX + "dataDestination")
    private IonosS3DataAddress dataDestination;
}
