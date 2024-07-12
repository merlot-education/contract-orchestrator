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

package eu.merloteducation.contractorchestrator.models.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ServiceContractProvisioning {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue
    private Long id;

    private OffsetDateTime validUntil;


    @OneToOne(mappedBy = "serviceContractProvisioning")
    private ContractTemplate contractTemplate;

    public boolean transitionAllowed(ContractState targetState) {
        return targetState != null; // validUntil can be null if selected time is unlimited
    }

    public abstract ServiceContractProvisioning makeCopy();
}
