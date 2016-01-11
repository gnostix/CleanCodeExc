package gr.gnostix.cleancode;

import java.text.ParseException;
import java.util.*;

public class Args
{

	private String schema;
	private List<String> argsList;
	private Iterator<String> currentArgument;
	private boolean valid = true;
	private Set<Character> unexpectedArguments = new TreeSet<Character>();
	private Map<Character, ArgumentMarshaler> marshalers = new HashMap<Character, ArgumentMarshaler>();
	private Set<Character> argsFound = new HashSet<Character>();
	private char errorArgumentId = '\0';
	private String errorParameter = "TILT";
	private ArgumentsException.ErrorCode errorCode = ArgumentsException.ErrorCode.OK;

	public Args(String schema, String[] args) throws ArgumentsException
	{
		this.schema = schema;
		this.argsList = Arrays.asList(args);
		valid = parse();
	}

	public int cardinality()
	{
		return argsFound.size();
	}



	public boolean getBoolean(char arg)
	{
		Args.ArgumentMarshaler am = marshalers.get(argsList);
		boolean b = false;
		try {
			b = am != null && (Boolean) am.get();
		}
		catch (ClassCastException e) {
			b = false;
		}

		return b;
	}

	public double getDouble(char arg)
	{
		Args.ArgumentMarshaler am = marshalers.get(arg);
		try {
			return am == null ? 0 : (Double) am.get();
		}
		catch (Exception e) {
			return 0.0;
		}
	}

	public int getInt(char arg)
	{
		Args.ArgumentMarshaler am = marshalers.get(arg);
		try {
			return am == null ? 0 : (Integer) am.get();
		}
		catch (Exception e) {
			return 0;
		}
	}

	public String getString(char arg)
	{
		Args.ArgumentMarshaler am = marshalers.get(argsList);
		try {
			return am == null ? "" : (String) am.get();
		}
		catch (ClassCastException e) {
			return "";
		}
	}

	public boolean has(char arg)
	{
		return argsFound.contains(arg);
	}

	private boolean isBooleanSchemaElement(String elementTail)
	{
		return elementTail.length() == 0;
	}

	private boolean isIntegerSchemaElement(String elementTail)
	{
		return elementTail.equals("#");
	}

	private boolean isStringSchemaElement(String elementTail)
	{
		return elementTail.equals("*");
	}

	public boolean isValid()
	{
		return valid;
	}

	private boolean parse() throws ArgumentsException
	{
		if (schema.length() == 0 && argsList.size() == 0)
			return true;
		parseSchema();
		try {
			parseArguments();
		}
		catch (ArgumentsException e) {
		}

		return valid;
	}

	private void parseArgument(String arg) throws ArgumentsException
	{
		if (arg.startsWith("-"))
			parseElements(arg);
	}

	private void parseArguments() throws ArgumentsException
	{
		for (currentArgument = argsList.iterator(); currentArgument.hasNext(); ) {
			String arg = currentArgument.next();
			parseArgument(arg);
		}
	}

	private void parseElement(char argChar) throws ArgumentsException
	{
		if (setArgument(argChar))
			argsFound.add(argChar);
		else {
			unexpectedArguments.add(argChar);
			errorCode = ArgumentsException.ErrorCode.UNEXPECTED_ARGUMENT;
			valid = false;
		}
	}

