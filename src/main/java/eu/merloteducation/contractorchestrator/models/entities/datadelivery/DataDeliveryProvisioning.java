package eu.merloteducation.contractorchestrator.models.entities.datadelivery;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.ServiceContractProvisioning;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class DataDeliveryProvisioning extends ServiceContractProvisioning {

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Nullable
    private TransferProvisioning consumerTransferProvisioning;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Nullable
    private TransferProvisioning providerTransferProvisioning;

    @Override
    public boolean transitionAllowed(ContractState targetState) {
        return super.transitionAllowed(targetState) && switch(targetState) {
            // on signed consumer at least the consumer provisioning needs to be done
            case SIGNED_CONSUMER ->
                    consumerTransferProvisioning != null
                    && consumerTransferProvisioning.configurationValid()
                    && consumerTransferProvisioning.commonConfigurationValid(this);
            // on released both consumer and provider must have valid provisioning
            case RELEASED ->
                    consumerTransferProvisioning != null
                    && providerTransferProvisioning != null
                    && consumerTransferProvisioning.configurationValid()
                    && consumerTransferProvisioning.commonConfigurationValid(this)
                    && providerTransferProvisioning.configurationValid()
                    && providerTransferProvisioning.commonConfigurationValid(this);
            // on other cases the provisioning is irrelevant
            default -> true;
        };
    }

    @Override
    public ServiceContractProvisioning makeCopy() {
        DataDeliveryProvisioning provisioning = new DataDeliveryProvisioning();
        if (this.consumerTransferProvisioning != null) {
            provisioning.setConsumerTransferProvisioning(this.consumerTransferProvisioning.makeCopy());
        }
        if (this.providerTransferProvisioning != null) {
            provisioning.setProviderTransferProvisioning(this.providerTransferProvisioning.makeCopy());
        }
        return provisioning;
    }
}
