package ibb.api.ontologyservice.ontology;

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

    public Ontology get(String ontologyName) {
        OntologyConfig ontologyConfig = config.ontologies().get(ontologyName);
        if (ontologyConfig == null) {
            return null;
        }
        return new OntologyElasticSearchImpl(esClient, ontologyName, ontologyConfig);
    }
}
