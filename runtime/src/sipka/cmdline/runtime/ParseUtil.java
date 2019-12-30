package sipka.cmdline.runtime;

import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;

public class ParseUtil {
	private ParseUtil() {
		throw new UnsupportedOperationException();
	}

	public static <T extends Enum<T>> T parseEnumUpperCaseArgument(Class<T> enumclass, String arg,
			Iterator<? extends String> it) throws NullPointerException, IllegalArgumentException {
		requireHasNextArgument(arg, it);
		//we use the english locale to converting to upper case for determinism
		return Enum.valueOf(enumclass, it.next().toUpperCase(Locale.ENGLISH));
	}

	public static <T extends Enum<T>> T parseEnumArgument(Class<T> enumclass, String arg, Iterator<? extends String> it)
			throws NullPointerException, IllegalArgumentException {
		requireHasNextArgument(arg, it);
		return Enum.valueOf(enumclass, it.next());
	}

	public static String parseStringArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, IllegalArgumentException {
		requireHasNextArgument(arg, it);
		return it.next();
	}

	public static byte parseByteArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, IllegalArgumentException {
		requireHasNextArgument(arg, it);
		return Byte.parseByte(it.next());
	}

	public static short parseShortArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, IllegalArgumentException {
		requireHasNextArgument(arg, it);
		return Short.parseShort(it.next());
	}

	public static int parseIntegerArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, IllegalArgumentException {
		requireHasNextArgument(arg, it);
		return Integer.parseInt(it.next());
	}

	public static long parseLongArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, IllegalArgumentException {
		requireHasNextArgument(arg, it);
		return Long.parseLong(it.next());
	}

	public static float parseFloatArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, IllegalArgumentException {
		requireHasNextArgument(arg, it);
		return Float.parseFloat(it.next());
	}

	public static double parseDoubleArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, IllegalArgumentException {
		requireHasNextArgument(arg, it);
		return Double.parseDouble(it.next());
	}

	public static boolean parseBooleanArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, IllegalArgumentException {
		requireHasNextArgument(arg, it);
		return Boolean.parseBoolean(it.next());
	}

	public static char parseCharacterArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, IllegalArgumentException {
		requireHasNextArgument(arg, it);
		String a = it.next();
		if (a.length() == 1) {
			return a.charAt(0);
		}
		throw new IllegalArgumentException("Failed to parse character: " + arg);
	}

	public static String toKeyValueArgument(String prefix, String key, String value) throws NullPointerException {
		Objects.requireNonNull(key, "key");
		StringBuilder sb = new StringBuilder();
		if (prefix != null) {
			sb.append(prefix);
		}
		if (key.indexOf('=') >= 0) {
			sb.append(key.replace("=", "\\="));
		} else {
			sb.append(key);
		}
		if (value != null) {
			sb.append('=');
			sb.append(value);
		} //else value is null, no = sign after key
		return sb.toString();
	}

	private static int getFirstUnescapedEqualsIndex(String arg, int startidx) {
		int len = arg.length();
		while (startidx < len) {
			int idx = arg.indexOf('=', startidx);
			if (idx < 0) {
				return -1;
			}
			if (idx == startidx) {
				return startidx;
			}
			if (arg.charAt(idx - 1) == '\\') {
				startidx = idx + 1;
			} else {
				return idx;
			}
		}
		return -1;
	}

	public static void parseEqualsFormatArgument(final int prefixlen, final String arg, final String[] result)
			throws NullPointerException, IllegalArgumentException {
		//all '=' until the separator '=' must be escaped with \=

		//allowed formats: (with -X as prefix)
		//    -Xkey=value
		//    -X=value (empty key)
		//    -Xkey (null value)
		//    -X (empty key, null value)
		//    -Xkey= (empty value)
		//    -X= (empty key, empty value)
		//and any form of these where the key or value is quoted
		//    quote is escaped using \"
		int len = arg.length();
		if (len < prefixlen) {
			throw new IllegalArgumentException("Too short argument: " + arg + " for prefix length: " + prefixlen);
		}
		Objects.requireNonNull(result, "result");
		if (len == prefixlen) {
			//    -X (empty key, null value)
			result[0] = "";
			result[1] = null;
			return;
		}

		int equalsidx = getFirstUnescapedEqualsIndex(arg, prefixlen);
		if (equalsidx < 0) {
			result[0] = arg.substring(prefixlen).replace("\\=", "=");
			result[1] = null;
			return;
		}
		result[0] = arg.substring(prefixlen, equalsidx).replace("\\=", "=");
		result[1] = arg.substring(equalsidx + 1);
		return;
	}

	public static ParsingIterator createCommandFileArgumentIterator(Iterator<? extends String> args) {
		Objects.requireNonNull(args, "args");
		return new CommandFileArgumentsIterator(args);
	}

	public static ParsingIterator createSimpleArgumentIterator(Iterator<? extends String> args) {
		Objects.requireNonNull(args, "args");
		return new ArgumentsIterator(args);
	}

	public static String requireNextArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(it, "iterator");
		if (!it.hasNext()) {
			throw new IllegalArgumentException("Argument missing for: " + arg);
		}
		return it.next();
	}

	public static void requireHasNextArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(it, "iterator");
		if (!it.hasNext()) {
			throw new IllegalArgumentException("Argument missing for: " + arg);
		}
	}
}
