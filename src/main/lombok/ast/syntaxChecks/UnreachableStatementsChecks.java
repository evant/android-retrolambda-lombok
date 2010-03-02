package lombok.ast.syntaxChecks;

import java.util.List;

import lombok.ast.Block;
import lombok.ast.Break;
import lombok.ast.Continue;
import lombok.ast.Node;
import lombok.ast.Return;
import lombok.ast.SyntaxProblem;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class UnreachableStatementsChecks {
	private final List<SyntaxProblem> problems;
	
	public UnreachableStatementsChecks(List<SyntaxProblem> problems) {
		this.problems = problems;
	}
	
	public void unreachablesAfterBreak(Break statement) {
		checkForUnreachables(statement);
	}
	
	public void unreachablesAfterContinue(Continue statement) {
		checkForUnreachables(statement);
	}
	
	public void unreachableAfterReturn(Return statement) {
		checkForUnreachables(statement);
	}
	
	private void checkForUnreachables(Node n) {
		Node p = n.getParent();
		if (p instanceof Block) {
			boolean found = false;
			for (Node s : ((Block)p).contents().getRawContents()) {
				if (found) {
					problems.add(new SyntaxProblem(s, "Unreachable code"));
				}
				if (s == n) found = true;
			}
		}
	}
}
