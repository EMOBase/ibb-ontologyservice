package ibb.api.ontologyservice.ontology;

import java.util.List;

public interface Ontology {
    public static class Suggestion {
        /**
         * The term that can be used as query for {@code search}
         */
        public String term;

        /**
         * The number of hits that would be returned by {@code search}
         * if {@code term} was used as query
         */
        public long hits;
    }

    public static class QueryResult<T> {

        /**
         * The total number of hits for the query.
         * This must larger than or equal to the number of items in {@code data}
         */
        public long total;
        public List<T> data;
    }

    void reindex();
    long countTerm();

    /**
     * @param query
     * @param from
     * @return a list of suggestions that can be used as query for {@code search}
     */
    QueryResult<Suggestion> suggest(String query, long from, int size);
    QueryResult<OntologyTerm> search(String query, long from, int size);
}
