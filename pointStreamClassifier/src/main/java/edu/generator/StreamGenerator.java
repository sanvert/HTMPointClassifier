package edu.generator;

public interface StreamGenerator<T> {
    T generate();
    String generateString();
}
