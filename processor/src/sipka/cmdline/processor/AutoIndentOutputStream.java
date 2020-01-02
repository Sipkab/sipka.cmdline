/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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