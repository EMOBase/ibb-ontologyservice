package ibb.api.ontologyservice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import ibb.api.ontologyservice.ontology.Ontology;
import ibb.api.ontologyservice.ontology.OntologyFactory;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class OntologyElasticSearchImplTest {
    
    @Inject
    OntologyServiceConfig config;

    @Inject
    OntologyFactory ontologyFactory;

    @Test
    public void shouldIndexGeneOntologySuccessfully() {
        String ontologyName = "GO";
        Ontology ontology = ontologyFactory.get(ontologyName);
        assertNotNull(ontology, "Can not find ontology " + ontologyName);
        ontology.reindex();
        assertThat(ontology.countTerm(), greaterThan(0L));
    }
}
