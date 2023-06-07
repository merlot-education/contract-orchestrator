package eu.merloteducation.contractorchestrator;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ContractOrchestratorApplication {
	@RabbitListener(queues = "contract.queue")
	public void listen(String in) {
		System.out.println("Message read from contract.queue : " + in);
	}
	public static void main(String[] args) {
		SpringApplication.run(ContractOrchestratorApplication.class, args);
	}

}
