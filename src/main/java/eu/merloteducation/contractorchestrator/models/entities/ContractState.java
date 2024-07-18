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

import lombok.Getter;

@Getter
public enum ContractState {
    IN_DRAFT(10, 1, 19),
    SIGNED_CONSUMER(31, 2, 12),
    RELEASED(40, 4, 40),
    REVOKED(60, 8, 0),
    DELETED(70, 16, 0),
    ARCHIVED(80, 32, 0),
    PURGED(90, 64, 0);

    private final int numVal;
    private final int stateBit;
    private final int allowedStatesBits;

    ContractState(int numVal, int stateBit, int allowedStatesBits) {
        this.numVal = numVal;
        this.stateBit = stateBit;
        this.allowedStatesBits = allowedStatesBits;
    }

    public boolean checkTransitionAllowed(ContractState end) {
        return (allowedStatesBits & end.getStateBit()) != 0;
    }
}
