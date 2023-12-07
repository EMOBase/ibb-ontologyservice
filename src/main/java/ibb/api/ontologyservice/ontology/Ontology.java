package ibb.api.ontologyservice.ontology;

import java.util.List;
import java.util.Optional;

public interface Ontology {

    public static class QueryResult<T> {

        /**
         * The total number of hits for the query.
         * This must larger than or equal to the number of items in {@code data}
         */
        public long total;
        public List<T> data;
    }

    void reindex();
    long count();
    OntologyTerm get(String id);
    List<OntologyTerm> getChildren(String id, List<String> predicates);
    QueryResult<OntologyTerm> search(String query, String aspect, int from, int size);

    default Optional<OntologyTerm> getOptional(String id) {
        return Optional.ofNullable(get(id));
    }
}
