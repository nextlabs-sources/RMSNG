package com.nextlabs.common.cli;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class DefaultConverters {

    static final class BooleanConverter implements Converter<Boolean> {

        public static final BooleanConverter INSTANCE = new BooleanConverter();

        @Override
        public Boolean convert(String value) {
            return Boolean.valueOf(value);
        }
    }

    static final class FileConverter implements Converter<File> {

        public static final FileConverter INSTANCE = new FileConverter();

        @Override
        public File convert(String value) {
            return new File(value);
        }
    }

    static final class IntegerConverter implements Converter<Integer> {

        public static final IntegerConverter INSTANCE = new IntegerConverter();

        @Override
        public Integer convert(String value) {
            try {
                return Integer.valueOf(value);
            } catch (Exception e) {
                throw new ConversionException("Unable to convert value: " + value, e);
            }
        }
    }

    static final class LongConverter implements Converter<Long> {

        public static final LongConverter INSTANCE = new LongConverter();

        @Override
        public Long convert(String value) {
            try {
                return Long.valueOf(value);
            } catch (Exception e) {
                throw new ConversionException("Unable to convert value: " + value, e);
            }
        }
    }

    static final class PathConverter implements Converter<Path> {

        public static final PathConverter INSTANCE = new PathConverter();

        @Override
        public Path convert(String value) {
            try {
                return Paths.get(value);
            } catch (Exception e) {
                throw new ConversionException("Unable to convert value: " + value, e);
            }
        }
    }

    static final class StringConverter implements Converter<String> {

        public static final StringConverter INSTANCE = new StringConverter();

        @Override
        public String convert(String value) {
            return value;
        }
    }

    private DefaultConverters() {
    }
}
