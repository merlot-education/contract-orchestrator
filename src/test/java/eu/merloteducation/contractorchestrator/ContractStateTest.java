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

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractStateTest {

    @Test
    void transitionFromInDraft() {
        ContractState state = ContractState.IN_DRAFT;

        assertTrue(state.checkTransitionAllowed(ContractState.IN_DRAFT));
        assertTrue(state.checkTransitionAllowed(ContractState.SIGNED_CONSUMER));
        assertTrue(state.checkTransitionAllowed(ContractState.DELETED));

        assertFalse(state.checkTransitionAllowed(ContractState.REVOKED));
        assertFalse(state.checkTransitionAllowed(ContractState.RELEASED));
        assertFalse(state.checkTransitionAllowed(ContractState.ARCHIVED));
    }

    @Test
    void transitionFromDeleted() {
        ContractState state = ContractState.DELETED;

        assertFalse(state.checkTransitionAllowed(ContractState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ContractState.SIGNED_CONSUMER));
        assertFalse(state.checkTransitionAllowed(ContractState.DELETED));
        assertFalse(state.checkTransitionAllowed(ContractState.REVOKED));
        assertFalse(state.checkTransitionAllowed(ContractState.RELEASED));
        assertFalse(state.checkTransitionAllowed(ContractState.ARCHIVED));
    }

    @Test
    void transitionFromConsumerSigned() {
        ContractState state = ContractState.SIGNED_CONSUMER;

        assertTrue(state.checkTransitionAllowed(ContractState.RELEASED));
        assertTrue(state.checkTransitionAllowed(ContractState.REVOKED));

        assertFalse(state.checkTransitionAllowed(ContractState.SIGNED_CONSUMER));
        assertFalse(state.checkTransitionAllowed(ContractState.DELETED));
        assertFalse(state.checkTransitionAllowed(ContractState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ContractState.ARCHIVED));
    }

    @Test
    void transitionFromRevoked() {
        ContractState state = ContractState.REVOKED;

        assertFalse(state.checkTransitionAllowed(ContractState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ContractState.SIGNED_CONSUMER));
        assertFalse(state.checkTransitionAllowed(ContractState.DELETED));
        assertFalse(state.checkTransitionAllowed(ContractState.REVOKED));
        assertFalse(state.checkTransitionAllowed(ContractState.RELEASED));
        assertFalse(state.checkTransitionAllowed(ContractState.ARCHIVED));
    }

    @Test
    void transitionFromReleased() {
        ContractState state = ContractState.RELEASED;

        assertTrue(state.checkTransitionAllowed(ContractState.REVOKED));
        assertTrue(state.checkTransitionAllowed(ContractState.ARCHIVED));

        assertFalse(state.checkTransitionAllowed(ContractState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ContractState.SIGNED_CONSUMER));
        assertFalse(state.checkTransitionAllowed(ContractState.DELETED));
        assertFalse(state.checkTransitionAllowed(ContractState.RELEASED));
    }

    @Test
    void transitionFromArchived() {
        ContractState state = ContractState.ARCHIVED;

        assertFalse(state.checkTransitionAllowed(ContractState.IN_DRAFT));
        assertFalse(state.checkTransitionAllowed(ContractState.SIGNED_CONSUMER));
        assertFalse(state.checkTransitionAllowed(ContractState.DELETED));
        assertFalse(state.checkTransitionAllowed(ContractState.REVOKED));
        assertFalse(state.checkTransitionAllowed(ContractState.RELEASED));
        assertFalse(state.checkTransitionAllowed(ContractState.ARCHIVED));
    }
}