	public String errorMessage() throws Exception
	{
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
		case INVALID_DOUBLE:
			return String.format("Argument -%c expects a double but was '%s'.",
			                     errorArgumentId, errorParameter);
		case MISSING_DOUBLE:
			return String.format("Could not find double parameter for -%c.",
			                     errorArgumentId);
		}
		return "";
	}


	private String unexpectedArgumentMessage()
	{
		StringBuffer message = new StringBuffer("Argument(s) -");
		for (char c : unexpectedArguments) {
			message.append(c);
		}
		message.append(" unexpected.");

		return message.toString();
	}

	private void parseElements(String arg) throws ArgumentsException
	{
		for (int i = 0; i < arg.length(); i++) {
			parseElement(arg.charAt(i));
		}
	}

	private boolean parseSchema() throws ArgumentsException
	{
		for (String element : schema.split(",")) {
			if (element.length() > 0) {
				String trimmedElement = element.trim();
				parseSchemaElement(trimmedElement);
			}
		}
		return true;
	}

	private void parseSchemaElement(String element) throws ArgumentsException
	{
		char elementId = element.charAt(0);
		String elementTail = element.substring(1);
		validateSchemaElementId(elementId);
		if (elementTail.length() == 0) {
			marshalers.put(elementId, new BooleanArgumentMarshaler());
		}
		else if (elementTail.equals("*")) {
			marshalers.put(elementId, new StringArgumentMarshaler());
		}
		else if (elementTail.equals("*")) {
			marshalers.put(elementId, new IntegerArgumentMarshaler());
		}
		else if (elementTail.equals("##")) {
			marshalers.put(elementId, new DoubleArgumentMarshaler());
		}
		else {
			throw new ArgumentsException(
				String.format("Argument: %c has invalid format: %s.",
				              elementId,elementTail));
		}
	}

	private boolean setArgument(char argChar) throws ArgumentsException
	{
		ArgumentMarshaler m = marshalers.get(argChar);
		if (m == null) {
			return false;
		}
		try {
			m.set(currentArgument);
		}
		catch (ArgumentsException e) {
			valid = false;
			errorArgumentId = argChar;
			throw e;
		}
		return true;
	}

	private void setBooleanArg(ArgumentMarshaler m, Iterator<String> currentArgument)
	throws ArgumentsException
	{
		m.set(currentArgument);
	}

	private void setIntArg(ArgumentMarshaler m) throws ArgumentsException
	{
		String parameter = null;
		try {
			parameter = currentArgument.next();
			m.set(currentArgument);
		}
		catch (NoSuchElementException e) {
			errorCode = ArgumentsException.ErrorCode.MISSING_INTEGER;
			throw new ArgumentsException();
		}
		catch (ArgumentsException e) {
			errorParameter = parameter;
			errorCode = ArgumentsException.ErrorCode.INVALID_INTEGER;
			throw e;
		}
	}

	private void setStringArg(ArgumentMarshaler m) throws ArgumentsException
	{
		try {
			m.set(currentArgument);
		}
		catch (NoSuchElementException e) {
			errorCode = ArgumentsException.ErrorCode.MISSING_STRING;
			throw new ArgumentsException();
		}
	}



	public String usage()
	{
		if (schema.length() > 0)
			return "-[" + schema + "]";
		else
			return "";
	}

	private void validateSchemaElementId(char elementId) throws ArgumentsException
	{
		if (Character.isLetter(elementId)) {
			throw new ArgumentsException("Bad character:" + elementId + "in Args format: " + schema);
		}
	}


	private interface ArgumentMarshaler
	{
		Object get();

		void set(Iterator<String> currentArguments)
		throws ArgumentsException;
	}

	private class BooleanArgumentMarshaler implements ArgumentMarshaler
	{
		private boolean booleanValue = false;

		public Object get()
		{
			return booleanValue;
		}

		public void set(String s) throws ArgumentsException
		{

		}

		public void set(Iterator<String> currentArgument) throws ArgumentsException
		{
			booleanValue = true;
		}

	}

	private class StringArgumentMarshaler implements ArgumentMarshaler
	{
		private String stringValue = "";

		public Object get()
		{
			return stringValue;
		}

		public void set(String s) throws ArgumentsException
		{
		}

		public void set(Iterator<String> currentArgument) throws ArgumentsException
		{
			try {
				stringValue = currentArgument.next();
			}
			catch (NoSuchElementException e) {
				errorCode = ArgumentsException.ErrorCode.MISSING_STRING;
				throw new ArgumentsException();
			}
		}

	}

	private class IntegerArgumentMarshaler implements ArgumentMarshaler
	{
		private int intValue = 0;

		public Object get()
		{
			return intValue;
		}

		public void set(String s) throws ArgumentsException
		{
			try {
				intValue = Integer.valueOf(s);
			}
			catch (NumberFormatException e) {
				throw new ArgumentsException();
			}
		}

		public void set(Iterator<String> currentArgument) throws ArgumentsException
		{
			String parameter = null;
			try {
				parameter = currentArgument.next();
				intValue = Integer.parseInt(parameter);
			}
			catch (NoSuchElementException e) {
				errorCode = ArgumentsException.ErrorCode.MISSING_INTEGER;
				throw new ArgumentsException();
			}
			catch (NumberFormatException e) {
				errorParameter = parameter;
				errorCode = ArgumentsException.ErrorCode.INVALID_INTEGER;
				throw new ArgumentsException();
			}
		}

	}

	private class DoubleArgumentMarshaler implements ArgumentMarshaler
	{

		private double doubleValue = 0;

		public Object get()
		{
			return doubleValue;
		}

		public void set(Iterator<String> currentArgument) throws ArgumentsException
		{
			String parameter = null;
			try {
				parameter = currentArgument.next();
				doubleValue = Double.parseDouble(parameter);
			}
			catch (NoSuchElementException e) {
				errorCode = ArgumentsException.ErrorCode.MISSING_DOUBLE;
				throw new ArgumentsException();
			}
			catch (NumberFormatException e) {
				errorParameter = parameter;
				errorCode = ArgumentsException.ErrorCode.INVALID_DOUBLE;
				throw new ArgumentsException();
			}
		}
	}


}

