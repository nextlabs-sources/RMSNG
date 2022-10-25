package com.nextlabs.common.cli;

public interface Converter<T> {

    public T convert(String value);
}
