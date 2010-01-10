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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import javax.tools.Diagnostic.Kind;

import lombok.Data;
import lombok.NonNull;

@SupportedAnnotationTypes("lombok.ast.template.GenerateAstNode")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TemplateProcessor extends AbstractProcessor {
	@Data
	private static class FieldData {
		private final String name;
		private final boolean mandatory;
		private final String type;
		private final boolean isList;
		private final boolean notAstNode;
		
		public String titleCasedName() {
			String n = name.replace("_", "");
			return n.isEmpty() ? "" : Character.toTitleCase(n.charAt(0)) + n.substring(1);
		}
	}
	
	@Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (Element element : roundEnv.getElementsAnnotatedWith(GenerateAstNode.class)) {
			if (element.getKind() != ElementKind.CLASS) {
				processingEnv.getMessager().printMessage(Kind.ERROR, "@GenerateAstNode is only supported on plain classes", element);
				continue;
			}
			
			List<FieldData> fields = new ArrayList<FieldData>();
			String className;
			String extending = null;
			
			TypeElement annotated = (TypeElement)element;
			className = annotated.getQualifiedName().toString();
			if (className.endsWith("Template")) className = className.substring(0, className.length() - "Template".length());
			else {
				processingEnv.getMessager().printMessage(Kind.ERROR, "@GenerateAstNode annotated classes must end in 'Template'. Example: IfTemplate");
				return true;
			}
			
			for (AnnotationMirror annotation : annotated.getAnnotationMirrors()) {
				if (!annotation.getAnnotationType().toString().equals(GenerateAstNode.class.getName())) continue;
				
				for (Entry<? extends ExecutableElement, ? extends AnnotationValue> value : annotation.getElementValues().entrySet()) {
					extending = value.getValue().toString();
					if (extending.endsWith(".class")) {
						extending = extending.substring(0, extending.length() - ".class".length());
					}
				}
				
				if (extending == null) extending = "lombok.ast.Node";
			}
			
			for (Element enclosed : annotated.getEnclosedElements()) {
				if (enclosed.getKind() != ElementKind.FIELD) continue;
				VariableElement field = (VariableElement) enclosed;
				String fieldName = String.valueOf(field.getSimpleName());
				boolean isMandatory = field.getAnnotation(NonNull.class) != null;
				TypeMirror type = field.asType();
				if (type instanceof DeclaredType) {
					DeclaredType t = (DeclaredType) type;
					if (t.toString().startsWith("java.util.List<")) {
						fields.add(new FieldData(fieldName, true, t.getTypeArguments().get(0).toString(), true, !isAstNodeChild(t.getTypeArguments().get(0))));
						continue;
					}
				}
				
				fields.add(new FieldData(fieldName, isMandatory, type.toString(), false, !isAstNodeChild(type)));
			}
			
			try {
				generateSourceFile(annotated, className, extending, fields);
			} catch (IOException e) {
				processingEnv.getMessager().printMessage(Kind.ERROR, String.format(
						"Can't generate sourcefile %s: %s",
						className + "Template", e), annotated);
			}
		}
		
		return true;
	}
	
	private static final List<String> TREAT_AS_PRIMITIVE = Collections.unmodifiableList(Arrays.asList(
			"boolean", "byte", "char", "short", "int", "long", "float", "double", "void",
			"String", "java.util.String", "Object", "java.lang.Object", "Number", "java.lang.Number"
	));
	
	private boolean isAstNodeChild(TypeMirror m) {
		return !TREAT_AS_PRIMITIVE.contains(m.toString());
	}
	
	private void generateSourceFile(Element originatingElement, String className, String extending, List<FieldData> fields) throws IOException {
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
		out.write(" {\n");
		for (FieldData field : fields) {
			if (field.isList()) {
				if (field.isNotAstNode()) throw new UnsupportedOperationException("We don't support lists with non-ast.nodes yet!");
				out.write("\tprivate final java.util.List<lombok.ast.Node> ");
				out.write(field.getName());
				out.write(" = new java.util.ArrayList<lombok.ast.Node>();\n");
			} else if (field.isNotAstNode()) {
				out.write("\tprivate ");
				out.write(field.getType());
				out.write(" ");
				out.write(field.getName());
				out.write(";\n");
			} else {
				out.write("\tprivate lombok.ast.Node ");
				out.write(field.getName());
				out.write(";\n");
			}
			
			if (field.isList()) {
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
				if (fields.size() > 1) {
					out.write(".");
					out.write(field.getName());
				}
				out.write("\");\n");
			}
		}
		out.write("\t\n");
		for (FieldData field : fields) {
			if (field.isList()) {
//				if (fields.size() == 1) {
//					generateListAccessor(out, "asListAccessor", className, field);
//					generateDelegation();
//				} else {
					generateListAccessor(out, field.getName(), className, field);
//				}
				
				continue;
			}
			
			if (field.isNotAstNode()) {
				/* getter */ {
					out.write("\tpublic ");
					out.write(field.getType());
					out.write(field.getType().equals("boolean") ? " is" : " get");
					out.write(field.titleCasedName());
					out.write("() {\n");
					out.write("\t\treturn this.");
					out.write(field.getName());
					out.write(";\n\t}\n\t\n");
				}
				
				/* setter */ {
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
					out.write("\t\tthis.");
					out.write(field.getName());
					out.write(" = ");
					out.write(field.getName());
					out.write(";\n\t\treturn this;\n\t}\n\t\n");
				}
				continue;
			}
			
			/* fair weather getter */ {
				out.write("\tpublic ");
				out.write(field.getType());
				out.write(" get");
				out.write(field.titleCasedName());
				out.write("() {\n");
				out.write("\t\tassertChildType(");
				out.write(field.getName());
				out.write(", \"");
				out.write(field.getName());
				out.write("\", ");
				out.write("" + field.isMandatory());
				out.write(", ");
				out.write(field.getType());
				out.write(".class);\n\t\treturn (");
				out.write(field.getType());
				out.write(") ");
				out.write(field.getName());
				out.write(";\n\t}\n\t\n");
			}
			
			/* raw getter */ {
				out.write("\tpublic lombok.ast.Node getRaw");
				out.write(field.titleCasedName());
				out.write("() {\n\t\treturn ");
				out.write(field.getName());
				out.write(";\n\t}\n\t\n");
			}
			
			/* fair weather setter */ {
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
				out.write("\t\tthis.");
				out.write(field.getName());
				out.write(" = ");
				out.write(field.getName());
				out.write(";\n\t\treturn this;\n\t}\n\t\n");
			}
			
			/* raw setter */ {
				out.write("\tpublic ");
				out.write(className);
				out.write(" setRaw");
				out.write(field.titleCasedName());
				out.write("(lombok.ast.Node ");
				out.write(field.getName());
				out.write(") {\n");
				out.write("\t\tthis.");
				out.write(field.getName());
				out.write(" = ");
				out.write(field.getName());
				out.write(";\n\t\treturn this;\n\t}\n\t\n");
			}
		}
		
		/* checkSyntacticValidity */ {
			out.write("\t@java.lang.Override public void checkSyntacticValidity(java.util.List<lombok.ast.SyntaxProblem> problems) {\n");
			for (FieldData field : fields) {
				if (field.isList()) {
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
					continue;
				}
				if (field.isNotAstNode()) {
					if (field.isMandatory()) {
						out.write("\t\tif (this.");
						out.write(field.getName());
						out.write(" == null) problems.add(new lombok.ast.SyntaxProblem(this, \"");
						out.write(field.getName());
						out.write(" is mandatory\"));\n");
					}
					continue;
				}
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
			out.write("\t}\n\t\n");
		}
		
		/* accept */ {
			out.write("\t@java.lang.Override public void accept(lombok.ast.ASTVisitor visitor) {\n" +
					"\t\tif (visitor.visit");
			out.write(typeName);
			out.write("(this)) return;\n");
			for (FieldData field : fields) {
				if (field.isNotAstNode()) continue;
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
			out.write("\t}\n");
		}
		
		out.write("}\n");
		out.close();
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
}
