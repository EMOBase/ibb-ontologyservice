package ibb.api.ontologyservice.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Obo format definition
 * https://owlcollab.github.io/oboformat/doc/GO.format.obo-1_4.html
 */
public class OBOStanza {

    public static enum Type {
        TERM, TYPEDEF, INSTANCE, UNKNOWN;

        public static Type fromString(String typeString) {
            switch (typeString.toLowerCase()) {
                case "[term]" :
                    return OBOStanza.Type.TERM;
                case "[typedef]" :
                    return OBOStanza.Type.TYPEDEF;
                case "[instance]" :
                    return OBOStanza.Type.INSTANCE;
                default :
                    return OBOStanza.Type.UNKNOWN;
            }
        }
    }

    private Type type;
    private Map<String, List<String>> tags;

    public OBOStanza(Type type) {
        this.type = type;
        tags = new HashMap<>();
    }

    public void addAttribute(String tag, String value) {
        tags.computeIfAbsent(tag, t -> new ArrayList<>()).add(value);
    }

    public Optional<String> getAttributeOptional(String tag) {
        return getAttributes(tag).stream().findFirst();
    }

    /**
     * Use this function if the tag should only have one value.
     * Otherwise, use {@code getAttributes}
     * @param tag
     * @return
     */
    public String getAttribute(String tag) {
        List<String> attributes = getAttributes(tag);
        if (attributes.isEmpty()) {
            return null;
        } else {
            return attributes.get(0);
        }
    }

    public List<String> getAttributes(String tag) {
        return tags.getOrDefault(tag, List.of());
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return getAttribute("id");
    }

    @Override
    public String toString() {
        return "[" + type + "] " + getId();
    }
}
