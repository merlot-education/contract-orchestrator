package eu.merloteducation.contractorchestrator.models.entities;

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
    private String id;

    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    private ContractState state;

    private OffsetDateTime creationDate;

    private String offeringId;

    private String offeringName;

    private String providerId;

    private String consumerId;

    private String runtimeSelection;

    private String exchangeCountSelection;

    private String userCountSelection;

    private boolean consumerMerlotTncAccepted;

    private boolean providerMerlotTncAccepted;

    private boolean consumerOfferingTncAccepted;

    private boolean consumerProviderTncAccepted;

    private String providerTncUrl;

    private String providerTncHash;

    private String additionalAgreements;

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
