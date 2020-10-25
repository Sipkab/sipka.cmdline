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

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import sipka.cmdline.api.Converter;
import sipka.cmdline.api.Flag;
import sipka.cmdline.api.MultiParameter;
import sipka.cmdline.api.Parameter;
import sipka.cmdline.api.PositionalParameter;
import sipka.cmdline.processor.CommandLineProcessor;

public class ModelParameter {
	private static final String DEFAULT_MAP_PARAMETER_FORMAT_KEY_NAME = "key";
	private static final String DEFAULT_MAP_PARAMETER_FORMAT_VALUE_NAME = "value";

	private static final String DOC_TAG_HELP_META = "cmd-help-meta";
	private static final String DOC_TAG_PARAMETER_FORMAT = "cmd-format";

	private final TypeMirror parameterType;
	private final Element element;

	private Set<String> names = new LinkedHashSet<>();
	private Set<String> helpMetaNames = new LinkedHashSet<>();
	private ParameterLocation location;
	private boolean required;
	private ModelConverter converter;
	private PositionalParameter positional;
	private Flag flag;
	private ModelMultiParameter multiParameter;
	private boolean deprecated;

	private Entry<String, String> mapParameterFormatNames = null;
	private Parameter anotation;
	private String docComment;

	private String docCommentFormat;

	private ModelCommand parentCommand;

	public ModelParameter(CommandLineProcessor processor, VariableElement element, Parameter parameter,
			ParameterLocation location, ModelCommand parent) {
		this.element = element;
		this.anotation = parameter;
		this.location = location;
		this.parentCommand = parent;
		this.positional = element.getAnnotation(PositionalParameter.class);
		this.deprecated = processor.getElements().isDeprecated(element);
		this.docComment = processor.getElements().getDocComment(element);

		String[] names = parameter.value();
		if (names.length == 0) {
			if (this.positional == null) {
				names = new String[] { "-" + element.getSimpleName().toString() };
			} else {
				names = new String[] { element.getSimpleName().toString() };
			}
		}
		this.helpMetaNames = getDocCommentHelpMetaNames(docComment);
		for (String n : names) {
			this.names.add(n);
		}
		if (this.positional != null) {
			if (this.names.size() > 1) {
				throw new IllegalArgumentException(
						"Cannot specify multiple names for positional parameter: " + element);
			}
		}
		this.parameterType = element.asType();
		if (processor.getTypes().isAssignable(parameterType, processor.getErasedMapType())) {
			this.mapParameterFormatNames = new AbstractMap.SimpleEntry<>(DEFAULT_MAP_PARAMETER_FORMAT_KEY_NAME,
					DEFAULT_MAP_PARAMETER_FORMAT_VALUE_NAME);
		}
		this.required = parameter.required();
		this.flag = element.getAnnotation(Flag.class);
		if (this.deprecated) {
			if (this.required) {
				throw new IllegalArgumentException("A required parameter cannot be deprecated: " + element);
			}
			if (this.positional != null) {
				throw new IllegalArgumentException(
						"A positional parameter cannot be deprecated as there is no way to make it optional: "
								+ element);
			}
		}

		Converter converterannot = element.getAnnotation(Converter.class);
		if (converterannot != null) {
			TypeElement methoddeclaringtype = processor.getTypeElement(converterannot::converter);
			String methodname = converterannot.method();
			this.converter = new ModelConverter(methoddeclaringtype.equals(processor.getConverterAnnot())
					? (TypeElement) element.getEnclosingElement()
					: methoddeclaringtype, methodname);
		}

		if (this.flag != null) {
			if (parameterType.getKind() != TypeKind.BOOLEAN
					&& !processor.getTypes().isSameType(parameterType, processor.getJavaLangBoolean().asType())) {
				throw new IllegalArgumentException(
						Flag.class.getSimpleName() + " annotation is only available for boolean types: " + element);
			}
			if (required) {
				throw new IllegalArgumentException("Flag annotated parameter cannot be required: " + element);
			}
		}
		MultiParameter multiparam = element.getAnnotation(MultiParameter.class);
		if (multiparam != null) {
			if (this.positional != null) {
				throw new IllegalArgumentException(
						"Conflicting annotations " + this.positional + " and " + multiparam + " on " + element);
			}
			if (this.flag != null) {
				throw new IllegalArgumentException(
						"Conflicting annotations " + this.flag + " and " + multiparam + " on " + element);
			}
			TypeMirror multielemtype = processor.getTypeMirror(multiparam::value);
			String multimethod = multiparam.method();
			if (multimethod.isEmpty()) {
				if (processor.getTypes().isAssignable(parameterType, processor.getErasedCollectionType())) {
					multimethod = "add";
				} else {
					throw new IllegalArgumentException("Unknown method name for multi parameter on field: " + element);
				}
			}
			this.multiParameter = new ModelMultiParameter(multielemtype, multimethod);
		}

		if (mapParameterFormatNames != null) {
			if (this.positional != null) {
				throw new IllegalArgumentException("Map parameter cannot be positional: " + element);
			}
		}
	}

