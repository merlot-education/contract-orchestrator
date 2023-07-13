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
public class DataDeliveryContractTemplate extends ContractTemplate {
    @JsonView(ContractViews.DetailedView.class)
    private String exchangeCountSelection;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressName;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressBaseUrl;

    @JsonView(ContractViews.ProviderView.class)
    private String dataAddressDataType;

    @JsonView(ContractViews.ConsumerView.class)
    private String consumerEdcToken;

    @JsonView(ContractViews.ProviderView.class)
    private String providerEdcToken;

    public DataDeliveryContractTemplate() {
        super();
    }

    public DataDeliveryContractTemplate(DataDeliveryContractTemplate template) {
        super(template);
        this.exchangeCountSelection = template.getExchangeCountSelection();
        this.dataAddressBaseUrl = template.getDataAddressBaseUrl();
        this.dataAddressName = template.getDataAddressName();
        this.dataAddressDataType = template.getDataAddressDataType();
        this.consumerEdcToken = template.getConsumerEdcToken();
        this.providerEdcToken = template.getConsumerEdcToken();
    }

    @Override
    public void transitionState(ContractState targetState) {
        if (targetState == ContractState.SIGNED_CONSUMER) {
            if (exchangeCountSelection == null || exchangeCountSelection.isEmpty()) {
                throw new IllegalStateException(
                        String.format("Cannot transition from state %s to %s as mandatory fields are not set",
                                getState().name(), targetState.name()));
            }
        }
        super.transitionState(targetState);
    }
}
