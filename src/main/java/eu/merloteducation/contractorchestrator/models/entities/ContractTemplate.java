package eu.merloteducation.contractorchestrator.models.entities;

import io.netty.util.internal.StringUtil;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.*;


@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "discriminator")
public abstract class ContractTemplate {
    @Id
    @Setter(AccessLevel.NONE)
    private String id;

    @Column(name="discriminator", insertable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private String type;

    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    private ContractState state;

    @Setter(AccessLevel.NONE)
    private OffsetDateTime creationDate;

    private String offeringId;

    private String providerId;

    private String consumerId;

    private String runtimeSelection;

    private boolean consumerTncAccepted;

    private boolean providerTncAccepted;

    private boolean consumerAttachmentsAccepted;

    @ElementCollection
    private List<ContractTnc> termsAndConditions;

    private String additionalAgreements;

    @Size(max=10)
    private Set<String> attachments;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "provisioning_id")
    private ServiceContractProvisioning serviceContractProvisioning;

    private String consumerSignerUserId;

    private String consumerSignature;

    private String providerSignerUserId;

    private String providerSignature;

    protected ContractTemplate() {
        this.state = ContractState.IN_DRAFT;
        this.id = "Contract:" + UUID.randomUUID();
        this.creationDate = OffsetDateTime.now();
        this.attachments = new HashSet<>();
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
        this.consumerTncAccepted = template.isConsumerTncAccepted();
        this.consumerAttachmentsAccepted = template.isConsumerAttachmentsAccepted();
        this.providerTncAccepted = template.isProviderTncAccepted();
        this.termsAndConditions = template.getTermsAndConditions();
        this.additionalAgreements = template.getAdditionalAgreements();
        this.attachments = new HashSet<>(template.getAttachments());
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
                            || !consumerTncAccepted
                            || (!attachments.isEmpty() && !consumerAttachmentsAccepted)))
                    || (targetState == ContractState.RELEASED &&
                            (StringUtil.isNullOrEmpty(providerSignerUserId)
                                    || StringUtil.isNullOrEmpty(providerSignature) ||
                                    !providerTncAccepted))) {
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

    public void addAttachment(String attachment) {
        if (this.state != ContractState.IN_DRAFT) {
            throw new IllegalStateException("Cannot add attachment to contract since it is not in draft");
        }
        this.attachments.add(attachment);
    }
}
