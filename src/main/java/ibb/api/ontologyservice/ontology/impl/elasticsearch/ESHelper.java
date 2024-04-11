package ibb.api.ontologyservice.ontology.impl.elasticsearch;

import java.util.List;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch.core.SearchResponse;

public class ESHelper {
    public static <T> List<T> extractHits(SearchResponse<T> response) {
        return response.hits().hits().stream().map(hit -> hit.source()).collect(Collectors.toList());
    }
}
