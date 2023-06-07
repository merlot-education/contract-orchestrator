package eu.merloteducation.contractorchestrator.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageQueueService {

    @Autowired
    RabbitTemplate rabbitTemplate;

    public void sendDemoMessage() {
        rabbitTemplate.convertAndSend("orchestrator.exchange", "created.contract", "Hello, world!");
    }
}
