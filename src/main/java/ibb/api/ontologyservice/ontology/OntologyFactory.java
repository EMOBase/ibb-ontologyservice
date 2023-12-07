package ibb.api.ontologyservice.ontology;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import ibb.api.ontologyservice.OntologyServiceConfig;
import ibb.api.ontologyservice.ontology.impl.elasticsearch.OntologyElasticSearchImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OntologyFactory {

    @Inject
    ElasticsearchClient esClient;

    @Inject
    OntologyServiceConfig config;

    @ConfigProperty(name = "ontology-elasticsearch-impl.sync-after-reindex")
    boolean syncAfterReindex;

    public Ontology get(String ontologyName) {
        OntologyConfig ontologyConfig = config.ontologies().get(ontologyName);
        if (ontologyConfig == null) {
            return null;
        }
        return new OntologyElasticSearchImpl(esClient, ontologyName, ontologyConfig)
            .syncAfterReindex(syncAfterReindex);
    }

    public Optional<Ontology> getOptional(String ontologyName) {
        return Optional.ofNullable(get(ontologyName));
    }
}
