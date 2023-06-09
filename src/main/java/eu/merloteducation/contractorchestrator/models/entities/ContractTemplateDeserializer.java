package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.xml.crypto.Data;
import java.io.IOException;
import java.rmi.UnmarshalException;

public class ContractTemplateDeserializer extends StdDeserializer<ContractTemplate> {
    protected ContractTemplateDeserializer() {
        this(null);
    }

    protected ContractTemplateDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ContractTemplate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = mapper.readTree(jsonParser);
        Class<? extends ContractTemplate> requestClass = null;

        if (root.has("userCountSelection")) {
            requestClass = SaasContractTemplate.class;
        } else if (root.has("exchangeCountSelection")) {
            requestClass = DataDeliveryContractTemplate.class;
        }

        return mapper.treeToValue(root, requestClass);
    }

}