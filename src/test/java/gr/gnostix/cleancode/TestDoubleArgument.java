package gr.gnostix.cleancode;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by rebel on 7/1/16.
 */
public class TestDoubleArgument extends TestCase{

	public void testCreateWithNoSchemaOrArguments() throws Exception {
		Args args = new Args("", new String[0]);
		assertEquals(0, args.cardinality());
	}

	public void testWithNoSchemaButWithOneArgument() throws Exception {
		try {
			new Args("", new String[]{"-x"});
			fail();
		} catch (ArgumentsException e) {
			assertEquals(ArgumentsException.ErrorCode.UNEXPECTED_ARGUMENT,
			             e.getErrorCode());
			assertEquals('x', e.getErrorArgumentId());
		}
	}

	public void testWithNoSchemaButWithMultipleArguments() throws Exception {
		try {
			new Args("", new String[]{"-x", "-y"});
			fail();
		} catch (ArgumentsException e) {
			assertEquals(ArgumentsException.ErrorCode.UNEXPECTED_ARGUMENT,
			             e.getErrorCode());
			assertEquals('x', e.getErrorArgumentId());
		}
	}
	public void testNonLetterSchema() throws Exception {
		try {
			new Args("*", new String[]{});
			fail("Args constructor should have thrown exception");
		} catch (ArgumentsException e) {
			assertEquals(ArgumentsException.ErrorCode.INVALID_ARGUMENT_NAME,
			             e.getErrorCode());
			assertEquals('*', e.getErrorArgumentId());
		}
	}
	public void testInvalidArgumentFormat() throws Exception {
		try {
			new Args("f~", new String[]{});
			fail("Args constructor should have throws exception");
		} catch (ArgumentsException e) {
			assertEquals(ArgumentsException.ErrorCode.INVALID_FORMAT, e.getErrorCode());
			assertEquals('f', e.getErrorArgumentId());
		}
	}
	public void testSimpleBooleanPresent() throws Exception {
		Args args = new Args("x", new String[]{"-x"});
		assertEquals(1, args.cardinality());
		assertEquals(true, args.getBoolean('x'));
	}
	public void testSimpleStringPresent() throws Exception {
		Args args = new Args("x*", new String[]{"-x", "param"});
		assertEquals(1, args.cardinality());
		assertTrue(args.has('x'));
		assertEquals("param", args.getString('x'));
	}
	public void testMissingStringArgument() throws Exception {
		try {
			new Args("x*", new String[]{"-x"});
			fail();
		} catch (ArgumentsException e) {
			assertEquals(ArgumentsException.ErrorCode.MISSING_STRING, e.getErrorCode());
			assertEquals('x', e.getErrorArgumentId());
		}
	}
	public void testSpacesInFormat() throws Exception {
		Args args = new Args("x, y", new String[]{"-xy"});
		assertEquals(2, args.cardinality());
		assertTrue(args.has('x'));
		assertTrue(args.has('y'));
	}
	public void testSimpleIntPresent() throws Exception {
		Args args = new Args("x#", new String[]{"-x", "42"});
		assertEquals(1, args.cardinality());
		assertTrue(args.has('x'));
		assertEquals(42, args.getInt('x'));
	}
	public void testInvalidInteger() throws Exception {
		try {
			new Args("x#", new String[]{"-x", "Forty two"});
		fail();
	} catch (ArgumentsException e) {
		assertEquals(ArgumentsException.ErrorCode.INVALID_INTEGER, e.getErrorCode());
		assertEquals('x', e.getErrorArgumentId());
		assertEquals("Forty two", e.getErrorParameter());
	}
}
	public void testMissingInteger() throws Exception {
		try {
			new Args("x#", new String[]{"-x"});
			fail();
		} catch (ArgumentsException e) {
			assertEquals(ArgumentsException.ErrorCode.MISSING_INTEGER, e.getErrorCode());
			assertEquals('x', e.getErrorArgumentId());
		}
	}
	public void testSimpleDoublePresent() throws Exception {
		Args args = new Args("x##", new String[]{"-x", "42.3"});
		assertEquals(1, args.cardinality());
		assertTrue(args.has('x'));
		assertEquals(42.3, args.getDouble('x'), .001);
	}
	public void testInvalidDouble() throws Exception {
		try {
			new Args("x##", new String[]{"-x", "Forty two"});
			fail();
		} catch (ArgumentsException e) {
			assertEquals(ArgumentsException.ErrorCode.INVALID_DOUBLE, e.getErrorCode());
			assertEquals('x', e.getErrorArgumentId());
			assertEquals("Forty two", e.getErrorParameter());
		}
	}
	public void testMissingDouble() throws Exception {
		try {
			new Args("x##", new String[]{"-x"});
			fail();
		} catch (ArgumentsException e) {
			assertEquals(ArgumentsException.ErrorCode.MISSING_DOUBLE, e.getErrorCode());
			assertEquals('x', e.getErrorArgumentId());
		}
	}
}
