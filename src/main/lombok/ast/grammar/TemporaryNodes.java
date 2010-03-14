/*
 * Copyright © 2010 Reinier Zwitserloot and Roel Spilker.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
	
	@Override public Node setPosition(PositionKey key, Position position) {
		throw new UnsupportedOperationException();
	}
	
	@Override public Position getPosition(PositionKey key) {
		throw new UnsupportedOperationException();
	}
	
	@Override public Node getParent() {
		return null;
	}
}
