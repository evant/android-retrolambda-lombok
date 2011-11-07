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
package lombok.ast.ecj;

import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.CombinedBinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.IntLiteralMinValue;
import org.eclipse.jdt.internal.compiler.ast.Javadoc;
import org.eclipse.jdt.internal.compiler.ast.LongLiteralMinValue;

import com.google.common.collect.Lists;

/**
 * This class generates the EcjAstVisitor that we use, because Eclipse's own visitor sucks,
 * at least for our purposes.
 * 
 * Technically we should not check in the generated file and run this on every build, but its not
 * like the eclipse org.eclipse.jdt.internal.compiler.ast package gets changed every day, so, we don't bother.
 */
class GenerateEcjTreeVisitorCode {
	private static final String COPYRIGHT_NOTICE = "/*\n" + 
			" * Copyright (C) 2010 The Project Lombok Authors.\n" + 
			" * \n" + 
			" * Permission is hereby granted, free of charge, to any person obtaining a copy\n" + 
			" * of this software and associated documentation files (the \"Software\"), to deal\n" + 
			" * in the Software without restriction, including without limitation the rights\n" + 
			" * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n" + 
			" * copies of the Software, and to permit persons to whom the Software is\n" + 
			" * furnished to do so, subject to the following conditions:\n" + 
			" * \n" + 
			" * The above copyright notice and this permission notice shall be included in\n" + 
			" * all copies or substantial portions of the Software.\n" + 
			" * \n" + 
			" * THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n" + 
			" * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n" + 
			" * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n" + 
			" * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n" + 
			" * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n" + 
			" * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN\n" + 
			" * THE SOFTWARE.\n" + 
			" */\n";
	public static void main(String[] args) throws Exception {
		List<Class<?>> visits = findVisits();
		StringBuilder out = new StringBuilder();
		prefix(out);
		for (Class<?> c : visits) instanceofGen(out, c);
		infix(out);
		for (Class<?> c : visits) methodGen(out, c);
		suffix(out);
		System.out.println(out.toString());
	}
	
	private static final Class<?>[] EXTRA_TYPES = {
		CombinedBinaryExpression.class,
		IntLiteralMinValue.class,
		LongLiteralMinValue.class,
		Javadoc.class,
	};
	
	static List<Class<?>> findVisits() {
		List<Class<?>> visits = Lists.newArrayList();
		for (Method m : ASTVisitor.class.getMethods()) {
			if (m.getName().equals("visit") && m.getParameterTypes().length > 0) {
				Class<?> t = m.getParameterTypes()[0];
				if (!visits.contains(t) && !t.getSimpleName().startsWith("Javadoc")) visits.add(t);
			}
		}
		
		for (Class<?> extra : EXTRA_TYPES) {
			if (!visits.contains(extra)) visits.add(extra);
		}
		
		return visits;
	}
	
	private static void prefix(StringBuilder out) {
		out.append(COPYRIGHT_NOTICE);
		out.append("package lombok.ast.ecj;\n\n");
		out.append("import org.eclipse.jdt.internal.compiler.ast.*;\n\n");
		out.append("public abstract class EcjTreeVisitor {\n");
		out.append("\tpublic void visitEcjNode(ASTNode node) {\n");
		out.append("\t\tif (node == null) return;\n\t\tClass<?> clazz = node.getClass();\n\t\t\n");
	}
	
	private static void instanceofGen(StringBuilder out, Class<?> c) {
		out.append("\t\tif (clazz == ").append(c.getSimpleName());
		out.append(".class) {\n\t\t\tvisit").append(c.getSimpleName()).append("((").append(c.getSimpleName());
		out.append(") node);\n\t\t\treturn;\n\t\t}\n");
	}
	
	private static void infix(StringBuilder out) {
		out.append("\t\t\n\t\tvisitOther(node);\n");
		out.append("\t}\n\t\n");
		out.append("\tpublic void visitOther(ASTNode node) {\n");
		out.append("\t\tthrow new UnsupportedOperationException(\"Unknown ASTNode child: \" + ");
		out.append("node.getClass().getSimpleName());\n\t}\n\t\n");
		out.append("\tpublic void visitAny(ASTNode node) {\n");
		out.append("\t\tthrow new UnsupportedOperationException(\"visit\" + node.getClass().getSimpleName()");
		out.append(" + \" not implemented\");\n\t}\n");
	}
	
	private static void methodGen(StringBuilder out, Class<?> c) {
		out.append("\t\n\tpublic void visit").append(c.getSimpleName()).append("(");
		out.append(c.getSimpleName()).append(" node) {\n");
		out.append("\t\tvisitAny(node);\n\t}\n");
	}
	
	private static void suffix(StringBuilder out) {
		out.append("}\n");
	}
}
