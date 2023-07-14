package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@JsonDeserialize
public class DataDeliveryContractTemplate extends ContractTemplate {
    @JsonView(ContractViews.DetailedView.class)
    private String exchangeCountSelection;

    public DataDeliveryContractTemplate() {
        super();
        setServiceContractProvisioning(new DataDeliveryProvisioning());
    }

    public DataDeliveryContractTemplate(DataDeliveryContractTemplate template) {
        super(template);
        this.exchangeCountSelection = template.getExchangeCountSelection();
        setServiceContractProvisioning(template.getServiceContractProvisioning());
    }

    @Override
    public void transitionState(ContractState targetState) {
        DataDeliveryProvisioning serviceContractProvisioning =
                (DataDeliveryProvisioning) getServiceContractProvisioning();
        if (targetState == ContractState.SIGNED_CONSUMER) {
            if (exchangeCountSelection == null || exchangeCountSelection.isEmpty() ||
                    serviceContractProvisioning == null ||
                    serviceContractProvisioning.getDataAddressTargetFileName() == null ||
                    serviceContractProvisioning.getDataAddressTargetFileName().isEmpty() ||
                    serviceContractProvisioning.getDataAddressTargetBucketName() == null ||
                    serviceContractProvisioning.getDataAddressTargetBucketName().isEmpty()) {
                throw new IllegalStateException(
                        String.format("Cannot transition from state %s to %s as mandatory fields are not set",
                                getState().name(), targetState.name()));
            }
        } else if (targetState == ContractState.RELEASED) {
            if (serviceContractProvisioning.getDataAddressSourceFileName() == null ||
                    serviceContractProvisioning.getDataAddressSourceFileName().isEmpty() ||
                    serviceContractProvisioning.getDataAddressSourceBucketName() == null ||
                    serviceContractProvisioning.getDataAddressSourceBucketName().isEmpty() ||
                    serviceContractProvisioning.getDataAddressName() == null ||
                    serviceContractProvisioning.getDataAddressName().isEmpty() ||
                    serviceContractProvisioning.getDataAddressType() == null ||
                    serviceContractProvisioning.getDataAddressType().isEmpty()) {
                throw new IllegalStateException(
                        String.format("Cannot transition from state %s to %s as mandatory fields are not set",
                                getState().name(), targetState.name()));
            }
        }
        super.transitionState(targetState);
    }
}
