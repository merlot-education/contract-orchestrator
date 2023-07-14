package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.merloteducation.contractorchestrator.models.views.ContractViews;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@ToString
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name="discriminator")
@JsonDeserialize(using = ContractTemplateDeserializer.class)
public abstract class ContractTemplate {
    @Id
    @JsonView(ContractViews.BasicView.class)
    @Setter(AccessLevel.NONE)
    private String id;

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
    private String offeringName;

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
    private String providerTncUrl;

    @JsonView(ContractViews.DetailedView.class)
    private String additionalAgreements;

    @JsonView(ContractViews.DetailedView.class)
    private List<String> offeringAttachments;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "provisioning_id")
    @JsonView(ContractViews.DetailedView.class)
    private ServiceContractProvisioning serviceContractProvisioning;

    protected ContractTemplate() {
        this.state = ContractState.IN_DRAFT;
        this.id = "Contract:" + UUID.randomUUID();
        this.creationDate = OffsetDateTime.now();
        this.offeringAttachments = new ArrayList<>();
        this.additionalAgreements = "";
        this.serviceContractProvisioning = new ServiceContractProvisioning();
    }

    protected ContractTemplate(ContractTemplate template) {
        this.id = template.getId();
        this.state = template.getState();
        this.creationDate = template.getCreationDate();
        this.offeringId = template.getOfferingId();
        this.offeringName = template.getOfferingName();
        this.providerId = template.getProviderId();
        this.consumerId = template.getConsumerId();
        this.runtimeSelection = template.getRuntimeSelection();
        this.consumerMerlotTncAccepted = template.isConsumerMerlotTncAccepted();
        this.providerMerlotTncAccepted = template.isProviderMerlotTncAccepted();
        this.consumerOfferingTncAccepted = template.isConsumerOfferingTncAccepted();
        this.consumerProviderTncAccepted = template.isConsumerProviderTncAccepted();
        this.providerTncUrl = template.getProviderTncUrl();
        this.additionalAgreements = template.getAdditionalAgreements();
        this.offeringAttachments = new ArrayList<>(template.getOfferingAttachments());
        this.serviceContractProvisioning = template.getServiceContractProvisioning();
    }

    public void transitionState(ContractState targetState) {
        if (state.checkTransitionAllowed(targetState)) {
            if (targetState == ContractState.SIGNED_CONSUMER) {
                if (runtimeSelection == null || runtimeSelection.isEmpty() ||
                        !consumerMerlotTncAccepted  || !consumerOfferingTncAccepted || !consumerProviderTncAccepted ||
                        serviceContractProvisioning == null ||
                        serviceContractProvisioning.getDataAddressTargetFileName() == null ||
                        serviceContractProvisioning.getDataAddressTargetFileName().isEmpty() ||
                        serviceContractProvisioning.getDataAddressTargetBucketName() == null ||
                        serviceContractProvisioning.getDataAddressTargetBucketName().isEmpty()) {
                    throw new IllegalStateException(
                            String.format("Cannot transition from state %s to %s as mandatory fields are not set",
                                    state.name(), targetState.name()));
                }
            } else if (targetState == ContractState.RELEASED) {
                if (serviceContractProvisioning.getDataAddressSourceFileName() == null ||
                        serviceContractProvisioning.getDataAddressSourceFileName().isEmpty() ||
                        serviceContractProvisioning.getDataAddressSourceBucketName() == null ||
                        serviceContractProvisioning.getDataAddressSourceBucketName().isEmpty() ||
                        serviceContractProvisioning.getDataAddressName() == null ||
                        serviceContractProvisioning.getDataAddressName().isEmpty() ||
                        serviceContractProvisioning.getDataAddressType() == null ||
                        serviceContractProvisioning.getDataAddressType().isEmpty()) {
                    throw new IllegalStateException(
                            String.format("Cannot transition from state %s to %s as mandatory fields are not set",
                                    state.name(), targetState.name()));
                }
            }
            state = targetState;
        } else {
            throw new IllegalStateException(
                    String.format("Not allowed to transition from state %s to %s", state.name(), targetState.name()));
        }
    }
}
