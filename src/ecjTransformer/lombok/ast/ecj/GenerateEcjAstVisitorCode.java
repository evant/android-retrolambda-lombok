package lombok.ast.ecj;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.CombinedBinaryExpression;

/**
 * This class generates the EcjAstVisitor that we use, because Eclipse's own visitor sucks,
 * at least for our purposes.
 * 
 * Technically we should not check in the generated file and run this on every build, but its not
 * like the eclipse org.eclipse.jdt.internal.compiler.ast package gets changed every day, so, we don't bother.
 */
class GenerateEcjAstVisitorCode {
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
	
	static List<Class<?>> findVisits() {
		List<Class<?>> visits = new ArrayList<Class<?>>();
		for (Method m : ASTVisitor.class.getMethods()) {
			if (m.getName().equals("visit") && m.getParameterTypes().length > 0) {
				Class<?> t = m.getParameterTypes()[0];
				if (!visits.contains(t) && !t.getSimpleName().startsWith("Javadoc")) visits.add(t);
			}
		}
		
		Class<?> cbe = CombinedBinaryExpression.class;
		
		if (!visits.contains(cbe)) visits.add(cbe);
		
		return visits;
	}
	
	private static void prefix(StringBuilder out) {
		out.append("package lombok.ast.ecj;\n\n");
		out.append("import org.eclipse.jdt.internal.compiler.ast.*;\n\n");
		out.append("public abstract class EcjAstVisitor {\n");
		out.append("\tpublic void visitEcjNode(ASTNode node) {\n");
		out.append("\t\tif (node == null) return;\n\t\tClass<?> clazz = node.getClass();\n\t\t\n");
	}
	
	private static void instanceofGen(StringBuilder out, Class<?> c) {
		out.append("\t\tif (clazz == ").append(c.getSimpleName());
		out.append(".class) {\n\t\t\tvisit").append(c.getSimpleName()).append("((").append(c.getSimpleName());
		out.append(") node);\n\t\t\treturn;\n\t\t}\n");
	}
	
	private static void infix(StringBuilder out) {
		out.append("\t\t\n\t\tthrow new UnsupportedOperationException(\"Unknown ASTNode child: \" + ");
		out.append("node.getClass().getSimpleName());\n");
		out.append("\t}\n\t\n\tpublic void visitAny(ASTNode node) {\n");
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
