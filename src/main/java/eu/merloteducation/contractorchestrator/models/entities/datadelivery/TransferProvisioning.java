package eu.merloteducation.contractorchestrator.models.entities.datadelivery;

import io.netty.util.internal.StringUtil;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class TransferProvisioning {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue
    private Long id;

    private String selectedConnectorId;

    protected TransferProvisioning() {
        selectedConnectorId = "";
    }

    public boolean configurationValid(DataDeliveryProvisioning provisioning) {
        return !StringUtil.isNullOrEmpty(selectedConnectorId)
                && commonConfigurationValid(provisioning);
    }

    private boolean commonConfigurationValid(DataDeliveryProvisioning provisioning) {
        TransferProvisioning consumerProv = provisioning.getConsumerTransferProvisioning();
        TransferProvisioning providerProv = provisioning.getProviderTransferProvisioning();

        if (consumerProv != null && providerProv != null) {
            return !consumerProv.getSelectedConnectorId().equals(providerProv.getSelectedConnectorId());
        }

        return true;
    }

    public abstract TransferProvisioning makeCopy();
}
