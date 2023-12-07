package ibb.api.ontologyservice.ontology.impl.elasticsearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch.core.SearchResponse;
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
    private boolean syncAfterReindex;

    public OntologyElasticSearchImpl(ElasticsearchClient esClient, String ontologyName, OntologyConfig config) {
        this.esClient = esClient;
        this.name = ontologyName;
        this.config = config;
        this.syncAfterReindex = false;

        this.index = "ontologies-" + name.toLowerCase().replace(' ', '-');
        this.tempIndex = index + "-temp";
    }

    public OntologyElasticSearchImpl syncAfterReindex(boolean syncAfterReindex) {
        this.syncAfterReindex = syncAfterReindex;
        return this;
    }

    @Override
    public long count() {
        try {
            return esClient.count(r -> r.index(index).ignoreUnavailable(true)).count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OntologyTerm get(String id) {
        try {
            return esClient.get(r -> r.index(index).id(id), OntologyTerm.class).source();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reindex() {
        Log.infof("Indexing ontology %s", name);
        // reindexWithoutBackup();
        reindexWithBackup();
    }


    @Override
    public List<OntologyTerm> getChildren(String id, List<String> predicates) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getChildren'");
    }

    @Override
    public QueryResult<OntologyTerm> search(String query, String aspect, int from, int size) {
        try {
            SearchResponse<OntologyTerm> response = esClient.search(s -> s
                .index(index)
                .from(from)
                .size(size)
                .query(q -> q
                    .matchPhrasePrefix(m -> m.field("name").query(query))
                )
            , OntologyTerm.class);
            QueryResult<OntologyTerm> result = new QueryResult<>();

            result.total = Optional.ofNullable(response.hits().total()).map(t -> t.value()).orElseThrow(
                () -> new RuntimeException("Missing total"));
            result.data = response.hits().hits().stream().map(h -> h.source()).toList();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void reindexWithoutBackup() {
        try {
            deleteIndex(index);
            createIndexWithMapping(index);
            bulkInsert(index);
            if (syncAfterReindex) {
                refreshIndex(index);
            }
        } catch (Exception e) {
            Log.errorf(e, "Failed to index ontology %s", name);
            throw new RuntimeException(e);
        }
    }

    /**
     * Index into a temporary index before moving to the final index.
     * This is to avoid loss of data if the bulk insert fails.
     */
    public void reindexWithBackup() {
        try {
            deleteIndex(tempIndex);
            bulkInsert(tempIndex);
            refreshIndex(tempIndex);
            deleteIndex(index);
            createIndexWithMapping(index);
            copyIndex(tempIndex, index);
            if (syncAfterReindex) {
                refreshIndex(index);
            }
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

    private void createIndexWithMapping(String index) throws IOException {
        esClient.indices().create(r -> r.index(index)
            .mappings(m -> m
                .properties("id", id -> id.keyword(opts -> opts))
                .properties("name", name -> name.searchAsYouType(opts -> opts))
                .properties("aspect", aspect -> aspect.keyword(opts -> opts))
                .properties("definition", definition -> definition.text(opts -> opts.index(false)))
                .properties("obsolete", obsolete -> obsolete.boolean_(opts -> opts))
                .properties("relations", relations -> relations.object(o -> o
                    .properties("predicate", predicate -> predicate.keyword(opts -> opts))
                    .properties("object", object -> object.keyword(opts -> opts))
                ))
            )
        );
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
            AtomicLong counter = new AtomicLong(0);
            parser.stream().map(OntologyTerm::fromOBOStanza).forEach(term -> {
                counter.getAndIncrement();
                ingester.add(op -> op
                    .index(idx -> idx
                        .index(index)
                        .id(term.id)
                        .document(term)
                    ),
                    term.id
                );
            });
            Log.infof("Sent %d terms to Elasticsearch", counter.get());
        } 
        ingester.close();
    }

    private void copyIndex(String sourceIndex, String targetIndex) throws IOException {
        esClient.reindex(r -> r
            .source(s -> s.index(sourceIndex))
            .dest(d -> d.index(targetIndex))
        );
    }

    private void deleteIndex(String index) throws IOException {
        esClient.indices().delete(r -> r.index(index).ignoreUnavailable(true));
    }

    private void refreshIndex(String index) throws IOException {
        esClient.indices().refresh(r -> r.index(index).ignoreUnavailable(true));
    }
}
