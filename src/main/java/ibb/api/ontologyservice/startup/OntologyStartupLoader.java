package ibb.api.ontologyservice.startup;

import ibb.api.ontologyservice.OntologyServiceConfig;
import ibb.api.ontologyservice.ontology.Ontology;
import ibb.api.ontologyservice.ontology.OntologyFactory;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class OntologyStartupLoader {

    @Inject
    OntologyServiceConfig config;

    @Inject
    OntologyFactory ontologyFactory;

    void initDB(@Observes StartupEvent ev) {
        if (config.startup().noActions()) {
            return;
        }
        config.ontologies().keySet().stream().forEach(ontologyName -> {
            Ontology ontology = ontologyFactory.get(ontologyName);
            ontology.indexIfNotExists();
        });
    }
}
