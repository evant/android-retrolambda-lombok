/*
 * Copyright (C) 2010 The Project Lombok Authors.
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
	
	public static CompilationUnit upToTop(Node node) {
		while (node != null && !(node instanceof CompilationUnit)) node = node.getParent();
		return (CompilationUnit) node;
	}
}
