package eu.merloteducation.contractorchestrator.repositories;

import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, String> {

    Page<ContractTemplate> findAllByProviderIdOrConsumerId(String providerId, String consumerId, Pageable pageable);
}
