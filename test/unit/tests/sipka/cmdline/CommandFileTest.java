package tests.sipka.cmdline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import sipka.cmdline.runtime.ParseUtil;
import sipka.cmdline.runtime.ParsingIterator;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class CommandFileTest extends SakerTestCase {

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		assertFalse(ParseUtil.createCommandFileArgumentIterator(Collections.emptyIterator()).hasNext());

		assertEquals(argsToList(""), listOf(""));
		assertEquals(argsToList("@test/resources/CommandFileTest/empty.txt"), listOf());
		assertEquals(argsToList("@test/resources/CommandFileTest/single.txt"), listOf("single"));
		assertEquals(argsToList("@test/resources/CommandFileTest/dual.txt"), listOf("one", "two"));
		assertEquals(argsToList("@test/resources/CommandFileTest/triple.txt"), listOf("one", "two", "three"));
		assertEquals(argsToList("@test/resources/CommandFileTest/recursive.txt"), listOf("one", "@recursive.txt", "three"));
		
		assertEquals(argsToList("\\@test/resources/CommandFileTest/empty.txt"), listOf("@test/resources/CommandFileTest/empty.txt"));

		assertEquals(
				argsToList("@test/resources/CommandFileTest/empty.txt", "@test/resources/CommandFileTest/empty.txt"),
				listOf());
		assertEquals(argsToList("@test/resources/CommandFileTest/dual.txt", "@test/resources/CommandFileTest/dual.txt"),
				listOf("one", "two", "one", "two"));
		assertEquals(argsToList("@test/resources/CommandFileTest/dual.txt", "@test/resources/CommandFileTest/empty.txt",
				"@test/resources/CommandFileTest/dual.txt"), listOf("one", "two", "one", "two"));
		assertEquals(argsToList("a", "@test/resources/CommandFileTest/dual.txt",
				"@test/resources/CommandFileTest/empty.txt", "@test/resources/CommandFileTest/dual.txt"),
				listOf("a", "one", "two", "one", "two"));

		assertEquals(
				argsToList("a", "@test/resources/CommandFileTest/dual.txt", "a",
						"@test/resources/CommandFileTest/empty.txt", "@test/resources/CommandFileTest/dual.txt"),
				listOf("a", "one", "two", "a", "one", "two"));
		assertEquals(argsToList("a", "@test/resources/CommandFileTest/dual.txt",
				"@test/resources/CommandFileTest/empty.txt", "@test/resources/CommandFileTest/dual.txt", "a"),
				listOf("a", "one", "two", "one", "two", "a"));

		assertEquals(
				argsToList("a", "@test/resources/CommandFileTest/dual.txt", "a",
						"@test/resources/CommandFileTest/empty.txt", "b", "@test/resources/CommandFileTest/dual.txt"),
				listOf("a", "one", "two", "a", "b", "one", "two"));

	}

	private static List<String> argsToList(String... args) throws IOException {
		List<String> result = new ArrayList<>();
		try (ParsingIterator it = ParseUtil.createCommandFileArgumentIterator(listOf(args).iterator())) {
			it.forEachRemaining(result::add);
		}
		return result;
	}

}
