package lombok.ast.syntaxChecks;

import java.util.List;

import lombok.ast.ClassLiteral;
import lombok.ast.MethodDeclaration;
import lombok.ast.Node;
import lombok.ast.SyntaxProblem;
import lombok.ast.TypeArguments;
import lombok.ast.TypeReference;
import lombok.ast.TypeVariable;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class TypeChecks {
	private final List<SyntaxProblem> problems;
	
	public TypeChecks(List<SyntaxProblem> problems) {
		this.problems = problems;
	}
	
	public void checkNoPrimitivesInGenerics(TypeArguments node) {
		for (Node n : node.generics().getRawContents()) {
			if (n instanceof TypeReference) {
				if (((TypeReference)n).isPrimitive()) {
					problems.add(new SyntaxProblem(node, "Primitive types aren't allowed in type arguments."));
				}
			}
		}
	}
	
	public void checkNoPrimitivesInGenerics(TypeVariable node) {
		for (Node n : node.extending().getRawContents()) {
			if (n instanceof TypeReference) {
				if (((TypeReference)n).isPrimitive()) {
					problems.add(new SyntaxProblem(node, "Primitive types aren't allowed in type variable bounds."));
				}
			}
		}
	}
	
	public void checkVoidNotLegalJustAboutEverywhere(TypeReference node) {
		if (!node.isVoid()) return;
		if (node.getArrayDimensions() > 0) {
			problems.add(new SyntaxProblem(node, "array of void type is not legal"));
			return;
		}
		if (node.getParent() instanceof MethodDeclaration) {
			if (node == ((MethodDeclaration)node.getParent()).getRawReturnTypeReference()) return;
		}
		
		if (node.getParent() instanceof ClassLiteral) {
			if (node == ((ClassLiteral)node.getParent()).getRawTypeReference()) return;
		}
		
		problems.add(new SyntaxProblem(node, "The void type is not legal here"));
	}
}
