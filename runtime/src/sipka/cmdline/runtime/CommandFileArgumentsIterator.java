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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;

class CommandFileArgumentsIterator extends ArgumentsIterator {
	private Stream<String> fileStream;
	private Iterator<String> fileIt;

	CommandFileArgumentsIterator(Iterator<? extends String> args) {
		super(args);
	}

	@Override
	protected void moveToNext() {
		if (this.fileIt != null) {
			if (this.fileIt.hasNext()) {
				super.setNext(this.fileIt.next());
				return;
			}
			this.fileStream.close();
			this.fileStream = null;
			this.fileIt = null;
		}
		super.moveToNext();
	}

	@Override
	protected boolean setNext(String next) {
		if (next.startsWith("@")) {
			Path path;
			String pathstr = next.substring(1);
			try {
				path = Paths.get(pathstr);
			} catch (InvalidPathException e) {
				throw new InvalidArgumentValueException("Invalid command file path: " + pathstr, e, next);
			}
			try {
				fileStream = Files.lines(path, StandardCharsets.UTF_8);
				fileIt = fileStream.iterator();
			} catch (IOException e) {
				throw new ArgumentResolutionException("Failed to open command file: " + pathstr, e, next);
			}
			if (fileIt.hasNext()) {
				return super.setNext(fileIt.next());
			}
			return false;
		}
		if (next.startsWith("\\@")) {
			return super.setNext(next.substring(1));
		}
		return super.setNext(next);
	}

	@Override
	public void close() throws IOException {
		Throwable t = null;
		try {
			super.close();
		} catch (Throwable e) {
			t = e;
			throw e;
		} finally {
			Stream<String> fs = fileStream;
			if (fs != null) {
				fileStream = null;
				fileIt = null;
				try {
					fs.close();
				} catch (Throwable e) {
					if (t != null) {
						e.addSuppressed(t);
					}
					throw e;
				}
			}
		}
	}

}
