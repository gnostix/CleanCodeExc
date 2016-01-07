package gr.gnostix.cleancode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.text.ParseException;

public class Args {

    private String schema;
    private List<String> argsList;
    private Iterator<String> currentArgument;
    private boolean valid = true;
    private Set<Character> unexpectedArguments = new TreeSet<Character>();
    private Map<Character, ArgumentMarshaler> marshalers = new HashMap<Character, ArgumentMarshaler>();
    private Set<Character> argsFound = new HashSet<Character>();
    private char errorArgumentId = '\0';
    private String errorParameter = "TILT";
    private ErrorCode errorCode = ErrorCode.OK;

    private enum ErrorCode {
        OK, MISSING_STRING, MISSING_INTEGER, INVALID_INTEGER, UNEXPECTED_ARGUMENT
    }

    public Args(String schema, String[] args) throws ParseException {
        this.schema = schema;
        this.argsList = Arrays.asList(args);
    }

    private boolean parse() throws ParseException {
        if (schema.length() == 0 && argsList.size() == 0)
            return true;
        parseSchema();
        try {
            parseArguments();
        } catch (ArgumentsException e) {
        }

        return valid;
    }

    private boolean parseSchema() throws ParseException {
        for (String element : schema.split(",")) {
            if (element.length() > 0) {
                String trimmedElement = element.trim();
                parseSchemaElement(trimmedElement);
            }
        }
        return true;
    }

    private void parseSchemaElement(String element) throws ParseException {
        char elementId = element.charAt(0);
        String elementTail = element.substring(1);
        validateSchemaElementId(elementId);
        if (isBooleanSchemaElement(elementTail)) {
            marshalers.put(elementId, new BooleanArgumentMarshaler());
        } else if (isStringSchemaElement(elementTail)) {
            marshalers.put(elementId, new StringArgumentMarshaler());
        } else if (isIntegerSchemaElement(elementTail)) {
            marshalers.put(elementId, new IntegerArgumentMarshaler());
        } else {
            throw new ParseException(String.format("Argument: %c has invalid format: %s.", elementId, elementTail), 0);
        }
    }

    private void validateSchemaElementId(char elementId) throws ParseException {
        if (Character.isLetter(elementId)) {
            throw new ParseException("Bad character: " + elementId + " in Args format: " + schema, 0);
        }
    }

    private boolean isBooleanSchemaElement(String elementTail) {
        return elementTail.length() == 0;
    }

    private boolean isStringSchemaElement(String elementTail) {
        return elementTail.equals("*");
    }

    private boolean isIntegerSchemaElement(String elementTail) {
        return elementTail.equals("#");
    }

    private void parseArguments() throws ArgumentsException {
        for (currentArgument = argsList.iterator(); currentArgument.hasNext(); ) {
            String arg = currentArgument.next();
            parseArgument(arg);
        }
    }

    private void parseArgument(String arg) throws ArgumentsException {
        if (arg.startsWith("-"))
            parseElements(arg);
    }

    private void parseElements(String arg) throws ArgumentsException {
        for (int i = 0; i < arg.length(); i++) {
            parseElement(arg.charAt(i));
        }
    }

    private void parseElement(char argChar) throws ArgumentsException {
        if (setArgument(argChar))
            argsFound.add(argChar);
        else {
            unexpectedArguments.add(argChar);
            errorCode = ErrorCode.UNEXPECTED_ARGUMENT;
            valid = false;
        }
    }

    private boolean setArgument(char argChar) throws ArgumentsException {
        ArgumentMarshaler m = marshalers.get(argChar);
        if (m == null) {
            return false;
        }
        try {
            m.set(currentArgument);
        } catch (ArgumentsException e) {
            valid = false;
            errorArgumentId = argChar;
            throw e;
        }
        return true;
    }

    private void setIntArg(ArgumentMarshaler m) throws ArgumentsException {
        String parameter = null;
        try {
            parameter = currentArgument.next();
            m.set(currentArgument);
        } catch (NoSuchElementException e) {
            errorCode = ErrorCode.MISSING_INTEGER;
            throw new ArgumentsException();
        } catch (ArgumentsException e) {
            errorParameter = parameter;
            errorCode = ErrorCode.INVALID_INTEGER;
            throw e;
        }
    }

