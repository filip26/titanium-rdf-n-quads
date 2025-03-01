package com.apicatalog.rdf.api;

public class RdfConsumerException extends Exception {

    private static final long serialVersionUID = -5794012244060873543L;

    String subject;
    String predicate;
    String object;
    String datatype;
    String lanTag;
    String direction;
    String graph;

    public RdfConsumerException(String message) {
        super(message);
    }

    public RdfConsumerException(Throwable e) {
        super(e);
    }

    public RdfConsumerException(String message, Throwable e) {
        super(message, e);
    }

    public RdfConsumerException(String subject, String predicate, String object, String graph, Exception e) {
        super(e);
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.graph = graph;
    }

    public RdfConsumerException(String subject, String predicate, String object, String datatype, String graph, Exception e) {
        super(e);
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.datatype = datatype;
        this.graph = graph;
    }

    public RdfConsumerException(String subject, String predicate, String object, String langTag, String direction, String graph, Exception e) {
        super(e);
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.lanTag = langTag;
        this.direction = direction;
        this.graph = graph;
    }

    public String getGraph() {
        return graph;
    }

    public String getObject() {
        return object;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getSubject() {
        return subject;
    }

    public String getDirection() {
        return direction;
    }

    public String getLanTag() {
        return lanTag;
    }

    public String getDatatype() {
        return datatype;
    }
}
