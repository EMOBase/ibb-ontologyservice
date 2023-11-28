package ibb.api.ontologyservice.ontology.impl.elasticsearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import ibb.api.ontologyservice.ontology.Ontology;
import ibb.api.ontologyservice.ontology.OntologyConfig;
import ibb.api.ontologyservice.ontology.OntologyTerm;
import ibb.api.ontologyservice.parser.OBOParser;
import io.quarkus.logging.Log;

public class OntologyElasticSearchImpl implements Ontology {

    private String name;
    private OntologyConfig config;
    private ElasticsearchClient esClient;
    private String index;
    private String tempIndex;

    public OntologyElasticSearchImpl(ElasticsearchClient esClient, String ontologyName, OntologyConfig config) {
        this.esClient = esClient;
        this.name = ontologyName;
        this.config = config;

        this.index = "ontologies-" + name.toLowerCase().replace(' ', '-');
        this.tempIndex = index + "-temp";
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public long countTerm() {
        try {
            return esClient.count(r -> r.index(index)).count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reindex() {
        Log.infof("Indexing ontology %s", name);
        try {
            deleteIndex(tempIndex);
            bulkInsert(tempIndex);
            moveIndex(tempIndex, index);
        } catch (Exception e) {
            Log.errorf(e, "Failed to index ontology %s", name);
            throw new RuntimeException(e);
        } finally {
            try {
                deleteIndex(tempIndex);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void bulkInsert(String index) throws IOException {
        BulkIngester<String> ingester = BulkIngester.of(b -> b
            .client(esClient)
            .maxOperations(5000)
            .flushInterval(1, TimeUnit.SECONDS)
            .listener(new BulkIndexListener())
        );

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(config.localFile());
        try (OBOParser parser = new OBOParser(inputStream)) {
            parser.stream().map(OntologyTerm::fromOBOStanza).forEach(term -> {
                ingester.add(op -> op
                    .index(idx -> idx
                        .index(index)
                        .document(term)
                    ),
                    term.id
                );
            });
        } 
        ingester.close();
    }

    private void moveIndex(String sourceIndex, String targetIndex) throws IOException {
        deleteIndex(targetIndex);
        boolean exists = esClient.indices().exists(r -> r.index(sourceIndex)).value();
        if (exists) {
            esClient.reindex(r -> r
                .source(s -> s.index(sourceIndex))
                .dest(d -> d.index(targetIndex))
            );
            deleteIndex(sourceIndex);
        }
    }

    private void deleteIndex(String index) throws IOException {
        esClient.indices().delete(r -> r.index(index).ignoreUnavailable(true));
    }
}
