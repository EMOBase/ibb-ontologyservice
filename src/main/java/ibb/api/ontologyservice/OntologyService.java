package ibb.api.ontologyservice;

import java.util.Map;

import org.jboss.resteasy.reactive.RestPath;

import ibb.api.ontologyservice.ontology.Ontology;
import ibb.api.ontologyservice.ontology.OntologyFactory;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/ontologies")
public class OntologyService {

    @Inject
    OntologyFactory ontologyFactory;

    @GET
    @Path("/{ontologyName}")
    public Map<String, Object> getOntologyOverview(@RestPath String ontologyName) {
        Ontology ontology = ontologyFactory.get(ontologyName);
        long termCount = ontology.countTerm();
        return Map.of("name", ontologyName, "termCount", termCount);
    }
}
