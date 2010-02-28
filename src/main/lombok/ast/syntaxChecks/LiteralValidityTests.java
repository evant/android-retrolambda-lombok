package lombok.ast.syntaxChecks;

import java.util.List;

import lombok.ast.FloatingPointLiteral;
import lombok.ast.IntegralLiteral;
import lombok.ast.NullLiteral;
import lombok.ast.SyntaxProblem;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class LiteralValidityTests {
	private final List<SyntaxProblem> problems;
	
	public LiteralValidityTests(List<SyntaxProblem> problems) {
		this.problems = problems;
		
	}
	
	public void checkIntegralLiteralValidity(IntegralLiteral node) {
		if (node.getErrorReasonForValue() != null) {
			problems.add(new SyntaxProblem(node, node.getErrorReasonForValue()));
		}
	}
	
	public void checkFloatingPointLiteralValidity(FloatingPointLiteral node) {
		if (node.getErrorReasonForValue() != null) {
			problems.add(new SyntaxProblem(node, node.getErrorReasonForValue()));
		}
	}
	
	public void checkNullLiteralValidity(NullLiteral node) {
		if (node.getErrorReasonForValue() != null) {
			problems.add(new SyntaxProblem(node, node.getErrorReasonForValue()));
		}
	}
}
