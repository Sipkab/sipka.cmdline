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
package sipka.cmdline.processor.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sipka.cmdline.api.NameSubstitution;

public class ModelNameSubstitution {
	private Pattern pattern;
	private String replacement;

	public ModelNameSubstitution(NameSubstitution annot) {
		this.pattern = Pattern.compile(annot.pattern());
		this.replacement = annot.replacement();
	}

	public Pattern getPattern() {
		return pattern;
	}

	public String getReplacement() {
		return replacement;
	}

	public String substitute(String name) {
		Matcher matcher = pattern.matcher(name);
		if (matcher.matches()) {
			return matcher.replaceAll(replacement);
		}
		return name;
	}
}
