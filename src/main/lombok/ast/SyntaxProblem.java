package lombok.ast;

import lombok.Data;

@Data
public class SyntaxProblem {
	private final Node problemNode;
	private final String message;
	
	public void throwAstException() {
		throw new AstException(problemNode, message);
	}
}
