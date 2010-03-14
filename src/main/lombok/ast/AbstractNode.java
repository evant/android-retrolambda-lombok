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
package lombok.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.ast.printer.SourcePrinter;
import lombok.ast.printer.TextFormatter;
import lombok.ast.syntaxChecks.SyntacticValidityVisitorBase;

abstract class AbstractNode implements Node {
	@Getter private Node parent;
	private final Map<PositionKey, Position> extraPositions = new HashMap<PositionKey, Position>();
	
	/**
	 * Checks if a given child is syntactically valid and throws an {@code AstException} otherwise.
	 * This method will <em>NOT</em> recursively call {@link #checkSyntacticValidity(List)} on the child (on purpose - that's not a good idea here).
	 * 
	 * @param child The actual child node object that will be checked.
	 * @param name The name of this node. For example, in a binary operation, {@code "left side"}.
	 * @param mandatory If this node not being there (being {@code null}) is a problem or acceptable.
	 * @param typeAssertion If the node exists, it must be an instance of this type.
	 * @throws AstException If {@code child} is {@code null} <em>AND</em> {@code mandatory} is {@code true}, or
	 *    {@code child} is not of the appropriate type as specified.
	 */
	protected void assertChildType(Node child, String name, boolean mandatory, Class<?> typeAssertion) throws AstException {
		SyntaxProblem p = SyntacticValidityVisitorBase.verifyNodeRelation(this, child, name, mandatory, typeAssertion);
		if (p != null) p.throwAstException();
	}
	
	@Override public boolean isGenerated() {
		return getPosition().getGeneratedBy() != null;
	}
	
	@Override public Node getGeneratedBy() {
		return getPosition().getGeneratedBy();
	}
	
	@Override public boolean hasParent() {
		return parent != null;
	}
	
	/**
	 * Adopts (accepts as direct child) the provided node.
	 * 
	 * @param child The node to adopt
	 * @returns The {@code child} parameter for chaining.
	 * @throws IllegalStateException If {@code child} already has a parent (clone or unparent it first).
	 */
	protected AbstractNode adopt(AbstractNode child) throws IllegalStateException {
		child.ensureParentless();
		child.parent = this;
		return child;
	}
	
	/**
	 * Checks if this node is currently parentless.
	 * 
	 * @throws IllegalStateException if I have a parent.
	 */
	protected void ensureParentless() throws IllegalStateException {
		if (parent == null) return;
		throw new IllegalStateException(String.format(
				"I (%s) already have a parent, so you can't add me to something else; clone or unparent me first.",
				this.getClass().getName()));
	}
	
	/**
	 * Disowns a direct child (it will be parentless after this call).
	 * 
	 * @param child Child node to disown
	 * @throws IllegalStateException if {@code child} isn't a direct child of myself.
	 */
	protected void disown(AbstractNode child) throws IllegalStateException {
		ensureParentage(child);
		child.parent = null;
	}
	
	/**
	 * Checks if the provided node is a direct child of this node.
	 * 
	 * @param child This node must be a direct child of myself.
	 * @throws IllegalStateException If {@code child} isn't a direct child of myself.
	 */
	protected void ensureParentage(AbstractNode child) throws IllegalStateException {
		if (child.parent == this) return;
		
		throw new IllegalStateException(String.format(
				"Can't disown child of type %s - it isn't my child (I'm a %s)",
				child.getClass().getName(), this.getClass().getName()));
	}
	
	@Override public Position getPosition() {
		return getPosition(null);
	}
	
	@Override public Position getPosition(PositionKey key) {
		Position p = this.extraPositions.get(key);
		return p == null ? Position.UNPLACED : p;
	}
	
	@Override public Node setPosition(Position position) {
		return setPosition(null, position);
	}
	
	@Override public Node setPosition(PositionKey key, Position position) {
		if (position == null) throw new NullPointerException("position");
		this.extraPositions.put(key, position);
		return this;
	}
	
	@Override public String toString() {
		TextFormatter formatter = new TextFormatter(null);
		SourcePrinter printer = new SourcePrinter(formatter);
		accept(printer);
		return formatter.finish();
	}
	
	abstract static class WithParens extends AbstractNode implements Expression {
		private List<Position> parensPositions = new ArrayList<Position>();
		
		@Override
		public boolean needsParentheses() {
			return false;
		}
		
		@Override
		public List<Position> getParensPositions() {
			return parensPositions;
		}
		
		@Override
		public int getParens() {
			return this.parensPositions.size();
		}
		
		@Override
		public int getIntendedParens() {
			return this.parensPositions.size();
		}
	}
}
