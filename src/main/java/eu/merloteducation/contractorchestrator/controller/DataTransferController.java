package eu.merloteducation.contractorchestrator.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin({"http://localhost:4200", "https://marketplace.dev.merlot-education.eu", "https://marketplace.demo.merlot-education.eu"})
@RequestMapping("/transfer")
public class DataTransferController {
}
