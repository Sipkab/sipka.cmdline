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

import javax.lang.model.element.TypeElement;

import sipka.cmdline.api.Command;
import sipka.cmdline.processor.CommandLineProcessor;

public class ModelBaseCommand extends AbstractModelCommand {
	private Command annotation;
	private String genClassName;
	private String genPackage;
	private String genSimpleClassName;
	private Set<String> helpCommandName = new LinkedHashSet<>();

	public ModelBaseCommand(CommandLineProcessor processor, Command annotation, TypeElement typeElem) {
		super(processor, typeElem);
		this.annotation = annotation;
		this.genClassName = annotation.className();
		if (genClassName.isEmpty()) {
			this.genPackage = processor.getElements().getPackageOf(typeElem).getQualifiedName().toString();
			//TODO create a better default name
			this.genSimpleClassName = typeElem.getSimpleName() + "ModelImpl";
			this.genClassName = genPackage + "." + genSimpleClassName;
		} else if (genClassName.startsWith(".")) {
			if (genClassName.length() == 1) {
				throw new IllegalArgumentException("Invalid class name: " + genClassName);
			}
			this.genPackage = processor.getElements().getPackageOf(typeElem).getQualifiedName().toString();
			this.genSimpleClassName = genClassName.substring(1);
			this.genClassName = genPackage + "." + genSimpleClassName;
		} else {
			this.genPackage = CommandLineProcessor.getPackageNameFromQualified(genClassName);
			this.genSimpleClassName = CommandLineProcessor.getSimpleClassNameFromQualified(genClassName);
		}

		String[] hcname = annotation.helpCommand();
		for (String hcn : hcname) {
			helpCommandName.add(hcn);
		}
	}

	public Set<String> getHelpCommandName() {
		return helpCommandName;
	}

	public Command getAnnotation() {
		return annotation;
	}

	public String getGeneratedClassQualifiedName() {
		return genClassName;
	}

	public boolean createMainMethod() {
		return annotation.main();
	}

	@Override
	public ModelCommand getParentCommand() {
		return null;
	}

	public String getGeneratedPackageName() {
		return genPackage;
	}

	public String getGeneratedSimpleClassName() {
		return genSimpleClassName;
	}

	@Override
	public String getCommandClassQualifiedName() {
		return genClassName;
	}

}
