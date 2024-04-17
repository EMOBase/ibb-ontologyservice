package ibb.api.ontologyservice;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ibb.api.ontologyservice.ontology.Node;
import ibb.api.ontologyservice.ontology.Ontology;
import ibb.api.ontologyservice.ontology.OntologyFactory;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.inject.Inject;

/**
 * Generate a synonym file in Solr format for Tribolium Ontology.
 * This file is used by phenotypeservice.
 */
public class TrOnSynonymFileGenerator {
    private Set<String> skipTerms = Set.of(
        "<new term>",
        "anatomical entity",
        "multicellular structure",
        "anatomical structure",
        "organism subdivision",
        "organism",
        "material anatomical entity",
        "acellular anatomical structure",
        "multi tissue structure",
        "organ system subdivision"
    );

    private Map<String, Set<String>> synonyms = new HashMap<>();

    public void generate(Ontology ontology, Path outFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outFile)) {
            for (var root: ontology.getRoots()) {
                dfs(root, ontology, writer);
            }
        }
    }

    /**
     * Check if a synonym should be generated for the given term.
     * 
     * A synonym should be generated if the term has the "mixed" or "generic" aspect and is not in the skip list.
     */
    private boolean shouldGenerateSynonym(Node term) {
        return !skipTerms.contains(term.name) && !term.subsets.contains("concrete");
    }

    /**
     * Filter child terms that contain all words of the parent term.
     * 
     * For example, if the parent term is "head" and the child terms are "head capsule", "head cuticle" and "antena",
     * only "antena" will be returned since a search for "head" already returns "head capsule" and "head cuticle".
     */
    private Set<String> filterChildrenNames(String parentName, Set<String> childNames) {
        Set<String> parentWords = Set.of(parentName.split(" "));
        return childNames.stream()
            .filter(name -> name.equals(parentName) || !Set.of(name.split(" ")).containsAll(parentWords))
            .collect(Collectors.toSet());
    }

    private Set<String> dfs(Node term, Ontology ontology, BufferedWriter writer) throws IOException {
        if (synonyms.keySet().contains(term.name)) {
            return synonyms.get(term.name);
        }

        Set<String> names = new HashSet<>();
        if (shouldGenerateSynonym(term)) {
            names.add(term.name);
        }
        for (var edge: ontology.getChildren(term.id)) {
            var child = edge.child;
            Set<String> childNames = dfs(child, ontology, writer);
            names.addAll(filterChildrenNames(term.name, childNames));
        };
        synonyms.put(term.name, names);
        if (shouldGenerateSynonym(term)) {
            if (names.size() > 1) {
                writer.write(term.name + " => " + String.join(", ", names));
                writer.newLine();
            }
        }
        return names;
    }

    public static class TrOnSynonymFileGeneratorApp implements QuarkusApplication {
        @Inject
        OntologyFactory ontologyFactory;

        @Override
        public int run(String... args) throws Exception {
            TrOnSynonymFileGenerator generator = new TrOnSynonymFileGenerator();
            Ontology ontology = ontologyFactory.get("TrOn");
            if (ontology == null) {
                throw new RuntimeException("TrOn ontology not found");
            }
            Log.info("Generating synonym file for TrOn ontology");
            generator.generate(ontology, Path.of("tron_synonyms.txt"));
            Log.info("Done");
            return 0;
        }
    }

    public static void main(String[] args) throws IOException {
        Quarkus.run(TrOnSynonymFileGeneratorApp.class, args);
    }
}
