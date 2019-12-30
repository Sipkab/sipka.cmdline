package sipka.cmdline.processor;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class AutoIndentOutputStream extends FilterOutputStream {

	private int identCount = 0;
	private boolean addIdent = false;
	private byte[] indentation = "\t".getBytes();

	public AutoIndentOutputStream(OutputStream out) {
		super(out);
	}

	public void setIndentation(String indentation) {
		this.indentation = indentation.getBytes();
	}

	@Override
	public void write(int b) throws IOException {
		switch (b) {
			case '{': {
				addIdentIfNecessary();
				++identCount;
				break;
			}
			case '}': {
				if (identCount > 0) {
					--identCount;
				}
				addIdentIfNecessary();
				break;
			}
			case '\r':
			case '\n': {
				addIdent = true;
				break;
			}
			default: {
				addIdentIfNecessary();
				break;
			}
		}
		super.write(b);
	}

	private void addIdentIfNecessary() throws IOException {
		if (addIdent) {
			addIdent = false;
			for (int i = 0; i < identCount; i++) {
				out.write(indentation);
			}
		}
	}

}