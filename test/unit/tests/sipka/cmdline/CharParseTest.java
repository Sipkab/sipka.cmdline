package tests.sipka.cmdline;

import java.util.Collections;
import java.util.Map;

import sipka.cmdline.runtime.ParseUtil;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class CharParseTest extends SakerTestCase {
	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		assertEquals(ParseUtil.parseCharacterArgument("", listOf("a").iterator()), 'a');
		assertException(IllegalArgumentException.class,
				() -> ParseUtil.parseCharacterArgument("", listOf("ax").iterator()));
		assertException(IllegalArgumentException.class,
				() -> ParseUtil.parseCharacterArgument("", listOf("").iterator()));
		assertException(IllegalArgumentException.class,
				() -> ParseUtil.parseCharacterArgument("", Collections.emptyIterator()));
	}

}
