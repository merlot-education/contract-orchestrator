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

    public DataDeliveryContractTemplate(DataDeliveryContractTemplate template) {
        super(template);
        this.exchangeCountSelection = template.getExchangeCountSelection();
        setServiceContractProvisioning(template.getServiceContractProvisioning());
    }

    @Override
    public void transitionState(ContractState targetState) {
        DataDeliveryProvisioning serviceContractProvisioning =
                (DataDeliveryProvisioning) getServiceContractProvisioning();
        if ((targetState == ContractState.SIGNED_CONSUMER &&
                (StringUtil.isNullOrEmpty(exchangeCountSelection) ||
                        serviceContractProvisioning == null ||
                        StringUtil.isNullOrEmpty(serviceContractProvisioning.getDataAddressTargetFileName()) ||
                        StringUtil.isNullOrEmpty(serviceContractProvisioning.getDataAddressTargetBucketName())))
                || (targetState == ContractState.RELEASED &&
                (StringUtil.isNullOrEmpty(serviceContractProvisioning.getDataAddressSourceFileName()) ||
                        StringUtil.isNullOrEmpty(serviceContractProvisioning.getDataAddressSourceBucketName()) ||
                        StringUtil.isNullOrEmpty(serviceContractProvisioning.getDataAddressName()) ||
                        StringUtil.isNullOrEmpty(serviceContractProvisioning.getDataAddressType())))) {
            throw new IllegalStateException(
                    String.format("Cannot transition from state %s to %s as mandatory fields are not set",
                            getState().name(), targetState.name()));
        }
        super.transitionState(targetState);
    }
}
