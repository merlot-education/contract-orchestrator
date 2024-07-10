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

package eu.merloteducation.contractorchestrator;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.fasterxml.jackson.core.JsonProcessingException;
import eu.merloteducation.gxfscataloglibrary.models.credentials.CastableCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiablePresentation;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.PojoCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxDataAccountExport;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxSOTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxVcard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.NodeKindIRITypeId;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalRegistrationNumberCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.serviceofferings.GxServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.AllowedUserCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.DataExchangeCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.OfferingRuntime;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.ParticipantTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotCoopContractServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotDataDeliveryServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotSaasServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class SelfDescriptionDemoData {

    static ExtendedVerifiablePresentation createVpFromCsList(List<PojoCredentialSubject> csList, String issuer) throws JsonProcessingException {
        ExtendedVerifiablePresentation vp = new ExtendedVerifiablePresentation();
        List<ExtendedVerifiableCredential> vcList = new ArrayList<>();
        for (PojoCredentialSubject cs : csList) {
            CastableCredentialSubject ccs = CastableCredentialSubject.fromPojo(cs);
            VerifiableCredential vc = VerifiableCredential
                    .builder()
                    .id(URI.create(cs.getId() + "#" + cs.getType()))
                    .issuanceDate(Date.from(Instant.now()))
                    .credentialSubject(ccs)
                    .issuer(URI.create("did:web:some-issuer"))
                    .build();
            vcList.add(ExtendedVerifiableCredential.fromMap(vc.getJsonObject()));
        }
        vp.setVerifiableCredentials(vcList);
        return vp;
    }
    static GxServiceOfferingCredentialSubject getGxServiceOfferingCs(String id, String name, String providedBy){
        GxServiceOfferingCredentialSubject cs = new GxServiceOfferingCredentialSubject();
        cs.setId(id);
        cs.setName(name);
        cs.setPolicy(List.of("policy"));
        cs.setProvidedBy(new NodeKindIRITypeId(providedBy));
        GxDataAccountExport accountExport = new GxDataAccountExport();
        accountExport.setFormatType("application/json");
        accountExport.setAccessType("digital");
        accountExport.setRequestType("API");
        cs.setDataAccountExport(List.of(accountExport));
        cs.setDataProtectionRegime(List.of("GDPR2016"));
        cs.setDescription("Some offering description");
        GxSOTermsAndConditions tnc1 = new GxSOTermsAndConditions();
        tnc1.setUrl("http://example.com/1");
        tnc1.setHash("1234");
        GxSOTermsAndConditions tnc2 = new GxSOTermsAndConditions();
        tnc2.setUrl("http://example.com/2");
        tnc2.setHash("1234");
        cs.setTermsAndConditions(List.of(tnc1, tnc2));
        return cs;
    }

    static MerlotServiceOfferingCredentialSubject getMerlotServiceOfferingCs(String id){
        MerlotServiceOfferingCredentialSubject cs = new MerlotServiceOfferingCredentialSubject();
        cs.setId(id);
        cs.setCreationDate("2023-05-24T13:32:22.712661Z");
        cs.setExampleCosts("5€");
        cs.setMerlotTermsAndConditionsAccepted(true);
        OfferingRuntime option1 = new OfferingRuntime();
        option1.setRuntimeCount(4);
        option1.setRuntimeMeasurement("day(s)");
        OfferingRuntime option2 = new OfferingRuntime();
        option2.setRuntimeCount(0);
        option2.setRuntimeMeasurement("unlimited");
        cs.setRuntimeOptions(List.of(option1, option2));
        return cs;
    }

    static MerlotSaasServiceOfferingCredentialSubject getMerlotSaasServiceOfferingCs(String id){
        MerlotSaasServiceOfferingCredentialSubject cs = new MerlotSaasServiceOfferingCredentialSubject();
        cs.setId(id);
        cs.setHardwareRequirements("1.21 Gigawatts");
        AllowedUserCount userCount = new AllowedUserCount();
        userCount.setUserCountUpTo(0);
        cs.setUserCountOptions(List.of(userCount));
        return cs;
    }

    static MerlotDataDeliveryServiceOfferingCredentialSubject getMerlotDataDeliveryServiceOfferingCs(String id, String transferType){
        MerlotDataDeliveryServiceOfferingCredentialSubject cs = new MerlotDataDeliveryServiceOfferingCredentialSubject();
        cs.setId(id);
        cs.setDataAccessType("Download");
        cs.setDataTransferType(transferType);
        DataExchangeCount exchangeCount = new DataExchangeCount();
        exchangeCount.setExchangeCountUpTo(0);
        cs.setExchangeCountOptions(List.of(exchangeCount));
        return cs;
    }

    static MerlotCoopContractServiceOfferingCredentialSubject getMerlotCoopContractServiceOfferingCs(String id){
        MerlotCoopContractServiceOfferingCredentialSubject cs = new MerlotCoopContractServiceOfferingCredentialSubject();
        cs.setId(id);
        return cs;
    }

    static MerlotLegalParticipantCredentialSubject getMerlotParticipantCs(String id) {
        MerlotLegalParticipantCredentialSubject expected = new MerlotLegalParticipantCredentialSubject();
        expected.setId(id);
        expected.setLegalName("MyOrga");

        ParticipantTermsAndConditions termsAndConditions = new ParticipantTermsAndConditions();
        termsAndConditions.setUrl("http://example.com");
        termsAndConditions.setHash("1234");
        expected.setTermsAndConditions(termsAndConditions);

        return expected;
    }

    static GxLegalParticipantCredentialSubject getGxParticipantCs(String id) {

        GxLegalParticipantCredentialSubject expected = new GxLegalParticipantCredentialSubject();
        expected.setId(id);
        expected.setName("MyOrga");

        GxVcard vCard = new GxVcard();
        vCard.setLocality("Berlin");
        vCard.setPostalCode("12345");
        vCard.setCountryCode("DE");
        vCard.setCountrySubdivisionCode("DE-BE");
        vCard.setStreetAddress("Some Street 3");
        expected.setLegalAddress(vCard);
        expected.setHeadquarterAddress(vCard);
        expected.setLegalRegistrationNumber(List.of(new NodeKindIRITypeId(id + "-regId")));

        return expected;
    }

    static GxLegalRegistrationNumberCredentialSubject getGxRegistrationNumberCs(String id) {

        GxLegalRegistrationNumberCredentialSubject expected = new GxLegalRegistrationNumberCredentialSubject();
        expected.setId(id + "-regId");
        expected.setLeiCode("0110");

        return expected;
    }
}