	public ModelParameter(CommandLineProcessor processor, ExecutableElement element, Parameter parameter,
			ParameterLocation location, ModelCommand parent) {
		this.element = element;
		this.anotation = parameter;
		this.location = location;
		this.parentCommand = parent;
		this.positional = element.getAnnotation(PositionalParameter.class);
		this.deprecated = processor.getElements().isDeprecated(element);
		this.docComment = processor.getElements().getDocComment(element);

		String[] names = parameter.value();
		if (names.length == 0) {
			if (this.positional == null) {
				names = new String[] { "-" + element.getSimpleName().toString() };
			} else {
				names = new String[] { element.getSimpleName().toString() };
			}
		}
		this.helpMetaNames = getDocCommentHelpMetaNames(docComment);
		for (String n : names) {
			this.names.add(n);
		}
		if (this.positional != null) {
			if (this.names.size() > 1) {
				throw new IllegalArgumentException(
						"Cannot specify multiple names for positional parameter: " + element);
			}
		}
		List<? extends VariableElement> methodparams = element.getParameters();
		Types types = processor.getTypes();
		if (methodparams.size() == 1) {
			this.parameterType = methodparams.get(0).asType();
		} else if (methodparams.size() == 2) {
			if (types.isAssignable(processor.getJavaLangString().asType(), methodparams.get(0).asType())
					&& types.isAssignable(processor.getJavaLangString().asType(), methodparams.get(1).asType())) {
				String keyname = Objects.toString(methodparams.get(0).getSimpleName(),
						DEFAULT_MAP_PARAMETER_FORMAT_KEY_NAME);
				String valname = Objects.toString(methodparams.get(1).getSimpleName(),
						DEFAULT_MAP_PARAMETER_FORMAT_VALUE_NAME);
				this.mapParameterFormatNames = new AbstractMap.SimpleEntry<>(keyname, valname);
				this.parameterType = null;
			} else {
				throw new IllegalArgumentException(
						"Invalid parameter method, expected dual string assignable parameters: " + element);
			}
		} else {
			throw new IllegalArgumentException(
					"Invalid parameter method, expected single or dual string parameters: " + element);
		}

		this.required = parameter.required();
		this.flag = element.getAnnotation(Flag.class);
		if (this.deprecated) {
			if (this.required) {
				throw new IllegalArgumentException("A required parameter cannot be deprecated: " + element);
			}
			if (this.positional != null) {
				throw new IllegalArgumentException(
						"A positional parameter cannot be deprecated as there is no way to make it optional: "
								+ element);
			}
		}
		Converter converterannot = element.getAnnotation(Converter.class);
		if (converterannot != null) {
			TypeElement methoddeclaringtype = processor.getTypeElement(converterannot::converter);
			String methodname = converterannot.method();
			this.converter = new ModelConverter(methoddeclaringtype.equals(processor.getConverterAnnot())
					? (TypeElement) element.getEnclosingElement()
					: methoddeclaringtype, methodname);
		}
		if (this.flag != null) {
			if (parameterType.getKind() != TypeKind.BOOLEAN
					&& !types.isSameType(parameterType, processor.getJavaLangBoolean().asType())) {
				throw new IllegalArgumentException(
						Flag.class.getSimpleName() + " annotation is only available for boolean types: " + element);
			}
			if (required) {
				throw new IllegalArgumentException("Flag annotated parameter cannot be required: " + element);
			}
		}
		if (mapParameterFormatNames != null) {
			if (this.positional != null) {
				throw new IllegalArgumentException("Map parameter cannot be positional: " + element);
			}
		}
	}

	public void resolve(CommandLineProcessor processor) {
		TypeMirror targettype = this.parameterType;
		if (this.converter == null) {
			if (multiParameter != null) {
				targettype = multiParameter.getElementType();
			}
			ModelCommonConverter commonconverter = processor.getCommonConverterForType(parentCommand, targettype);
			if (commonconverter != null) {
				this.converter = new ModelConverter(commonconverter);
			} else if (targettype != null && targettype.getKind() == TypeKind.DECLARED) {
				TypeElement fieldte = (TypeElement) ((DeclaredType) targettype).asElement();
				if (fieldte != null) {
					Converter elemconverter = fieldte.getAnnotation(Converter.class);
					if (elemconverter != null) {
						TypeElement methoddeclaringtype = processor.getTypeElement(elemconverter::converter);
						if (methoddeclaringtype.equals(processor.getConverterAnnot())) {
							methoddeclaringtype = fieldte;
						}
						String methodname = elemconverter.method();
						this.converter = new ModelConverter(methoddeclaringtype, methodname);
					}
				}
			}
		}
		this.docCommentFormat = CommandLineProcessor.getDocCommentTag(docComment, DOC_TAG_PARAMETER_FORMAT);
		if (docCommentFormat != null) {
			this.docCommentFormat = this.docCommentFormat.trim();

			if (this.docCommentFormat.isEmpty()) {
				this.docCommentFormat = null;
			}
		} else {
			this.docCommentFormat = findDocCommentFormat(processor, targettype, parentCommand);
		}
		if (this.docCommentFormat != null) {
			if (!CommandLineProcessor.isSingleLine(docCommentFormat)) {
				throw new IllegalArgumentException("Parameter format doc comment must be single line: "
						+ this.docCommentFormat + " on " + element);
			}
			if (mapParameterFormatNames != null) {
				throw new IllegalArgumentException(
						"Parameter doc comment format cannot be defined for map parameters: " + element);
			}
			this.docCommentFormat = CommandLineProcessor.unescapeDocComment(docCommentFormat);
		}
	}

