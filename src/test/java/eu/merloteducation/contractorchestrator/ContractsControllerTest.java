package eu.merloteducation.contractorchestrator;

import eu.merloteducation.contractorchestrator.controller.ContractsController;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

@WebMvcTest(ContractsController.class)
public class ContractsControllerTest {

    @BeforeEach
    public void setUp() throws Exception {
    }
}
