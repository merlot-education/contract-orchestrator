package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "discriminator")
@JsonDeserialize(using = ContractTemplateDeserializer.class)
public abstract class ContractTemplate {
    @Id
    @JsonView(ContractViews.BasicView.class)
    @Setter(AccessLevel.NONE)
    private String id;

    @Column(name="discriminator", insertable = false, updatable = false)
    @JsonView(ContractViews.BasicView.class)
    @Setter(AccessLevel.NONE)
    @JsonProperty("type")
    private String type;

    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @JsonView(ContractViews.BasicView.class)
    private ContractState state;

    @JsonView(ContractViews.BasicView.class)
    @Setter(AccessLevel.NONE)
    private OffsetDateTime creationDate;

    @JsonView(ContractViews.BasicView.class)
    private String offeringId;

    @JsonView(ContractViews.BasicView.class)
    private String providerId;

    @JsonView(ContractViews.BasicView.class)
    private String consumerId;

    @JsonView(ContractViews.DetailedView.class)
    private String runtimeSelection;

    @JsonView(ContractViews.ConsumerView.class)
    private boolean consumerMerlotTncAccepted;

    @JsonView(ContractViews.ProviderView.class)
    private boolean providerMerlotTncAccepted;

    @JsonView(ContractViews.ConsumerView.class)
    private boolean consumerOfferingTncAccepted;

    @JsonView(ContractViews.ConsumerView.class)
    private boolean consumerProviderTncAccepted;

    @JsonView(ContractViews.DetailedView.class)
    @ElementCollection
    private List<ContractTnc> termsAndConditions;

    @JsonView(ContractViews.DetailedView.class)
    private String additionalAgreements;

    @JsonView(ContractViews.DetailedView.class)
    private List<String> offeringAttachments;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "provisioning_id")
    @JsonView(ContractViews.DetailedView.class)
    private ServiceContractProvisioning serviceContractProvisioning;

    @JsonView(ContractViews.ConsumerView.class)
    private String consumerSignerUserId;

    @JsonView(ContractViews.ConsumerView.class)
    private String consumerSignature;

    @JsonView(ContractViews.ProviderView.class)
    private String providerSignerUserId;

    @JsonView(ContractViews.ProviderView.class)
    private String providerSignature;

    protected ContractTemplate() {
        this.state = ContractState.IN_DRAFT;
        this.id = "Contract:" + UUID.randomUUID();
        this.creationDate = OffsetDateTime.now();
        this.offeringAttachments = new ArrayList<>();
        this.additionalAgreements = "";
        this.serviceContractProvisioning = new DefaultProvisioning();
    }

    protected ContractTemplate(ContractTemplate template, boolean regenerate) {
        this.state = regenerate ? ContractState.IN_DRAFT : template.getState();
        this.id = regenerate ? "Contract:" + UUID.randomUUID() : template.getId();
        this.creationDate = OffsetDateTime.now();
        this.offeringId = template.getOfferingId();
        this.providerId = template.getProviderId();
        this.consumerId = template.getConsumerId();
        this.runtimeSelection = template.getRuntimeSelection();
        this.consumerMerlotTncAccepted = template.isConsumerMerlotTncAccepted();
        this.providerMerlotTncAccepted = template.isProviderMerlotTncAccepted();
        this.consumerOfferingTncAccepted = template.isConsumerOfferingTncAccepted();
        this.consumerProviderTncAccepted = template.isConsumerProviderTncAccepted();
        this.termsAndConditions = template.getTermsAndConditions();
        this.additionalAgreements = template.getAdditionalAgreements();
        this.offeringAttachments = new ArrayList<>(template.getOfferingAttachments());
        this.providerSignerUserId = regenerate ? null : template.getProviderSignerUserId();
        this.providerSignature = regenerate ? null : template.getProviderSignature();
        this.consumerSignerUserId = regenerate ? null : template.getConsumerSignerUserId();
        this.consumerSignature = regenerate ? null : template.getConsumerSignature();
        this.serviceContractProvisioning = template.getServiceContractProvisioning();
    }

    public void transitionState(ContractState targetState) {
        if (state.checkTransitionAllowed(targetState)) {
            if ((targetState == ContractState.SIGNED_CONSUMER &&
                    (StringUtil.isNullOrEmpty(runtimeSelection)
                            || StringUtil.isNullOrEmpty(consumerSignerUserId)
                            || StringUtil.isNullOrEmpty(consumerSignature)
                            || !consumerMerlotTncAccepted || !consumerOfferingTncAccepted || !consumerProviderTncAccepted)) ||
                    (targetState == ContractState.RELEASED &&
                            (StringUtil.isNullOrEmpty(providerSignerUserId)
                                    || StringUtil.isNullOrEmpty(providerSignature) ||
                                    !providerMerlotTncAccepted))) {
                throw new IllegalStateException(
                        String.format("Cannot transition from state %s to %s as mandatory fields are not set",
                                state.name(), targetState.name()));

            }
            state = targetState;
        } else {
            throw new IllegalStateException(
                    String.format("Not allowed to transition from state %s to %s", state.name(), targetState.name()));
        }
    }
}
