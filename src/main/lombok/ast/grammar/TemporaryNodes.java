package lombok.ast.grammar;

import java.util.ArrayList;
import java.util.List;

import lombok.ast.ASTVisitor;
import lombok.ast.Node;
import lombok.ast.Position;
import lombok.ast.SyntaxProblem;

abstract class TemporaryNode implements Node {
	@Override public void accept(ASTVisitor visitor) {
		visitor.visitParseArtefact(this);
	}
	
	@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
		StringBuilder errorName = new StringBuilder();
		boolean first = true;
		for (char c : getClass().getSimpleName().toCharArray()) {
			if (first) {
				errorName.append(c);
				first = false;
				continue;
			}
			
			if (Character.isUpperCase(c)) errorName.append(" ").append(Character.toLowerCase(c));
			else errorName.append(c);
		}
		problems.add(new SyntaxProblem(this, errorName.toString()));
	}
	
	static class OrphanedTypeVariables extends TemporaryNode {
		List<Node> variables = new ArrayList<Node>();
		
		@Override public OrphanedTypeVariables copy() {
			OrphanedTypeVariables result = new OrphanedTypeVariables();
			for (Node n : variables) result.variables.add(n == null ? null : n.copy());
			return result;
		}
	}
	
	static class StatementExpressionList extends TemporaryNode {
		List<Node> expressions = new ArrayList<Node>();
		
		@Override public StatementExpressionList copy() {
			StatementExpressionList result = new StatementExpressionList();
			for (Node n : expressions) result.expressions.add(n == null ? null : n.copy());
			return result;
		}
	}
	
	static class ExtendsClause extends TemporaryNode {
		List<Node> superTypes = new ArrayList<Node>();
		
		@Override public ExtendsClause copy() {
			ExtendsClause result = new ExtendsClause();
			for (Node n : superTypes) result.superTypes.add(n == null ? null : n.copy());
			return result;
		}
	}
	
	static class ImplementsClause extends TemporaryNode {
		List<Node> superInterfaces = new ArrayList<Node>();
		
		@Override public ImplementsClause copy() {
			ImplementsClause result = new ImplementsClause();
			for (Node n : superInterfaces) result.superInterfaces.add(n == null ? null : n.copy());
			return result;
		}
	}
	
	@Override public Node getGeneratedBy() {
		return null;
	}
	
	@Override public boolean hasParent() {
		return false;
	}
	
	@Override public boolean isGenerated() {
		return false;
	}
	
	@Override public boolean isSyntacticallyValid() {
		return false;
	}
	
	@Override public void setPosition(Position position) {
	}
	
	@Override public Position getPosition() {
		return null;
	}
	
	@Override public Node getParent() {
		return null;
	}
}
