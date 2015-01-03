/*
 * Copyright (C) 2010 The Project Lombok Authors.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.ast.template;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;
import javax.tools.JavaFileObject;
import javax.tools.Diagnostic.Kind;

import lombok.Data;

@SupportedAnnotationTypes({"lombok.ast.template.GenerateAstNode", "lombok.ast.template.SyntaxCheck", "lombok.ast.template.ParentAccessor"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TemplateProcessor extends AbstractProcessor {
	private static final Pattern COUNT_FINDER = Pattern.compile("^(.*?)(-?\\d+)$");
	
	private SyntaxValidityCheckerGenerator validityGenerator;
	
	@Data
	static class ParentRelation {
		/** The full name of the method to be generated. */
		private final String methodName;
		/** The reverse check needs to use the list accessor. */
		private final boolean list;
		/** The reverse check needs to call this method to check if 'this' is equal to it. */
		private final String methodThatShouldGiveThis;
		/** The method will be generated here. */
		private final String typeNameFrom;
		/** The method will return this type. */
		private final String typeNameTo;
		
		private static String calcRelName(Element e) {
			for (AnnotationMirror ann : e.getAnnotationMirrors()) {
				if (!ann.getAnnotationType().toString().equals(ParentAccessor.class.getName())) continue;
				
				for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : ann.getElementValues().entrySet()) {
					String v = (String)entry.getValue().getValue();
					if (v != null) return v;
				}
				return "";
			}
			return null;
		}
		
		public static ParentRelation create(ExecutableElement method) {
			String methodThatShouldGiveThis = method.getSimpleName().toString();
			
			/* Calling rawFoo() is marginally more useful than calling astFoo(), but rawFoo() may not exist. Let's check. */
			if (methodThatShouldGiveThis.startsWith("ast")) {
				String alt = "raw" + methodThatShouldGiveThis.substring("ast".length());
				for (Element sibling : method.getEnclosingElement().getEnclosedElements()) {
					if (sibling instanceof ExecutableElement) {
						ExecutableElement siblingMethod = (ExecutableElement)sibling;
						if (siblingMethod.getParameters().isEmpty() && siblingMethod.getSimpleName().toString().equals(alt)) {
							methodThatShouldGiveThis = alt;
							break;
						}
					}
				}
			}
			
			return create(method, method.getReturnType(), method.getEnclosingElement().asType(), methodThatShouldGiveThis);
		}
		
		public static ParentRelation create(VariableElement field) {
			String methodThatShouldGiveThis = field.getAnnotation(ForcedType.class) != null ? "ast" : "raw";
			methodThatShouldGiveThis += TemplateProcessor.titleCasedName(field.getSimpleName().toString());
			
			return create(field, field.asType(), field.getEnclosingElement().asType(), methodThatShouldGiveThis);
		}
		
		private static ParentRelation create(Element source, TypeMirror from, TypeMirror to, String methodThatShouldGiveThis) {
			String relName = calcRelName(source);
			if (relName == null) return null;
			
			String fromStr = TemplateProcessor.getType(from, true);
			String toStr = TemplateProcessor.getType(to, true);
			if (toStr.endsWith("Template")) toStr = toStr.substring(0, toStr.length() - "Template".length());
			String methodName = relName.isEmpty() ? ("upTo" + toStr) : ("upIf" + relName + "To" + toStr);
			return new ParentRelation(methodName, TemplateProcessor.isList(from), methodThatShouldGiveThis, fromStr, toStr);
		}
	}
	
	private static boolean isList(TypeMirror t) {
		if (t instanceof DeclaredType) {
			String r = t.toString();
			return r.startsWith("java.util.List<") || r.startsWith("List<") ||
					r.startsWith("lombok.ast.StrictListAccessor<") || r.startsWith("StrictListAccessor<") ||
					r.startsWith("lombok.ast.RawListAccessor<") || r.startsWith("RawListAccessor<");
		}
		
		return false;
	}
	
	private static String getType(TypeMirror t, boolean delist) {
		String result = t.toString();
		
		if (delist && isList(t)) {
			result = ((DeclaredType)t).getTypeArguments().get(0).toString();
		}
		
		if (result.startsWith("lombok.ast.")) {
			String rest = result.substring("lombok.ast.".length());
			return rest.indexOf('.') == -1 ? rest : result;
		}
		return result;
	}
	
	@Data
	static class FieldData {
		/** Name of the field */
		private final String name;
		/** The order of this field. */
		private final int count;
		/** If {@code true} then this field would always contain a non-null value in a legal AST. */
		private final boolean mandatory;
		/** Terminals such as string literals may have their own internal parser. If so, this field contains name of method that goes from raw to value. */
		private final String rawFormParser;
		/** Reverse of {@code rawFormParer} - contains method that goes from value to raw. If this is set, {@code rawFormParser} is set and vice versa. */
		private final String rawFormGenerator;
		/** The java code that forms the expression that assigns the initial value of this field. Always set. */
		private final String initialValue;
		/** The javax.model mirror class representing this field. */
		private final VariableElement element;
		/** If {@code true}, this field's type is an AST Node type, and thus assignments to it should update children array, etcetera. */
		private final boolean astNode;
		/** If {@code true}, no setter should be generated, probably because the template has its own. */
		private final boolean suppressSetter;
		/** If {@code true}, there will be no rawFieldName() method, only an astFieldName() method. */
		private final boolean forcedType;
		/**
		 * Never set if {@code astNode} is {@code true}, but may be set otherwise. Contains the expression required to create a copy of the field.
		 * @see NotChildOfNode#codeToCopy()
		 */
		private final String codeToCopy;
		
		/**
		 * Returns the type of this field in a way that you can legally put into the generated java file. For Lists of X, returns X.
		 */
		public String getType() {
			return TemplateProcessor.getType(element.asType(), isAstNode());
		}
		
		public boolean isList() {
			return isAstNode() && TemplateProcessor.isList(element.asType());
		}
		
		FieldData(VariableElement field) {
			String name = String.valueOf(field.getSimpleName());
			Matcher m = COUNT_FINDER.matcher(name);
			if (m.matches()) {
				this.name = m.group(1);
				this.count = Integer.parseInt(m.group(2));
			} else {
				this.name = name;
				this.count = 0;
			}
			Mandatory mandatory = field.getAnnotation(Mandatory.class);
			this.element = field;
			this.mandatory = mandatory != null;
			NotChildOfNode ncon = field.getAnnotation(NotChildOfNode.class);
			this.astNode = ncon == null;
			this.rawFormParser = astNode ? "" : ncon.rawFormParser();
			this.rawFormGenerator = astNode ? "" : ncon.rawFormGenerator();
			if (field.getAnnotation(ForcedType.class) != null) {
				this.forcedType = true;
			} else {
				if (ncon != null && rawFormParser.isEmpty()) this.forcedType = true;
				else this.forcedType = false;
			}
			/* grab initial value */ {
				if (mandatory == null || mandatory.value().isEmpty()) {
					this.initialValue = defaultInitialValueFor(field.asType());
				} else {
					if (ncon != null) this.initialValue = mandatory.value();
					else this.initialValue = String.format("adopt(%s)", mandatory.value());
				}
			}
			this.suppressSetter = ncon != null && ncon.suppressSetter();
			if (ncon != null) {
				this.codeToCopy = ncon.codeToCopy().isEmpty() ? ("this." + this.name) : ncon.codeToCopy();
			} else {
				this.codeToCopy = null;
			}
		}
		
		private String defaultInitialValueFor(TypeMirror asType) {
			String defaultValue = getDefaultValueForType(asType.toString());
			return defaultValue == null ? "null" : defaultValue;
		}
		
		String titleCasedName() {
			return TemplateProcessor.titleCasedName(name);
		}
	}
	
	private static String titleCasedName(String in) {
		String n = in.replace("_", "");
		Matcher m = COUNT_FINDER.matcher(n);
		if (m.matches()) n = m.group(1);
		return n.isEmpty() ? "" : Character.toTitleCase(n.charAt(0)) + n.substring(1);
	}
	
	/**
	 * Turns "X.class" into just "X", otherwise just calls toString().
	 */
	private static String getClassName(Object type) {
		String c = type.toString();
		return c.endsWith(".class") ? c.substring(0, c.length() - ".class".length()) : c;
	}
	
	@Override public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		validityGenerator = new SyntaxValidityCheckerGenerator(processingEnv);
	}
	
	@Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		handleSyntaxCheck(roundEnv);
		Relations parentRelations = generateParentRelations(roundEnv);
		handleGenerateAstNode(roundEnv, parentRelations);
		
		return true;
	}
	
	private static class Relations {
		private Map<String, Collection<ParentRelation>> map = new HashMap<String, Collection<ParentRelation>>();
		
		void put(String key, ParentRelation value) {
			if (key.startsWith("lombok.ast.")) key = key.substring("lombok.ast.".length());
			Collection<ParentRelation> c = map.get(key);
			if (c == null) {
				c = new HashSet<ParentRelation>();
				map.put(key, c);
			}
			c.add(value);
		}
		
		Collection<ParentRelation> get(String key) {
			if (key.startsWith("lombok.ast.")) key = key.substring("lombok.ast.".length());
			Collection<ParentRelation> c = map.get(key);
			return c == null ? Collections.<ParentRelation>emptyList() : c;
		}
	}
	
	private Relations generateParentRelations(RoundEnvironment roundEnv) {
		Relations out = new Relations();
		for (Element element : roundEnv.getElementsAnnotatedWith(ParentAccessor.class)) {
			if (element.getKind() == ElementKind.FIELD) {
				ParentRelation rel = ParentRelation.create((VariableElement)element);
				if (rel != null) out.put(rel.getTypeNameFrom(), rel);
			}
			if (element.getKind() == ElementKind.METHOD) {
				ParentRelation rel = ParentRelation.create((ExecutableElement)element);
				if (rel != null) out.put(rel.getTypeNameFrom(), rel);
			}
		}
		/** In a full build, the above is fine, but when incrementally building for example only Templates.java, the round does not include interfaces with @ParentAccessor annotations. */
		final Set<TypeMirror> typesToScan = new HashSet<TypeMirror>();
		for (Element element : roundEnv.getElementsAnnotatedWith(GenerateAstNode.class)) {
			for (AnnotationMirror m : element.getAnnotationMirrors()) {
				if (!m.getAnnotationType().toString().endsWith("GenerateAstNode")) continue;
				for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : m.getElementValues().entrySet()) {
					if (e.getKey().getSimpleName().toString().equals("implementing")) {
						e.getValue().accept(new SimpleAnnotationValueVisitor6<Void, Void>() {
							@Override public Void visitArray(List<? extends AnnotationValue> vals, Void p) {
								for (AnnotationValue inner : vals) inner.accept(this, null);
								return null;
							}
							
							@Override public Void visitType(TypeMirror t, Void p) {
								typesToScan.add(t);
								return null;
							}
						}, null);
					}
				}
			}
		}
		for (TypeMirror tm : typesToScan) {
			Element toScan = processingEnv.getTypeUtils().asElement(tm);
			if (toScan != null) for (Element child : toScan.getEnclosedElements()) {
				if (child.getKind() == ElementKind.FIELD) {
					ParentRelation rel = ParentRelation.create((VariableElement)child);
					if (rel != null) out.put(rel.getTypeNameFrom(), rel);
				}
				if (child.getKind() == ElementKind.METHOD) {
					ParentRelation rel = ParentRelation.create((ExecutableElement)child);
					if (rel != null) out.put(rel.getTypeNameFrom(), rel);
				}
			}
		}
		return out;
	}
	
	/** Searches for methods and classes annotated with @SyntaxCheck and adds them to the appropriate list. */
	private void handleSyntaxCheck(RoundEnvironment roundEnv) {
		int added = 0;
		for (Element element : roundEnv.getElementsAnnotatedWith(SyntaxCheck.class)) {
			if (element.getKind() == ElementKind.CLASS) {
				for (Element member : ((TypeElement)element).getEnclosedElements()) {
					if (member.getKind() == ElementKind.METHOD) {
						ExecutableElement method = (ExecutableElement)member;
						if (method.getModifiers().contains(Modifier.PUBLIC)) {
							added++;
							validityGenerator.recordCheckMethod((ExecutableElement)member);
						}
					}
				}
			} else if (element.getKind() == ElementKind.METHOD) {
				added++;
				validityGenerator.recordCheckMethod((ExecutableElement)element);
			} else {
				processingEnv.getMessager().printMessage(Kind.ERROR, "@SyntaxCheck is only supported on methods and classes", element);
			}
		}
		
		if (added == 0) return;
		if (validityGenerator.isFinished()) {
			processingEnv.getMessager().printMessage(Kind.ERROR, "@SyntaxCheck is not legal in code generated by annotation processors");
		} else {
			validityGenerator.finish();
		}
	}
	
	private void handleGenerateAstNode(RoundEnvironment roundEnv, Relations parentRelations) {
		for (Element element : roundEnv.getElementsAnnotatedWith(GenerateAstNode.class)) {
			if (element.getKind() != ElementKind.CLASS) {
				processingEnv.getMessager().printMessage(Kind.ERROR, "@GenerateAstNode is only supported on plain classes", element);
				continue;
			}
			
			List<FieldData> fields = new ArrayList<FieldData>();
			List<ExecutableElement> methodsToCopy = new ArrayList<ExecutableElement>();
			
			String className;
			String extending = null;
			List<String> implementing = new ArrayList<String>();
			List<TypeElement> bases = new ArrayList<TypeElement>();
			
			TypeElement annotated;
			/* Calculate file and class name of the source file we need to generate */ {
				annotated = (TypeElement)element;
				bases.add(annotated);
				className = annotated.getQualifiedName().toString();
				if (className.endsWith("Template")) className = className.substring(0, className.length() - "Template".length());
				else {
					processingEnv.getMessager().printMessage(Kind.ERROR, "@GenerateAstNode annotated classes must end in 'Template'. Example: IfTemplate");
					return;
				}
			}
			
			/* Pick up the parameters of the annotation (this is the type that the class to be generated must extend / implement) */ {
				for (AnnotationMirror annotation : annotated.getAnnotationMirrors()) {
					if (!annotation.getAnnotationType().toString().equals(GenerateAstNode.class.getName())) continue;
					
					for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation.getElementValues().entrySet()) {
						if (entry.getKey().getSimpleName().contentEquals("extending")) {
							extending = getClassName(entry.getValue().getValue());
						}
						
						if (entry.getKey().getSimpleName().contentEquals("implementing")) {
							Collection<?> list = (Collection<?>)entry.getValue().getValue();
							for (Object type : list) implementing.add(getClassName(type));
						}
						
						if (entry.getKey().getSimpleName().contentEquals("mixin")) {
							Collection<?> list = (Collection<?>)entry.getValue().getValue();
							for (Object type : list) bases.add(
									processingEnv.getElementUtils().getTypeElement(getClassName(type)));
						}
					}
					
					if (extending == null) extending = "lombok.ast.AbstractNode";
				}
			}
			
			/* Analyze all fields of template class and mixins */ {
				Set<String> covered = new HashSet<String>();
				for (TypeElement base : bases) {
					for (Element enclosed : base.getEnclosedElements()) {
						if (enclosed.getKind() != ElementKind.FIELD) continue;
						if (((VariableElement)enclosed).getModifiers().contains(Modifier.STATIC)) continue;
						FieldData fieldData = new FieldData((VariableElement) enclosed);
						if (!covered.add(fieldData.getName())) continue;
						fields.add(fieldData);
					}
				}
			}
			
			Collections.sort(fields, new Comparator<FieldData>() {
				@Override public int compare(FieldData f1, FieldData f2) {
					int c1 = f1.getCount();
					int c2 = f2.getCount();
					if (c1 < c2) return -1;
					if (c1 > c2) return +1;
					return f1.getName().compareTo(f2.getName());
				}
			});
			
			/* Analyze all methods of template class and mixins */ {
				Set<String> covered = new HashSet<String>();
				for (TypeElement base : bases) {
					for (Element enclosed : base.getEnclosedElements()) {
						if (enclosed.getKind() != ElementKind.METHOD) continue;
						ExecutableElement method = (ExecutableElement) enclosed;
						boolean copyMethod = method.getAnnotation(CopyMethod.class) != null;
						if (!copyMethod) continue;
						StringBuilder sig = new StringBuilder();
						sig.append(method.getReturnType().toString());
						sig.append(" ");
						sig.append(method.getSimpleName().toString());
						sig.append("(");
						for (VariableElement param : method.getParameters()) {
							sig.append(param.asType().toString());
							sig.append(", ");
						}
						sig.append(")");
						if (!covered.add(sig.toString())) continue;
						methodsToCopy.add(method);
					}
				}
			}
			
			try {
				validityGenerator.recordFieldDataForCheck(className, fields);
				generateSourceFile(annotated, className, extending, implementing, fields, methodsToCopy, parentRelations);
			} catch (IOException e) {
				processingEnv.getMessager().printMessage(Kind.ERROR, String.format(
						"Can't generate sourcefile %s: %s",
						className + "Template", e), annotated);
			}
		}
	}
	
	private void generateSourceFile(Element originatingElement, String className, String extending, List<String> implementing, List<FieldData> fields,
			List<ExecutableElement> methodsToCopy, Relations relations) throws IOException {
		
		JavaFileObject file = processingEnv.getFiler().createSourceFile(className, originatingElement);
		Writer out = file.openWriter();
		out.write("//Generated by lombok.ast.template.TemplateProcessor. DO NOT EDIT, DO NOT CHECK IN!\n\n");
		
		String pkgName, typeName; {
			int idx = className.lastIndexOf('.');
			if (idx == -1) {
				pkgName = null;
				typeName = className;
			} else {
				pkgName = className.substring(0, idx);
				typeName = className.substring(idx+1);
			}
		}
		
		if (pkgName != null) {
			out.write("package ");
			out.write(pkgName);
			out.write(";\n\n");
		}
		
		out.write("public class ");
		out.write(typeName);
		if (extending != null) {
			out.write(" extends ");
			out.write(extending);
		}
		if (implementing != null && !implementing.isEmpty()) {
			out.write(" implements ");
			boolean first = true;
			for (String impl : implementing) {
				if (!first) out.write(", ");
				first = false;
				out.write(impl);
			}
		}
		
		out.write(" {\n");
		for (FieldData field : fields) {
			if (field.isList()) {
				generateFieldForList(out, className, typeName, fields.size(), field);
				continue;
			}
			
			if (!field.isAstNode()) {
				generateFieldsForBasic(out, field);
				continue;
			}
			
			generateFieldsForNode(out, field);
		}
		
		out.write("\t\n");
		List<ParentRelation> parentRelations = new ArrayList<ParentRelation>();
		parentRelations.addAll(relations.get(className));
		if (extending != null) parentRelations.addAll(relations.get(extending));
		if (implementing != null) for (String impl : implementing) parentRelations.addAll(relations.get(impl));
		
		for (ParentRelation relation : parentRelations) {
			generateUpMethod(out, relation);
		}
		
		if (!parentRelations.isEmpty()) out.write("\t\n");
		
		for (FieldData field : fields) {
			if (field.isList()) {
				generateListAccessor(out, className, field);
				
				continue;
			}
			
			if (!field.isAstNode()) {
				generateFairWeatherGetter(out, field, false);
				if (!field.isSuppressSetter()) {
					if (field.getRawFormGenerator().isEmpty()) {
						generateFairWeatherSetter(out, className, field);
					} else {
						generateFairWeatherSetterForRawBasics(out, className, field);
					}
				}
				if (!field.getRawFormParser().isEmpty()) {
					generateRawGetter(out, field, true);
					generateGetErrorReason(out, field);
					if (!field.isSuppressSetter()) generateRawSetterForBasic(out, className, field);
				}
				continue;
			}
			
			generateFairWeatherGetter(out, field, true);
			generateFairWeatherSetter(out, className, field);
			if (!field.isForcedType()) generateRawGetter(out, field, false);
			generateRawSetter(out, className, field);
		}
		
		/* children */ {
			out.write("\t@java.lang.Override public java.util.List<Node> getChildren() {\n");
			out.write("\t\tjava.util.List<Node> result = new java.util.ArrayList<Node>();\n");
			for (FieldData data : fields) {
				if (!data.isAstNode()) continue;
				if (!data.isList()) {
					out.write("\t\tif (this.");
					out.write(data.getName());
					out.write(" != null) result.add(this.");
					out.write(data.getName());
					out.write(");\n");
				} else {
					out.write("\t\tresult.addAll(this.");
					out.write(data.getName());
					out.write(".backingList());\n");
				}
			}
			out.write("\t\treturn result;\n\t}\n\t\n");
		}
		
		/* replaceChild */ {
			out.write("\t@java.lang.Override public boolean replaceChild(Node original, Node replacement) throws lombok.ast.AstException {\n");
			for (FieldData field : fields) {
				if (!field.isAstNode()) continue;
				if (!field.isList()) {
					out.write("\t\tif (this.");
					out.write(field.getName());
					out.write(" == original) {\n");
					if (!field.isForcedType()) {
						out.write("\t\t\tthis.raw");
						out.write(field.titleCasedName());
						out.write("(replacement);\n");
						out.write("\t\t\treturn true;\n");
					} else {
						out.write("\t\t\tif (replacement instanceof ");
						out.write(field.getType());
						out.write(") {\n");
						out.write("\t\t\t\tthis.ast");
						out.write(field.titleCasedName());
						out.write("((");
						out.write(field.getType());
						out.write(") replacement);\n");
						out.write("\t\t\t\treturn true;\n");
						out.write("\t\t\t} else throw new lombok.ast.AstException(this, String.format(\n");
						out.write("\t\t\t\t\t\"Cannot replace node: replacement must be of type %s but is of type %s\",\n");
						out.write("\t\t\t\t\t\"");
						out.write(field.getType());
						out.write("\", replacement == null ? \"null\" : replacement.getClass().getName()));\n");
					}
					out.write("\t\t}\n");
				} else {
					out.write("\t\tif (this.raw");
					out.write(field.titleCasedName());
					out.write("().replace(original, replacement)) return true;\n");
				}
			}
			out.write("\t\treturn false;\n\t}\n\t\n");
		}
		
		/* detach */ {
			out.write("\t@java.lang.Override public boolean detach(Node child) {\n");
			for (FieldData field : fields) {
				if (!field.isAstNode()) continue;
				if (!field.isList()) {
					out.write("\t\tif (this.");
					out.write(field.getName());
					out.write(" == child) {\n");
					out.write("\t\t\tthis.disown((AbstractNode) child);\n");
					out.write("\t\t\tthis.");
					out.write(field.getName());
					out.write(" = null;\n");
					out.write("\t\t\treturn true;\n");
					out.write("\t\t}\n");
				} else {
					out.write("\t\tif (this.raw");
					out.write(field.titleCasedName());
					out.write("().remove(child)) return true;\n");
				}
			}
			out.write("\t\treturn false;\n\t}\n\t\n");
		}
		
		/* accept */ {
			out.write("\t@java.lang.Override public void accept(lombok.ast.AstVisitor visitor) {\n");
			out.write("\t\tif (visitor.visit");
			out.write(typeName);
			out.write("(this)) return;\n");
			for (FieldData field : fields) {
				if (!field.isAstNode()) continue;
				if (field.isList()) {
					out.write("\t\tfor (lombok.ast.Node child : this.");
					out.write(field.getName());
					out.write(".asIterable()) {\n\t\t\tchild.accept(visitor);\n\t\t}\n");
					continue;
				}
				out.write("\t\tif (this.");
				out.write(field.getName());
				out.write(" != null) this.");
				out.write(field.getName());
				out.write(".accept(visitor);\n");
			}
			out.write("\t\tvisitor.afterVisit");
			out.write(typeName);
			out.write("(this);\n");
			out.write("\t\tvisitor.endVisit(this);\n");
			out.write("\t}\n\t\n");
		}
		
		/* copy */ {
			out.write("\t@java.lang.Override public ");
			out.write(typeName);
			out.write(" copy() {\n\t\t");
			out.write(typeName);
			out.write(" result = new ");
			out.write(typeName);
			out.write("();\n");
			for (FieldData field : fields) {
				if (!field.isAstNode()) {
					out.write("\t\tresult.");
					out.write(field.getName());
					out.write(" = ");
					out.write(field.getCodeToCopy());
					out.write(";\n");
					if (!field.getRawFormParser().isEmpty()) {
						out.write("\t\tresult.raw");
						out.write(field.titleCasedName());
						out.write(" = this.raw");
						out.write(field.titleCasedName());
						out.write(";\n");
						
						out.write("\t\tresult.errorReasonFor");
						out.write(field.titleCasedName());
						out.write(" = this.errorReasonFor");
						out.write(field.titleCasedName());
						out.write(";\n");
					}
				} else if (field.isList()) {
					out.write("\t\tfor (Node n : this.");
					out.write(field.getName());
					out.write(".backingList()) {\n\t\t\tresult.raw");
					out.write(field.titleCasedName());
					out.write("().addToEnd(n == null ? null : n.copy());\n\t\t}\n");
				} else {
					out.write("\t\tif (this.");
					out.write(field.getName());
					out.write(" != null) result.raw");
					out.write(field.titleCasedName());
					out.write("(this.");
					out.write(field.getName());
					out.write(".copy());\n");
				}
			}
			out.write("\t\treturn result;\n\t}\n\t\n");
		}
		
		/* extra methods */ {
			for (ExecutableElement delegate : methodsToCopy) {
				boolean isVoid = delegate.getReturnType().getKind() == TypeKind.VOID;
				CopyMethod cma = delegate.getAnnotation(CopyMethod.class);
				String accessModifier = cma == null ? "public" : cma.accessModifier();
				boolean isStatic = cma == null ? false : cma.isStatic();
				if (!delegate.getTypeParameters().isEmpty()) {
					throw new IllegalArgumentException("We don't support generics parameters on extra methods in templates.");
				}
				String docComment = processingEnv.getElementUtils().getDocComment(delegate);
				
				if (docComment != null) {
					out.write("\t/**\n");
					for (String s : docComment.split(Pattern.quote("\n"))) {
						out.write("\t *");
						out.write(s);
						out.write("\n");
					}
					out.write("\t */\n");
				}
				
				out.write("\t");
				
				out.write(accessModifier);
				if (!accessModifier.isEmpty()) out.write(" ");
				if (isStatic) out.write("static ");
				
				out.write(isVoid ? "void" : delegate.getReturnType().toString());
				out.write(" ");
				out.write(delegate.getSimpleName().toString());
				out.write("(");
				/* Add parameters, but skip the first one which is used to transport 'this' reference, if not static. */ {
					int idx = 0;
					boolean first = true;
					for (VariableElement p : delegate.getParameters()) {
						idx++;
						if (idx == 1 && !isStatic) continue;
						if (!first) out.write(", ");
						first = false;
						out.write(p.asType().toString());
						out.write(" ");
						out.write(p.getSimpleName().toString());
					}
				}
				out.write(") {\n\t\t");
				out.write(isVoid ? "" : "return ");
				TypeElement container = (TypeElement)delegate.getEnclosingElement();
				out.write(container.toString());
				out.write(".");
				out.write(delegate.getSimpleName().toString());
				out.write("(");
				if (!isStatic) out.write("this");
				/* Generate parameters, but skip first if non-static */ {
					boolean first = true;
					for (VariableElement p : delegate.getParameters()) {
						if (first) {
							first = false;
							if (isStatic) out.write(p.getSimpleName().toString());
							continue;
						}
						out.write(", ");
						out.write(p.getSimpleName().toString());
					}
				}
				out.write(");\n\t}\n\t\n");
			}
		}
		
		out.write("}\n");
		out.close();
	}
	
	private void generateUpMethod(Writer out, ParentRelation relation) throws IOException {
		out.write(String.format("\tpublic %s %s() {\n", relation.getTypeNameTo(), relation.getMethodName()));
		out.write(String.format("\t\tif (!(this.getParent() instanceof %s)) return null;\n", relation.getTypeNameTo()));
		out.write(String.format("\t\t%s out = (%<s)this.getParent();\n", relation.getTypeNameTo()));
		if (relation.isList()) {
			out.write(String.format("\t\tif (!out.%s().contains(this)) return null;\n", relation.getMethodThatShouldGiveThis()));
		} else {
			out.write(String.format("\t\tif (out.%s() != this) return null;\n", relation.getMethodThatShouldGiveThis()));
		}
		out.write("\t\treturn out;\n\t}\n\t\n");
	}
	
	private void generateFieldForList(Writer out, String className, String typeName, int fieldsSize, FieldData field) throws IOException {
		// lombok.ast.ListAccessor<CatchBlock, Try> catches = ListAccessor.of(this, CatchBlock.class, "Try.catches");
		out.write("\tlombok.ast.ListAccessor<");
		out.write(field.getType());
		out.write(", ");
		out.write(className);
		out.write("> ");
		out.write(field.getName());
		out.write(" = ListAccessor.of(");
		out.write("this, ");
		out.write(field.getType());
		out.write(".class, \"");
		out.write(typeName);
		if (fieldsSize > 1) {
			out.write(".");
			out.write(field.getName());
		}
		out.write("\");\n");
	}
	
	private void generateFairWeatherGetter(Writer out, FieldData field, boolean generateCheck) throws IOException {
		out.write("\tpublic ");
		out.write(field.getType());
		out.write(" ast");
		out.write(field.titleCasedName());
		out.write("() {\n");
		if (!field.getRawFormParser().isEmpty()) {
			out.write("\t\tif (this.errorReasonFor");
			out.write(field.titleCasedName());
			out.write(" != null) return ");
			out.write(field.getInitialValue());
			out.write(";\n");
		}
		
		if (generateCheck) {
			out.write("\t\tif (!(this.");
			out.write(field.getName());
			out.write(" instanceof ");
			out.write(field.getType());
			out.write(")) return null;\n");
		}
		out.write("\t\treturn ");
		if (generateCheck) {
			out.write("(");
			out.write(field.getType());
			out.write(") ");
		}
		out.write("this.");
		out.write(field.getName());
		out.write(";\n\t}\n\t\n");
	}
	
	private void generateRawGetter(Writer out, FieldData field, boolean basic) throws IOException {
		out.write("\t");
		out.write(field.isForcedType() ? "private " : "public ");
		out.write(basic ? "java.lang.String" : "lombok.ast.Node");
		out.write(" raw");
		out.write(field.titleCasedName());
		out.write("() {\n\t\treturn this.");
		if (basic) {
			out.write("raw");
			out.write(field.titleCasedName());
		} else {
			out.write(field.getName());
		}
		out.write(";\n\t}\n\t\n");
	}
	
	private void generateGetErrorReason(Writer out, FieldData field) throws IOException {
		out.write("\tpublic java.lang.String getErrorReasonFor");
		out.write(field.titleCasedName());
		out.write("() {\n");
		out.write("\t\treturn this.errorReasonFor");
		out.write(field.titleCasedName());
		out.write(";\n\t}\n\t\n");
	}
	
	private void generateRawSetter(Writer out, String className, FieldData field) throws IOException {
		String initialValueElse = "";
		
		if (field.isMandatory() && field.getInitialValue().equals("null")) {
			String.format("\t\telse %s = %s;\n", field.getName(), field.getInitialValue());
		}
		
		Object[] params = {
				className,
				field.titleCasedName(),
				field.getName(),
				initialValueElse,
				field.isForcedType() ? "private" : "public",
		};
		
		out.write(String.format(
				"\t%5$s %1$s raw%2$s(lombok.ast.Node %3$s) {\n" +
				"\t\tif (%3$s == this.%3$s) return this;\n" +
				"\t\tif (%3$s != null) this.adopt((lombok.ast.AbstractNode)%3$s);\n" +
				"%4$s" +
				"\t\tif (this.%3$s != null) this.disown(this.%3$s);\n" +
				"\t\tthis.%3$s = (lombok.ast.AbstractNode)%3$s;\n" +
				"\t\treturn this;\n" +
				"\t}\n\t\n", params));
	}
	
	private void generateRawSetterForBasic(Writer out, String className, FieldData field) throws IOException {
		Object[] params = {
				className,
				field.titleCasedName(),
				field.getName(),
				getDefaultValueForType(field.getType()),
				field.getRawFormParser()
		};
		out.write(String.format(
				"\tpublic %1$s raw%2$s(java.lang.String %3$s) {\n" +
				"\t\tthis.raw%2$s = %3$s;\n" +
				"\t\tthis.%3$s = %4$s;\n" +
				"\t\tthis.errorReasonFor%2$s = null;\n" +
				"\t\ttry {\n" +
				"\t\t\tthis.%3$s = %1$sTemplate.%5$s(%3$s);\n" +
				"\t\t} catch (java.lang.IllegalArgumentException e) {\n" +
				"\t\t\tthis.errorReasonFor%2$s = e.getMessage() == null ? e.toString() : e.getMessage();\n" +
				"\t\t} catch (Exception e) {\n" +
				"\t\t\tthis.errorReasonFor%2$s = e.toString();\n" +
				"\t\t}\n" +
				"\t\treturn this;\n" +
				"\t}\n", params));
	}
	
	private static final Map<String, String> DEFAULT_VALUES; static {
		Map<String, String> m = new HashMap<String, String>();
		m.put("boolean", "false");
		m.put("byte", "0");
		m.put("short", "0");
		m.put("char", "'\\0'");
		m.put("int", "0");
		m.put("long", "0L");
		m.put("float", "0.0F");
		m.put("double", "0.0");
		DEFAULT_VALUES = Collections.unmodifiableMap(m);
	}
	
	private static boolean isPrimitiveType(String type) {
		return DEFAULT_VALUES.containsKey(type);
	}
	
	private static String getDefaultValueForType(String type) {
		return String.valueOf(DEFAULT_VALUES.get(type));
	}
	
	private void generateFairWeatherSetter(Writer out, String className, FieldData field) throws IOException {
		out.write("\tpublic ");
		out.write(className);
		out.write(" ast");
		out.write(field.titleCasedName());
		out.write("(");
		out.write(field.getType());
		out.write(" ");
		out.write(field.getName());
		out.write(") {\n");
		if (field.isMandatory() && field.initialValue.equals("null")) {
			// if there's an initial value, the raw setter will use that, so we can just chain the call.
			out.write("\t\tif (");
			out.write(field.getName());
			out.write(" == null) ");
			out.write("throw new java.lang.NullPointerException(\"");
			out.write(field.getName());
			out.write(" is mandatory\");\n");
		}
		if (field.isAstNode()) {
			out.write("\t\treturn this.raw");
			out.write(field.titleCasedName());
			out.write("(");
			out.write(field.getName());
			out.write(");\n");
		} else {
			out.write("\t\tthis.");
			out.write(field.getName());
			out.write(" = ");
			out.write(field.getName());
			
			if (!isPrimitiveType(field.getType()) && !field.initialValue.equals("null")) {
				out.write("== null ? ");
				out.write(field.getInitialValue());
				out.write(" : ");
				out.write(field.getName());
			}
			out.write(";\n\t\treturn this;\n");
		}
		
		out.write("\t}\n\t\n");
	}
	
	private void generateFairWeatherSetterForRawBasics(Writer out, String className, FieldData field) throws IOException {
		out.write("\tpublic ");
		out.write(className);
		out.write(" ast");
		out.write(field.titleCasedName());
		out.write("(");
		out.write(field.getType());
		out.write(" ");
		out.write(field.getName());
		out.write(") {\n");
		if (field.isMandatory()) {
			out.write("\t\tif (");
			out.write(field.getName());
			out.write(" == null) throw new java.lang.NullPointerException(\"");
			out.write(field.getName());
			out.write(" is mandatory\");\n");
		}
		out.write("\t\tthis.errorReasonFor");
		out.write(field.titleCasedName());
		out.write(" = null;\n");
		out.write("\t\tthis.");
		out.write(field.getName());
		out.write(" = ");
		out.write(field.getName());
		out.write(";\n");
		out.write("\t\tthis.raw");
		out.write(field.titleCasedName());
		out.write(" = ");
		out.write(className);
		out.write("Template.");
		out.write(field.getRawFormGenerator());
		out.write("(");
		out.write(field.getName());
		out.write(");\n\t\treturn this;\n\t}\n\t\n");
	}
	
	private void generateFieldsForBasic(Writer out, FieldData field) throws IOException {
		out.write("\tprivate ");
		out.write(field.getType());
		out.write(" ");
		out.write(field.getName());
		if (!field.getInitialValue().isEmpty()) {
			out.write(" = ");
			out.write(field.getInitialValue());
		}
		out.write(";\n");
		if (field.getRawFormParser().isEmpty()) return;
		
		out.write("\tprivate java.lang.String raw");
		out.write(field.titleCasedName());
		out.write(";\n");
		
		out.write("\tprivate java.lang.String errorReasonFor");
		out.write(field.titleCasedName());
		if (field.isMandatory()) {
			out.write(" = \"missing ");
			out.write(field.getName());
			out.write("\"");
		}
		out.write(";\n");
	}
	
	private void generateListAccessor(Writer out, String className, FieldData field) throws IOException {
		out.write("\tpublic lombok.ast.RawListAccessor<");
		out.write(field.getType());
		out.write(", ");
		out.write(className);
		out.write("> raw");
		out.write(field.titleCasedName());
		out.write("() {\n\t\treturn this.");
		out.write(field.getName());
		out.write(".asRaw();\n\t}\n\t\n");
		
		out.write("\tpublic lombok.ast.StrictListAccessor<");
		out.write(field.getType());
		out.write(", ");
		out.write(className);
		out.write("> ast");
		out.write(field.titleCasedName());
		out.write("() {\n\t\treturn this.");
		out.write(field.getName());
		out.write(".asStrict();\n\t}\n\t\n");
	}
	
	private void generateFieldsForNode(Writer out, FieldData field) throws IOException {
		out.write("\tprivate lombok.ast.AbstractNode ");
		out.write(field.getName());
		if (!field.getInitialValue().isEmpty()) {
			out.write(" = ");
			out.write(field.getInitialValue());
		}
		out.write(";\n");
	}
}
