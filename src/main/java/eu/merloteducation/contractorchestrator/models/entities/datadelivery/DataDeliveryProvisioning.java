package eu.merloteducation.contractorchestrator.models.entities.datadelivery;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.DefaultProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.ServiceContractProvisioning;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class DataDeliveryProvisioning extends ServiceContractProvisioning {

    private String selectedProviderConnectorId;

    private String selectedConsumerConnectorId;

    protected DataDeliveryProvisioning() {
        super();
        this.selectedProviderConnectorId = "";
        this.selectedConsumerConnectorId = "";
    }

    protected DataDeliveryProvisioning(DataDeliveryProvisioning provisioning) {
        super(provisioning);
        this.selectedConsumerConnectorId = provisioning.getSelectedConsumerConnectorId();
        this.selectedProviderConnectorId = provisioning.getSelectedProviderConnectorId();
    }

    @Override
    public boolean transitionAllowed(ContractState targetState) {
        return super.transitionAllowed(targetState) && switch (targetState) {
            case SIGNED_CONSUMER ->
                    !StringUtil.isNullOrEmpty(selectedConsumerConnectorId);
            case RELEASED ->
                    !StringUtil.isNullOrEmpty(selectedProviderConnectorId)
                    && !selectedConsumerConnectorId.equals(selectedProviderConnectorId);
            default -> true;
        };
    }
}
