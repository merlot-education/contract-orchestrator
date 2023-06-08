package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class DataDeliveryContractTemplate extends ContractTemplate {
    @JsonView(ContractViews.DetailedView.class)
    private String exchangeCountSelection;

    public DataDeliveryContractTemplate() {
        super();
    }

    public DataDeliveryContractTemplate(DataDeliveryContractTemplate template) {
        super(template);
        this.exchangeCountSelection = template.getExchangeCountSelection();
    }
}
