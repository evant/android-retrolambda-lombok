package lombok.ast.ecj;

import lombok.ast.Node;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;

public class EcjTreeConverter extends EcjTreeVisitor {
	private final CompilationUnitDeclaration cud;
	private Node result;
	
	public EcjTreeConverter(CompilationUnitDeclaration cud) {
		this.cud = cud;
	}
	
	public Node convert() {
		visitEcjNode(cud);
		return result;
	}
}
