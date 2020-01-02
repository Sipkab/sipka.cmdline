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

import java.util.LinkedHashSet;
import java.util.Set;

import sipka.cmdline.api.SubCommand;
import sipka.cmdline.processor.CommandLineProcessor;

public class ModelSubCommand extends AbstractModelCommand {
	private ModelCommand parentCommand;
	private SubCommand annotation;

	private Set<String> names;
	private boolean defaultCommand;

	public ModelSubCommand(ModelCommand parentCommand, CommandLineProcessor processor, SubCommand annot) {
		super(processor, processor.getTypeElement(annot::type));
		this.parentCommand = parentCommand;
		this.annotation = annot;
		this.names = new LinkedHashSet<>();
		for (String n : annot.name()) {
			names.add(n);
		}
		if (names.isEmpty()) {
			throw new IllegalArgumentException("No names specified for sub command: " + annot);
		}
		this.defaultCommand = annot.defaultCommand();
	}

	public SubCommand getAnnotation() {
		return annotation;
	}

	public Set<String> getNames() {
		return names;
	}

	public boolean isDefaultCommand() {
		return defaultCommand;
	}

	@Override
	public ModelCommand getParentCommand() {
		return parentCommand;
	}

	@Override
	public String getCommandClassQualifiedName() {
		return getTypeElement().getQualifiedName().toString();
	}
	
}