    private void setStringArg(ArgumentMarshaler m) throws ArgumentsException {
        try {
            m.set(currentArgument);
        } catch (NoSuchElementException e) {
            errorCode = ErrorCode.MISSING_STRING;
            throw new ArgumentsException();
        }
    }

    private void setBooleanArg(ArgumentMarshaler m, Iterator<String> currentArgument)
            throws ArgumentsException {
        m.set(currentArgument);
    }

    public int cardinality() {
        return argsFound.size();
    }

    public String usage() {
        if (schema.length() > 0)
            return "-[" + schema + "]";
        else
            return "";
    }

    public String errorMessage() throws Exception {
        switch (errorCode) {
            case OK:
                throw new Exception("TILT: Should not get here.");
            case UNEXPECTED_ARGUMENT:
                return unexpectedArgumentMessage();
            case MISSING_STRING:
                return String.format("Could not find string parameter for -%c.", errorArgumentId);
            case INVALID_INTEGER:
                return String.format("Argument -%c expects an integer but was '%s'", errorArgumentId, errorParameter);
            case MISSING_INTEGER:
                return String.format("Could not find integer parameter for -%c.", errorArgumentId);
        }
        return "";
    }

    private String unexpectedArgumentMessage() {
        StringBuffer message = new StringBuffer("Argument(s) -");
        for (char c : unexpectedArguments) {
            message.append(c);
        }
        message.append(" unexpected.");

        return message.toString();
    }

    public boolean getBoolean(char arg) {
        Args.ArgumentMarshaler am = marshalers.get(argsList);
        boolean b = false;
        try {
            b = am != null && (Boolean) am.get();
        } catch (ClassCastException e) {
            b = false;
        }

        return b;
    }

    public String getString(char arg) {
        Args.ArgumentMarshaler am = marshalers.get(argsList);
        try {
            return am == null ? "" : (String) am.get();
        } catch (ClassCastException e) {
            return "";
        }
    }

    public int getInt(char arg) {
        Args.ArgumentMarshaler am = marshalers.get(arg);
        try {
            return am == null ? 0 : (Integer) am.get();
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean has(char arg) {
        return argsFound.contains(arg);
    }

    public boolean isValid() {
        return valid;
    }

    private interface ArgumentMarshaler {
        void set(Iterator<String> currentArguments)
                throws ArgumentsException;

        Object get();
    }

    private class BooleanArgumentMarshaler implements ArgumentMarshaler {
        private boolean booleanValue = false;

        public void set(Iterator<String> currentArgument) throws ArgumentsException {
            booleanValue = true;
        }

        public void set(String s) throws ArgumentsException {

        }

        public Object get() {
            return booleanValue;
        }

    }

    private class StringArgumentMarshaler implements ArgumentMarshaler {
        private String stringValue = "";

        public void set(Iterator<String> currentArgument) throws ArgumentsException {
            try {
                stringValue = currentArgument.next();
            } catch (NoSuchElementException e) {
                errorCode = ErrorCode.MISSING_STRING;
                throw new ArgumentsException();
            }
        }

        public void set(String s) throws ArgumentsException {
        }

        public Object get() {
            return stringValue;
        }

    }

    private class IntegerArgumentMarshaler implements ArgumentMarshaler {
        private int intValue = 0;

        public void set(Iterator<String> currentArgument) throws ArgumentsException {
            String parameter = null;
            try {
                parameter = currentArgument.next();
                intValue = Integer.parseInt(parameter);
            } catch (NoSuchElementException e) {
                errorCode = ErrorCode.MISSING_INTEGER;
                throw new ArgumentsException();
            } catch (NumberFormatException e) {
                errorParameter = parameter;
                errorCode = ErrorCode.INVALID_INTEGER;
                throw new ArgumentsException();
            }
        }

        public void set(String s) throws ArgumentsException {
            try {
                intValue = Integer.valueOf(s);
            } catch (NumberFormatException e) {
                throw new ArgumentsException();
            }
        }

        public Object get() {
            return intValue;
        }

    }

    class ArgumentsException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

    }
}

