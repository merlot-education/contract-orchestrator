package eu.merloteducation.contractorchestrator.repositories;

import eu.merloteducation.contractorchestrator.models.entities.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IContractRepository extends JpaRepository<Contract, String> {
}
