/*
 * Copyright © 2010 Reinier Zwitserloot and Roel Spilker.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.processing.AbstractProcessor;
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
import javax.tools.JavaFileObject;
import javax.tools.Diagnostic.Kind;

import org.apache.commons.lang.StringUtils;

import lombok.Data;

@SupportedAnnotationTypes("lombok.ast.template.GenerateAstNode")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TemplateProcessor extends AbstractProcessor {
	@Data
	private static class FieldData {
		private final String name;
		private final boolean mandatory;
		private final String rawFormParser;
		private final String rawFormGenerator;
		private final String initialValue;
		private final String type;
		private final boolean isList;
		private final boolean astNode;
		
		public FieldData(VariableElement field) {
			this.name = String.valueOf(field.getSimpleName());
			boolean isMandatory = false; {
				for (AnnotationMirror ann :field.getAnnotationMirrors()) {
					if (ann.getAnnotationType().toString().equals("lombok.NonNull")) {
						isMandatory = true;
						break;
					}
				}
			}
			this.mandatory = isMandatory;
			TypeMirror type = field.asType();
			NotChildOfNode ncon = field.getAnnotation(NotChildOfNode.class);
			this.astNode = ncon == null;
			this.rawFormParser = astNode ? "" : ncon.rawFormParser();
			this.rawFormGenerator = astNode ? "" : ncon.rawFormGenerator();
			/* grab initial value */ {
				InitialValue iv = field.getAnnotation(InitialValue.class);
				this.initialValue = iv == null ? "" : iv.value();
			}
			
			if (type instanceof DeclaredType) {
				DeclaredType t = (DeclaredType) type;
				this.isList = t.toString().startsWith("java.util.List<") || t.toString().startsWith("List<");
				if (this.isList) type = t.getTypeArguments().get(0);
			} else this.isList = false;
			
			this.type = type.toString();
		}
		
		public String titleCasedName() {
			String n = name.replace("_", "");
			return n.isEmpty() ? "" : Character.toTitleCase(n.charAt(0)) + n.substring(1);
		}
	}
	
	private static String getClassName(Object type) {
		String c = type.toString();
		return c.endsWith(".class") ? c.substring(0, c.length() - ".class".length()) : c;
	}
	
	@Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (Element element : roundEnv.getElementsAnnotatedWith(GenerateAstNode.class)) {
			if (element.getKind() != ElementKind.CLASS) {
				processingEnv.getMessager().printMessage(Kind.ERROR, "@GenerateAstNode is only supported on plain classes", element);
				continue;
			}
			
			List<FieldData> fields = new ArrayList<FieldData>();
			List<ExecutableElement> methodsToCopy = new ArrayList<ExecutableElement>();
			List<ExecutableElement> additionalChecks = new ArrayList<ExecutableElement>();
			
			String className;
			String extending = null;
			List<String> implementing = new ArrayList<String>();
			
			TypeElement annotated;
			/* Calculate file and class name of the source file we need to generate */ {
				annotated = (TypeElement)element;
				className = annotated.getQualifiedName().toString();
				if (className.endsWith("Template")) className = className.substring(0, className.length() - "Template".length());
				else {
					processingEnv.getMessager().printMessage(Kind.ERROR, "@GenerateAstNode annotated classes must end in 'Template'. Example: IfTemplate");
					return true;
				}
			}
			
			/* Pick up the parameters of the annotation (this is the type that the class to be generated must extend / implement) */ {
				for (AnnotationMirror annotation : annotated.getAnnotationMirrors()) {
					if (!annotation.getAnnotationType().toString().equals(GenerateAstNode.class.getName())) continue;
					
					for (Entry<? extends ExecutableElement, ? extends AnnotationValue> value : annotation.getElementValues().entrySet()) {
						if (value.getKey().getSimpleName().contentEquals("extending")) {
							extending = getClassName(value.getValue().getValue());
						}
						
						if (value.getKey().getSimpleName().contentEquals("implementing")) {
							Collection<?> list = (Collection<?>)value.getValue().getValue();
							for (Object type : list) implementing.add(getClassName(type));
						}
					}
					
					if (extending == null) extending = "lombok.ast.AbstractNode";
				}
			}
			
			/* Analyze all fields of template class */ {
				for (Element enclosed : annotated.getEnclosedElements()) {
					if (enclosed.getKind() != ElementKind.FIELD) continue;
					fields.add(new FieldData((VariableElement) enclosed));
				}
			}
			
			/* Analyze all methods of template class */ {
				for (Element enclosed : annotated.getEnclosedElements()) {
					if (enclosed.getKind() != ElementKind.METHOD) continue;
					ExecutableElement method = (ExecutableElement) enclosed;
					boolean copyMethod = method.getAnnotation(CopyMethod.class) != null;
					boolean additionalCheck = method.getAnnotation(AdditionalCheck.class) != null;
					if (copyMethod) methodsToCopy.add(method);
					if ( additionalCheck) additionalChecks.add(method);
				}
			}
			
			try {
				generateSourceFile(annotated, className, extending, implementing, fields, methodsToCopy, additionalChecks);
			} catch (IOException e) {
				processingEnv.getMessager().printMessage(Kind.ERROR, String.format(
						"Can't generate sourcefile %s: %s",
						className + "Template", e), annotated);
			}
		}
		
		return true;
	}
	
	private void generateSourceFile(Element originatingElement, String className, String extending, List<String> implementing, List<FieldData> fields,
			List<ExecutableElement> methodsToCopy, List<ExecutableElement> additionalChecks) throws IOException {
		
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
			if (field.isList() && !field.isAstNode()) {
				throw new UnsupportedOperationException("Generating an AST Node with a list containing non lombok.ast.Node-based objects is not supported: " + field.getName());
			}
			
			if (field.isList()) {
				generateFieldsForList(out, className, typeName, fields.size(), field);
				continue;
			}
			
			if (!field.isAstNode()) {
				generateFieldsForBasic(out, field);
				continue;
			}
			
			generateFieldsForNode(out, field);
		}
		
		out.write("\t\n");
		for (FieldData field : fields) {
			if (field.isList()) {
				generateListAccessor(out, field.getName(), className, field);
				
				continue;
			}
			
			if (!field.isAstNode()) {
				generateFairWeatherGetter(out, field, false);
				if (field.getRawFormGenerator().isEmpty()) {
					generateFairWeatherSetter(out, className, field);
				} else {
					generateFairWeatherSetterForRawBasics(out, className, field);
				}
				if (!field.getRawFormParser().isEmpty()) {
					generateRawGetter(out, field, true);
					generateRawSetterForBasic(out, className, field);
				}
				continue;
			}
			
			generateFairWeatherGetter(out, field, true);
			generateFairWeatherSetter(out, className, field);
			generateRawGetter(out, field, false);
			generateRawSetter(out, className, field);
		}
		
		/* checkSyntacticValidity */ {
			out.write("\t@java.lang.Override public void checkSyntacticValidity(java.util.List<lombok.ast.SyntaxProblem> problems) {\n");
			for (FieldData field : fields) {
				if (field.isList()) {
					generateCheckForList(out, field);
					continue;
				}
				
				if (!field.isAstNode()) {
					generateCheckForBasicField(out, field);
					continue;
				}
				
				generateCheckForNodeField(out, field);
			}
			
			for (ExecutableElement additionalCheck : additionalChecks) {
				if (!additionalCheck.getModifiers().contains(Modifier.STATIC))
					throw new IllegalArgumentException("additional checks must be static");
				if (additionalCheck.getParameters().size() != 2)
					throw new IllegalArgumentException("additional checks must have 2 parameters (List<SyntaxProblem> problems, Self(NotTemplate))");
				
				out.write("\t\t");
				out.write(className);
				out.write("Template.");
				out.write(additionalCheck.getSimpleName().toString());
				out.write("(problems, this);\n");
			}
			
			out.write("\t}\n\t\n");
		}
		
		/* accept */ {
			out.write("\t@java.lang.Override public void accept(lombok.ast.ASTVisitor visitor) {\n");
			out.write("\t\tif (visitor.visit");
			out.write(typeName);
			out.write("(this)) return;\n");
			for (FieldData field : fields) {
				if (!field.isAstNode()) continue;
				if (field.isList()) {
					out.write("\t\tfor (lombok.ast.Node child : this.");
					out.write(field.getName());
					out.write(") {\n\t\t\tchild.accept(visitor);\n\t\t}\n");
					continue;
				}
				out.write("\t\tif (this.");
				out.write(field.getName());
				out.write(" != null) this.");
				out.write(field.getName());
				out.write(".accept(visitor);\n");
			}
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
					out.write(" = this.");
					out.write(field.getName());
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
					out.write(") {\n\t\t\tresult.");
					out.write(field.getName());
					out.write("Accessor.addToEndRaw(n == null ? null : n.copy());\n\t\t}\n");
				} else {
					out.write("\t\tif (this.");
					out.write(field.getName());
					out.write(" != null) result.setRaw");
					out.write(field.titleCasedName());
					out.write("(this.");
					out.write(field.getName());
					out.write(");\n");
				}
			}
			out.write("\t\treturn result;\n\t}\n\t\n");
		}
		
		/* extra methods */ {
			for (ExecutableElement delegate : methodsToCopy) {
				boolean isVoid = delegate.getReturnType().getKind() == TypeKind.VOID;
				out.write("\t");
				out.write(delegate.getAnnotation(CopyMethod.class).accessModifier());
				out.write(" ");
				
				if (!delegate.getTypeParameters().isEmpty()) {
					throw new IllegalArgumentException("We don't support generics parameters on extra methods in templates.");
				}
				out.write(isVoid ? "void" : delegate.getReturnType().toString());
				out.write(" ");
				out.write(delegate.getSimpleName().toString());
				out.write("(");
				/* Add parameters, but skip the first one which is used to transport 'this' reference */ {
					int idx = 0;
					for (VariableElement p : delegate.getParameters()) {
						idx++;
						if (idx == 1) continue;
						if (idx > 2) out.write(", ");
						out.write(p.asType().toString());
						out.write(" ");
						out.write(p.getSimpleName().toString());
					}
				}
				out.write(") {\n\t\t");
				out.write(isVoid ? "" : "return ");
				out.write(className);
				out.write("Template.");
				out.write(delegate.getSimpleName().toString());
				out.write("(this");
				/* Generate parameters, but skip first */ {
					boolean first = true;
					for (VariableElement p : delegate.getParameters()) {
						if (first) {
							first = false;
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
	
	private void generateFieldsForList(Writer out, String className, String typeName, int fieldsSize, FieldData field) throws IOException {
		out.write("\tprivate final java.util.List<lombok.ast.AbstractNode> ");
		out.write(field.getName());
		out.write(" = new java.util.ArrayList<lombok.ast.AbstractNode>();\n");
		
		// private lombok.ast.ListAccessor<CatchBlock, Try> catchesAccessor = ListAccessor.of(catches, this, CatchBlock.class, "Try.catches");
		out.write("\tprivate lombok.ast.ListAccessor<");
		out.write(field.getType());
		out.write(", ");
		out.write(className);
		out.write("> ");
		out.write(field.getName());
		out.write("Accessor = ListAccessor.of(");
		out.write(field.getName());
		out.write(", this, ");
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
		out.write(field.getType().equals("boolean") ? " is" : " get");
		out.write(field.titleCasedName());
		out.write("() {\n");
		if (!field.getRawFormParser().isEmpty()) {
			out.write("\t\tif (this.errorReasonFor");
			out.write(field.titleCasedName());
			out.write(" != null) throw new lombok.ast.AstException(this, this.errorReasonFor");
			out.write(field.titleCasedName());
			out.write(");\n");
		}
		
		if (generateCheck) {
			out.write("\t\tassertChildType(");
			out.write(field.getName());
			out.write(", \"");
			out.write(field.getName());
			out.write("\", ");
			out.write("" + field.isMandatory());
			out.write(", ");
			out.write(field.getType());
			out.write(".class);\n");
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
		out.write("\tpublic ");
		out.write(basic ? "java.lang.String" : "lombok.ast.Node");
		out.write(" getRaw");
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
	
	private void generateRawSetter(Writer out, String className, FieldData field) throws IOException {
		Object[] params = {
				className,
				field.titleCasedName(),
				field.getName()
		};
		
		out.write(String.format(
				"\tpublic %1$s setRaw%2$s(lombok.ast.Node %3$s) {\n" +
				"\t\tif (%3$s == this.%3$s) return this;\n" +
				"\t\tif (%3$s != null) this.adopt((lombok.ast.AbstractNode)%3$s);\n" +
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
				"\tpublic %1$s setRaw%2$s(java.lang.String %3$s) {\n" +
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
	
	private static String getDefaultValueForType(String type) {
		return String.valueOf(DEFAULT_VALUES.get(type));
	}
	
	private void generateFairWeatherSetter(Writer out, String className, FieldData field) throws IOException {
		out.write("\tpublic ");
		out.write(className);
		out.write(" set");
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
		if (field.isAstNode()) {
			out.write("\t\treturn this.setRaw");
			out.write(field.titleCasedName());
			out.write("(");
			out.write(field.getName());
			out.write(");\n");
		} else {
			out.write("\t\tthis.");
			out.write(field.getName());
			out.write(" = ");
			out.write(field.getName());
			out.write(";\n\t\treturn this;\n");
		}
		
		out.write("\t}\n\t\n");
	}
	
	private void generateFairWeatherSetterForRawBasics(Writer out, String className, FieldData field) throws IOException {
		out.write("\tpublic ");
		out.write(className);
		out.write(" set");
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
	
	private void generateListAccessor(Writer out, String methodName, String className, FieldData field) throws IOException {
		out.write("\tpublic lombok.ast.ListAccessor<");
		out.write(field.getType());
		out.write(", ");
		out.write(className);
		out.write("> ");
		out.write(methodName);
		out.write("() {\n\t\treturn this.");
		out.write(field.getName());
		out.write("Accessor;\n\t}\n");
	}
	
	private void generateFieldsForNode(Writer out, FieldData field) throws IOException {
		out.write("\tprivate lombok.ast.AbstractNode ");
		out.write(field.getName());
		out.write(";\n");
	}
	
	private void generateCheckForList(Writer out, FieldData field) throws IOException {
		out.write("\t\tfor (int i = 0; i < this.");
		out.write(field.getName());
		out.write(".size(); i++) {\n");
		out.write("\t\t\tcheckChildValidity(problems, this.");
		out.write(field.getName());
		out.write(".get(i), \"");
		out.write(field.getName());
		out.write("[\" + i + \"]\", true, ");
		out.write(field.getType());
		out.write(".class);\n");
		out.write("\t\t}\n");
	}
	
	private void generateCheckForBasicField(Writer out, FieldData field) throws IOException {
		if (!field.getRawFormParser().isEmpty()) {
			out.write("\t\tif (this.errorReasonFor");
			out.write(field.titleCasedName());
			out.write(" != null) problems.add(new lombok.ast.SyntaxProblem(this, this.errorReasonFor");
			out.write(field.titleCasedName());
			out.write("));\n");
		} else {
			if (field.isMandatory()) {
				out.write("\t\tif (this.");
				out.write(field.getName());
				out.write(" == null) problems.add(new lombok.ast.SyntaxProblem(this, \"");
				out.write(field.getName());
				out.write(" is mandatory\"));\n");
			}
		}
	}
	
	private void generateCheckForNodeField(Writer out, FieldData field) throws IOException {
		out.write("\t\tcheckChildValidity(problems, this.");
		out.write(field.getName());
		out.write(", \"");
		out.write(field.getName());
		out.write("\", ");
		out.write("" + field.isMandatory());
		out.write(", ");
		out.write(field.getType());
		out.write(".class);\n");
	}
}
