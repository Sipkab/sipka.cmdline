/*
 * Copyright (C) 2020 Bence Sipka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sipka.cmdline.runtime;

import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;

public class ParseUtil {
	private ParseUtil() {
		throw new UnsupportedOperationException();
	}

	public static <T extends Enum<T>> T parseEnumUpperCaseArgument(Class<T> enumclass, String arg,
			Iterator<? extends String> it) throws NullPointerException, ArgumentException {
		String next = requireNextArgument(arg, it);
		//we use the english locale to converting to upper case for determinism
		try {
			return Enum.valueOf(enumclass, next.toUpperCase(Locale.ENGLISH));
		} catch (IllegalArgumentException e) {
			StringBuilder sb = new StringBuilder();
			sb.append("Unrecognized enum value: ");
			sb.append(next);
			sb.append(" for type: ");
			sb.append(enumclass.getName());
			T[] enumConstants = enumclass.getEnumConstants();
			if (enumConstants.length >= 0) {
				sb.append(" Expected any of (case-insensitive): ");
				for (int i = 0; i < enumConstants.length; i++) {
					T ec = enumConstants[i];
					sb.append(ec.name());
					if (i + 1 < enumConstants.length) {
						sb.append(", ");
					}
				}
			}
			throw new InvalidArgumentValueException(sb.toString(), arg);
		}
	}

	public static <T extends Enum<T>> T parseEnumArgument(Class<T> enumclass, String arg, Iterator<? extends String> it)
			throws NullPointerException, ArgumentException {
		String next = requireNextArgument(arg, it);
		try {
			return Enum.valueOf(enumclass, next);
		} catch (IllegalArgumentException e) {
			StringBuilder sb = new StringBuilder();
			sb.append("Unrecognized enum value: ");
			sb.append(next);
			sb.append(" for type: ");
			sb.append(enumclass.getName());
			T[] enumConstants = enumclass.getEnumConstants();
			if (enumConstants.length >= 0) {
				sb.append(" Expected any of: ");
				for (int i = 0; i < enumConstants.length; i++) {
					T ec = enumConstants[i];
					sb.append(ec.name());
					if (i + 1 < enumConstants.length) {
						sb.append(", ");
					}
				}
			}
			throw new InvalidArgumentValueException(sb.toString(), arg);
		}
	}

	public static String parseStringArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, ArgumentException {
		String next = requireNextArgument(arg, it);
		return next;
	}

	public static byte parseByteArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, ArgumentException {
		String next = requireNextArgument(arg, it);
		try {
			return Byte.parseByte(next);
		} catch (NumberFormatException e) {
			throw new InvalidArgumentFormatException("Invalid input number for byte: " + next, e, arg);
		}
	}

	public static short parseShortArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, ArgumentException {
		String next = requireNextArgument(arg, it);
		try {
			return Short.parseShort(next);
		} catch (NumberFormatException e) {
			throw new InvalidArgumentFormatException("Invalid input number for short: " + next, e, arg);
		}
	}

	public static int parseIntegerArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, ArgumentException {
		String next = requireNextArgument(arg, it);
		try {
			return Integer.parseInt(next);
		} catch (NumberFormatException e) {
			throw new InvalidArgumentFormatException("Invalid input number for int: " + next, e, arg);
		}
	}

	public static long parseLongArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, ArgumentException {
		String next = requireNextArgument(arg, it);
		try {
			return Long.parseLong(next);
		} catch (NumberFormatException e) {
			throw new InvalidArgumentFormatException("Invalid input number for long: " + next, e, arg);
		}
	}

	public static float parseFloatArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, ArgumentException {
		String next = requireNextArgument(arg, it);
		try {
			return Float.parseFloat(next);
		} catch (NumberFormatException e) {
			throw new InvalidArgumentFormatException("Invalid input number for float: " + next, e, arg);
		}
	}

	public static double parseDoubleArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, ArgumentException {
		String next = requireNextArgument(arg, it);
		try {
			return Double.parseDouble(next);
		} catch (NumberFormatException e) {
			throw new InvalidArgumentFormatException("Invalid input number for double: " + next, e, arg);
		}
	}

	public static boolean parseBooleanArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, ArgumentException {
		String next = requireNextArgument(arg, it);
		if ("true".equalsIgnoreCase(next)) {
			return true;
		}
		if ("false".equals(next)) {
			return false;
		}
		throw new InvalidArgumentFormatException("Expected true or false for boolean argument.", arg);
	}

	public static char parseCharacterArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, ArgumentException {
		String next = requireNextArgument(arg, it);
		if (next.length() == 1) {
			return next.charAt(0);
		}
		throw new InvalidArgumentFormatException("Invalid input value for character: " + next, arg);
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
			//don't throw ArgumentException subtype as this is a harder failure
			//the caller should satisfy this precondition
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
			throws NullPointerException, ArgumentException {
		Objects.requireNonNull(it, "iterator");
		if (!it.hasNext()) {
			throw new MissingArgumentException("Missing argument", arg);
		}
		String result = it.next();
		if (result == null) {
			//don't throw ArgumentException subtype as this is a harder failure
			throw new NullPointerException("null argument value for: " + arg);
		}
		return result;
	}

	public static void requireHasNextArgument(String arg, Iterator<? extends String> it)
			throws NullPointerException, ArgumentException {
		Objects.requireNonNull(it, "iterator");
		if (!it.hasNext()) {
			throw new MissingArgumentException("Missing argument", arg);
		}
	}
}
