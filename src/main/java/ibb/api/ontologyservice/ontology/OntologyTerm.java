package ibb.api.ontologyservice.ontology;

import java.util.ArrayList;
import java.util.List;

import ibb.api.ontologyservice.parser.OBOStanza;

public class OntologyTerm {

    public static class Relation {
        public String predicate;
        public String object;

        public Relation() {}
        public Relation(String predicate, String object) {
            this.predicate = predicate;
            this.object = object;
        }
    }

    public String id;
    public String name;
    public List<String> aspects;
    public String definition;

    public boolean obsolete;

    public List<Relation> relations = new ArrayList<>();

    public static OntologyTerm fromOBOStanza(OBOStanza stanza) {
        OntologyTerm term = new OntologyTerm();
        term.id = stanza.getId();
        term.name = stanza.getAttribute("name");
        
        term.aspects = new ArrayList<>();
        term.aspects.addAll(stanza.getAttributes("namespace"));
        term.aspects.addAll(stanza.getAttributes("subset"));

        term.definition = stanza.getAttribute("def");
        term.obsolete = stanza.getAttributeOptional("is_obsolete")
            .filter(value -> "true".equals(value))
            .isPresent();

        stanza.getAttributes("is_a")
            .stream()
            .map(attrValue -> new Relation("is_a", attrValue))
            .forEach(term.relations::add);

        stanza.getAttributes("relationship")
            .stream()
            .map(attrValue -> {
                String predicate = attrValue.substring(0, attrValue.indexOf(' '));
                String object = attrValue.substring(attrValue.indexOf(' ') + 1);
                return new Relation(predicate, object);
            })
            .forEach(term.relations::add);
        return term;
    }
}
