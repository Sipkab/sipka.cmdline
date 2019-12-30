package sipka.cmdline.processor.model;

import java.io.PrintStream;
import java.util.List;

import javax.lang.model.element.Element;

public class ParameterLocation {
	private List<? extends Element> elementPath;

	public ParameterLocation(List<? extends Element> elementpath) {
		this.elementPath = elementpath;
	}

	public void printAccess(PrintStream ps) {
		for (Element ve : elementPath) {
			ps.print('.');
			ps.print(ve.getSimpleName());
		}
	}

}
