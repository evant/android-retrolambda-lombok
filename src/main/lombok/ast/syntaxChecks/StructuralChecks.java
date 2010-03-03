package lombok.ast.syntaxChecks;

import java.util.List;

import lombok.ast.MethodDeclaration;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.SyntaxProblem;
import lombok.ast.TypeDeclaration;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class StructuralChecks {
	private final List<SyntaxProblem> problems;
	
	public StructuralChecks(List<SyntaxProblem> problems) {
		this.problems = problems;
	}
	
	public void checkAbstractMembersOnlyInAbstractTypes(MethodDeclaration md) {
		Node rawModifiers = md.getRawModifiers();
		if (!(rawModifiers instanceof Modifiers)) return;
		if (!((Modifiers)rawModifiers).isAbstract()) return;
		if (md.getParent() instanceof TypeDeclaration) {
			Node rawModifiersOfParent = ((TypeDeclaration)md.getParent()).getRawModifiers();
			if (!(rawModifiersOfParent instanceof Modifiers)) return;
			if (((Modifiers)rawModifiers).isAbstract()) return;
			problems.add(new SyntaxProblem(md, "Abstract methods are only allowed in interfaces and abstract classes"));
		}
	}
}
