package eu.merloteducation.contractorchestrator.repositories;

import eu.merloteducation.contractorchestrator.models.entities.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, String> {

    Page<Contract> findAllByProviderIdOrConsumerId(String providerId, String consumerId, Pageable pageable);
}
