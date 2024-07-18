/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "consumer_signature_id")
    private ContractSignature consumerSignature;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "provider_signature_id")
    private ContractSignature providerSignature;

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
        this.providerSignature = regenerate ? null : template.getProviderSignature();
        this.consumerSignature = regenerate ? null : template.getConsumerSignature();
        this.serviceContractProvisioning = template.getServiceContractProvisioning().makeCopy();
    }

    public void addAttachment(String attachment) {
        if (this.state != ContractState.IN_DRAFT) {
            throw new IllegalStateException("Cannot add attachment to contract since it is not in draft");
        }
        this.attachments.add(attachment);
    }

    public boolean transitionAllowed(ContractState targetState) {

        boolean templateAllowed = switch (targetState) {
            case SIGNED_CONSUMER ->
                    !StringUtil.isNullOrEmpty(runtimeSelection)
                    && consumerSignature != null
                    && consumerTncAccepted
                    && (attachments.isEmpty() || consumerAttachmentsAccepted);
            case RELEASED ->
                    providerSignature != null
                    && providerTncAccepted;
            default -> true;
        };

        ServiceContractProvisioning provisioning = getServiceContractProvisioning();
        boolean provisioningAllowed = provisioning != null && provisioning.transitionAllowed(targetState);

        return templateAllowed && provisioningAllowed; // ask both the template and provisioning
    }

    public void transitionState(ContractState targetState) {
        if (state.checkTransitionAllowed(targetState)) { // general transition by state allowed?
            if (this.transitionAllowed(targetState)) { // all mandatory fields set for transition?
                state = targetState;
            } else {
                throw new IllegalStateException(
                        String.format("Cannot transition from state %s to %s as mandatory fields are not set or invalid.",
                                state.name(), targetState.name()));
            }
        } else {
            throw new IllegalStateException(
                    String.format("Not allowed to transition from state %s to %s", state.name(), targetState.name()));
        }
    }
}
