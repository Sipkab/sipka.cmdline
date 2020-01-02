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

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

class ArgumentsIterator implements ParsingIterator {
	private Iterator<? extends String> it;
	private String next;

	ArgumentsIterator(Iterator<? extends String> args) {
		this.it = args;
		moveToNext();
	}

	protected void moveToNext() {
		while (it != null) {
			if (it.hasNext()) {
				String n = it.next();
				if (setNext(n)) {
					return;
				}
				continue;
			}
			it = null;
			return;
		}
	}

	protected boolean setNext(String next) {
		this.next = next;
		return true;
	}

	@Override
	public String next() {
		if (it == null) {
			throw new NoSuchElementException();
		}
		String result = next;
		this.next = null;
		moveToNext();
		return result;
	}

	@Override
	public String peek() {
		if (it == null) {
			throw new NoSuchElementException();
		}
		return next;
	}

	@Override
	public boolean hasNext() {
		return it != null;
	}

	@Override
	public void close() throws IOException {
		//set the iterator to null to not return any more elements
		it = null;
	}
}