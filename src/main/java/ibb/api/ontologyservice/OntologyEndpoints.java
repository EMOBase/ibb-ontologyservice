package ibb.api.ontologyservice;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import ibb.api.ontologyservice.helper.LambdaExceptionHelper;
import ibb.api.ontologyservice.ontology.Edge;
import ibb.api.ontologyservice.ontology.Node;
import ibb.api.ontologyservice.ontology.Ontology;
import ibb.api.ontologyservice.ontology.Ontology.QueryResult;
import ibb.api.ontologyservice.ontology.OntologyFactory;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/")
public class OntologyEndpoints {

    @Inject
    OntologyServiceConfig config;

    @Inject
    OntologyFactory ontologyFactory;

    @GET
    @Path("/ontologies")
    public Map<String, Long> getSummary() throws IOException {
        return config.ontologies().keySet().stream().collect(Collectors.toMap(
                ontologyName -> ontologyName,
                LambdaExceptionHelper.rethrowFunction(ontologyName -> ontologyFactory.get(ontologyName).count())));
    }

    @GET
    @Path("/ontologies/{ontologyName}/count")
    public Map<String, Long> count(@RestPath String ontologyName) throws IOException {
        Ontology ontology = ontologyFactory.getOptional(ontologyName).orElseThrow(
                () -> new NotFoundException("Ontology " + ontologyName + " not found"));
        long count = ontology.count();
        return Map.of("count", count);
    }

    @POST
    @Path("/ontologies/{ontologyName}/reload")
    public void reload(@RestPath String ontologyName) throws IOException {
        Ontology ontology = ontologyFactory.getOptional(ontologyName).orElseThrow(
            () -> new NotFoundException("Ontology " + ontologyName + " not found"));
        ontology.load();
    }

    @GET
    @Path("/ontologies/{ontologyName}/roots")
    public List<Node> getRoots(@RestPath String ontologyName) throws IOException {
        Ontology ontology = ontologyFactory.getOptional(ontologyName).orElseThrow(
                () -> new NotFoundException("Ontology " + ontologyName + " not found"));
        return ontology.getRoots();
    }

    @GET
    @Path("/ontologies/{ontologyName}/terms/{id}/children")
    public List<Edge> getChildren(
            @RestPath String ontologyName,
            @RestPath String id) throws IOException {
        Ontology ontology = ontologyFactory.getOptional(ontologyName).orElseThrow(
                () -> new NotFoundException("Ontology " + ontologyName + " not found"));
        return ontology.getChildren(id);
    }

    @GET
    @Path("/ontologies/{ontologyName}/terms/{ids}")
    public List<Node> get(
            @RestPath String ontologyName,
            String commaSeparatedIds) throws IOException {

        List<String> ids = List.of(commaSeparatedIds.split(",")).stream()
            .map(String::trim)
            .filter(id -> !id.isEmpty())
            .collect(Collectors.toList());

        Ontology ontology = ontologyFactory.getOptional(ontologyName).orElseThrow(
                () -> new NotFoundException("Ontology " + ontologyName + " not found"));
        return ontology.getByIds(ids);
    }

    @GET
    @Path("/ontologies/{ontologyName}/search")
    public QueryResult<Node> search(
            @RestPath String ontologyName,
            @RestQuery @NotBlank String query,
            @RestQuery List<String> subsets,
            @RestQuery @DefaultValue("0") int from,
            @RestQuery @DefaultValue("20") int size) throws IOException {
        Ontology ontology = ontologyFactory.getOptional(ontologyName).orElseThrow(
                () -> new NotFoundException("Ontology " + ontologyName + " not found"));
        return ontology.search(query, subsets, from, size);
    }
}
