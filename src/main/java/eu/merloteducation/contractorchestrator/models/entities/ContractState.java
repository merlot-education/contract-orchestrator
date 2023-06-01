package eu.merloteducation.contractorchestrator.models.entities;

import lombok.Getter;

@Getter
public enum ContractState {
    IN_DRAFT(10, 1, 19),
    SIGNED_CONSUMER(31, 2, 12),
    RELEASED(40, 4, 40),
    REVOKED(60, 8, 0),
    DELETED(70, 16, 0),
    ARCHIVED(80, 32, 0);

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
