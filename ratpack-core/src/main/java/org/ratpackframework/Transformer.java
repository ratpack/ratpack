package org.ratpackframework;

public interface Transformer<F, T> {

    T transform(F from);

}
