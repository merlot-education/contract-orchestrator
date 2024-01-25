package eu.merloteducation.contractorchestrator.models.entities;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.TermsAndConditions;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
public class ContractTnc {
    private String content;
    private String hash;

    public ContractTnc(TermsAndConditions termsAndConditions) {
        this.content = termsAndConditions.getContent();
        this.hash = termsAndConditions.getHash();
    }
}
