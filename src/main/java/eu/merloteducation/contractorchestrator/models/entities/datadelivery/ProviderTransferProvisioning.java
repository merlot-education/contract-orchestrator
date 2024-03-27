package eu.merloteducation.contractorchestrator.models.entities.datadelivery;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ProviderTransferProvisioning {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue
    private Long id;

    private String selectedProviderConnectorId;

    protected ProviderTransferProvisioning() {
        selectedProviderConnectorId = "";
    }

    public boolean transitionAllowed(ContractState targetState, DataDeliveryProvisioning provisioning) {
        return switch (targetState) {
            case RELEASED ->
                    !StringUtil.isNullOrEmpty(selectedProviderConnectorId)
                            && !provisioning.getConsumerTransferProvisioning().getSelectedConsumerConnectorId()
                            .equals(selectedProviderConnectorId);
            default -> true;
        };
    }

    public abstract ProviderTransferProvisioning makeCopy();
}