	public String getDocCommentFormat() {
		return docCommentFormat;
	}

	public String getDocComment() {
		return docComment;
	}

	public Parameter getAnotation() {
		return anotation;
	}

	public Element getElement() {
		return element;
	}

	public ParameterLocation getLocation() {
		return location;
	}

	public boolean isMapParameter() {
		return mapParameterFormatNames != null;
	}

	public Entry<String, String> getMapParameterFormatNames() {
		return mapParameterFormatNames;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public Set<String> getNames() {
		return names;
	}

	public void setNames(Set<String> names) {
		this.names = names;
	}

	public TypeMirror getParameterType() {
		return parameterType;
	}

	public boolean isRequired() {
		return required;
	}

	public ModelConverter getConverter() {
		return converter;
	}

	public PositionalParameter getPositional() {
		return positional;
	}

	public Flag getFlag() {
		return flag;
	}

	public ModelMultiParameter getMultiParameter() {
		return multiParameter;
	}

	public boolean isMultiParameter() {
		return multiParameter != null || element.getKind() == ElementKind.METHOD;
	}

	public Set<String> getHelpMetaNames() {
		return helpMetaNames;
	}

	private static Set<String> getDocCommentHelpMetaNames(String doccomment) {
		if (doccomment == null) {
			return Collections.emptySet();
		}
		Iterator<String> it = CommandLineProcessor.getDocCommentTagIterator(doccomment, DOC_TAG_HELP_META);
		if (!it.hasNext()) {
			return Collections.emptySet();
		}
		LinkedHashSet<String> result = new LinkedHashSet<>();
		do {
			String n = it.next().trim();
			if (!n.isEmpty()) {
				result.add(n);
			}
		} while (it.hasNext());
		return result;
	}

	private String findDocCommentFormat(CommandLineProcessor processor, TypeMirror targettype, ModelCommand parent) {
		if (this.converter != null) {
			List<ExecutableElement> methods = processor.getMethodsWithName(this.converter.getMethodDeclaringType(),
					this.converter.getMethodName());
			String found = findDocCommentFormatInMethods(processor, methods);
			if (found != null) {
				return found;
			}
		}

		if (targettype == null) {
			return null;
		}
		switch (targettype.getKind()) {
			case BYTE: {
				return "<byte>";
			}
			case SHORT: {
				return "<short>";
			}
			case INT: {
				return "<int>";
			}
			case LONG: {
				return "<long>";
			}
			case CHAR: {
				return "<char>";
			}
			case BOOLEAN: {
				if (flag == null) {
					return "<boolean>";
				}
				return null;
			}
			case FLOAT: {
				return "<float>";
			}
			case DOUBLE: {
				return "<double>";
			}
			case DECLARED: {
				DeclaredType dt = (DeclaredType) targettype;
				TypeElement elem = (TypeElement) dt.asElement();
				if (elem == null) {
					break;
				}
				if (processor.getJavaLangBoolean().equals(elem)) {
					if (flag == null) {
						return "<boolean>";
					}
					return null;
				}
				if (processor.getJavaLangByte().equals(elem)) {
					return "<byte>";
				}
				if (processor.getJavaLangShort().equals(elem)) {
					return "<short>";
				}
				if (processor.getJavaLangInteger().equals(elem)) {
					return "<int>";
				}
				if (processor.getJavaLangLong().equals(elem)) {
					return "<long>";
				}
				if (processor.getJavaLangFloat().equals(elem)) {
					return "<float>";
				}
				if (processor.getJavaLangDouble().equals(elem)) {
					return "<double>";
				}
				if (processor.getJavaLangCharacter().equals(elem)) {
					return "<char>";
				}
				if (processor.getJavaLangString().equals(elem)) {
					return "<string>";
				}
				if (elem.getKind() == ElementKind.ENUM) {
					return "<enum>";
				}
				break;
			}
			default: {
				break;
			}
		}

		return null;
	}

	private static String findDocCommentFormatInMethods(CommandLineProcessor processor,
			List<ExecutableElement> methods) {
		Types types = processor.getTypes();
		TypeMirror parsingittype = processor.getParsingIteratorType().asType();
		for (ExecutableElement ee : methods) {
			List<? extends VariableElement> methodparams = ee.getParameters();
			if (methodparams.size() != 2) {
				//required params: String parametername, Iterator<? extends String> args
				continue;
			}
			if (types.isAssignable(parsingittype, methodparams.get(1).asType())) {
				String methodformattag = CommandLineProcessor
						.getDocCommentTag(processor.getElements().getDocComment(ee), DOC_TAG_PARAMETER_FORMAT);
				if (methodformattag != null) {
					return methodformattag.trim();
				}
			}
		}
		return null;
	}
}
