package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Getter
@Setter
public class Contract {
    @Id
    @JsonView({ContractViews.BasicView.class, ContractViews.DetailedView.class})
    private String id;

    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @JsonView({ContractViews.BasicView.class, ContractViews.DetailedView.class})
    private ContractState state;

    @JsonView({ContractViews.BasicView.class, ContractViews.DetailedView.class})
    private OffsetDateTime creationDate;

    @JsonView({ContractViews.BasicView.class, ContractViews.DetailedView.class})
    private String offeringId;

    @JsonView({ContractViews.BasicView.class, ContractViews.DetailedView.class})
    private String offeringName;

    @JsonView({ContractViews.BasicView.class, ContractViews.DetailedView.class})
    private String providerId;

    @JsonView({ContractViews.BasicView.class, ContractViews.DetailedView.class})
    private String consumerId;

    @JsonView(ContractViews.DetailedView.class)
    private String runtimeSelection;

    @JsonView(ContractViews.DetailedView.class)
    private String exchangeCountSelection;

    @JsonView(ContractViews.DetailedView.class)
    private String userCountSelection;

    @JsonView(ContractViews.DetailedView.class)
    private boolean consumerMerlotTncAccepted;

    @JsonView(ContractViews.DetailedView.class)
    private boolean providerMerlotTncAccepted;

    @JsonView(ContractViews.DetailedView.class)
    private boolean consumerOfferingTncAccepted;

    @JsonView(ContractViews.DetailedView.class)
    private boolean consumerProviderTncAccepted;

    @JsonView(ContractViews.DetailedView.class)
    private String providerTncUrl;

    @JsonView(ContractViews.DetailedView.class)
    private String providerTncHash;

    @JsonView(ContractViews.DetailedView.class)
    private String additionalAgreements;

    @JsonView(ContractViews.DetailedView.class)
    private List<String> offeringAttachments;

    public Contract() {
        this.state = ContractState.IN_DRAFT;
    }

    public void transitionState(ContractState targetState) {
        if (state.checkTransitionAllowed(targetState)) {
            state = targetState;
        } else {
            throw new IllegalStateException(
                    String.format("Cannot transition from state %s to %s", state.name(), targetState.name()));
        }
    }
}
