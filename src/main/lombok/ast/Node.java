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

import java.util.List;

public interface Node {
	boolean isSyntacticallyValid();
	
	/**
	 * Add a {@link SyntaxProblem} to the list for each syntactic problem with your node, then call this method on your child nodes.
	 * Something like {@code a +} is not syntactically valid (It's missing second argument to binary operator), but something like
	 * {@code a + b} would be, <i>even if</i> both {@code a} and {@code b} end up being objects, which do not support the + operator.
	 * That is a semantic and not a syntactic problem.
	 */
	void checkSyntacticValidity(List<SyntaxProblem> problems);
	
	boolean isGenerated();
	
	Node getGeneratedBy();
	
	boolean hasParent();
	
	void setPosition(Position position);
	
	void accept(ASTVisitor visitor);
	
	Node copy();
	
	String toString();
	
	Node getParent();
	
	Position getPosition();
}
