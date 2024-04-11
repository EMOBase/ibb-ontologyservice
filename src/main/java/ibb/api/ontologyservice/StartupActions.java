package ibb.api.ontologyservice;

import java.io.IOException;

import ibb.api.ontologyservice.ontology.Ontology;
import ibb.api.ontologyservice.ontology.OntologyFactory;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class StartupActions {

    @Inject
    OntologyServiceConfig config;

    @Inject
    OntologyFactory ontologyFactory;

    void init(@Observes StartupEvent ev) throws IOException {
        if (config.startup().noActions()) {
            return;
        }
        for (var ontologyName : config.ontologies().keySet()) {
            Ontology ontology = ontologyFactory.get(ontologyName);
            long count = ontology.count();
            if (count == 0) {
                ontology.load();
            } else {
                Log.infof("Ontology %s already indexed with %d terms", ontologyName, count);
            }
        }
    }
}
