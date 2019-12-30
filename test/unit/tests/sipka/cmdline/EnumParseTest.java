package tests.sipka.cmdline;

import java.util.Collections;
import java.util.Map;

import javax.lang.model.element.ElementKind;

import sipka.cmdline.runtime.ParseUtil;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class EnumParseTest extends SakerTestCase {
	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		assertEquals(ParseUtil.parseEnumArgument(ElementKind.class, "", listOf("CLASS").iterator()), ElementKind.CLASS);
		assertException(IllegalArgumentException.class,
				() -> ParseUtil.parseEnumArgument(ElementKind.class, "", listOf("class").iterator()));

		assertException(IllegalArgumentException.class,
				() -> ParseUtil.parseEnumArgument(ElementKind.class, "", Collections.emptyIterator()));

		assertEquals(ParseUtil.parseEnumUpperCaseArgument(ElementKind.class, "", listOf("CLASS").iterator()),
				ElementKind.CLASS);
		assertEquals(ParseUtil.parseEnumUpperCaseArgument(ElementKind.class, "", listOf("class").iterator()),
				ElementKind.CLASS);
		assertEquals(ParseUtil.parseEnumUpperCaseArgument(ElementKind.class, "", listOf("ClAsS").iterator()),
				ElementKind.CLASS);
	}

}
