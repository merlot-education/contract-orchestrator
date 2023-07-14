package eu.merloteducation.contractorchestrator.models.entities;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public class ServiceContractProvisioningDeserializer extends StdDeserializer<ServiceContractProvisioning> {
    protected ServiceContractProvisioningDeserializer() {
        this(null);
    }

    protected ServiceContractProvisioningDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ServiceContractProvisioning deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = mapper.readTree(jsonParser);
        Class<? extends ServiceContractProvisioning> requestClass = null;

        if (root.has("dataAddressSourceBucketName") || root.has("dataAddressTargetBucketName")) {
            requestClass = DataDeliveryProvisioning.class;
        } else {
            requestClass = DefaultProvisioning.class;
        }

        return mapper.treeToValue(root, requestClass);
    }

}
