package ibb.api.ontologyservice.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Obo format definition
 * https://owlcollab.github.io/oboformat/doc/GO.format.obo-1_4.html
 */
public final class OBOParser extends AbstractParser<OBOStanza> {

    private OBOStanza partialStanzaFromPreviousIteration;

    public OBOParser(InputStream inputStream) {
        super(inputStream);
    }

    /**
     * Iterate the lines until a complete stanza is found
     * @return the next complete stanza or null if end of file is already reached
     */
    @Override
    protected OBOStanza getNextItem() throws IOException {
        OBOStanza currentStanza = partialStanzaFromPreviousIteration;
        boolean isCompleteStanza = false;

        while (!isCompleteStanza) {
            Optional<String> lineOptional = Optional.ofNullable(bufferedReader.readLine()).map(String::trim);

            if (lineOptional.isEmpty()) {
                // End of file
                isCompleteStanza = true;
                partialStanzaFromPreviousIteration = null;
            } else {
                String line = lineOptional.get();
                if (shouldIgnoreLine(line)) {
                    continue;
                } else if (currentStanza == null) {
                    // First stanza found
                    if (startOfNewStanza(line)) {
                        currentStanza = new OBOStanza(OBOStanza.Type.fromString(line));
                    }
                } else {
                    if (startOfNewStanza(line)) {
                        isCompleteStanza = true;
                        partialStanzaFromPreviousIteration = new OBOStanza(OBOStanza.Type.fromString(line));
                    } else {
                        addLineAsAttribute(currentStanza, line);
                    }
                }
            }
        }
        return currentStanza;
    }

    private void addLineAsAttribute(OBOStanza stanza, String line) {
        String[] cols = line.split(":", 2);
        if (cols.length < 2) {
            // Line without a colon
            // How to handle this?
            return;
        }
        String tagName = cols[0].trim();
        String tagValueWithComment = cols[1];
        String tagValue = tagValueWithComment.split("!", 2)[0].trim();
        stanza.addAttribute(tagName, tagValue);
    }

    private boolean startOfNewStanza(String line) {
        return line.startsWith("[");
    }

    private boolean shouldIgnoreLine(String line) {
        return line.startsWith("!") || line.isEmpty();
    }
}

