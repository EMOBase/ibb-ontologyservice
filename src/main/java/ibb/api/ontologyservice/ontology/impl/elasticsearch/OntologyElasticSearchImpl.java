package ibb.api.ontologyservice.ontology.impl.elasticsearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import ibb.api.ontologyservice.ontology.Edge;
import ibb.api.ontologyservice.ontology.Node;
import ibb.api.ontologyservice.ontology.Ontology;
import ibb.api.ontologyservice.ontology.OntologyConfig;
import ibb.api.ontologyservice.parser.OBOParser;
import ibb.api.ontologyservice.parser.OBOStanza;
import io.quarkus.logging.Log;

public class OntologyElasticSearchImpl implements Ontology {

    private String name;
    private OntologyConfig config;
    private ElasticsearchClient esClient;
    private String nodeAlias;
    private String edgeAlias;

    private static TypeMapping NODE_INDEX_MAPPPING = TypeMapping.of(m -> m
            .properties("id", id -> id.keyword(opts -> opts))
            .properties("name", name -> name.searchAsYouType(opts -> opts))
            .properties("subsets", subsets -> subsets.keyword(opts -> opts))
            .properties("aspect", aspect -> aspect.keyword(opts -> opts))
            .properties("definition", definition -> definition.text(opts -> opts.index(false)))
            .properties("isRoot", isRoot -> isRoot.boolean_(opts -> opts))
            .properties("obsolete", obsolete -> obsolete.boolean_(opts -> opts)));

    private static TypeMapping EDGE_INDEX_MAPPING = TypeMapping.of(m -> m
            .properties("childId", i -> i.keyword(opts -> opts))
            .properties("parentId", i -> i.keyword(opts -> opts))
            .properties("relation", p -> p.keyword(opts -> opts)));

    private void createIndexWithMapping(String index, TypeMapping mapping) throws IOException {
        esClient.indices().create(r -> r
                .index(index)
                .mappings(mapping));
    }

    public OntologyElasticSearchImpl(
            ElasticsearchClient esClient,
            String ontologyName,
            OntologyConfig config,
            String indexPrefix) {
        this.esClient = esClient;
        this.name = ontologyName;
        this.config = config;

        String prefix = indexPrefix + "-ontologies-" + name.toLowerCase().replace(' ', '_');
        this.nodeAlias = prefix + "-nodes";
        this.edgeAlias = prefix + "-edges";
    }

    @Override
    public long count() throws IOException {
        return esClient.count(r -> r.index(nodeAlias).allowNoIndices(true).ignoreUnavailable(true)).count();
    }

    @Override
    public void load() throws IOException {
        Log.infof("Indexing ontology %s", name);

        long timestamp = System.currentTimeMillis();
        String newNodeIndex = nodeAlias + "-" + timestamp;
        String newEdgeIndex = edgeAlias + "-" + timestamp;

        try {
            createIndexWithMapping(newNodeIndex, NODE_INDEX_MAPPPING);
            createIndexWithMapping(newEdgeIndex, EDGE_INDEX_MAPPING);

            bulkInsert(newNodeIndex, newEdgeIndex);
            List<String> existingNodeIndices = esClient.indices().getAlias(r -> r.index(nodeAlias).ignoreUnavailable(true).allowNoIndices(true)).result().keySet().stream().toList();
            List<String> existingEdgeIndices = esClient.indices().getAlias(r -> r.index(edgeAlias).ignoreUnavailable(true).allowNoIndices(true)).result().keySet().stream().toList();
            esClient.indices().updateAliases(body -> {
                    body
                        .actions(action -> action.add(a -> a.alias(nodeAlias).index(newNodeIndex)))
                        .actions(action -> action.add(a -> a.alias(edgeAlias).index(newEdgeIndex)));
                    if (!existingNodeIndices.isEmpty()) {
                        body.actions(action -> action.remove(r -> r.indices(existingNodeIndices).alias(nodeAlias)));
                    }
                    if (!existingEdgeIndices.isEmpty()) {
                        body.actions(action -> action.remove(r -> r.indices(existingEdgeIndices).alias(edgeAlias)));
                    }
                    return body;
            });
            esClient.indices().refresh(r -> r
                .index(newNodeIndex, newEdgeIndex)
                .ignoreUnavailable(true)
                .allowNoIndices(true));

            if (!existingNodeIndices.isEmpty() || !existingEdgeIndices.isEmpty()) {
                esClient.indices().delete(r -> r.index(existingNodeIndices).index(existingEdgeIndices));
            }

            long nodeCount = esClient.count(r -> r.index(newNodeIndex).ignoreUnavailable(true)).count();
            long edgeCount = esClient.count(r -> r.index(newEdgeIndex).ignoreUnavailable(true)).count();
            Log.infof("Indexed ontology %s (%d nodes - %d edges) in %d s", name, nodeCount, edgeCount, (System.currentTimeMillis() - timestamp)/1000);
        } catch (Exception e) {
            Log.errorf(e, "Failed to index ontology %s", name);
            esClient.indices().delete(r -> r.index(newNodeIndex, newEdgeIndex).ignoreUnavailable(true));
            throw e;
        }
    }

