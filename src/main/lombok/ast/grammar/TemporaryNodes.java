package lombok.ast.grammar;

import java.util.ArrayList;
import java.util.List;

import lombok.ast.ASTVisitor;
import lombok.ast.Node;
import lombok.ast.SyntaxProblem;

class TemporaryNodes {
	static class OrphanedTypeVariables extends Node {
		List<Node> variables = new ArrayList<Node>();
		
		@Override public void accept(ASTVisitor visitor) {
			visitor.visitParseArtefact(this);
		}
		
		@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
			problems.add(new SyntaxProblem(this, "Orphaned type variables"));
		}
		
		@Override public OrphanedTypeVariables copy() {
			OrphanedTypeVariables result = new OrphanedTypeVariables();
			for (Node n : variables) result.variables.add(n == null ? null : n.copy());
			return result;
		}
	}
	
	static class StatementExpressionList extends Node {
		List<Node> expressions = new ArrayList<Node>();
		
		@Override public void accept(ASTVisitor visitor) {
			visitor.visitParseArtefact(this);
		}
		
		@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
			problems.add(new SyntaxProblem(this, "Statement expression list"));
		}
		
		@Override public StatementExpressionList copy() {
			StatementExpressionList result = new StatementExpressionList();
			for (Node n : expressions) result.expressions.add(n == null ? null : n.copy());
			return result;
		}
	}
}
