package sipka.cmdline.processor.model;

import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;

import javax.lang.model.element.TypeElement;

public interface ModelCommand {
	public Collection<ModelCommonConverter> getCommonConverters();

	public TypeElement getTypeElement();

	public Collection<ModelSubCommand> getSubCommands();

	public ModelSubCommand getDefaultSubCommand();

	public ModelCommand getParentCommand();

	//in declaration order
	public List<ModelParameter> getParameters();

	public boolean isDeprecated();

	public String getDocComment();

	public String getCommandClassQualifiedName();

	//ordered descending
	public NavigableMap<String, ModelParameter> getMapParameters();

	public List<ModelParameter> getRequiredParameters();

	//sorted by position ascending. Non-negative first ascending (0, 1, 2...), then negatives descending (-1, -2, -3...)
	public List<ModelParameter> getPositionalParameters();
}
