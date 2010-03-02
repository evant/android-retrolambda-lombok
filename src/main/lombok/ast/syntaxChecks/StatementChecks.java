package lombok.ast.syntaxChecks;

import java.util.List;

import lombok.ast.DoWhile;
import lombok.ast.For;
import lombok.ast.ForEach;
import lombok.ast.If;
import lombok.ast.Node;
import lombok.ast.SyntaxProblem;
import lombok.ast.Try;
import lombok.ast.TypeDeclaration;
import lombok.ast.VariableDeclaration;
import lombok.ast.While;
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
	
	public void checkDeclarationsAsDirectChildWhile(While node) {
		checkDeclarationsAsDirectChild(node, node.getRawStatement());
	}
	
	public void checkDeclarationsAsDirectChildDo(DoWhile node) {
		checkDeclarationsAsDirectChild(node, node.getRawStatement());
	}
	
	public void checkDeclarationsAsDirectChildForEach(ForEach node) {
		checkDeclarationsAsDirectChild(node, node.getRawStatement());
	}
	
	public void checkDeclarationsAsDirectChildIf(If node) {
		checkDeclarationsAsDirectChild(node, node.getRawStatement());
		checkDeclarationsAsDirectChild(node, node.getRawElseStatement());
	}
	
	public void checkDeclarationsAsDirectChildFor(For node) {
		checkDeclarationsAsDirectChild(node, node.getRawStatement());
	}
	
	private void checkDeclarationsAsDirectChild(Node n, Node c) {
		if (c instanceof VariableDeclaration) {
			problems.add(new SyntaxProblem(c, "Variable declarations only make sense in the context of a block."));
		}
		
		if (c instanceof TypeDeclaration) {
			problems.add(new SyntaxProblem(c, "Type declarations only make sense in the context of a block or other type."));
		}
	}
}
