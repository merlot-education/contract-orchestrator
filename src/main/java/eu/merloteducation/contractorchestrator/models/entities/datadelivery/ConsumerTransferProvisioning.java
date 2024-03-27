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
public abstract class ConsumerTransferProvisioning {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue
    private Long id;

    private String selectedConsumerConnectorId;

    protected ConsumerTransferProvisioning() {
        selectedConsumerConnectorId = "";
    }

    public boolean transitionAllowed(ContractState targetState, DataDeliveryProvisioning provisioning) {
        return switch (targetState) {
            case SIGNED_CONSUMER ->
                    !StringUtil.isNullOrEmpty(selectedConsumerConnectorId);
            default -> true;
        };
    }

    public abstract ConsumerTransferProvisioning makeCopy();
}
