package eu.merloteducation.contractorchestrator.models.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor
@Getter
public class ContractSignature {
    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue
    private Long id;

    private String signerName;
    private OffsetDateTime signatureDate;

    public ContractSignature(String signerName) {
        this.signerName = signerName;

        this.signatureDate = OffsetDateTime.now();
    }
}
