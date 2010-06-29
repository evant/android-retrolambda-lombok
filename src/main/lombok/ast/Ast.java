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

/**
 * Contains utility methods for traversing and manipulating ASTs.
 */
public class Ast {
	/**
	 * Walks the parentage of {@code node} until it encounters either the top ({@code null} as parent), or a node
	 * that is an instance of {@code type}. If {@code node} itself is an instance of {@code type}, then it'll be returned.
	 * If the top is reached, or {@code} node is {@code null}, then {@code null} is returned.
	 */
	public static <N extends Node> N closestParentOfType(Class<N> type, Node node) {
		while (node != null) {
			if (type.isInstance(node)) return type.cast(node);
			node = node.getParent();
		}
		return null;
	}
	
	/**
	 * Walks the parentage of {@code node} and checks that each parent is of the stated types, then returns the highest
	 * parent in the chain, or {@code null} if the parentage isn't as expected. The parent chain must be listed in reverse.
	 * 
	 * For example:
	 * 
	 * {@code up(someVariableEntry, TypeDeclaration.class, TypeBody.class, VariableDeclaration.class, VariableDefinition.class)}
	 * 
	 * will get the type declaration that contains the listed field declaration, or {@code null} if this is some other
	 * kind of definition, such as the one in a for loop, or a local variable.
	 */
	public static <N extends Node> N up(Node node, Class<N> target, Class<?>... parents) {
		Node n = node.getParent();
		for (int i = parents.length - 1; i >= 0; i--) {
			if (!parents[i].isInstance(n)) return null;
			n = node.getParent();
		}
		if (target.isInstance(n)) return target.cast(n);
		return null;
	}
	
	/* TODO
	 * A) up(VariableDeclaration.class, "definition", up(VariableDefinition.class, "entries", entry))
	 * B) up(entry, VariableDeclaration.class, "VariableDefinition:entries", "VariableDeclaration:definition");
	 * C) ifDefinitionOfVariableDeclaration(ifEntryOfVariableDefinition(entry))
	 * D) as above but not auto-generated, instead written on an as-needed basis with better names, e.g: ifFieldGetTypeBody().
	 */
	
	/**
	 * Sets the position of {@code node} to {@code position}, and then does the same for all of {@code node}'s children, recursively.
	 */
	public static Node setAllPositions(Node node, Position position) {
		node.setPosition(position);
		for (Node child : node.getChildren()) setAllPositions(child, position);
		return node;
	}
	
	/**
	 * Get the current lombok.ast version.
	 */
	public static String getVersion() {
		return Version.getVersion();
	}
}
