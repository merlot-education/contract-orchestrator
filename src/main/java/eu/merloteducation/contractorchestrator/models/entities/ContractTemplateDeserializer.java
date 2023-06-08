package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class ContractTemplateDeserializer extends StdDeserializer<ContractTemplate> {
    protected ContractTemplateDeserializer() {
        this(null);
    }

    protected ContractTemplateDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ContractTemplate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        // in here you can add any logic you want
        if (node.has("legs")) {
            Integer legs = (Integer) (node.get("legs")).numberValue();
            String furColor = node.get("furColor").asText();

            return new SaasContractTemplate();
        } else {
            Boolean isLethal = Boolean.valueOf(node.get("isLethal").textValue());
            return new DataDeliveryContractTemplate();
        }
    }

}