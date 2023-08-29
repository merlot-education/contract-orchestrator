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
