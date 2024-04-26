package eu.merloteducation.contractorchestrator.models.entities.datadelivery;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.ServiceContractProvisioning;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class DataDeliveryContractTemplate extends ContractTemplate {
    private String exchangeCountSelection;

    public DataDeliveryContractTemplate() {
        super();
    }

    public DataDeliveryContractTemplate(DataDeliveryContractTemplate template, boolean regenerate) {
        super(template, regenerate);
        this.exchangeCountSelection = template.getExchangeCountSelection();
    }

    @Override
    public DataDeliveryProvisioning getServiceContractProvisioning() {
        return (DataDeliveryProvisioning) super.getServiceContractProvisioning();
    }

    @Override
    public boolean transitionAllowed(ContractState targetState) {
        return super.transitionAllowed(targetState) && switch (targetState) {
            case SIGNED_CONSUMER ->
                    !StringUtil.isNullOrEmpty(exchangeCountSelection);
            default -> true;
        };
    }
}
