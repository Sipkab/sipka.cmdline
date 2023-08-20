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

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

import sipka.cmdline.api.Command;
import sipka.cmdline.api.CommonConverter;
import sipka.cmdline.api.Converter;
import sipka.cmdline.api.Flag;
import sipka.cmdline.processor.model.ModelBaseCommand;
import sipka.cmdline.processor.model.ModelCommand;
import sipka.cmdline.processor.model.ModelCommonConverter;
import sipka.cmdline.processor.model.ModelConverter;
import sipka.cmdline.processor.model.ModelMultiParameter;
import sipka.cmdline.processor.model.ModelParameter;
import sipka.cmdline.processor.model.ModelSubCommand;
import sipka.cmdline.runtime.ArgumentException;
import sipka.cmdline.runtime.MissingArgumentException;
import sipka.cmdline.runtime.ParseUtil;
import sipka.cmdline.runtime.ParsingIterator;
import sipka.cmdline.runtime.UnrecognizedArgumentException;

public class CommandLineProcessor implements Processor {
	private static final String COMMAND_FILE_PARAMETER_NAME = "@command-file";
	private static final String COMMAND_FILE_DELETE_PARAMETER_NAME = "@!delete!@command-file";

	private static final List<String> COMMAND_FILE_PARAMETER_DESCRIPTION_LINES = Arrays.asList(
			"File path prefixed with '@' to directly include arguments from the ",
			"specified file. Each argument is on its separate line. They are",
			"directly inserted in place of the " + COMMAND_FILE_PARAMETER_NAME + " argument. ",
			"The argument can appear anywhere on the command line. Escaping",
			"is not supported for arguments in the command file. ", "The file path may be absolute or relative.", "",
			"E.g: @path/to/arguments.txt");

	private static final List<String> COMMAND_FILE_DELETE_PARAMETER_DESCRIPTION_LINES = Arrays
			.asList("Same as @command-file but the file will be deleted after the arguments are parsed.");

	public static final String OPTION_GENERATE_HELP_INFO = "sipka.cmdline.help.generate";
	public static final String OPTION_HELP_LINE_LENGTH_ERROR_LIMIT = "sipka.cmdline.help.line.errorlimit";
	public static final String OPTION_GENERATE_HELP_REFERENCE = "sipka.cmdline.help.generate.reference";

	private static final String KEY_VALUE_HELP_APPENDIX = "<key>=<value>";

	private static final String DEPRECATED_HELP_FLAG = "[deprecated]";
	private static final String DEFAULT_COMMAND_HELP_FLAG = "[default]";
	private static final String REQUIRED_HELP_FLAG = "[required]";
	private static final String POSITIONAL_HELP_FLAG = "[positional]";
	private static final String MULTI_HELP_FLAG = "[multi]";

	private static final String HELP_FLAG_INDENT = "  ";
	private static final int HELP_FLAGS_MAX_LENGTH = 2 + 12;
	private static final int COLUMN_SPACE_WIDTH = 4;

	private static final String INDENTATION = "\t";

	private Elements elements;
	private Filer filer;
	private Types types;
	private Messager messager;

	private boolean supportsLambda;

	private TypeElement commandAnnot;
	private TypeElement converterAnnot;
	private TypeElement commonConverterAnnot;

	private TypeElement javaLangString;
	private TypeElement javaLangBoolean;
	private TypeElement javaLangCharacter;
	private TypeElement javaLangByte;
	private TypeElement javaLangShort;
	private TypeElement javaLangInteger;
	private TypeElement javaLangLong;
	private TypeElement javaLangFloat;
	private TypeElement javaLangDouble;

	private TypeElement parsingIteratorType;

	private TypeMirror collectionType;
	private TypeMirror mapType;

	private boolean generateHelpInfo = true;
	private boolean generateHelpReference = false;
	//TODO make command files configureable
	private boolean commandFileEnabled = true;
	private int helpLineErrorLimit = -1;
	private String parameterSeparatorLines = "\n";

	private Set<TypeElement> commandElements = new LinkedHashSet<>();

	public Elements getElements() {
		return elements;
	}

	public Types getTypes() {
		return types;
	}

	public Messager getMessager() {
		return messager;
	}

	public boolean isSupportsLambda() {
		return supportsLambda;
	}

	public TypeElement getParsingIteratorType() {
		return parsingIteratorType;
	}

	public TypeElement getJavaLangBoolean() {
		return javaLangBoolean;
	}

	public TypeElement getJavaLangByte() {
		return javaLangByte;
	}

	public TypeElement getJavaLangCharacter() {
		return javaLangCharacter;
	}

	public TypeElement getJavaLangDouble() {
		return javaLangDouble;
	}

	public TypeElement getJavaLangFloat() {
		return javaLangFloat;
	}

	public TypeElement getJavaLangInteger() {
		return javaLangInteger;
	}

	public TypeElement getJavaLangLong() {
		return javaLangLong;
	}

	public TypeElement getJavaLangShort() {
		return javaLangShort;
	}

	public TypeElement getJavaLangString() {
		return javaLangString;
	}

	@Override
	public Set<String> getSupportedOptions() {
		Set<String> supportedoptions = new TreeSet<>();
		supportedoptions.add(OPTION_GENERATE_HELP_INFO);
		supportedoptions.add(OPTION_HELP_LINE_LENGTH_ERROR_LIMIT);
		supportedoptions.add(OPTION_GENERATE_HELP_REFERENCE);
		return supportedoptions;
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> result = new TreeSet<>();
		result.add(Command.class.getName());
		return result;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}

	@Override
	public void init(ProcessingEnvironment processingEnv) {
		elements = processingEnv.getElementUtils();
		types = processingEnv.getTypeUtils();
		filer = processingEnv.getFiler();
		messager = processingEnv.getMessager();

		SourceVersion srcver;
		try {
			srcver = processingEnv.getSourceVersion();
			supportsLambda = srcver.compareTo(SourceVersion.RELEASE_7) > 0;
		} catch (Exception e) {
			if ("saker.java.compiler.api.processing.exc.SourceVersionNotFoundException"
					.equals(e.getClass().getName())) {
				//the source version enum is not available in this JVM
				//the name of the source version is the message of the exception
				String enumname = e.getMessage();
				if (!enumname.startsWith("RELEASE_")) {
					//can't interpret
					throw e;
				}
				try {
					int srcvernum = Integer.parseInt(enumname.substring(8));
					supportsLambda = srcvernum >= 8;
				} catch (NumberFormatException nfe) {
					e.addSuppressed(nfe);
					throw e;
				}
			} else {
				//unrecognized exception
				throw e;
			}
		}

		Map<String, String> procoptions = processingEnv.getOptions();
		String helpinfoarg = procoptions.get(OPTION_GENERATE_HELP_INFO);
		if (helpinfoarg != null) {
			generateHelpInfo = Boolean.parseBoolean(helpinfoarg);
		}
		String helpreferencearg = procoptions.get(OPTION_GENERATE_HELP_REFERENCE);
		if (helpreferencearg != null) {
			generateHelpReference = Boolean.parseBoolean(helpreferencearg);
		}
		String linerrorlimitarg = procoptions.get(OPTION_HELP_LINE_LENGTH_ERROR_LIMIT);
		if (linerrorlimitarg != null) {
			helpLineErrorLimit = Integer.parseUnsignedInt(linerrorlimitarg);
		}
	}

	public static String getSimpleClassNameFromQualified(String qname) {
		return qname.substring(qname.lastIndexOf('.') + 1);
	}

