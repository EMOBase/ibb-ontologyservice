package ibb.api.ontologyservice.ontology;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import ibb.api.ontologyservice.parser.OBOStanza;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class Edge {
    
    public String childId;
    public String parentId;
    public String relation;

    public Node child;
    public Node parent;

    public Edge() {}
    public Edge(String childId, String relation, String parentId) {
        this.childId = childId;
        this.parentId = parentId;
        this.relation = relation;
    }

    public static List<Edge> fromOBOStanza(OBOStanza stanza) {
        List<Edge> edges = new ArrayList<>();
        stanza.getAttributes("is_a")
            .stream()
            .map(attrValue -> new Edge(stanza.getId(), "is_a", attrValue))
            .forEach(edges::add);
        
        stanza.getAttributes("relationship")
            .stream()
            .map(attrValue -> {
                String predicate = attrValue.substring(0, attrValue.indexOf(' '));
                String object = attrValue.substring(attrValue.indexOf(' ') + 1);
                return new Edge(stanza.getId(), predicate, object);
            })
            .forEach(edges::add);
        return edges;
    }
}
