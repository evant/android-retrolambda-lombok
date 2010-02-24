package lombok.ast.grammar;

import java.util.ArrayList;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.Node;
import lombok.ast.Position;

abstract class TemporaryNode implements Node {
	private Position position = Position.UNPLACED;

	@Override public void accept(AstVisitor visitor) {
		visitor.visitParseArtefact(this);
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
	
	@Override public Node setPosition(Position position) {
		this.position = position;
		return this;
	}
	
	@Override public Position getPosition() {
		return position;
	}
	
	@Override public Node getParent() {
		return null;
	}
}
