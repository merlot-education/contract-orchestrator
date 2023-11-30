package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.modelslib.api.contract.ContractPdfDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface PdfServiceClient {
    @PostExchange("/PdfProcessor/PdfContract")
    byte[] getPdfContract(@RequestBody ContractPdfDto data);
}