    @Override
    public List<Edge> getChildren(String id) throws IOException {
        SearchResponse<Edge> edgeResponse = esClient.search(s -> s
                .index(edgeAlias)
                .size(Ontology.MAX_SEARCH_RESULTS)
                .query(q -> q.term(t -> t.field("parentId").value(id)))
                , Edge.class);
        List<Edge> edges = ESHelper.extractHits(edgeResponse);
        Map<String, Node> childrenMap = getByIds(edges.stream().map(e -> e.childId).toList()).stream().collect(Collectors.toMap(n -> n.id, n -> n));
        return edges.stream().map(e -> {
            e.child = childrenMap.get(e.childId);
            return e;
        }).toList();
    }

    @Override
    public QueryResult<Node> search(String query, List<String> subsets, int from, int size) throws IOException {
        SearchResponse<Node> response = esClient.search(s -> s
                .index(nodeAlias)
                .from(from)
                .size(size)
                .query(q -> q.bool(b ->  {
                    b.must(m -> m.matchPhrasePrefix(mm -> mm.field("name").query(query)));
                    if (subsets != null && !subsets.isEmpty()) {
                        List<FieldValue> values = subsets.stream().map(FieldValue::of).toList();
                        b.filter(f -> f.terms(t -> t.field("subsets").terms(tt -> tt.value(values))));
                    }
                    return b;
                })),
                Node.class);

        QueryResult<Node> result = new QueryResult<>();
        result.total = Optional.ofNullable(response.hits().total()).map(t -> t.value()).orElseThrow(
                () -> new RuntimeException("Missing total"));
        result.data = response.hits().hits().stream().map(h -> h.source()).toList();
        return result;
    }

    @Override
    public List<Node> getByIds(List<String> ids) {
        try {
            return esClient.search(s -> s
                    .index(nodeAlias)
                    .size(Ontology.MAX_SEARCH_RESULTS)
                    .query(q -> q.ids(i -> i.values(ids))), Node.class).hits().hits().stream()
                    .map(h -> h.source()).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Node> getRoots() throws IOException {
        SearchResponse<Node> response = esClient.search(s -> s
                .index(nodeAlias)
                .size(Ontology.MAX_SEARCH_RESULTS)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("isRoot").value(true)))
                        .filter(f -> f.term(t -> t.field("obsolete").value(false)))
                )),
                Node.class);
        return ESHelper.extractHits(response);
    }

    private void bulkInsert(String nodeIndex, String edgeIndex) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(config.localFile());
        try (
                var parser = new OBOParser(inputStream);
                var ingester = BulkIngester.<Void>of(b -> b.client(esClient));) {

            AtomicLong nodeCounter = new AtomicLong(0);
            AtomicLong edgeCounter = new AtomicLong(0);

            parser.stream().filter(stanza -> stanza.getType() == OBOStanza.Type.TERM).forEach(stanza -> {
                Node node = Node.fromOBOStanza(stanza);
                nodeCounter.getAndIncrement();
                ingester.add(op -> op
                        .index(idx -> idx
                                .index(nodeIndex)
                                .id(node.id)
                                .document(node)));
                Edge.fromOBOStanza(stanza).forEach(edge -> {
                    edgeCounter.getAndIncrement();
                    ingester.add(op -> op
                            .index(idx -> idx
                                    .index(edgeIndex)
                                    .document(edge)));
                });
            });
            Log.infof("Sent %d nodes and %d edges to Elasticsearch", nodeCounter.get(), edgeCounter.get());
        }
    }
}
