package ibb.api.ontologyservice;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class OntologyEndpointsTest {

    @ParameterizedTest
    @CsvSource({"TrOn,965", "GO,47658"})
    public void countShouldWork(String ontologyName, Long expectedTermCount) {
        RestAssured.given()
            .when().get("/ontologies/" + ontologyName + "/count")
            .then().statusCode(200)
            .body("count", equalTo(expectedTermCount.intValue()));
    }

    @Test
    public void searchShouldWork() {
        RestAssured.given()
            .when().get("/ontologies/GO/search?query=head se")
            .then().statusCode(200)
            .body("data.name", hasItems("head segmentation", "anterior head segmentation", "posterior head segmentation"))
            .body("total", equalTo(3));
    }

    @Test
    public void getShouldWorkWhenTermFound() {
        RestAssured.given()
            .when().get("/ontologies/GO/terms/GO:0035287")
            .then().statusCode(200)
            .body("name", everyItem(equalTo("head segmentation")));
    }

    @Test
    public void getShouldWorkWhenTermNotFound() {
        RestAssured.given()
            .when().get("/ontologies/GO/terms/GO:0000000")
            .then().statusCode(200)
            .body("size()", equalTo(0));
    }
}
