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
import java.util.List;

import lombok.Getter;

abstract class AbstractNode implements Node {
	@Getter private Position position = Position.UNPLACED;
	@Getter private Node parent;
	
	@Override public boolean isSyntacticallyValid() {
		List<SyntaxProblem> list = new ArrayList<SyntaxProblem>();
		checkSyntacticValidity(list);
		return list.isEmpty();
	}
	
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
		SyntaxProblem p = checkChildValidity0(child, name, mandatory, typeAssertion);
		if (p != null) p.throwAstException();
	}
	
	/**
	 * Checks if a given child is syntactically valid; you specify exactly what is required for this to hold in the parameters.
	 * This method will recursively call {@link #checkSyntacticValidity(List)} on the child.
	 * 
	 * @param problems The problems list that any syntactic problems will be added to.
	 * @param child The actual child node object that will be checked.
	 * @param name The name of this node. For example, in a binary operation, {@code "left side"}.
	 * @param mandatory If this node not being there (being {@code null}) is a problem or acceptable.
	 * @param typeAssertion If the node exists, it must be an instance of this type.
	 */
	protected void checkChildValidity(List<SyntaxProblem> problems, Node child, String name, boolean mandatory, Class<?> typeAssertion) {
		SyntaxProblem problem = checkChildValidity0(child, name, mandatory, typeAssertion);
		if (problem != null) problems.add(problem);
		if (child != null) child.checkSyntacticValidity(problems);
	}
	
	private SyntaxProblem checkChildValidity0(Node child, String name, boolean mandatory, Class<?> typeAssertion) {
		String typeAssertionName = typeAssertion.getSimpleName().toLowerCase();
		boolean typeAssertionVowel = typeAssertionName.isEmpty();
		String nameTC;
		if (!typeAssertionVowel) {
			char c = typeAssertionName.charAt(0);
			typeAssertionVowel = (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u');
		}
		nameTC = name.isEmpty() ? "" :
				("" + Character.toTitleCase(name.charAt(0)) + name.substring(1));
		
		if (child == null) {
			if (mandatory) return new SyntaxProblem(this, String.format("Missing %s %s",
					name, typeAssertion.getSimpleName().toLowerCase()));
		} else {
			if (!typeAssertion.isInstance(child)) return new SyntaxProblem(this, String.format(
					"%s isn't a%s %s",
					nameTC, typeAssertionVowel ? "n" : "", typeAssertionName));
		}
		
		return null;
	}
	
	@Override public boolean isGenerated() {
		return position.getGeneratedBy() != null;
	}
	
	@Override public Node getGeneratedBy() {
		return position.getGeneratedBy();
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
	
	@Override public Node setPosition(Position position) {
		if (position == null) throw new NullPointerException("position");
		this.position = position;
		return this;
	}
	
	@Override public String toString() {
//		SourcePrinter printer = new SourcePrinter();
//		this.accept(printer);
//		return printer.toString();
		return super.toString();
	}
}
