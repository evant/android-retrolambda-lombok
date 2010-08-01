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
package lombok.ast.printer;

import java.io.IOException;

import lombok.ast.Node;

public interface SourceFormatter {
	String FAIL = "?!?";
	
	/**
	 * The AST is not valid; it applies to the current level.
	 * 
	 * @param fail Human readable explanation of what's wrong with the AST at this level.
	 */
	void fail(String fail);
	
	void property(String name, Object value);
	void keyword(String text);
	void operator(String text);
	
	/**
	 * Generate an extra vertical spacer if the output format allows for it. Only occurs between block elements.
	 * Example: Between a package statement and the import statements, {@code verticalSpace} will be called.
	 */
	void verticalSpace();
	
	/**
	 * Generate horizontal space. Where relevant, the space should be 1 character wide.
	 */
	void space();
	
	/**
	 * Add raw text to the output stream.
	 * 
	 * NB: Parentheses, braces and brackets that are structurally relevant as opening and/or closing a group are always appended separately; therefore, if you
	 * want to do special highlighting on parens that go together, you only have to check if the text appended is a single character long and contains a brace, bracket, or paren.
	 * 
	 * @param text The raw text to print. If your output format requires it, it is the implementor's responsibility to escape this text.
	 */
	void append(String text);
	
	/**
	 * Opens a new AST level which is normally rendered inline (example: expressions, literals, modifiers, identifiers, etc).
	 * 
	 * @param node The AST node that this level represents. Can be {@code null} which signals that the current node has an inline substructure.
	 */
	void buildInline(Node node);
	
	/**
	 * Closes the previous {@link #buildInline(Node)} call.
	 */
	void closeInline();
	
	/**
	 * The next {@code buildBlock(Node)} call should <em>NOT</em> be rendered in its own vertical area, but should instead be treated as an {@code inline} element.
	 * Example: The execution statement is part of an if statement but has to be a statement, which is normally a block element. This call is generated before
	 * the execution statement so that the execution statement is printed inline with the if statement.
	 */
	void startSuppressBlock();
	
	/**
	 * Closes the previous {@link #startSuppressBlock()} call.
	 */
	void endSuppressBlock();
	
	/**
	 * The next {@code buildBlock(Node)} call should <em>NOT</em> indent.
	 * Example: The {@code case} or {@code default} statement inside a {@code switch} block.
	 */
	void startSuppressIndent();
	
	/**
	 * Closes the previous {@link #startSuppressIndent()} call.
	 */
	void endSuppressIndent();
	
	/**
	 * Opens a new AST level which is normally rendered as a block (example: statements, methods, type bodies).
	 * 
	 * @param node The AST node that this level represents. Can be {@code null} which signals that the current node has a block substructure.
	 */
	void buildBlock(Node node);
	
	/**
	 * Closes the previous {@link #buildBlock(Node)} call.
	 */
	void closeBlock();
	
	/**
	 * Registers a parse error with the formatter.
	 */
	void addError(int errorStart, int errorEnd, String errorMessage);
	
	/**
	 * Generate the source representation and return it as a string.
	 */
	String finish() throws IOException;
	
	/**
	 * Reports the total time taken in milliseconds by the parser.
	 */
	void setTimeTaken(long taken);
	
	/**
	 * The next {@link #buildInline(Node)} or {@link #buildBlock(Node)}'s relation to the current block is named by this call.
	 * 
	 * Not all elements will get a name, and the name only applies to the next {@code buildBlock/Inline} call, not to any further calls.
	 */
	void nameNextElement(String name);
}
