package lombok.ast.syntaxChecks;

import java.util.List;

import lombok.ast.Identifier;
import lombok.ast.SyntaxProblem;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class BasicChecks {
	private final List<SyntaxProblem> problems;
	
	public BasicChecks(List<SyntaxProblem> problems) {
		this.problems = problems;
	}
	
	public void checkNameOfIdentifier(Identifier identifier) {
		String n = identifier.getName();
		if (n == null || n.length() == 0) {
			problems.add(new SyntaxProblem(identifier, "Empty identifier"));
			return;
		}
		
		if (!Character.isJavaIdentifierStart(n.charAt(0))) {
			problems.add(new SyntaxProblem(identifier,
					"Not a legal start character for a java identifier: " + n.charAt(0)));
			return;
		}
		
		for (int i = 1; i < n.length(); i++) {
			if (!Character.isJavaIdentifierPart(n.charAt(i))) {
				problems.add(new SyntaxProblem(identifier,
						"Not a legal character in a java identifier: " + n.charAt(i)));
				return;
			}
		}
	}
}
