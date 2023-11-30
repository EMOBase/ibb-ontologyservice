package ibb.api.ontologyservice.parser;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Code inspired by apache commons CSVParser
 * https://github.com/apache/commons-csv/blob/master/src/main/java/org/apache/commons/csv/CSVParser.java
 */
public abstract class AbstractParser<T> implements Closeable, Iterable<T> {

    protected BufferedReader bufferedReader;
    private AbstractParserIterator<T> iterator;
    private boolean closed;

    public AbstractParser(InputStream inputStream) {
        this.bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream));
        this.iterator = new AbstractParserIterator<>(this);
        this.closed = false;
    }

    @Override
    public Iterator<T> iterator() {
        return iterator;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            bufferedReader.close();
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                iterator(), Spliterator.ORDERED), false);
    }

    protected abstract T getNextItem() throws IOException;
}
