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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import sipka.cmdline.api.CommonConverter;
import sipka.cmdline.api.IncludeCommonConverters;
import sipka.cmdline.api.NameSubstitution;
import sipka.cmdline.api.Parameter;
import sipka.cmdline.api.ParameterContext;
import sipka.cmdline.api.PositionalParameter;
import sipka.cmdline.api.SubCommand;
import sipka.cmdline.processor.CommandLineProcessor;

public abstract class AbstractModelCommand implements ModelCommand {
	private TypeElement typeElem;
	private Collection<ModelSubCommand> subCommands = new ArrayList<>();
	private ModelSubCommand defaultSubCommand;
	private Collection<ModelCommonConverter> commonConverters;
	private List<ModelParameter> parameters;
	private String docComment;
	private boolean deprecated;

	private List<ModelParameter> positionalParameters = new ArrayList<>();

	private NavigableMap<String, ModelParameter> mapParameters = new TreeMap<>(Collections.reverseOrder());
	private List<ModelParameter> requiredParameters = new ArrayList<>();

	public AbstractModelCommand(CommandLineProcessor processor, TypeElement typeElem) {
		this.typeElem = typeElem;
		this.commonConverters = getCommonConverters(processor, typeElem);
		this.docComment = processor.getElements().getDocComment(typeElem);
		this.deprecated = processor.getElements().isDeprecated(typeElem);

		SubCommand[] subs = typeElem.getAnnotationsByType(SubCommand.class);
		Map<String, ModelSubCommand> commandnames = new TreeMap<>();
		for (SubCommand s : subs) {
			ModelSubCommand subc = new ModelSubCommand(this, processor, s);
			if (subc.isDefaultCommand()) {
				if (this.defaultSubCommand != null) {
					throw new IllegalArgumentException(
							"Multiple default commands: " + this.defaultSubCommand.getTypeElement().getQualifiedName()
									+ " and " + subc.getTypeElement().getQualifiedName());
				}
				this.defaultSubCommand = subc;
			}
			for (String n : subc.getNames()) {
				ModelSubCommand prev = commandnames.put(n, subc);
				if (prev != null) {
					throw new IllegalArgumentException("Command with name: " + n + " defined multiple times: "
							+ subc.getAnnotation() + " and " + prev.getAnnotation());
				}
			}
			subCommands.add(subc);
		}

		this.parameters = getParameters(processor, typeElem);
		for (ModelParameter param : parameters) {
			if (param.isMapParameter()) {
				for (String n : param.getNames()) {
					ModelParameter prev = mapParameters.put(n, param);
					if (prev != null) {
						throw new IllegalArgumentException("Multiple Map parameters defined with same prefix: " + n
								+ " - " + param.getElement() + " and " + prev.getElement());
					}
				}
			} else {
				PositionalParameter positional = param.getPositional();
				if (positional != null) {
					positionalParameters.add(param);
				}
				for (String n : param.getNames()) {
					if (commandnames.containsKey(n)) {
						throw new IllegalArgumentException("Both parameter (" + param.getElement() + ") and command ("
								+ commandnames.get(n).getAnnotation() + ") defined with name: " + n);
					}
				}
			}

			if (param.isRequired()) {
				requiredParameters.add(param);
			}
		}

		positionalParameters.sort((l, r) -> {
			int lval = l.getPositional().value();
			int rval = r.getPositional().value();
			int lsig = Integer.signum(lval);
			int rsig = Integer.signum(rval);
			int sigcmp = Integer.compare(lsig, rsig);
			if (sigcmp != 0) {
				return -sigcmp;
			}
			if (lval < 0) {
				return -Integer.compare(lval, rval);
			}
			int valcmp = Integer.compare(lval, rval);
			if (valcmp != 0) {
				return valcmp;
			}
			//sort required params first in the same priority
			return -Boolean.compare(l.isRequired(), r.isRequired());
		});
		if (positionalParameters.size() > 0
				&& positionalParameters.get(positionalParameters.size() - 1).getPositional().value() < 0) {
			//has end positional parameter
			if (defaultSubCommand != null) {
				throw new IllegalArgumentException("Cannot have default command and end positional parameters.");
			}
		}
		if (!positionalParameters.isEmpty()) {
			//make sure required positional parameters are at the start
			Iterator<ModelParameter> it = positionalParameters.iterator();
			boolean prevreq = it.next().isRequired();
			while (it.hasNext()) {
				ModelParameter posparam = it.next();
				if (posparam.isRequired() != prevreq) {
					if (!prevreq) {
						throw new IllegalArgumentException(
								"Required positional parameters must occur before not required ones. Invalid order for: "
										+ posparam.getElement());
					}
					prevreq = posparam.isRequired();
				}
			}
		}

	}

	public void resolve(CommandLineProcessor processor) {
		for (ModelSubCommand sc : subCommands) {
			sc.resolve(processor);
		}
		for (ModelParameter p : parameters) {
			p.resolve(processor);
		}
	}

	@Override
	public List<ModelParameter> getPositionalParameters() {
		return positionalParameters;
	}

	@Override
	public NavigableMap<String, ModelParameter> getMapParameters() {
		return mapParameters;
	}

