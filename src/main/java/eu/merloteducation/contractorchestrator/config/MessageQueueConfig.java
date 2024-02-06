package eu.merloteducation.contractorchestrator.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageQueueConfig {

    public static final String ORCHESTRATOR_EXCHANGE = "orchestrator.exchange";
    public static final String CONTRACT_CREATED_KEY = "created.contract";
    public static final String CONTRACT_PURGED_KEY = "purged.contract";
    public static final String ORGANIZATION_REVOKED_KEY = "revoked.organization";
    public static final String ORGANIZATION_REQUEST_KEY = "request.organization";
    public static final String OFFERING_REQUEST_KEY = "request.offering";
    public static final String ORGANIZATIONCONNECTOR_REQUEST_KEY = "request.organizationconnector";
    public static final String ORGANIZATION_REVOKED_QUEUE = "contract.revoke.organization.queue";
    @Bean
    DirectExchange orchestratorExchange() {
        return new DirectExchange(ORCHESTRATOR_EXCHANGE);
    }

    @Bean
    Binding organizationRevokedBinding(Queue organizationRevokedQueue, DirectExchange orchestratorExchange) {
        return BindingBuilder.bind(organizationRevokedQueue).to(orchestratorExchange).with(ORGANIZATION_REVOKED_KEY);
    }

    @Bean
    public Queue organizationRevokedQueue() {
        return new Queue(ORGANIZATION_REVOKED_QUEUE, false);
    }

    @Bean
    public MessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}
