package sipka.cmdline.runtime;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator interface used during argument parsing.
 * <p>
 * The iterator declares additional functions to perform the argument parsing in a more convenient way.
 * <p>
 * The interface implements {@link Closeable} as it may use managed resources to parse the arguments. (E.g. command
 * files)
 */
public interface ParsingIterator extends Iterator<String>, Closeable {
	/**
	 * Peeks the next argument.
	 * <p>
	 * The method returns the same element as {@link #next()}, but doesn't advance the iterator itself.
	 * 
	 * @return The next argument.
	 * @throws NoSuchElementException
	 *             If there are no more elements.
	 */
	public String peek() throws NoSuchElementException;
}
