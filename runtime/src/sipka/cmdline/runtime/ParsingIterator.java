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
