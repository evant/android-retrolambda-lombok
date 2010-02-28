package lombok.ast.syntaxChecks;

import java.util.List;

import lombok.ast.SyntaxProblem;
import lombok.ast.Try;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class StatementChecks {
	private final List<SyntaxProblem> problems;
	
	public StatementChecks(List<SyntaxProblem> problems) {
		this.problems = problems;
	}
	
	public void checkNotLoneTry(Try node) {
		if (node.catches().size() == 0 && node.getRawFinally() == null) {
			problems.add(new SyntaxProblem(node, "try statement with no catches and no finally"));
		}
	}
}
