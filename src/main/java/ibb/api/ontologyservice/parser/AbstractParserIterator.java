package ibb.api.ontologyservice.parser;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

class AbstractParserIterator<T> implements Iterator<T> {
    private T current;
    private AbstractParser<T> parser;

    public AbstractParserIterator(AbstractParser<T> parser) {
        this.parser = parser;
    }

    @Override
    public boolean hasNext() {
        if (parser.isClosed()) {
            return false;
        }
        if (current == null) {
            current = getNextItem();
        }
        return current != null;
    }

    @Override
    public T next() {
        if (parser.isClosed()) {
            throw new NoSuchElementException("Parser has been closed");
        }
        T next = current;
        current = null;

        if (next == null) {
            next = getNextItem();
            if (next == null) {
                throw new NoSuchElementException("No more items available");
            }
        }
        return next;
    }

    private T getNextItem() {
        try {
            return parser.getNextItem();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }
}

