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
import java.util.List;
import java.util.stream.Stream;

class CommandFileArgumentsIterator extends ArgumentsIterator {
	private static final String PREFIX_COMMAND_FILE = "@";
	private static final String PREFIX_COMMAND_FILE_DELETE = "@!delete!@";

	private Stream<String> fileStream;
	private Iterator<String> fileIt;

	CommandFileArgumentsIterator(Iterator<? extends String> args) {
		super(args);
	}

	@Override
	protected void moveToNext() {
		Iterator<String> fileit = this.fileIt;
		if (fileit != null) {
			if (fileit.hasNext()) {
				super.setNext(fileit.next());
				return;
			}
			Stream<String> fstream = this.fileStream;
			if (fstream != null) {
				fstream.close();
			}
			this.fileStream = null;
			this.fileIt = null;
		}
		super.moveToNext();
	}

	@Override
	protected boolean setNext(String next) {
		if (next.startsWith(PREFIX_COMMAND_FILE)) {
			Path path;
			if (next.startsWith(PREFIX_COMMAND_FILE_DELETE)) {
				String pathstr = next.substring(10);
				try {
					path = Paths.get(pathstr);
				} catch (InvalidPathException e) {
					throw new InvalidArgumentValueException("Invalid command file path: " + pathstr, e, next);
				}
				ArgumentResolutionException thrown = null;
				try {
					List<String> lines = Files.readAllLines(path);
					fileIt = lines.iterator();
				} catch (IOException e) {
					thrown = new ArgumentResolutionException("Failed to read command file: " + pathstr, e, next);
					throw thrown;
				} finally {
					try {
						Files.deleteIfExists(path);
					} catch (IOException e) {
						if (thrown != null) {
							thrown.addSuppressed(e);
						} else {
							throw new ArgumentResolutionException(
									"Failed to delete command file after read: " + pathstr, e, next);
						}
					}
				}
			} else {
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
			//iterate the remaining direct arguments and handle command file deletions
			ArgumentException deleteexc = null;
			try {
				if (it != null) {
					while (it.hasNext()) {
						String next = it.next();
						if (next.startsWith(PREFIX_COMMAND_FILE_DELETE)) {
							String pathstr = next.substring(10);
							Path path;
							try {
								path = Paths.get(pathstr);
							} catch (InvalidPathException e) {
								InvalidArgumentValueException valexc = new InvalidArgumentValueException(
										"Invalid command file path: " + pathstr, e, next);
								if (deleteexc == null) {
									deleteexc = valexc;
								} else {
									deleteexc.addSuppressed(valexc);
								}
								continue;
							}
							try {
								Files.deleteIfExists(path);
							} catch (IOException e) {
								if (deleteexc != null) {
									deleteexc.addSuppressed(e);
								} else {
									deleteexc = new ArgumentResolutionException(
											"Failed to delete command file after on close: " + pathstr, e, next);
								}
							}
						}
					}
				}
			} finally {
				try {
					super.close();
				} catch (Throwable e) {
					if (deleteexc != null) {
						e.addSuppressed(deleteexc);
					}
					throw e;
				}
			}
			if (deleteexc != null) {
				throw deleteexc;
			}
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
						//t is already being thrown, add this as suppressed
						t.addSuppressed(e);
					} else {
						throw e;
					}
				}
			}
		}
	}

}
