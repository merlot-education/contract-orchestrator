package eu.merloteducation.contractorchestrator.models.edc;

import java.util.*;

public class EdcConstants {
    public static final Map<String, String> EDC_CONTEXT = Map.of(
            "@vocab", "https://w3id.org/edc/v0.0.1/ns/",
            "edc", "https://w3id.org/edc/v0.0.1/ns/",
            "odrl", "http://www.w3.org/ns/odrl/2/");

    public static final String EDC_PREFIX = "edc:";
    public static final String ODRL_PREFIX = "odrl:";
    public static final String DCAT_PREFIX = "dcat:";
    public static final String DCT_PREFIX = "dct:";

    private EdcConstants() {}
}
