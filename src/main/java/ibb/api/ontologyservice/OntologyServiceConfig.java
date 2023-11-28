package ibb.api.ontologyservice;

import java.util.Map;

import ibb.api.ontologyservice.ontology.OntologyConfig;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "ontologyservice")
public interface OntologyServiceConfig {
    interface StartupConfig {
        boolean noActions();
    }

    StartupConfig startup();
    Map<String, OntologyConfig> ontologies();
}
