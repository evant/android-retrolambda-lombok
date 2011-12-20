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

import java.util.List;

public interface Expression extends Node, AnnotationValue {
	/**
	 * Returns the actual amount of parentheses physically around this expression.
	 * 
	 * @see #astParensPositions()
	 * @see #getIntendedParens()
	 */
	int getParens();
	
	/**
	 * Returns the same value as {@link #getParens()}, <i>unless</i> that method returns {@code 0},
	 * and {@link #needsParentheses()} is {@code true}, then this method returns {@code 1}.
	 */
	int getIntendedParens();
	
	/**
	 * Returns the start/end position of each paren pair around this node. The only canonical aspect
	 * of this list is the size of it. The positions are set to appropriate files after parsing, from
	 * the innermost parens at index 0 to the outermost at the final index.
	 * 
	 * @see #getParens()
	 */
	List<Position> astParensPositions();
	
	/**
	 * Returns {@code true} if the expression would need parentheses because without them the interpretation
	 * of this node would be different, due to operator precedence rules.
	 * 
	 * @see #getIntendedParens()
	 */
	boolean needsParentheses();
	
	/**
	 * Returns true if the given expression is a valid statement expression.
	 * Statement expressions can be executed as statements simply by appending a semicolon to them.
	 */
	boolean isStatementExpression();
}
