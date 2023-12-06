package ibb.api.ontologyservice;

import ibb.api.ontologyservice.ontology.Ontology;
import ibb.api.ontologyservice.ontology.OntologyFactory;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Path("/ontologies")
public class OntologyService {

    @Inject
    OntologyServiceConfig config;

    @Inject
    OntologyFactory ontologyFactory;

    void init(@Observes StartupEvent ev) {
        if (config.startup().noActions()) {
            return;
        }
        config.ontologies().keySet().stream().forEach(ontologyName -> {
            Ontology ontology = ontologyFactory.get(ontologyName);
            long count = ontology.countTerm();
            if (count == 0) {
                ontology.reindex();
            } else {
                Log.infof("Ontology %s already indexed with %d terms", ontologyName, count);
            }
        });
    }
}
