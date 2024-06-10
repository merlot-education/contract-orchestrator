package eu.merloteducation.contractorchestrator.models.entities;

import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxSOTermsAndConditions;
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

    public ContractTnc(GxSOTermsAndConditions termsAndConditions) {
        this.content = termsAndConditions.getUrl();
        this.hash = termsAndConditions.getHash();
    }
}
