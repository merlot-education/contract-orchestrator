package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@JsonDeserialize
public class SaasContractTemplate extends ContractTemplate {

    @JsonView(ContractViews.DetailedView.class)
    private String userCountSelection;

    public SaasContractTemplate() {
        super();
    }

    public SaasContractTemplate(SaasContractTemplate template) {
        super(template);
        this.userCountSelection = template.getUserCountSelection();
    }
}
