package ibb.api.ontologyservice.ontology;

import java.io.IOException;
import java.util.List;

public interface Ontology {
    public static final int MAX_SEARCH_RESULTS = 10000;

    public static class QueryResult<T> {

        /**
         * The total number of hits for the query.
         * This must larger than or equal to the number of items in {@code data}
         */
        public long total;
        public List<T> data;
    }

    void load() throws IOException;
    long count() throws IOException;
    List<Node> getByIds(List<String> ids) throws IOException;
    List<Node> getRoots() throws IOException;
    List<Edge> getChildren(String id) throws IOException;
    QueryResult<Node> search(String query, List<String> aspects, int from, int size) throws IOException;
}
