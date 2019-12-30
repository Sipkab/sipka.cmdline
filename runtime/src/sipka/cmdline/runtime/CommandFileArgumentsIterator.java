package sipka.cmdline.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
			Path path = Paths.get(next.substring(1));
			try {
				fileStream = Files.lines(path, StandardCharsets.UTF_8);
				fileIt = fileStream.iterator();
			} catch (IOException e) {
				throw new IllegalArgumentException("Failed to read command file: " + path, e);
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
