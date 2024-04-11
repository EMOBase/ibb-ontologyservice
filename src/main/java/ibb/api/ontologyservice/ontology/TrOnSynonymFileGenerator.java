package ibb.api.ontologyservice.ontology;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        "organism"
    );

    private Set<String> generated = new HashSet<>();

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
        return (term.subsets.contains("mixed") || term.subsets.contains("generic")) && !skipTerms.contains(term.name);
    }

    /**
     * Filter child terms that contain all words of the parent term.
     * 
     * For example, if the parent term is "head" and the child terms are "head capsule", "head cuticle" and "antena",
     * only "antena" will be returned since a search for "head" already returns "head capsule" and "head cuticle".
     */
    private List<String> filterChildrenNames(String parentName, List<String> childNames) {
        Set<String> parentWords = Set.of(parentName.split(" "));
        return childNames.stream()
            .filter(name -> !Set.of(name.split(" ")).containsAll(parentWords))
            .collect(Collectors.toList());
    }

    private List<String> dfs(Node term, Ontology ontology, BufferedWriter writer) throws IOException {
        List<String> names = new ArrayList<>();
        names.add(term.name);
        for (var edge: ontology.getChildren(term.id)) {
            var child = edge.child;
            List<String> childNames = dfs(child, ontology, writer);
            names.addAll(filterChildrenNames(term.name, childNames));
        };
        if (shouldGenerateSynonym(term)) {
            if (names.size() > 1 && !generated.contains(term.id)) {
                generated.add(term.id);
                writer.write(names.get(0) + " => " + String.join(", ", names));
                writer.newLine();
            }
        }
        return names;
    }
}
