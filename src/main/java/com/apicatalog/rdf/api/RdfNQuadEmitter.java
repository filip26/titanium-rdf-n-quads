package com.apicatalog.rdf.api;

public class RdfNQuadEmitter implements RdfQuadConsumer {

    static final String I18N_BASE = "https://www.w3.org/ns/i18n#";
    static final String LANG_STRING = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString";
    
    protected final RdfNQuadConsumer consumer;

    protected RdfNQuadEmitter(RdfNQuadConsumer consumer) {
        this.consumer = consumer;
    }

    public static RdfQuadConsumer newInstance(RdfNQuadConsumer consumer) {
        return new RdfNQuadEmitter(consumer);
    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String object, String graph) {
        consumer.nquad(subject, predicate, object, graph);
        return this;
    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String literal, String datatype, String graph) {
        consumer.nquad(subject, predicate, literal, datatype, null, graph);
        return this;
    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String literal, String language, String direction, String graph) {
        if (direction != null) {
            consumer.nquad(
                    subject,
                    predicate,
                    literal,
                    I18N_BASE
                            .concat(language)
                            .concat("_")
                            .concat(direction),
                    null,
                    graph);
        } else {
            consumer.nquad(subject, predicate, literal, LANG_STRING, language, graph);
        }

        return this;
    }
}
