package eu.merloteducation.contractorchestrator.repositories;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, String> {

    @Query("select c from ContractTemplate c where c.providerId = :orgaId or c.consumerId = :orgaId")
    Page<ContractTemplate> findAllByOrgaId(String orgaId, Pageable pageable);

    @Query("select c from ContractTemplate c where (c.providerId = :orgaId or c.consumerId = :orgaId) and c.state = :state")
    Page<ContractTemplate> findAllByOrgaIdAndState(String orgaId, ContractState state, Pageable pageable);
}
