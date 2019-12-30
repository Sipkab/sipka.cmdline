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