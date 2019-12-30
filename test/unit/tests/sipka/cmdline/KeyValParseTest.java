package tests.sipka.cmdline;

import java.util.Map;

import sipka.cmdline.runtime.ParseUtil;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class KeyValParseTest extends SakerTestCase {
	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		assertEquals(parse("a=b"), arrayOf("a", "b"));
		assertEquals(parse("a="), arrayOf("a", ""));
		assertEquals(parse("=b"), arrayOf("", "b"));
		assertEquals(parse("="), arrayOf("", ""));
		assertEquals(parse("a"), arrayOf("a", null));
		assertEquals(parse(""), arrayOf("", null));
		
		assertEquals(parse("\"a\"=b"), arrayOf("\"a\"", "b"));
		assertEquals(parse("\"a\"=\"b\""), arrayOf("\"a\"", "\"b\""));
		assertEquals(parse("a=\"b\""), arrayOf("a", "\"b\""));

		assertEquals(parse("\\="), arrayOf("=", null));
		assertEquals(parse("=="), arrayOf("", "="));
		assertEquals(parse("\\=="), arrayOf("=", ""));
		assertEquals(parse("\\=\\="), arrayOf("==", null));
		assertEquals(parse("\\=\\==="), arrayOf("==", "="));
		assertEquals(parse("\\=\\===\\="), arrayOf("==", "=\\="));

		assertException(NullPointerException.class, () -> ParseUtil.parseEqualsFormatArgument(2, null, new String[2]));
		assertException(NullPointerException.class, () -> ParseUtil.parseEqualsFormatArgument(2, "-Kx", null));
		assertException(IllegalArgumentException.class,
				() -> ParseUtil.parseEqualsFormatArgument(3, "-K", new String[2]));
	}

	private static String[] parse(String input) {
		System.out.println("KeyValParseTest.parse() " + input);
		String[] result = { "UNASSIGNED", "UNASSIGNED" };
		ParseUtil.parseEqualsFormatArgument(2, "-K" + input, result);

		String reprinted = ParseUtil.toKeyValueArgument("-K", result[0], result[1]);
		System.out.println("KeyValParseTest.parse()    re -> " + reprinted);
		String[] result2 = { "UNASSIGNED2", "UNASSIGNED2" };
		ParseUtil.parseEqualsFormatArgument(2, reprinted, result2);
		assertEquals(result, result2);

		return result;
	}

}
