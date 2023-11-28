package ibb.api.ontologyservice.ontology;

public interface Ontology {
    String getName();
    void reindex();
    long countTerm();
    
    default void indexIfNotExists() {
        if (countTerm() == 0) {
            reindex();
        }
    }
}
