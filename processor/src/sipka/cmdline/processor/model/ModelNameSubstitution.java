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
