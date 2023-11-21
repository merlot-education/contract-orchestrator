package eu.merloteducation.contractorchestrator.service;

import eu.merloteducation.contractorchestrator.models.dto.ContractPdfDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface PdfServiceClient {
    @PostExchange("/pdfProcessor/pdfContract")
    byte[] getPdfContract(@RequestBody ContractPdfDto data);
}
