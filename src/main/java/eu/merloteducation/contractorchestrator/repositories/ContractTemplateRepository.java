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

package eu.merloteducation.contractorchestrator.repositories;

import eu.merloteducation.contractorchestrator.models.entities.ContractState;
import eu.merloteducation.contractorchestrator.models.entities.ContractTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, String> {

    @Query("select c from ContractTemplate c where c.providerId = :orgaId or c.consumerId = :orgaId")
    Page<ContractTemplate> findAllByOrgaId(String orgaId, Pageable pageable);

    @Query("select c from ContractTemplate c where (c.providerId = :orgaId or c.consumerId = :orgaId) and c.state = :state")
    Page<ContractTemplate> findAllByOrgaIdAndState(String orgaId, ContractState state, Pageable pageable);

    @Query("select c from ContractTemplate c where (c.providerId = :orgaId or c.consumerId = :orgaId) and c.state = :state")
    List<ContractTemplate> findAllByOrgaIdAndState(String orgaId, ContractState state);
}
