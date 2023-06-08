package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
public class ContractTemplate {
    @Id
    @JsonView({ContractViews.BasicView.class, ContractViews.DetailedView.class})
    @Setter(AccessLevel.NONE)
    private String id;

    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @JsonView({ContractViews.BasicView.class, ContractViews.DetailedView.class})
    private ContractState state;

    @JsonView({ContractViews.BasicView.class, ContractViews.DetailedView.class})
    @Setter(AccessLevel.NONE)
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
    private String additionalAgreements;

    @JsonView(ContractViews.DetailedView.class)
    @Setter(AccessLevel.NONE)
    private List<String> offeringAttachments;

    public ContractTemplate() {
        this.state = ContractState.IN_DRAFT;
        this.id = "Contract:" + UUID.randomUUID();
        this.creationDate = OffsetDateTime.now();
        this.offeringAttachments = new ArrayList<>();
        this.additionalAgreements = "";
    }

    public ContractTemplate(ContractTemplate template) {
        this.id = template.getId();
        this.state = template.getState();
        this.creationDate = template.getCreationDate();
        this.offeringId = template.getOfferingId();
        this.offeringName = template.getOfferingName();
        this.providerId = template.getProviderId();
        this.consumerId = template.getConsumerId();
        this.runtimeSelection = template.getRuntimeSelection();
        this.exchangeCountSelection = template.getExchangeCountSelection();
        this.userCountSelection = template.getUserCountSelection();
        this.consumerMerlotTncAccepted = template.isConsumerMerlotTncAccepted();
        this.providerMerlotTncAccepted = template.isProviderMerlotTncAccepted();
        this.consumerOfferingTncAccepted = template.isConsumerOfferingTncAccepted();
        this.consumerProviderTncAccepted = template.isConsumerProviderTncAccepted();
        this.providerTncUrl = template.getProviderTncUrl();
        this.additionalAgreements = template.getAdditionalAgreements();
        this.offeringAttachments = new ArrayList<>(template.getOfferingAttachments());
    }

    public void transitionState(ContractState targetState) {
        if (state.checkTransitionAllowed(targetState)) {
            state = targetState;
        } else {
            throw new IllegalStateException(
                    String.format("Cannot transition from state %s to %s", state.name(), targetState.name()));
        }
    }

    public void addAttachment(String attachment) {
        this.offeringAttachments.add(attachment);
    }
}
