/*
 *  Copyright 2023-2024 Dataport AöR
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

package eu.merloteducation.contractorchestrator.models.mappers;

import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.cooperation.CooperationContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.DataDeliveryContractTemplate;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.TransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioning;
import eu.merloteducation.contractorchestrator.models.entities.saas.SaasContractTemplate;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxVcard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalRegistrationNumberCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.modelslib.api.contract.ContractBasicDto;
import eu.merloteducation.modelslib.api.contract.ContractDto;
import eu.merloteducation.modelslib.api.contract.ContractVcard;
import eu.merloteducation.modelslib.api.contract.cooperation.CooperationContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.DataDeliveryContractDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.TransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ConsumerTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.datadelivery.ionoss3extension.IonosS3ProviderTransferProvisioningDto;
import eu.merloteducation.modelslib.api.contract.saas.SaasContractDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.serviceoffering.ServiceOfferingDto;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring", imports = { GxLegalParticipantCredentialSubject.class,
        GxLegalRegistrationNumberCredentialSubject.class, MerlotLegalParticipantCredentialSubject.class })
public interface ContractToDtoMapper {
    @Mapping(target = "id", source = "contract.id")
    @Mapping(target = "creationDate", source = "contract.creationDate")
    @Mapping(target = "offering", source = "offeringDetails")
    @Mapping(target = "providerId", source = "offeringDetails.providerDetails.providerId")
    @Mapping(target = "providerLegalName", source = "offeringDetails.providerDetails.providerLegalName")
    @Mapping(target = "providerActive", source = "providerOrgaDetails.metadata.active")
    @Mapping(target = "consumerId", source = "consumerOrgaDetails", qualifiedByName = "mapParticipantId")
    @Mapping(target = "consumerLegalName", source = "consumerOrgaDetails", qualifiedByName = "mapParticipantLegalName")
    @Mapping(target = "consumerActive", source = "consumerOrgaDetails.metadata.active")
    @Mapping(target = "state", source = "contract.state")
    ContractBasicDto contractToContractBasicDto(ContractTemplate contract, MerlotParticipantDto providerOrgaDetails,
                                                MerlotParticipantDto consumerOrgaDetails, ServiceOfferingDto offeringDetails);

    @Mapping(target = "type", source = "contract.type")
    @Mapping(target = "details.id", source = "contract.id")
    @Mapping(target = "details.creationDate", source = "contract.creationDate")
    @Mapping(target = "details.providerId", source = "contract.providerId")
    @Mapping(target = "details.providerLegalName", source = "offeringDetails.providerDetails.providerLegalName")
    @Mapping(target = "details.providerLegalAddress", source = "providerOrgaDetails", qualifiedByName = "mapParticipantLegalAddress")
    @Mapping(target = "details.providerActive", source = "providerOrgaDetails.metadata.active")
    @Mapping(target = "details.consumerId", source = "contract.consumerId")
    @Mapping(target = "details.consumerLegalName", source = "consumerOrgaDetails", qualifiedByName = "mapParticipantLegalName")
    @Mapping(target = "details.consumerLegalAddress", source = "consumerOrgaDetails", qualifiedByName = "mapParticipantLegalAddress")
    @Mapping(target = "details.consumerActive", source = "consumerOrgaDetails.metadata.active")
    @Mapping(target = "details.state", source = "contract.state")
    @Mapping(target = "details.providerSignerUserName", source = "contract.providerSignature.signerName")
    @Mapping(target = "details.providerSignatureDate", source = "contract.providerSignature.signatureDate")
    @Mapping(target = "details.consumerSignerUserName", source = "contract.consumerSignature.signerName")
    @Mapping(target = "details.consumerSignatureDate", source = "contract.consumerSignature.signatureDate")
    @Mapping(target = "details.termsAndConditions", source = "contract.termsAndConditions")
    @Mapping(target = "negotiation.runtimeSelection", source = "contract.runtimeSelection", defaultValue = "")
    @Mapping(target = "negotiation.additionalAgreements", source = "contract.additionalAgreements")
    @Mapping(target = "negotiation.attachments", source = "contract.attachments")
    @Mapping(target = "negotiation.consumerTncAccepted", source = "contract.consumerTncAccepted")
    @Mapping(target = "negotiation.consumerAttachmentsAccepted", source = "contract.consumerAttachmentsAccepted")
    @Mapping(target = "negotiation.providerTncAccepted", source = "contract.providerTncAccepted")
    @Mapping(target = "provisioning.validUntil", source = "contract.serviceContractProvisioning.validUntil")
    @Mapping(target = "offering", source = "offeringDetails")
    ContractDto contractToContractDto(ContractTemplate contract, MerlotParticipantDto providerOrgaDetails,
                                      MerlotParticipantDto consumerOrgaDetails, ServiceOfferingDto offeringDetails);

    @InheritConfiguration(name = "contractToContractDto")
    CooperationContractDto contractToContractDto(CooperationContractTemplate contract,
                                                 MerlotParticipantDto providerOrgaDetails, MerlotParticipantDto consumerOrgaDetails,
                                                 ServiceOfferingDto offeringDetails);

    @InheritConfiguration(name = "contractToContractDto")
    @Mapping(target = "negotiation.userCountSelection", source = "contract.userCountSelection", defaultValue = "")
    SaasContractDto contractToContractDto(SaasContractTemplate contract, MerlotParticipantDto providerOrgaDetails,
                                          MerlotParticipantDto consumerOrgaDetails, ServiceOfferingDto offeringDetails);

    @InheritConfiguration(name = "contractToContractDto")
    @Mapping(target = "negotiation.exchangeCountSelection", source = "contract.exchangeCountSelection", defaultValue = "")
    @Mapping(target = "provisioning.consumerTransferProvisioning", source = "contract.serviceContractProvisioning.consumerTransferProvisioning", qualifiedByName = "transferProvisioningToDto")
    @Mapping(target = "provisioning.providerTransferProvisioning", source = "contract.serviceContractProvisioning.providerTransferProvisioning", qualifiedByName = "transferProvisioningToDto")
    DataDeliveryContractDto contractToContractDto(DataDeliveryContractTemplate contract,
                                                  MerlotParticipantDto providerOrgaDetails, MerlotParticipantDto consumerOrgaDetails,
                                                  ServiceOfferingDto offeringDetails);

    @Mapping(target = "selectedConnectorId", source = "selectedConnectorId")
    @Mapping(target = "dataAddressTargetBucketName", source = "dataAddressTargetBucketName")
    @Mapping(target = "dataAddressTargetPath", source = "dataAddressTargetPath")
    @Mapping(target = "dataAddressType", constant = "IonosS3Dest")
    IonosS3ConsumerTransferProvisioningDto ionosProvisioningToConsumerProvisioningDto(IonosS3ConsumerTransferProvisioning provisioning);

    @Mapping(target = "selectedConnectorId", source = "selectedConnectorId")
    @Mapping(target = "dataAddressSourceBucketName", source = "dataAddressSourceBucketName")
    @Mapping(target = "dataAddressSourceFileName", source = "dataAddressSourceFileName")
    @Mapping(target = "dataAddressType", constant = "IonosS3Source")
    IonosS3ProviderTransferProvisioningDto ionosProvisioningToProviderProvisioningDto(IonosS3ProviderTransferProvisioning provisioning);

    ContractVcard gxVcardToContractVcard(GxVcard vcard);


    default String map(OffsetDateTime offsetDateTime) {

        return offsetDateTime != null ? offsetDateTime.toString() : null;
    }

    @Named("mapParticipantId")
    default String mapParticipantId(MerlotParticipantDto consumerOrgaDetails) {
        return consumerOrgaDetails.getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class)
                .getId();
    }

    @Named("mapParticipantLegalName")
    default String mapParticipantLegalName(MerlotParticipantDto consumerOrgaDetails) {
        return consumerOrgaDetails.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class)
                .getLegalName();
    }

    @Named("mapParticipantLegalAddress")
    default ContractVcard mapParticipantLegalAddress(MerlotParticipantDto consumerOrgaDetails) {
        return gxVcardToContractVcard(consumerOrgaDetails.getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class)
                .getLegalAddress());
    }

    @Named("transferProvisioningToDto")
    default TransferProvisioningDto transferProvisioningToDto(TransferProvisioning provisioning) {
        if (provisioning == null) {
            return null;
        }
        if (provisioning instanceof IonosS3ConsumerTransferProvisioning ionosProvisioning) {
            return ionosProvisioningToConsumerProvisioningDto(ionosProvisioning);
        }
        if (provisioning instanceof IonosS3ProviderTransferProvisioning ionosProvisioning) {
            return ionosProvisioningToProviderProvisioningDto(ionosProvisioning);
        }
        throw new IllegalArgumentException("Unknown transfer provisioning type.");
    }
}
