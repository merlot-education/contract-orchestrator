package eu.merloteducation.contractorchestrator.models.dto.datadelivery;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.contractorchestrator.models.dto.ContractProvisioningDto;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataDeliveryContractProvisioningDto extends ContractProvisioningDto {
    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressType;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressSourceBucketName;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressSourceFileName;

    @JsonView(ContractViews.ProviderView.class)
    private String selectedProviderConnectorId;

    @JsonView(ContractViews.ConsumerView.class)
    private String dataAddressTargetBucketName;

    @JsonView(ContractViews.ConsumerView.class)
    private String dataAddressTargetFileName;

    @JsonView(ContractViews.ConsumerView.class)
    private String selectedConsumerConnectorId;
}
