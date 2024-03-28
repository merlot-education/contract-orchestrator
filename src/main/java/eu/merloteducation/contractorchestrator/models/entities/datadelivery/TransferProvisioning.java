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

    public boolean configurationValid() {
        return !StringUtil.isNullOrEmpty(selectedConnectorId);
    }

    public boolean commonConfigurationValid(DataDeliveryProvisioning provisioning) {
        boolean result = true;

        TransferProvisioning consumerProv = provisioning.getConsumerTransferProvisioning();
        TransferProvisioning providerProv = provisioning.getProviderTransferProvisioning();
        if (consumerProv != null && providerProv != null) {
            result = !consumerProv.getSelectedConnectorId().equals(providerProv.getSelectedConnectorId());
        }

        return result;
    }

    public abstract TransferProvisioning makeCopy();
}
