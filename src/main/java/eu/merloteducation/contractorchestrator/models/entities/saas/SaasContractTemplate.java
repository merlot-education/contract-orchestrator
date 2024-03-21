package eu.merloteducation.contractorchestrator.models.entities.saas;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.DefaultProvisioning;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class SaasContractTemplate extends ContractTemplate {

    private String userCountSelection;

    private DefaultProvisioning serviceContractProvisioning;

    public SaasContractTemplate() {
        super();
    }

    public SaasContractTemplate(SaasContractTemplate template, boolean regenerate) {
        super(template, regenerate);
        this.userCountSelection = template.getUserCountSelection();
    }

    @Override
    public boolean transitionAllowed(ContractState targetState) {
        return super.transitionAllowed(targetState) && switch (targetState) {
            case SIGNED_CONSUMER ->
                    !StringUtil.isNullOrEmpty(userCountSelection);
            case REVOKED -> false;
            default -> true;
        };
    }
}