	@Override
	public List<ModelParameter> getRequiredParameters() {
		return requiredParameters;
	}

	@Override
	public String getDocComment() {
		return docComment;
	}

	@Override
	public boolean isDeprecated() {
		return deprecated;
	}

	@Override
	public List<ModelParameter> getParameters() {
		return parameters;
	}

	@Override
	public Collection<ModelCommonConverter> getCommonConverters() {
		return commonConverters;
	}

	@Override
	public TypeElement getTypeElement() {
		return typeElem;
	}

	@Override
	public Collection<ModelSubCommand> getSubCommands() {
		return subCommands;
	}

	@Override
	public ModelSubCommand getDefaultSubCommand() {
		return defaultSubCommand;
	}

	public List<ModelParameter> getParameters(CommandLineProcessor processor, TypeElement type) {
		List<ModelParameter> result = new ArrayList<>();
		collectParameters(processor, type, new LinkedList<>(), result::add);
		return result;
	}

	private void collectParameters(CommandLineProcessor processor, TypeElement type, LinkedList<Element> locationstack,
			Consumer<ModelParameter> result) {
		for (Element enclosedelem : type.getEnclosedElements()) {
			ElementKind enclosedkind = enclosedelem.getKind();
			switch (enclosedkind) {
				case FIELD: {
					VariableElement field = (VariableElement) enclosedelem;
					locationstack.addLast(field);
					try {
						Parameter paramannot = enclosedelem.getAnnotation(Parameter.class);
						ParameterContext paramcontextannot = enclosedelem.getAnnotation(ParameterContext.class);
						if (paramannot != null) {
							if (paramcontextannot != null) {
								throw new IllegalArgumentException(Parameter.class.getSimpleName() + " and "
										+ ParameterContext.class.getSimpleName() + " both present on field: "
										+ enclosedelem);
							}
							result.accept(new ModelParameter(processor, field, paramannot,
									new ParameterLocation(new ArrayList<>(locationstack)), this));
							continue;
						}
						if (paramcontextannot != null) {
							TypeElement fieldtype = CommandLineProcessor.getTypeElementOf(field);
							if (fieldtype == null) {
								throw new IllegalArgumentException("Invalid parameter context type: " + field);
							}
							List<ModelNameSubstitution> substitutions = new ArrayList<>();
							for (NameSubstitution subst : paramcontextannot.substitutions()) {
								substitutions.add(new ModelNameSubstitution(subst));
							}
							collectParameters(processor, fieldtype, locationstack, pr -> {
								pr.setNames(applySubstitutions(pr.getNames(), substitutions));
								result.accept(pr);
							});
						}
					} finally {
						locationstack.removeLast();
					}
					break;
				}
				case METHOD: {
					ExecutableElement method = (ExecutableElement) enclosedelem;
					Parameter paramannot = enclosedelem.getAnnotation(Parameter.class);
					if (paramannot == null) {
						continue;
					}
					locationstack.addLast(enclosedelem);
					try {
						ModelParameter modelparam = new ModelParameter(processor, method, paramannot,
								new ParameterLocation(new ArrayList<>(locationstack)), this);
						result.accept(modelparam);
					} finally {
						locationstack.removeLast();
					}
					break;
				}
				default: {
					continue;
				}
			}
			if (enclosedkind != ElementKind.FIELD) {
				continue;
			}
		}
		TypeMirror sc = type.getSuperclass();
		if (sc.getKind() == TypeKind.DECLARED) {
			collectParameters(processor, (TypeElement) ((DeclaredType) sc).asElement(), locationstack, result);
		}
	}

	private static Set<String> applySubstitutions(Set<String> names, List<ModelNameSubstitution> substitutions) {
		Set<String> result = new LinkedHashSet<>();
		for (String n : names) {
			for (ModelNameSubstitution subst : substitutions) {
				n = subst.substitute(n);
			}
			result.add(n);
		}
		return result;
	}

	public static Collection<ModelCommonConverter> getCommonConverters(CommandLineProcessor processor,
			TypeElement annotatedelem) {
		Collection<ModelCommonConverter> commonConverters = new ArrayList<>();
		collectCommonConverters(processor, annotatedelem, commonConverters);
		return commonConverters;
	}

	private static void collectCommonConverters(CommandLineProcessor processor, TypeElement annotatedelem,
			Collection<ModelCommonConverter> result) {
		for (CommonConverter cc : annotatedelem.getAnnotationsByType(CommonConverter.class)) {
			TypeMirror targettype = processor.getTypeMirror(cc::type);
			TypeElement methoddeclaringtype = processor.getTypeElement(cc::converter);
			String methodname = cc.method();
			result.add(new ModelCommonConverter(targettype,
					methoddeclaringtype.equals(processor.getCommonConverterAnnot()) ? annotatedelem
							: methoddeclaringtype,
					methodname));
		}
		IncludeCommonConverters includes = annotatedelem.getAnnotation(IncludeCommonConverters.class);
		if (includes != null) {
			for (TypeElement includedte : processor.getTypeElements(includes::value)) {
				collectCommonConverters(processor, includedte, result);
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + typeElem.getQualifiedName() + "]";
	}

}