	public static String getPackageNameFromQualified(String qname) {
		int idx = qname.lastIndexOf('.');
		if (idx < 0) {
			return null;
		}
		return qname.substring(0, idx);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (commandAnnot == null) {
			commandAnnot = elements.getTypeElement(Command.class.getCanonicalName());
			converterAnnot = elements.getTypeElement(Converter.class.getCanonicalName());
			commonConverterAnnot = elements.getTypeElement(CommonConverter.class.getCanonicalName());

			javaLangString = elements.getTypeElement(String.class.getCanonicalName());
			javaLangBoolean = elements.getTypeElement(Boolean.class.getCanonicalName());
			javaLangCharacter = elements.getTypeElement(Character.class.getCanonicalName());
			javaLangByte = elements.getTypeElement(Byte.class.getCanonicalName());
			javaLangShort = elements.getTypeElement(Short.class.getCanonicalName());
			javaLangInteger = elements.getTypeElement(Integer.class.getCanonicalName());
			javaLangLong = elements.getTypeElement(Long.class.getCanonicalName());
			javaLangFloat = elements.getTypeElement(Float.class.getCanonicalName());
			javaLangDouble = elements.getTypeElement(Double.class.getCanonicalName());

			parsingIteratorType = elements.getTypeElement(ParsingIterator.class.getCanonicalName());
		}
		for (Element e : roundEnv.getElementsAnnotatedWith(commandAnnot)) {
			TypeElement te = (TypeElement) e;
			commandElements.add(te);
		}

		if (roundEnv.processingOver()) {
			//generate the sources
			Collection<TypeElement> cmdelements = new ArrayList<>(commandElements);
			commandElements.clear();
			for (TypeElement cmdelem : cmdelements) {
				ModelBaseCommand mc = new ModelBaseCommand(this, cmdelem.getAnnotation(Command.class), cmdelem);
				mc.resolve(this);
				Set<Element> dependentelements = new HashSet<>();
				addDependentElement(dependentelements, cmdelem);

				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					try (PrintStream ps = new PrintStream(new AutoIndentOutputStream(baos))) {
						generateForModel(mc, ps, dependentelements);
					}
					try (OutputStream os = filer
							.createSourceFile(mc.getGeneratedClassQualifiedName(),
									dependentelements.toArray(new Element[dependentelements.size()]))
							.openOutputStream()) {
						baos.writeTo(os);
					}
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				if (generateHelpReference) {
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
						try (DataOutputStream dos = new DataOutputStream(baos)) {
							//version
							dos.writeInt(1);
							generateHelpReference(mc, dos);
						}
						try (OutputStream os = filer
								.createResource(StandardLocation.locationFor("HELP_REFERENCE_OUTPUT"), "",
										mc.getGeneratedClassQualifiedName(),
										dependentelements.toArray(new Element[dependentelements.size()]))
								.openOutputStream()) {
							baos.writeTo(os);
						}
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			}
//			Set<TypeElement> allgenerated = new LinkedHashSet<>();
//			while (!cmdelements.isEmpty()) {
//				for (TypeElement te : cmdelements) {
//					if (allgenerated.add(te)) {
//						Set<Element> dependentelements = new LinkedHashSet<>();
//						Set<ExecutableElement> commandcalledmethods = new LinkedHashSet<>();
//
//						Command cmd = te.getAnnotation(Command.class);
//						boolean generatemain = cmd.main();
//						boolean generatecommand = cmd.withCommand();
//						if (generatemain && !generatecommand) {
//							throw new IllegalArgumentException("Cannot generate main without command. Set both to true to have a main method.");
//						}
//
//						String genclassname = cmd.className();
//						PackageElement packageof = elements.getPackageOf(te);
//						String qname = genclassname.isEmpty() ? getImplQualifiedTypeName(te) : genclassname;
//						String cname = getSimpleClassNameFromQualified(qname);
//
//						try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//							try (PrintStream ps = new PrintStream(new AutoIndentOutputStream(baos))) {
//								ps.println("package " + packageof.getQualifiedName() + ";");
//								ps.println();
//								ps.println("import " + ParseUtil.class.getCanonicalName() + ";");
//								ps.println();
//								ps.println("public class " + cname + " extends " + te.getQualifiedName() + " {");
//
//								String resulttype = generatecommand ? qname : te.getQualifiedName().toString();
//
//								ps.println("@SuppressWarnings({ \"fallthrough\", \"unused\" })");
//								ps.println("public static " + cname + " parse(java.util.Iterator<? extends String> arguments) {");
//								ps.println(ArgumentsIterator.class.getCanonicalName() + " args = ParseUtil.createCommandFileArgumentIterator(arguments);");
//								ps.println("String a;");
//								ps.println(resulttype + " result = new " + resulttype + "();");
//								{
//									List<Var> commandstack = new ArrayList<>();
//									Var resultvar = new Var(te, "result");
//									commandstack.add(resultvar);
//									writeParsingSwitch(ps, commandstack, te, resultvar, getCommonConverters(Collections.emptyMap(), te), dependentelements,
//											generatecommand, commandcalledmethods);
////									if (generatecommand) {
////										writeCommandCallerSetters(ps, commandstack, commandcalledmethods);
////									}
//								}
////								ps.println("return result;");
//								ps.println("}");
//
//								Set<TypeElement> throwntypes = getThrownTypes(commandcalledmethods);
//
//								ps.println("");
//								ps.println("public static " + cname + " parse(java.lang.Iterable<? extends String> args) {");
//								ps.println("return parse(args.iterator());");
//								ps.println("}");
//
//								ps.println("");
//								ps.println("public static " + cname + " parse(String... args) {");
//								ps.println("return parse(java.util.Arrays.asList(args).iterator());");
//								ps.println("}");
//
//								if (generatemain) {
//									ps.println("");
//									ps.print("public static void main(String... args)");
//									printThrowsTypes(ps, throwntypes);
//									ps.println(" {");
//									ps.println("parse(args).callCommand();");
//									ps.println("}");
//
//								}
//
//								if (!throwntypes.isEmpty()) {
//									ps.println("");
//									if (supportsLambda) {
//										ps.println("@FunctionalInterface");
//									}
//									ps.println("private interface Runnable {");
//									ps.print("public void run()");
//									printThrowsTypes(ps, throwntypes);
//									ps.println(";");
//									ps.println("}");
//								}
//								ps.println("");
//								ps.println("private Runnable subCommandCaller;");
//								ps.println("");
//
//								ps.println("private " + cname + "() {");
//								ps.println("}");
//								ps.println("");
//
//								ps.print("public void callCommand()");
//								printThrowsTypes(ps, throwntypes);
//								ps.println(" {");
//								ps.println("this.subCommandCaller.run();");
//								ps.println("}");
//
//								ps.println("}");
//							}
//							try (OutputStream os = filer.createSourceFile(qname, dependentelements.toArray(new Element[dependentelements.size()]))
//									.openOutputStream()) {
//								baos.writeTo(os);
//							}
//						} catch (IOException e) {
//							throw new UncheckedIOException(e);
//						}
//					}
//				}
//				if (commandElements.isEmpty()) {
//					break;
//				}
//				cmdelements.addAll(commandElements);
//				commandElements.clear();
//			}
		}
		return false;
	}

	private void generateHelpReference(ModelBaseCommand mc, DataOutput dos) throws IOException {
		LinkedList<ModelCommand> commandsstack = new LinkedList<>();
		commandsstack.addLast(mc);
		generateHelpReference(dos, mc, commandsstack);
	}

	private void generateHelpReference(DataOutput dos, ModelCommand cmd, LinkedList<ModelCommand> commandstack)
			throws IOException {
		generateCommandHelpReference(dos, commandstack);
		for (ModelSubCommand sub : cmd.getSubCommands()) {
			commandstack.addLast(sub);
			generateHelpReference(dos, sub, commandstack);
			commandstack.removeLast();
		}
	}

	private static void writeDataStringCollection(DataOutput os, Collection<String> coll) throws IOException {
		if (coll == null) {
			os.writeInt(0);
			return;
		}
		os.writeInt(coll.size());
		for (String s : coll) {
			os.writeUTF(s);
		}
	}

	private static void writeParameterReference(DataOutput dos, ModelParameter p, boolean positional)
			throws IOException {
		Set<String> flags = new TreeSet<>();
		if (positional) {
			flags.add("positional");
		}
		if (p.isRequired()) {
			flags.add("required");
		}
		if (p.isDeprecated()) {
			flags.add("deprecated");
		}
		if (p.isMultiParameter()) {
			flags.add("multi-parameter");
		}
		if (p.isMapParameter()) {
			flags.add("map-parameter");
		}
		Set<String> names = p.getNames();
		Set<String> metanames = p.getHelpMetaNames();
		String paramdoccomment = removeDocCommentTags(emptyIfNull(p.getDocComment()));
		String formatstr = emptyIfNull(p.getDocCommentFormat());

		writeDataStringCollection(dos, names);
		writeDataStringCollection(dos, flags);
		writeDataStringCollection(dos, metanames);
		dos.writeUTF(paramdoccomment);
		dos.writeUTF(formatstr);
	}

	private static void writeCommandFileParameterReference(DataOutput dos) throws IOException {
		writeDataStringCollection(dos, Collections.singleton(COMMAND_FILE_PARAMETER_NAME));
		writeDataStringCollection(dos, null); //flags
		writeDataStringCollection(dos, null); //metanames
		dos.writeUTF(String.join("\n", COMMAND_FILE_PARAMETER_DESCRIPTION_LINES));
		dos.writeUTF("");

		writeDataStringCollection(dos, Collections.singleton(COMMAND_FILE_DELETE_PARAMETER_NAME));
		writeDataStringCollection(dos, null); //flags
		writeDataStringCollection(dos, null); //metanames
		dos.writeUTF(String.join("\n", COMMAND_FILE_DELETE_PARAMETER_DESCRIPTION_LINES));
		dos.writeUTF("");
	}

	private void generateCommandHelpReference(DataOutput dos, List<ModelCommand> cmdlist) throws IOException {
		ModelCommand lastcmd = cmdlist.get(cmdlist.size() - 1);

		List<String> cmdpath = getCommandNamePath(cmdlist);

		//TODO sort non positional parameters by name?
		List<ModelParameter> parameters = lastcmd.getParameters();
		List<ModelParameter> posparams = lastcmd.getPositionalParameters();
		StringBuilder usagesb = new StringBuilder();
		appendUsageString(cmdlist, usagesb, null);

		String doccomment = removeDocCommentTags(emptyIfNull(lastcmd.getDocComment()));

		int paramcount = posparams.size();
		if (commandFileEnabled) {
			//two kinds of command file parameters
			paramcount += 2;
		}
		for (ModelParameter p : parameters) {
			if (p.getPositional() != null) {
				continue;
			}
			paramcount++;
		}

		writeDataStringCollection(dos, cmdpath);
		dos.writeUTF(usagesb.toString());
		dos.writeUTF(doccomment);
		dos.writeInt(paramcount);
		for (ModelParameter p : posparams) {
			if (p.getPositional().value() < 0) {
				continue;
			}
			writeParameterReference(dos, p, true);
		}
		for (ModelParameter p : parameters) {
			if (p.getPositional() != null) {
				continue;
			}
			writeParameterReference(dos, p, false);
		}
		for (ModelParameter p : posparams) {
			if (p.getPositional().value() >= 0) {
				continue;
			}
			writeParameterReference(dos, p, true);
		}
		if (commandFileEnabled) {
			writeCommandFileParameterReference(dos);
		}
	}

	private static List<String> getCommandNamePath(List<ModelCommand> cmdlist) {
		List<String> cmdpath = new ArrayList<>();
		Iterator<ModelCommand> cmdlistit = cmdlist.iterator();
		cmdlistit.next();
		while (cmdlistit.hasNext()) {
			ModelCommand c = cmdlistit.next();
			if (c instanceof ModelSubCommand) {
				ModelSubCommand sc = (ModelSubCommand) c;
				cmdpath.add(sc.getNames().iterator().next());
			}
		}
		return cmdpath;
	}

	private static int getLeadingSpaceCount(CharSequence cs) {
		int len = cs.length();
		for (int i = 0; i < len; i++) {
			if (cs.charAt(i) != ' ') {
				return i;
			}
		}
		return len;
	}

	private static int getMaxStringWidth(Iterable<? extends String> strings) {
		int res = 0;
		for (String s : strings) {
			res = Math.max(res, s.length());
		}
		return res;
	}

	private static int getMaxSubCommandNameWidth(Iterable<? extends ModelSubCommand> commands) {
		int res = HELP_FLAGS_MAX_LENGTH;
		for (ModelSubCommand cmd : commands) {
			for (String n : cmd.getNames()) {
				res = Math.max(n.length(), res);
			}
		}
		return res;
	}

	private static int getAllLeadingSpaceCount(Iterable<String> lines) {
		int c = Integer.MAX_VALUE;
		Iterator<String> it = lines.iterator();
		if (!it.hasNext()) {
			return 0;
		}
		while (it.hasNext()) {
			String l = it.next();
			int lsc = getLeadingSpaceCount(l);
			if (lsc == 0) {
				return 0;
			}
			if (lsc != l.length()) {
				c = Math.min(c, lsc);
			}
		}
		if (c == Integer.MAX_VALUE) {
			return 0;
		}
		return c;
	}

	private static String removeLeadingSpaceCount(String s, int spacecount) {
		if (spacecount > s.length()) {
			return "";
		}
		return s.substring(spacecount);
	}

	private static int getTrailingWhiteSpaceLength(String s) {
		int len = s.length();
		if (len == 0) {
			return 0;
		}
		int i = len - 1;
		for (; i >= 0; i--) {
			char ch = s.charAt(i);
			if (ch == ' ' || ch == '\t') {
				continue;
			}
			return len - (i + 1);
		}
		return len;
	}

	private static final String HELP_BLOCK_INDENT = "    ";

	private void appendHelpBlockParagraphed(StringBuilder sb, Iterable<String> leftcol, Iterable<String> rightcol) {
		for (String l : leftcol) {
			sb.append(HELP_BLOCK_INDENT);
			sb.append(l, 0, l.length() - getTrailingWhiteSpaceLength(l));
			sb.append('\n');
		}
		Iterator<String> rit = rightcol.iterator();
		if (!rit.hasNext()) {
			//append a single new line to explicitly signal that there is no doc for it
			sb.append('\n');
		} else {
			while (rit.hasNext()) {
				String l = rit.next();
				sb.append(HELP_BLOCK_INDENT + HELP_BLOCK_INDENT);
				sb.append(l, 0, l.length() - getTrailingWhiteSpaceLength(l));
				sb.append('\n');
			}
		}
		sb.append(parameterSeparatorLines);
	}

	private static void appendLinesBlock(StringBuilder sb, Iterable<String> lines) {
		int spacecount = getAllLeadingSpaceCount(lines);
		for (String l : lines) {
			l = removeLeadingSpaceCount(l, spacecount);
			sb.append(HELP_BLOCK_INDENT);
			sb.append(l);
			sb.append('\n');
		}
	}

	private static String emptyIfNull(String s) {
		if (s == null) {
			return "";
		}
		return s;
	}

	private static String getFirstSentence(String comment) {
		int len = comment.length();
		for (int i = 0; i < len; i++) {
			char c = comment.charAt(i);
			if (c == '.') {
				//end of sentence
				if (i + 1 >= len) {
					//the whole comment is a single sentence.
					return comment;
				}
				char nc = comment.charAt(i + 1);
				if (Character.isWhitespace(nc)) {
					return comment.substring(0, i + 1);
				}
				//the character after the comment is not whitespace. proceed to allow multiple sentences
			}
		}
		return comment;
	}

	private static List<String> splitToLines(String s) {
		//removes leading and trailing empty lines

		String[] split = s.split("\n");
		ArrayList<String> result = new ArrayList<>();
		int i = 0;
		for (; i < split.length; ++i) {
			String spl = split[i];
			if (spl.isEmpty()) {
				continue;
			}
			if (getLeadingSpaceCount(spl) == spl.length()) {
				continue;
			}
			break;
		}
		for (; i < split.length; ++i) {
			String spl = split[i];
			result.add(spl);
		}
		int size = result.size();
		for (int j = size - 1; j >= 0; j--) {
			String spl = result.get(j);
			if (spl.isEmpty() || getLeadingSpaceCount(spl) == spl.length()) {
				result.remove(j);
				continue;
			}
			break;
		}
		return result;
	}

	public static String getDocCommentTag(String doccomment, String tagname) {
		if (doccomment == null) {
			return null;
		}
		int len = doccomment.length();
		for (int i = 0; i < len; i++) {
			if (doccomment.charAt(i) != '@') {
				continue;
			}
			if (!doccomment.startsWith(tagname, i + 1)) {
				continue;
			}
			//found the place where the tagged doccomment starts
			int nextat = doccomment.indexOf('@', i + 1);
			if (nextat < 0) {
				nextat = len;
			}
			return doccomment.substring(i + 1 + tagname.length(), nextat);
		}
		return null;
	}

	public static Iterator<String> getDocCommentTagIterator(String doccomment, String tagname) {
		String attag = "@" + tagname;
		return new Iterator<String>() {
			private String next;
			private int nextSearchIndex = 0;
			{
				moveToNext();
			}

			private void moveToNext() {
				int idx = doccomment.indexOf(attag, nextSearchIndex);
				if (idx < 0) {
					next = null;
					return;
				}
				int nextatidx = doccomment.indexOf('@', idx + 1);
				if (nextatidx < 0) {
					next = doccomment.substring(idx + attag.length());
					nextSearchIndex = doccomment.length();
				} else {
					next = doccomment.substring(idx + attag.length(), nextatidx);
					nextSearchIndex = nextatidx;
				}
			}

			@Override
			public String next() {
				if (next == null) {
					throw new NoSuchElementException();
				}
				String res = next;
				moveToNext();
				return res;
			}

			@Override
			public boolean hasNext() {
				return next != null;
			}
		};
	}

	public static String removeDocCommentTags(String doccomment) {
		String result;
		int idx = doccomment.indexOf('@');
		if (idx < 0) {
			result = doccomment;
		} else {
			result = doccomment.substring(0, idx);
		}
		result = result.replace("\t", "    ").replace("<pre>", "").replace("</pre>", "");
		return result;
	}

	public static boolean isSingleLine(String cs) {
		return cs.indexOf('\n') < 0;
	}

	private void generatePrintHelpMethodNewImpl(PrintStream ps, List<ModelCommand> cmdlist,
			Set<Element> dependentelements) {
		ModelCommand lastcmd = cmdlist.get(cmdlist.size() - 1);
		ps.println("public static String getHelpString(");
		{
			int i = 1;
			for (Iterator<ModelCommand> it = cmdlist.iterator(); it.hasNext();) {
				ModelCommand cmd = it.next();
				dependentelements.add(cmd.getTypeElement());
				ps.print(INDENTATION + INDENTATION);
				ps.print(cmd.getTypeElement().getQualifiedName());
				ps.print(" cmd");
				ps.print(i);
				if (it.hasNext()) {
					ps.println(", ");
				}
				i++;
			}
		}
		ps.println(") {");

		{
			Collection<ModelSubCommand> subcommands = lastcmd.getSubCommands();
			//TODO sort non positional parameters by name?
			List<ModelParameter> parameters = lastcmd.getParameters();
			List<ModelParameter> posparams = lastcmd.getPositionalParameters();

			String doccomment = emptyIfNull(lastcmd.getDocComment());
			StringBuilder helpsb = new StringBuilder();
			{
				helpsb.append("Usage:\n");
				helpsb.append(HELP_BLOCK_INDENT);
				appendUsageString(cmdlist, helpsb, dependentelements);
				helpsb.append('\n');
				helpsb.append('\n');
			}
			if (!doccomment.isEmpty()) {
				helpsb.append("Description:\n");
				appendLinesBlock(helpsb, splitToLines(removeDocCommentTags(doccomment)));
			}

			if (!subcommands.isEmpty()) {
				ModelSubCommand defaultsubcommand = lastcmd.getDefaultSubCommand();
				helpsb.append("\nSubcommands:\n");
				int nameswidth = getMaxSubCommandNameWidth(subcommands);
				if (cmdlist.size() == 1) {
					ModelBaseCommand basecmd = (ModelBaseCommand) lastcmd;
					nameswidth = Math.max(getMaxStringWidth(basecmd.getHelpCommandName()), nameswidth);
				}
				for (ModelSubCommand sc : subcommands) {
					dependentelements.add(sc.getTypeElement());
					String subdoccomment = emptyIfNull(sc.getDocComment());
					Collection<String> names = new ArrayList<>(sc.getNames());
					if (sc.isDeprecated()) {
						names.add(HELP_FLAG_INDENT + DEPRECATED_HELP_FLAG);
					}
					if (sc == defaultsubcommand) {
						names.add(HELP_FLAG_INDENT + DEFAULT_COMMAND_HELP_FLAG);
					}
					appendHelpBlockParagraphed(helpsb, names,
							splitToLines(getFirstSentence(removeDocCommentTags(subdoccomment))));
				}

				if (cmdlist.size() == 1) {
					ModelBaseCommand basecmd = (ModelBaseCommand) lastcmd;
					Set<String> helpcommandname = basecmd.getHelpCommandName();
					if (!helpcommandname.isEmpty()) {
						helpcommandname = new LinkedHashSet<>(helpcommandname);
						removeSubCommandNamesFromCollection(helpcommandname, basecmd.getSubCommands());
						if (!helpcommandname.isEmpty()) {
							appendHelpBlockParagraphed(helpsb, helpcommandname,
									splitToLines("Prints help for the specified commands."));
						}
					}
				}
			}

			if (!parameters.isEmpty() || commandFileEnabled) {
				helpsb.append("\nParameters:\n");
				for (ModelParameter p : posparams) {
					if (p.getPositional().value() < 0) {
						continue;
					}
					dependentelements.add(p.getElement());
					String paramdoccomment = emptyIfNull(p.getDocComment());
					List<String> names = new ArrayList<>(p.getNames());
					names.add(HELP_FLAG_INDENT + POSITIONAL_HELP_FLAG);
					if (p.isRequired()) {
						names.add(HELP_FLAG_INDENT + REQUIRED_HELP_FLAG);
					}
					appendHelpBlockParagraphed(helpsb, names, splitToLines(removeDocCommentTags(paramdoccomment)));
				}
				for (ModelParameter p : parameters) {
					if (p.getPositional() != null) {
						continue;
					}
					dependentelements.add(p.getElement());
					String paramdoccomment = emptyIfNull(p.getDocComment());
					List<String> names = new ArrayList<>(p.getNames());
					if (p.isMapParameter()) {
						Entry<String, String> formatnames = p.getMapParameterFormatNames();
						ListIterator<String> it = names.listIterator();
						while (it.hasNext()) {
							it.set(it.next() + "<" + formatnames.getKey() + ">=<" + formatnames.getValue() + ">");
						}
					}
					String formatstr = p.getDocCommentFormat();
					if (formatstr != null && !formatstr.isEmpty()) {
						ListIterator<String> it = names.listIterator();
						while (it.hasNext()) {
							it.set(it.next() + " " + formatstr);
						}
					}
					if (p.isRequired()) {
						names.add(HELP_FLAG_INDENT + REQUIRED_HELP_FLAG);
					}
					if (p.isDeprecated()) {
						names.add(HELP_FLAG_INDENT + DEPRECATED_HELP_FLAG);
					}
					if (p.isMultiParameter()) {
						names.add(HELP_FLAG_INDENT + MULTI_HELP_FLAG);
					}
					for (String hmeta : p.getHelpMetaNames()) {
						names.add(HELP_FLAG_INDENT + hmeta);
					}
					appendHelpBlockParagraphed(helpsb, names, splitToLines(removeDocCommentTags(paramdoccomment)));
				}
				//TODO move positional parameters up so they're together before all other parameters
				for (ModelParameter p : posparams) {
					if (p.getPositional().value() >= 0) {
						continue;
					}
					dependentelements.add(p.getElement());
					String paramdoccomment = emptyIfNull(p.getDocComment());
					List<String> names = new ArrayList<>(p.getNames());
					names.add(HELP_FLAG_INDENT + POSITIONAL_HELP_FLAG);
					if (p.isRequired()) {
						names.add(HELP_FLAG_INDENT + REQUIRED_HELP_FLAG);
					}
					for (String hmeta : p.getHelpMetaNames()) {
						names.add(HELP_FLAG_INDENT + hmeta);
					}
					appendHelpBlockParagraphed(helpsb, names, splitToLines(removeDocCommentTags(paramdoccomment)));
				}
				if (commandFileEnabled) {
					appendHelpBlockParagraphed(helpsb, Collections.singleton(COMMAND_FILE_PARAMETER_NAME),
							COMMAND_FILE_PARAMETER_DESCRIPTION_LINES);
					appendHelpBlockParagraphed(helpsb, Collections.singleton(COMMAND_FILE_DELETE_PARAMETER_NAME),
							COMMAND_FILE_DELETE_PARAMETER_DESCRIPTION_LINES);
				}
			}

			int helplen = helpsb.length();
			ps.println("return");
			if (helplen == 0) {
				ps.println("\t  \"\"");
			} else {
				int lastlineend = 0;
				for (int i = 0; i < helplen; i++) {
					char c = helpsb.charAt(i);
					if (c == '\n') {
						if (lastlineend == 0) {
							ps.print(INDENTATION + "  ");
						} else {
							ps.print(INDENTATION + "+ ");
						}
						String substr = unescapeDocComment(helpsb.substring(lastlineend, i + 1));
						if (helpLineErrorLimit > 0 && substr.length() > helpLineErrorLimit) {
							messager.printMessage(Diagnostic.Kind.ERROR,
									"Help line limit exceeded with length: " + substr.length() + " with " + substr);
						}
						ps.println(elements.getConstantExpression(substr));
						lastlineend = i + 1;
					}
				}
				if (lastlineend < helplen) {
					if (lastlineend == 0) {
						ps.print(INDENTATION + "  ");
					} else {
						ps.print(INDENTATION + "+ ");
					}
					String substr = unescapeDocComment(helpsb.substring(lastlineend));
					if (helpLineErrorLimit > 0 && substr.length() > helpLineErrorLimit) {
						messager.printMessage(Diagnostic.Kind.ERROR,
								"Help line limit exceeded with length: " + substr.length() + " with " + substr);
					}
					ps.println(elements.getConstantExpression(substr + "\n"));
				}
			}
			ps.println(";");

		}

		ps.println("}");
		ps.println("");
	}

	private void appendUsageString(List<ModelCommand> cmdlist, StringBuilder helpsb, Set<Element> dependentelements) {
		ModelCommand lastcmd = cmdlist.get(cmdlist.size() - 1);
		//printing usage
		boolean hadarg = false;
		Iterator<ModelCommand> cmdlistit = cmdlist.iterator();
		cmdlistit.next();
		while (cmdlistit.hasNext()) {
			ModelCommand c = cmdlistit.next();
			if (c instanceof ModelSubCommand) {
				ModelSubCommand sc = (ModelSubCommand) c;
				if (hadarg) {
					helpsb.append(' ');
				}
				hadarg = true;
				helpsb.append(sc.getNames().iterator().next());
			}
		}
		List<ModelParameter> parameters = lastcmd.getParameters();
		if (!parameters.isEmpty() || commandFileEnabled) {
			List<ModelParameter> posparams = lastcmd.getPositionalParameters();
			for (ModelParameter p : posparams) {
				if (p.getPositional().value() < 0) {
					continue;
				}
				if (hadarg) {
					helpsb.append(' ');
				}
				hadarg = true;
				helpsb.append(p.getNames().iterator().next());
				if (!p.isRequired()) {
					helpsb.append('?');
				}
			}
			for (ModelParameter p : parameters) {
				if (p.getPositional() != null) {
					continue;
				}
				if (hadarg) {
					helpsb.append(' ');
				}
				hadarg = true;
				helpsb.append("[parameters]");
				break;
			}
			if (commandFileEnabled) {
				if (hadarg) {
					helpsb.append(' ');
				}
				hadarg = true;
				helpsb.append("[" + COMMAND_FILE_PARAMETER_NAME + "]");
			}
			for (ModelParameter p : posparams) {
				if (p.getPositional().value() >= 0) {
					continue;
				}
				if (hadarg) {
					helpsb.append(' ');
				}
				hadarg = true;
				helpsb.append(p.getNames().iterator().next());
				if (!p.isRequired()) {
					helpsb.append('?');
				}
			}
		}
		if (!lastcmd.getSubCommands().isEmpty()) {
			if (hadarg) {
				helpsb.append(' ');
			}
			hadarg = true;
			TypeElement cmdtypeelem = lastcmd.getTypeElement();
			addDependentElement(dependentelements, cmdtypeelem);
			if (getMethodsWithName(cmdtypeelem, "call").isEmpty()) {
				//this command has no call method, the subcommand is not optional
				helpsb.append("[subcommand] ...");
			} else {
				helpsb.append("[subcommand?] ...");
			}
		}
	}

	private static void addDependentElement(Set<Element> dependentelements, TypeElement cmdtypeelem) {
		if (dependentelements == null) {
			return;
		}
		dependentelements.add(cmdtypeelem);
	}

	public static String unescapeDocComment(String s) {
		return s.replace("&nbsp;", " ").replace("&gt;", ">").replace("&lt;", "<").replace("&#064;", "@");
	}

	private void generatePrintHelpMethod(PrintStream ps, ModelCommand cmd, Set<Element> dependentelements) {
		LinkedList<ModelCommand> commandsstack = new LinkedList<>();
		commandsstack.addLast(cmd);
		generatePrintHelpMethod(ps, cmd, commandsstack, dependentelements);
	}

	private void generatePrintHelpMethod(PrintStream ps, ModelCommand cmd, LinkedList<ModelCommand> commandstack,
			Set<Element> dependentelements) {
		generatePrintHelpMethodNewImpl(ps, commandstack, dependentelements);
		for (ModelSubCommand sub : cmd.getSubCommands()) {
			commandstack.addLast(sub);
			generatePrintHelpMethod(ps, sub, commandstack, dependentelements);
			commandstack.removeLast();
		}
	}

	private void generateParserMethod(PrintStream ps, ModelBaseCommand cmd, Collection<TypeElement> throwntypes,
			Set<Element> dependentelements) {
		LinkedList<ModelCommand> commandsstack = new LinkedList<>();
		commandsstack.addLast(cmd);
		generateParserMethod(ps, cmd, commandsstack, throwntypes, dependentelements);
	}

	private void writeParameterParsing(PrintStream ps, ModelParameter parameter, String thisvarname, ModelCommand cmd,
			Set<Element> dependentelements) {
		dependentelements.add(parameter.getElement());
		ModelConverter converter = parameter.getConverter();

		TypeMirror targettype;
		ModelMultiParameter multiparameter = parameter.getMultiParameter();
		Consumer<String> callwriter;
		if (multiparameter == null) {
			targettype = parameter.getParameterType();
			callwriter = call -> {
				ps.print(thisvarname);
				parameter.getLocation().printAccess(ps);

				ElementKind parameterelementkind = parameter.getElement().getKind();
				switch (parameterelementkind) {
					case FIELD: {
						ps.print(" = ");
						ps.print(call);
						ps.println(";");
						break;
					}
					case METHOD: {
						ps.print("(");
						ps.print(call);
						ps.println(");");
						break;
					}
					default: {
						throw new AssertionError("Unknown parameter element type: " + parameterelementkind);
					}
				}
			};
		} else {
			targettype = multiparameter.getElementType();
			callwriter = call -> {
				ps.print(thisvarname);
				parameter.getLocation().printAccess(ps);
				ps.print(".");
				ps.print(multiparameter.getMethodName());
				ps.print("(");
				ps.print(call);
				ps.println(");");
			};
		}
		if (converter != null) {
			//use the converter to convert the value
			dependentelements.add(converter.getMethodDeclaringType());
			callwriter.accept(getConverterParameterParseCall(converter));
			return;
		}
		String commonconvertercall = getParameterParsingCallWithCommonConverters(ps, cmd, targettype,
				dependentelements);
		if (commonconvertercall != null) {
			callwriter.accept(commonconvertercall);
			return;
		}
		TypeKind typekind = targettype.getKind();
		switch (typekind) {
			case BYTE: {
				callwriter.accept(getByteParseCall());
				break;
			}
			case SHORT: {
				callwriter.accept(getShortParseCall());
				break;
			}
			case INT: {
				callwriter.accept(getIntParseCall());
				break;
			}
			case LONG: {
				callwriter.accept(getLongParseCall());
				break;
			}
			case CHAR: {
				callwriter.accept(getCharacterParseCall());
				break;
			}
			case BOOLEAN: {
				callwriter.accept(getBooleanParseCall(parameter.getFlag()));
				break;
			}
			case FLOAT: {
				callwriter.accept(getFloatParseCall());
				break;
			}
			case DOUBLE: {
				callwriter.accept(getDoubleParseCall());
				break;
			}

			case DECLARED: {
				DeclaredType dt = (DeclaredType) targettype;
				TypeElement elem = (TypeElement) dt.asElement();
				if (elem == null) {
					throw new IllegalArgumentException("Failed to write parser for: " + parameter.getElement());
				}
				addDependentElement(dependentelements, elem);
				if (javaLangBoolean.equals(elem)) {
					callwriter.accept(getBooleanParseCall(parameter.getFlag()));
				} else if (javaLangByte.equals(elem)) {
					callwriter.accept(getByteParseCall());
				} else if (javaLangShort.equals(elem)) {
					callwriter.accept(getShortParseCall());
				} else if (javaLangInteger.equals(elem)) {
					callwriter.accept(getIntParseCall());
				} else if (javaLangLong.equals(elem)) {
					callwriter.accept(getLongParseCall());
				} else if (javaLangFloat.equals(elem)) {
					callwriter.accept(getFloatParseCall());
				} else if (javaLangDouble.equals(elem)) {
					callwriter.accept(getDoubleParseCall());
				} else if (javaLangCharacter.equals(elem)) {
					callwriter.accept(getCharacterParseCall());
				} else if (javaLangString.equals(elem)) {
					callwriter.accept(getStringParseCall());
				} else if (elem.getKind() == ElementKind.ENUM) {
					callwriter.accept(getEnumParseCall(elem));
				} else {
//					Converter elemconverter = elem.getAnnotation(Converter.class);
//					if (elemconverter != null) {
//						TypeElement methoddeclaringtype = getTypeElement(elemconverter::converter);
//						if (methoddeclaringtype.equals(converterAnnot)) {
//							methoddeclaringtype = elem;
//						}
//						dependentelements.add(methoddeclaringtype);
//						String methodname = elemconverter.method();
//						callwriter.accept(methoddeclaringtype.getQualifiedName() + "." + methodname + "(args)");
//					} else {
					throw new IllegalArgumentException(
							"Failed to write parser for: " + parameter.getNames() + " : " + parameter.getElement());
//					}
				}
				break;
			}
			default: {
				throw new IllegalArgumentException(
						"Failed to write parser for: " + parameter.getNames() + " : " + parameter.getElement());
			}
		}
	}

	public ModelCommonConverter getCommonConverterForType(ModelCommand cmd, TypeMirror targettype) {
		if (targettype == null) {
			return null;
		}
		for (ModelCommonConverter cc : cmd.getCommonConverters()) {
			if (types.isSameType(cc.getTargetType(), targettype)) {
				return cc;
			}
		}
		ModelCommand parent = cmd.getParentCommand();
		if (parent != null) {
			return getCommonConverterForType(parent, targettype);
		}
		return null;
	}

	public String getParameterParsingCallWithCommonConverters(PrintStream ps, ModelCommand cmd, TypeMirror targettype,
			Set<Element> dependentelements) {
		ModelCommonConverter cc = getCommonConverterForType(cmd, targettype);
		if (cc != null) {
			dependentelements.add(cc.getMethodDeclaringType());
			return cc.getMethodDeclaringType().getQualifiedName() + "." + cc.getMethodName() + "(a, args)";
		}
		return null;
	}

	private static String getConverterParameterParseCall(ModelConverter converter) {
		return converter.getMethodDeclaringType().getQualifiedName() + "." + converter.getMethodName() + "(a, args)";
	}

	private static void removeSubCommandNamesFromCollection(Collection<String> coll,
			Iterable<? extends ModelSubCommand> subcommands) {
		for (ModelSubCommand sc : subcommands) {
			coll.removeAll(sc.getNames());
		}
	}

	private void writeFindHelpStringCode(PrintStream ps, ModelCommand currentcmd,
			LinkedList<ModelCommand> commandstack) {
		Collection<ModelSubCommand> subcommands = currentcmd.getSubCommands();
		if (subcommands.isEmpty()) {
			writeReturnGetHelpString(ps, commandstack);
			return;
		}
		ps.println("if (!args.hasNext()) {");
		writeReturnGetHelpString(ps, commandstack);
		ps.println("}");

		ps.println("a = args.next();");
		for (ModelSubCommand sc : subcommands) {
			Set<String> names = sc.getNames();
			ps.print("if (");
			for (Iterator<String> it = names.iterator(); it.hasNext();) {
				String hcn = it.next();
				ps.print(elements.getConstantExpression(hcn));
				ps.print(".equals(a)");
				if (it.hasNext()) {
					ps.print(" || ");
				}
			}
			ps.println(") {");
			commandstack.addLast(sc);
			writeFindHelpStringCode(ps, sc, commandstack);
			commandstack.removeLast();
			ps.println("}");
		}
		ps.print("return \"No subcommand found with name: \" + a + \"\\n\\n\" + ");
		writeGetHelpStringCall(ps, commandstack);
		ps.println(";");

	}

	private static void writeReturnGetHelpString(PrintStream ps, LinkedList<ModelCommand> commandstack) {
		ps.print("return ");
		writeGetHelpStringCall(ps, commandstack);
		ps.println(";");
	}

	private static void writeGetHelpStringCall(PrintStream ps, LinkedList<ModelCommand> commandstack) {
		ps.println("getHelpString(");
		for (Iterator<ModelCommand> it = commandstack.iterator(); it.hasNext();) {
			ModelCommand argcmd = it.next();
			ps.print(INDENTATION + "(");
			ps.print(argcmd.getCommandClassQualifiedName());
			ps.print(") null");
			if (it.hasNext()) {
				ps.println(", ");
			}
		}
		ps.println();
		ps.print(")");
	}

	private void generateParserMethod(PrintStream ps, ModelCommand cmd, LinkedList<ModelCommand> commandstack,
			Collection<TypeElement> throwntypes, Set<Element> dependentelements) {
		ModelBaseCommand first = (ModelBaseCommand) commandstack.getFirst();
		if (generateHelpInfo && commandstack.size() == 1) {
			ps.println("private static String findHelpString(java.util.Iterator<String> args) {");
			ps.println("String a;");
			writeFindHelpStringCode(ps, first, new LinkedList<>(commandstack));
			ps.println("}");
			ps.println("");
		}

		ps.println("private static void parse(" + ParsingIterator.class.getCanonicalName() + " args, ");
		String thisvarname = null;
		{
			String currentvarname = "result";
			for (Iterator<ModelCommand> it = commandstack.iterator(); it.hasNext();) {
				ModelCommand argcmd = it.next();
				dependentelements.add(argcmd.getTypeElement());
				if (argcmd == cmd) {
					thisvarname = currentvarname;
				}
				ps.print(INDENTATION + INDENTATION);
				ps.print(argcmd.getCommandClassQualifiedName());
				ps.print(" ");
				ps.print(currentvarname);
				if (it.hasNext()) {
					ps.println(", ");
				}
				currentvarname = "sub" + currentvarname;
			}
		}
		if (thisvarname == null) {
			throw new AssertionError("Failed to determine command var name.");
		}
		final String fthisvarname = thisvarname;
		ps.println(") {");
		ps.println("String a;");
		Collection<ModelSubCommand> cmdsubcommands = cmd.getSubCommands();
		if (generateHelpInfo && commandstack.size() == 1) {

			Set<String> helpcommandname = first.getHelpCommandName();
			if (!helpcommandname.isEmpty()) {
				helpcommandname = new LinkedHashSet<>(helpcommandname);
				removeSubCommandNamesFromCollection(helpcommandname, cmdsubcommands);
				if (!helpcommandname.isEmpty()) {
					ps.println("if (args.hasNext()) {");
					{
						ps.println("a = args.peek();");
						ps.print("if (");
						for (Iterator<String> it = helpcommandname.iterator(); it.hasNext();) {
							String hcn = it.next();
							ps.print(elements.getConstantExpression(hcn));
							ps.print(".equals(a)");
							if (it.hasNext()) {
								ps.print(" || ");
							}
						}
						ps.println(") {");
						ps.println("args.next();");

						ps.println("String helpstr = findHelpString(args);");

						ps.print("result.subCommandCaller = ");
						printRunnableLambda(ps, () -> {
							ps.println("System.out.println(helpstr);");
						});
						ps.println(";");
						ps.println("return;");
						ps.println("}");
					}
					ps.println("}");
				}
			}
		}

		{
			List<ModelParameter> reqparams = cmd.getRequiredParameters();

			int reqcount = reqparams.size();
			if (!reqparams.isEmpty()) {
				int i = 0;
				while (i < reqcount) {
					ps.println("long requires" + (i / 64) + " = 0;");
					i += 64;
				}
			}
			ModelSubCommand defaultcommand = cmd.getDefaultSubCommand();

			ps.println("parse_block:");
			ps.println("{");

			List<ModelParameter> cmdparameters = cmd.getParameters();
			List<ModelParameter> posparams = cmd.getPositionalParameters();
			NavigableMap<String, ModelParameter> cmdmapparameters = cmd.getMapParameters();

			int positionalindex = 0;
			if (!posparams.isEmpty() && posparams.get(0).getPositional().value() >= 0) {
				ps.println("positional_block:");
				ps.println("{");
				for (ModelParameter posparam : posparams) {
					if (posparam.getPositional().value() < 0) {
						break;
					}
					ps.println("if (!args.hasNext()) { ");
					ps.println("break positional_block;");
					ps.println("}");
					ps.print("a = ");
					ps.print(elements.getConstantExpression(posparam.getNames().iterator().next()));
					ps.println(";");
					printRequiredAssign(ps, reqparams, posparam);
					writeParameterParsing(ps, posparam, thisvarname, cmd, dependentelements);
					positionalindex++;
				}
				//positional_block end:
				ps.println("}");
			}

			{
				ps.println("param_loop:");
				ps.println("while (args.hasNext()) {");
				ps.println("a = args.peek();");

				for (Entry<String, ModelParameter> entry : cmdmapparameters.entrySet()) {
					ModelParameter param = entry.getValue();
					String prefix = entry.getKey();
					ps.print("if (");
					ps.println("a.startsWith(" + elements.getConstantExpression(prefix) + ")) {");

					printRequiredAssign(ps, reqparams, param);

					ps.println("String[] mapkeyvalue = { null, null };");
					ps.println("ParseUtil.parseEqualsFormatArgument(" + prefix.length() + ", a, mapkeyvalue);");
					ps.print(thisvarname);
					param.getLocation().printAccess(ps);

					ElementKind parameterelementkind = param.getElement().getKind();
					switch (parameterelementkind) {
						case FIELD: {
							ps.println(".put(mapkeyvalue[0], mapkeyvalue[1]);");
							break;
						}
						case METHOD: {
							ps.println("(mapkeyvalue[0], mapkeyvalue[1]);");
							break;
						}
						default: {
							throw new AssertionError("Unknown parameter element type: " + parameterelementkind);
						}
					}

					ps.println("args.next();");
					ps.println("continue param_loop;");

					ps.println("}");
				}

				{
					ps.println("try {");
					ps.println("switch (a) {");

					for (ModelParameter param : cmdparameters) {
						if (param.isMapParameter() || param.getPositional() != null) {
							continue;
						}
						Set<String> names = param.getNames();
						writeCaseLabels(ps, names);
						ps.println(" {");
						ps.println("args.next();");
						printRequiredAssign(ps, reqparams, param);
						writeParameterParsing(ps, param, thisvarname, cmd, dependentelements);
						ps.println("break;");
						ps.println("}");
					}

					for (ModelSubCommand sc : cmdsubcommands) {
						dependentelements.add(sc.getTypeElement());
						if (sc == defaultcommand) {
							continue;
						}
						Set<String> names = sc.getNames();
						writeCaseLabels(ps, names);
						ps.println(" {");

						ps.println("args.next();");

						ps.print("parse(args");
						String currentvarname = "result";
						for (@SuppressWarnings("unused")
						ModelCommand c : commandstack) {
							ps.print(", ");
							ps.print(currentvarname);
							currentvarname = "sub" + currentvarname;
						}
						ps.print(", new " + sc.getCommandClassQualifiedName() + "()");
						ps.println(");");

						ps.println("break parse_block;");
						ps.println("}");
					}

					if (defaultcommand != null) {
						writeCaseLabels(ps, defaultcommand.getNames());
						ps.println();
						ps.println(INDENTATION + "args.next();");
						ps.println(INDENTATION + "//fall-through");
					}
					ps.println("default: {");
					if (defaultcommand != null) {
						ps.println("break param_loop;");
					} else {
						if (positionalindex < posparams.size()) {
							//has more positional parameters, do not throw the exception
							ps.println("break param_loop;");
						} else {
							ps.println("throw new " + UnrecognizedArgumentException.class.getCanonicalName()
									+ "(\"Unrecognized argument\", a);");
						}
					}
					ps.println("}"); // default:

					ps.println("}"); // switch

					ps.println("} catch (" + ArgumentException.class.getCanonicalName() + " e) {");
					//rethrow
					ps.println("throw e;");
					ps.println("} catch (" + RuntimeException.class.getCanonicalName() + " e) {");
					ps.println("throw new " + ArgumentException.class.getCanonicalName()
							+ "(\"Failed to interpret the argument(s)\", e, a);");
					ps.println("}"); // catch

				}
				ps.println("}");
				if (defaultcommand != null) {
					ps.print("parse(args");
					String currentvarname = "result";
					for (@SuppressWarnings("unused")
					ModelCommand c : commandstack) {
						ps.print(", ");
						ps.print(currentvarname);
						currentvarname = "sub" + currentvarname;
					}
					ps.print(", new " + defaultcommand.getCommandClassQualifiedName() + "()");
					ps.println(");");
				} else {
					if (positionalindex < posparams.size()) {
						ps.println("end_positional_block:");
						ps.println("{");
						for (int n = posparams.size(); positionalindex < n; positionalindex++) {
							ModelParameter posparam = posparams.get(positionalindex);
							ps.println("if (!args.hasNext()) { ");
							ps.println("break end_positional_block;");
							ps.println("}");
							ps.print("a = ");
							ps.print(elements.getConstantExpression(posparam.getNames().iterator().next()));
							ps.println(";");
							printRequiredAssign(ps, reqparams, posparam);
							writeParameterParsing(ps, posparam, thisvarname, cmd, dependentelements);
						}
						//end_positional_block end:
						ps.println("}");
						ps.println("if (args.hasNext()) {");
						ps.println("throw new " + UnrecognizedArgumentException.class.getCanonicalName()
								+ "(\"Unrecognized argument\", args.peek());");
						ps.println("}");
					}

					TypeElement cmdtypeelem = cmd.getTypeElement();
					List<ExecutableElement> callmethods = getMethodsWithName(cmdtypeelem, "call");
					List<ModelCommand> querycommands = commandstack.subList(0, commandstack.size() - 1);
					ModelMethodCall callmethodcall = getBestMethodMatch(callmethods, querycommands);
					if (callmethodcall == null) {
						ps.print("result.subCommandCaller = ");
						printRunnableLambda(ps, () -> {
							ps.println("throw new UnsupportedOperationException(\"no call method in "
									+ cmdtypeelem.getQualifiedName() + "\");");
						});
						ps.println(";");
					} else {
						ps.print("result.subCommandCaller = ");
						printRunnableLambda(ps, () -> {
							int initi = 0;
							String initvarname = "result";
							while (initi < commandstack.size()) {
								List<ModelCommand> initquerycommands = commandstack.subList(0, initi);
								ModelMethodCall call = getBestMethodMatch(
										getMethodsWithName(commandstack.get(initi).getTypeElement(), "init"),
										initquerycommands);
								if (call != null) {
									dependentelements.add(call.executable);
									printMethodCallWithCommandResultArguments(ps, initvarname, call);
									addThrownTypes(call.executable, throwntypes);
								}
								initvarname = "sub" + initvarname;
								++initi;
							}
							dependentelements.add(callmethodcall.executable);
							printMethodCallWithCommandResultArguments(ps, fthisvarname, callmethodcall);
							addThrownTypes(callmethodcall.executable, throwntypes);
							int closei = commandstack.size();
							String closevarname = fthisvarname;
							while (closei-- > 0) {
								List<ModelCommand> closequerycommands = commandstack.subList(0, closei);
								ModelMethodCall call = getBestMethodMatch(
										getMethodsWithName(commandstack.get(closei).getTypeElement(), "close"),
										closequerycommands);
								if (call != null) {
									dependentelements.add(call.executable);
									printMethodCallWithCommandResultArguments(ps, closevarname, call);
									addThrownTypes(call.executable, throwntypes);
								}
								closevarname = closevarname.substring(3);
							}
						});
						ps.println(";");
					}
				}

				//parse_block end:
				ps.println("}");
			}
			if (!reqparams.isEmpty()) {
				int i = 0;
				while (i < reqcount) {
					long checkflag;
					int diff = reqcount - i;
					if (diff >= 64) {
						checkflag = 0xFFFFFFFFFFFFFFFFL;
					} else {
						checkflag = (1 << diff) - 1;
					}
					String cfconst = toHexLongConstantString(checkflag);
					ps.println("if ((requires" + (i / 64) + " & " + cfconst + ") != " + cfconst + ") {");
					//TODO print info about which parameters are missing
					ps.println("throw new " + MissingArgumentException.class.getCanonicalName()
							+ "(\"Required parameters missing.\");");
					ps.println("}");
					i += 64;
				}
			}
		}
		ps.println("}");
		ps.println("");
		for (ModelSubCommand sub : cmdsubcommands) {
			commandstack.addLast(sub);
			generateParserMethod(ps, sub, commandstack, throwntypes, dependentelements);
			commandstack.removeLast();
		}
	}

	private static void printMethodCallWithCommandResultArguments(PrintStream ps, String thisvarname,
			ModelMethodCall cmethod) {
		ps.print(thisvarname);
		ps.print(".");
		ps.print(cmethod.executable.getSimpleName());
		ps.print("(");
		for (Iterator<Integer> it = cmethod.argumentQueryIndices.iterator(); it.hasNext();) {
			Integer argidx = it.next();
			int subc = argidx;
			while (subc-- > 0) {
				ps.print("sub");
			}
			ps.print("result");
			if (it.hasNext()) {
				ps.print(", ");
			}
		}
		ps.println(");");
	}

	private static void printRequiredAssign(PrintStream ps, List<ModelParameter> reqparams, ModelParameter param) {
		if (param.isRequired()) {
			int bitidx = reqparams.indexOf(param);
			String cfconst = toHexLongConstantString(1L << (bitidx % 64));
			ps.println("requires" + (bitidx / 64) + " |= " + cfconst + ";");
		}
	}

	private static String toHexLongConstantString(long checkflag) {
		String cfconst = "0x" + Long.toUnsignedString(checkflag, 16) + "L";
		return cfconst;
	}

	private void generateForModel(ModelBaseCommand mc, PrintStream ps, Set<Element> dependentelements) {
		String packname = mc.getGeneratedPackageName();
		if (packname != null) {
			ps.println("package " + packname + ";");
			ps.println();
		}
		ps.println("import " + ParseUtil.class.getCanonicalName() + ";");
		ps.println();
		String cname = mc.getGeneratedSimpleClassName();
		ps.println("@SuppressWarnings({ \"fallthrough\" })");
		ps.println("public class " + cname + " extends " + mc.getTypeElement().getQualifiedName() + " {");
		{
			Collection<TypeElement> throwntypes = new LinkedHashSet<>();

			ps.println("public static " + cname + " parse(java.util.Iterator<? extends String> arguments) {");
			{
				String iteratorcreatorfunctionname;
				if (commandFileEnabled) {
					iteratorcreatorfunctionname = "createCommandFileArgumentIterator";
				} else {
					iteratorcreatorfunctionname = "createSimpleArgumentIterator";
				}
				ps.println("try (" + ParsingIterator.class.getCanonicalName() + " args = ParseUtil."
						+ iteratorcreatorfunctionname + "(arguments)) {");
				ps.println(cname + " result = new " + cname + "();");
				ps.println("parse(args, result);");
				ps.println("return result;");
				ps.println("} catch (java.io.IOException e) {");
				ps.println("throw new " + UncheckedIOException.class.getCanonicalName()
						+ "(\"Failed to close argument iterator.\", e);");
				ps.println("}");
			}
			ps.println("}");
			ps.println("");

			if (generateHelpInfo) {
				generatePrintHelpMethod(ps, mc, dependentelements);
			}
			generateParserMethod(ps, mc, throwntypes, dependentelements);

			if (mc.createMainMethod()) {
				ps.print("public static void main(String... args)");
				printThrowsTypes(ps, throwntypes);
				ps.println(" {");
				ps.println("parse(java.util.Arrays.asList(args).iterator()).callCommand();");
				ps.println("}");
				ps.println("");
			}

			if (!throwntypes.isEmpty()) {
				if (supportsLambda) {
					ps.println("@FunctionalInterface");
				}
				ps.println("private interface Runnable {");
				ps.print("public void run()");
				printThrowsTypes(ps, throwntypes);
				ps.println(";");
				ps.println("}");
				ps.println("");
			}
			ps.println("private Runnable subCommandCaller;");
			ps.println("");

			ps.println("private " + cname + "() {");
			ps.println("}");
			ps.println("");

			ps.print("public void callCommand()");
			printThrowsTypes(ps, throwntypes);
			ps.println(" {");
			ps.println("this.subCommandCaller.run();");
			ps.println("}");
		}
		ps.println("}");
	}

	public TypeElement getCommandAnnot() {
		return commandAnnot;
	}

	public TypeElement getConverterAnnot() {
		return converterAnnot;
	}

	public TypeElement getCommonConverterAnnot() {
		return commonConverterAnnot;
	}

	private static void printThrowsTypes(PrintStream ps, Iterable<? extends TypeElement> types) {
		Iterator<? extends TypeElement> it = types.iterator();
		if (!it.hasNext()) {
			return;
		}
		ps.println();
		ps.print(INDENTATION);
		ps.println("throws ");
		while (it.hasNext()) {
			TypeElement tt = it.next();
			ps.print(INDENTATION);
			ps.print(INDENTATION);
			ps.print(tt.getQualifiedName());
			if (it.hasNext()) {
				ps.println(",");
			}
		}
		//TODO exec builder throws
	}

	private static void addThrownTypes(ExecutableElement ee, Collection<TypeElement> result) {
		for (TypeMirror tt : ee.getThrownTypes()) {
			if (tt.getKind() == TypeKind.DECLARED) {
				DeclaredType dt = (DeclaredType) tt;
				Element e = dt.asElement();
				if (e instanceof TypeElement) {
					result.add((TypeElement) e);
				}
			}

		}
	}

	public String getImplQualifiedTypeName(TypeElement te) {
		return elements.getPackageOf(te).getQualifiedName() + "." + te.getSimpleName() + "Impl";
	}

	public TypeMirror getErasedMapType() {
		if (mapType == null) {
			mapType = types.erasure(elements.getTypeElement(Map.class.getCanonicalName()).asType());
		}
		return mapType;
	}

	public TypeMirror getErasedCollectionType() {
		if (collectionType == null) {
			collectionType = types.erasure(elements.getTypeElement(Collection.class.getCanonicalName()).asType());
		}
		return collectionType;
	}

	private String getEnumParseCall(TypeElement enumtype) {
		String call;
		if (isUpperCaseEnum(enumtype)) {
			call = "ParseUtil.parseEnumUpperCaseArgument(" + enumtype.getQualifiedName() + ".class, a, args)";
		} else {
			call = "ParseUtil.parseEnumArgument(" + enumtype.getQualifiedName() + ".class, a, args)";
		}
		return call;
	}

	private static String getStringParseCall() {
		return "ParseUtil.parseStringArgument(a, args)";
	}

	private static String getFloatParseCall() {
		return "ParseUtil.parseFloatArgument(a, args)";
	}

	private static String getDoubleParseCall() {
		return "ParseUtil.parseDoubleArgument(a, args)";
	}

	private static String getBooleanParseCall(Flag flag) {
		String call;
		if (flag != null) {
			call = !flag.negate() + "";
		} else {
			call = "ParseUtil.parseBooleanArgument(a, args)";
		}
		return call;
	}

	private static String getCharacterParseCall() {
		return "ParseUtil.parseCharacterArgument(a, args)";
	}

	private static String getLongParseCall() {
		return "ParseUtil.parseLongArgument(a, args)";
	}

	private static String getIntParseCall() {
		return "ParseUtil.parseIntegerArgument(a, args)";
	}

	private static String getShortParseCall() {
		return "ParseUtil.parseShortArgument(a, args)";
	}

	private static String getByteParseCall() {
		return "ParseUtil.parseByteArgument(a, args)";
	}

	private Map<TypeElement, Boolean> uppercaseEnums = new LinkedHashMap<>();

	private boolean isUpperCaseEnum(TypeElement te) {
		return uppercaseEnums.computeIfAbsent(te, CommandLineProcessor::isUpperCaseEnumImpl);
	}

	private static boolean isUpperCaseEnumImpl(TypeElement te) {
		for (Element e : te.getEnclosedElements()) {
			if (e.getKind() == ElementKind.ENUM_CONSTANT) {
				String name = e.getSimpleName().toString();
				if (!name.equals(name.toUpperCase(Locale.ENGLISH))) {
					return false;
				}
			}
		}
		return true;
	}

	private void writeCaseLabels(PrintStream ps, Iterable<? extends String> name) {
		for (Iterator<? extends String> it = name.iterator(); it.hasNext();) {
			String n = it.next();
			ps.print("case ");
			ps.print(elements.getConstantExpression(n));
			if (it.hasNext()) {
				ps.println(":");
			} else {
				ps.print(":");
			}
		}
	}

	private static class ModelMethodCall {
		protected ExecutableElement executable;
		protected List<Integer> argumentQueryIndices = new ArrayList<>();

		public ModelMethodCall(ExecutableElement executable) {
			this.executable = executable;
		}

	}

	private ModelMethodCall getBestMethodMatch(List<ExecutableElement> methods, List<ModelCommand> queryparams) {
		if (methods.isEmpty()) {
			return null;
		}
		ModelMethodCall result = null;
		ExecutableElement ambiguity = null;

		int queryparamssize = queryparams.size();
		method_finder:
		for (ExecutableElement ee : methods) {
			List<? extends VariableElement> params = ee.getParameters();
			int paramssize = params.size();
			if (paramssize > queryparamssize) {
				continue;
			}
			if (result != null && paramssize < result.argumentQueryIndices.size()) {
				continue;
			}
			Iterator<? extends ModelCommand> qit = queryparams.iterator();
			Iterator<? extends VariableElement> pit = params.iterator();

			ModelMethodCall resultcandidate = new ModelMethodCall(ee);

			int qidx = -1;

			param_matcher:
			while (pit.hasNext()) {
				if (!qit.hasNext()) {
					continue method_finder;
				}
				VariableElement p = pit.next();
				do {
					++qidx;
					TypeElement qte = qit.next().getTypeElement();
					if (types.isAssignable(qte.asType(), p.asType())) {
						resultcandidate.argumentQueryIndices.add(qidx);
						continue param_matcher;
					}
				} while (qit.hasNext());
				continue method_finder;
			}
			if (result == null) {
				result = resultcandidate;
			} else {
				int cmp = Integer.compare(paramssize, result.argumentQueryIndices.size());
				if (cmp > 0) {
					result = resultcandidate;
					ambiguity = null;
				} else if (cmp == 0) {
					ambiguity = ee;
				}
			}
		}
		if (ambiguity != null) {
			throw new IllegalArgumentException("Ambiguous call methods: " + result.executable + " - " + ambiguity
					+ " for parameters " + queryparams + " in " + result.executable.getEnclosingElement() + " - "
					+ ambiguity.getEnclosingElement());
		}
		return result;
	}

	private void printRunnableLambda(PrintStream ps, Runnable content) {
		if (supportsLambda) {
			ps.println("() -> {");
			content.run();
			ps.print("}");
		} else {
			ps.println("new Runnable() {");
			ps.println("@Override");
			ps.print("public void run()");
			//TODO include thrown exceptions here
			ps.println(" {");
			content.run();
			ps.println("}");
			ps.print("}");
		}
	}

	public List<ExecutableElement> getMethodsWithName(TypeElement te, String name) {
		List<ExecutableElement> result = new ArrayList<>();
		addMethodsWithName(te, name, result, te);
		return result;
	}

	private void addMethodsWithName(TypeElement te, String name, Collection<ExecutableElement> result,
			TypeElement searchingtype) {
		enumeration_loop:
		for (Element e : te.getEnclosedElements()) {
			if (e.getKind() == ElementKind.METHOD) {
				if (e.getSimpleName().contentEquals(name)) {
					ExecutableElement ee = (ExecutableElement) e;
					for (ExecutableElement present : result) {
						if (elements.overrides(present, ee, searchingtype)) {
							continue enumeration_loop;
						}
					}
					result.add(ee);
				}
			}
		}
		TypeElement sc = getSuperClass(te);
		if (sc != null) {
			addMethodsWithName(sc, name, result, te);
		}
	}

	public TypeMirror getTypeMirror(Supplier<Class<?>> classsupplier) {
		try {
			return elements.getTypeElement(classsupplier.get().getCanonicalName()).asType();
		} catch (MirroredTypeException e) {
			TypeMirror tm = e.getTypeMirror();
			return tm;
		}
	}

	public Collection<? extends TypeElement> getTypeElements(Supplier<Class<?>[]> classsupplier) {
		try {
			Class<?>[] classes = classsupplier.get();
			Collection<TypeElement> result = new LinkedHashSet<>();
			for (Class<?> c : classes) {
				result.add(elements.getTypeElement(c.getCanonicalName()));
			}
			return result;
		} catch (MirroredTypesException e) {
			Collection<TypeElement> result = new LinkedHashSet<>();
			for (TypeMirror tm : e.getTypeMirrors()) {
				if (tm.getKind() == TypeKind.DECLARED) {
					DeclaredType dt = (DeclaredType) tm;
					TypeElement te = (TypeElement) dt.asElement();
					if (te != null) {
						result.add(te);
					}
				}
			}
			return result;
		}

	}

	public TypeElement getTypeElement(Supplier<Class<?>> classsupplier) {
		try {
			return elements.getTypeElement(classsupplier.get().getCanonicalName());
		} catch (MirroredTypeException e) {
			TypeMirror tm = e.getTypeMirror();
			TypeKind kind = tm.getKind();
			if (kind == TypeKind.DECLARED) {
				DeclaredType dt = (DeclaredType) tm;
				return (TypeElement) dt.asElement();
			}
			throw new IllegalArgumentException("Failed to determine type. (" + kind + " : " + tm + ")", e);
		}
	}

	public static TypeElement getTypeElementOf(VariableElement var) {
		TypeMirror t = var.asType();
		if (t.getKind() == TypeKind.DECLARED) {
			DeclaredType dt = (DeclaredType) t;
			return (TypeElement) dt.asElement();
		}
		return null;
	}

	private static TypeElement getSuperClass(TypeElement te) {
		TypeMirror sc = te.getSuperclass();
		if (sc.getKind() == TypeKind.DECLARED) {
			Element superte = ((DeclaredType) sc).asElement();
			if (superte instanceof TypeElement) {
				return (TypeElement) superte;
			}
		}
		return null;
	}

	@Override
	public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation,
			ExecutableElement member, String userText) {
		return Collections.emptyList();
	}

}
