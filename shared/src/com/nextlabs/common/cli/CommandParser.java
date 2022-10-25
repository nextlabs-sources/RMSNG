package com.nextlabs.common.cli;

import com.nextlabs.common.util.ClassUtils;
import com.nextlabs.common.util.StringUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;

public class CommandParser<T> {

    private Class<T> clazz;
    private final Map<Class<?>, Converter<?>> converters = new HashMap<Class<?>, Converter<?>>();
    private List<AccessibleOption> accessibleOptions = new ArrayList<AccessibleOption>();

    public static boolean hasHelp(String[] args) {
        if (args != null) {
            for (String s : args) {
                if (StringUtils.equalsIgnoreCase(s, "-h") || StringUtils.equalsIgnoreCase(s, "--help")) {
                    return true;
                }
            }
        }
        return false;
    }

    public CommandParser(Class<T> clazz) {
        boolean present = ClassUtils.isAnnotationPresentLocally(clazz, CLI.class);
        if (!present) {
            throw new IllegalArgumentException("No " + CLI.class.getName() + " annotation is set");
        }
        this.clazz = clazz;
        registerConverters();
        fetchParameters();
    }

    public void addConverter(Class<?> clazz, Converter<?> converter) {
        this.converters.put(clazz, converter);
    }

    private void checkIfPresent(Parameter parameter) {
        for (AccessibleOption accessibleOption : this.accessibleOptions) {
            Option option = accessibleOption.getOption();
            String opt = option.getOpt();
            String longOpt = option.getLongOpt();
            if (StringUtils.equalsIgnoreCase(opt, parameter.option())) {
                throw new IllegalArgumentException("Duplicate option: " + opt);
            } else if (StringUtils.equalsIgnoreCase(longOpt, parameter.longOption())) {
                throw new IllegalArgumentException("Duplicate long name option: " + longOpt);
            }
        }
    }

    private Option convertTo(Parameter parameter, Field field) {
        String optionName = StringUtils.hasText(parameter.option()) ? parameter.option() : null;
        String longOption = StringUtils.hasText(parameter.longOption()) ? parameter.longOption() : field.getName();
        boolean hasArgs = parameter.hasArgs();
        boolean mandatory = parameter.mandatory();
        String description = parameter.description();
        Option option = new Option(optionName, longOption, hasArgs, description);
        option.setRequired(mandatory);
        return option;
    }

    private void copyProperty(AccessibleOption accessibleOption, CommandLine commandLine, T result)
            throws ParseException {
        Field field = accessibleOption.getField();
        Option option = accessibleOption.getOption();
        Parameter parameter = accessibleOption.getParameter();
        String argumentValue = null;
        if (option.hasArg()) {
            String defaultValue = parameter.defaultValue();
            argumentValue = commandLine.getOptionValue(option.getLongOpt(), defaultValue);
        } else {
            argumentValue = String.valueOf(commandLine.hasOption(option.getLongOpt()));
        }
        if (argumentValue == null) {
            return;
        }
        Class<?> dataTypeClass = field.getType();
        Converter<?> converter = converters.get(dataTypeClass);
        if (converter != null) {
            Object value = converter.convert(argumentValue);
            try {
                ClassUtils.setField(field, result, value);
            } catch (IllegalArgumentException e) {
                ParseException ex = new ParseException("Unable to populate value for '" + field.getName() + "': " + e.getMessage());
                ex.initCause(e);
                throw ex;
            } catch (IllegalAccessException e) {
                ParseException ex = new ParseException("Unable to populate value for '" + field.getName() + "': " + e.getMessage());
                ex.initCause(e);
                throw ex;
            }
        } else {
            throw new ParseException("Unable to find proper converter for field '" + field.getName() + "' with type: " + dataTypeClass.getName());
        }
    }

    private void fetchParameters() {
        Field[] fields = ClassUtils.getDeclaredFields(clazz);
        for (Field field : fields) {
            boolean present = field.isAnnotationPresent(Parameter.class);
            if (present) {
                ClassUtils.makeAccessible(field);
                Parameter parameter = ClassUtils.getAnnotation(field, Parameter.class);
                checkIfPresent(parameter);
                Option option = convertTo(parameter, field);
                AccessibleOption accessibleOption = new AccessibleOption(field, parameter, option);
                this.accessibleOptions.add(accessibleOption);
            }
        }
    }

    private Options getOptions() {
        Options options = new Options();
        for (AccessibleOption accessibleOption : this.accessibleOptions) {
            Option option = accessibleOption.getOption();
            options.addOption(option);
        }
        return options;
    }

    private T newInstance(CommandLine commandLine) throws ParseException {
        T instance = null;
        try {
            instance = clazz.newInstance();
        } catch (InstantiationException e) {
            ParseException ex = new ParseException("Unable to instantiate: " + clazz.getName());
            ex.initCause(e);
            throw ex;
        } catch (IllegalAccessException e) {
            ParseException ex = new ParseException("Unable to instantiate: " + clazz.getName());
            ex.initCause(e);
            throw ex;
        }
        for (AccessibleOption accessibleOption : this.accessibleOptions) {
            copyProperty(accessibleOption, commandLine, instance);
        }
        return instance;
    }

    public T parse(String[] args) throws ParseException {
        Options options = getOptions();
        Parser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        return newInstance(commandLine);
    }

    public void printHelp(String cmd) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(cmd, getOptions());
    }

    private void registerConverters() {
        converters.put(int.class, DefaultConverters.IntegerConverter.INSTANCE);
        converters.put(long.class, DefaultConverters.LongConverter.INSTANCE);
        converters.put(boolean.class, DefaultConverters.BooleanConverter.INSTANCE);

        converters.put(Integer.class, DefaultConverters.IntegerConverter.INSTANCE);
        converters.put(Long.class, DefaultConverters.LongConverter.INSTANCE);
        converters.put(Boolean.class, DefaultConverters.BooleanConverter.INSTANCE);
        converters.put(String.class, DefaultConverters.StringConverter.INSTANCE);

        converters.put(File.class, DefaultConverters.FileConverter.INSTANCE);
        converters.put(Path.class, DefaultConverters.PathConverter.INSTANCE);
    }

    class DefaultParser extends PosixParser {

        @SuppressWarnings("rawtypes")
        @Override
        protected void processOption(String arg, ListIterator iter) throws ParseException {
            boolean hasOption = super.getOptions().hasOption(arg);
            if (hasOption) {
                super.processOption(arg, iter);
            }
        }
    }

    class AccessibleOption {

        private final Field field;
        private final Parameter parameter;
        private final Option option;

        public AccessibleOption(Field field, Parameter parameter, Option option) {
            this.field = field;
            this.parameter = parameter;
            this.option = option;
        }

        public Field getField() {
            return field;
        }

        public Option getOption() {
            return option;
        }

        public Parameter getParameter() {
            return parameter;
        }
    }
}
