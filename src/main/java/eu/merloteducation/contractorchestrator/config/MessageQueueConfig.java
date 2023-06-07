package eu.merloteducation.contractorchestrator.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageQueueConfig {
    @Bean
    DirectExchange orchestratorExchange() {
        return new DirectExchange("orchestrator.exchange");
    }

    @Bean
    Binding createdContractBinding(Queue contractQueue, DirectExchange orchestratorExchange) {
        return BindingBuilder.bind(contractQueue).to(orchestratorExchange).with("created.contract");
    }

    @Bean
    public Queue contractQueue() {
        return new Queue("contract.queue", false);
    }
}
