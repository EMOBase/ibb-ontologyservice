package ibb.api.ontologyservice;

import java.util.Map;

import ibb.api.ontologyservice.ontology.OntologyConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "ontologyservice")
public interface OntologyServiceConfig {
    interface StartupConfig {
        boolean noActions();
    }

    interface ElasticsearchConfig {
        @WithDefault("ontologyservice")
        String indexPrefix();
    }

    StartupConfig startup();
    Map<String, OntologyConfig> ontologies();
    ElasticsearchConfig elasticsearch();
}
