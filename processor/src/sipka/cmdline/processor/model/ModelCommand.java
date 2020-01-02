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
