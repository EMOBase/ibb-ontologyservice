package ibb.api.ontologyservice.ontology;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import ibb.api.ontologyservice.parser.OBOStanza;
import io.quarkus.runtime.annotations.RegisterForReflection;

// The Elasticsearch client maps documents to this class by reflection, which
// a native executable only allows for registered classes.
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class Node {

    public String id;
    public String name;
    public String aspect;
    public List<String> subsets;
    public String definition;
    public boolean obsolete;
    public boolean isRoot;

    public static Node fromOBOStanza(OBOStanza stanza) {
        Node node = new Node();
        node.id = stanza.getId();
        node.name = stanza.getAttribute("name").replace("_", " ");
        
        node.aspect = stanza.getAttribute("namespace");
        node.subsets = stanza.getAttributes("subset");

        node.definition = stanza.getAttribute("def");
        node.obsolete = stanza.getAttributeOptional("is_obsolete")
            .filter(value -> "true".equals(value))
            .isPresent();

        node.isRoot = stanza.getAttributes("is_a").isEmpty() && stanza.getAttributes("relationship").isEmpty();
        return node;
    }
}
