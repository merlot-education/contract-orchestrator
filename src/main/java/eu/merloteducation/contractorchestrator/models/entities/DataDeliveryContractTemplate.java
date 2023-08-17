package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import io.netty.util.internal.StringUtil;
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

    public DataDeliveryContractTemplate(DataDeliveryContractTemplate template, boolean regenerate) {
        super(template, regenerate);
        this.exchangeCountSelection = template.getExchangeCountSelection();
        setServiceContractProvisioning(template.getServiceContractProvisioning());
    }

    @Override
    public DataDeliveryProvisioning getServiceContractProvisioning() {
        return (DataDeliveryProvisioning) super.getServiceContractProvisioning();
    }

    @Override
    public void transitionState(ContractState targetState) {
        DataDeliveryProvisioning serviceContractProvisioning = getServiceContractProvisioning();
        if ((targetState == ContractState.SIGNED_CONSUMER &&
                (StringUtil.isNullOrEmpty(exchangeCountSelection) ||
                        serviceContractProvisioning == null ||
                        StringUtil.isNullOrEmpty(serviceContractProvisioning.getDataAddressTargetFileName()) ||
                        StringUtil.isNullOrEmpty(serviceContractProvisioning.getDataAddressTargetBucketName())||
                        StringUtil.isNullOrEmpty(serviceContractProvisioning.getSelectedConsumerConnectorId())))
                || (targetState == ContractState.RELEASED &&
                (StringUtil.isNullOrEmpty(serviceContractProvisioning.getDataAddressSourceFileName()) ||
                        StringUtil.isNullOrEmpty(serviceContractProvisioning.getDataAddressSourceBucketName()) ||
                        StringUtil.isNullOrEmpty(serviceContractProvisioning.getDataAddressType()) ||
                        StringUtil.isNullOrEmpty(serviceContractProvisioning.getSelectedProviderConnectorId())))) {
            throw new IllegalStateException(
                    String.format("Cannot transition from state %s to %s as mandatory fields are not set",
                            getState().name(), targetState.name()));
        }
        if (targetState == ContractState.RELEASED &&
                serviceContractProvisioning.getDataAddressSourceBucketName().equals(serviceContractProvisioning.getDataAddressTargetBucketName())) {
            throw new IllegalStateException(
                    String.format("Cannot transition from state %s to %s as source and target bucket must not be the same",
                            getState().name(), targetState.name()));
        }
        if (targetState == ContractState.RELEASED &&
                serviceContractProvisioning.getSelectedConsumerConnectorId().equals(serviceContractProvisioning.getSelectedProviderConnectorId())) {
            throw new IllegalStateException(
                    String.format("Cannot transition from state %s to %s as source and target connector must not be the same",
                            getState().name(), targetState.name()));
        }
        super.transitionState(targetState);
    }
}
