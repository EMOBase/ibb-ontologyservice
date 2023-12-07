package ibb.api.ontologyservice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ibb.api.ontologyservice.ontology.Ontology;
import ibb.api.ontologyservice.ontology.OntologyFactory;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class OntologyTest {

    @Inject
    OntologyFactory ontologyFactory;

    @ParameterizedTest
    @CsvSource({"TrOn,968", "GO,47664"})
    public void shouldIndexOnStartup(String ontologyName, Long expectedTermCount) {
        Ontology ontology = ontologyFactory.get(ontologyName);
        assertThat(ontology.count(), equalTo(expectedTermCount));
    }
}
