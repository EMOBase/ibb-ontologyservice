package ibb.api.ontologyservice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import ibb.api.ontologyservice.ontology.Ontology;
import ibb.api.ontologyservice.ontology.Ontology.QueryResult;
import ibb.api.ontologyservice.ontology.OntologyFactory;
import ibb.api.ontologyservice.ontology.OntologyTerm;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Path("/")
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
            long count = ontology.count();
            if (count == 0) {
                ontology.reindex();
            } else {
                Log.infof("Ontology %s already indexed with %d terms", ontologyName, count);
            }
        });
    }

    @GET
    @Path("/ontologies")
    public Map<String, Long> getSummary() {
        return config.ontologies().keySet().stream().collect(Collectors.toMap(
            ontologyName -> ontologyName,
            ontologyName -> ontologyFactory.get(ontologyName).count()
        ));
    }

    @GET
    @Path("/ontologies/{ontologyName}/count")
    public Map<String, Long> count(@RestPath String ontologyName) {
        Ontology ontology = ontologyFactory.getOptional(ontologyName).orElseThrow(
            () -> new NotFoundException("Ontology " + ontologyName + " not found")
        );
        long count = ontology.count();
        return Map.of("count", count);
    }

    @GET
    @Path("/ontologies/{ontologyName}/terms/{ids}")
    public List<OntologyTerm> get(@RestPath String ontologyName, @RestPath("ids") String commaSeparatedIds) {
        Ontology ontology = ontologyFactory.getOptional(ontologyName).orElseThrow(
            () -> new NotFoundException("Ontology " + ontologyName + " not found")
        );
        List<String> ids = List.of(commaSeparatedIds.split(","));
        if (ids.isEmpty()) {
            return List.of();
        }
        return ontology.get(ids);
    }


    @GET
    @Path("/ontologies/{ontologyName}/search")
    public QueryResult<OntologyTerm> search(
        @RestPath String ontologyName,
        @RestQuery String query,
        @RestQuery String aspect,
        @RestQuery @DefaultValue("0") int from,
        @RestQuery @DefaultValue("20") int size
    ) {
        Ontology ontology = ontologyFactory.getOptional(ontologyName).orElseThrow(
            () -> new NotFoundException("Ontology " + ontologyName + " not found")
        );
        return ontology.search(query, aspect, from, size);
    }
}
