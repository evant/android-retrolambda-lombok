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

import java.util.List;
import java.util.Stack;

import lombok.Getter;
import lombok.ast.Node;
import lombok.ast.grammar.Source;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class TextFormatter implements SourceFormatter {
	private static final String INDENT = "    ";
	private String newlineText = "\n";
	@Getter private final Source source;
	private final StringBuilder sb = new StringBuilder();
	private final List<String> errors = Lists.newArrayList();
	private int indent;
	private boolean suppress, newline;
	
	public TextFormatter(Source source) {
		this.source = source;
	}
	
	protected void setNewlineText(String newlineText) {
		this.newlineText = newlineText;
	}
	
	private TextFormatter a(String text) {
		if (text.length() == 0) return this;
		if (newline) printIndent();
		newline = false;
		sb.append(text);
		return this;
	}
	
	protected int getCurrentPosition(boolean accountForNewline) {
		int len = sb.length();
		if (accountForNewline && newline) {
			if (len > 0) len += newlineText.length();	//actual \n character.
			len += INDENT.length() * indent;
		}
		return len;
	}
	
	private TextFormatter a(char text) {
		return a(String.valueOf(text));
	}
	
	@Override public void fail(String fail) {
		a(FAIL).a(fail).a(FAIL);
	}
	
	@Override public void keyword(String text) {
		a(text);
	}
	
	@Override public void operator(String text) {
		a(text);
	}
	
	@Override public void verticalSpace() {
		printIndent();
	}
	
	@Override public void space() {
		a(" ");
	}
	
	@Override public void append(String text) {
		a(text);
	}
	
	@Override public void buildInline(Node node) {
	}
	
	@Override public void closeInline() {
	}
	
	@Override public void startSuppressBlock() {
		suppress = true;
	}
	
	@Override public void endSuppressBlock() {
		suppress = false;
	}
	
	@Override
	public void startSuppressIndent() {
		indent--;
	}
	@Override
	public void endSuppressIndent() {
		indent++;
	}
	
	private void printIndent() {
		if (sb.length() > 0) sb.append(newlineText);
		for (int i = 0; i < indent; i++) sb.append(INDENT);
	}
	
	private Stack<Integer> blockSuppressedStack = new Stack<Integer>();
	
	@Override public void buildBlock(Node node) {
		blockSuppressedStack.push((suppress ? 1 : 0) | (node == null ? 2 : 0));
		
		if (!suppress) {
			newline = true;
			if (node == null) indent++;
		}
		
		suppress = false;
	}
	
	@Override public void closeBlock() {
		int code = blockSuppressedStack.pop();
		if ((code & 2) > 0) indent--;
		if ((code & 1) == 0) newline = true;
	}
	
	@Override public void addError(int errorStart, int errorEnd, String errorMessage) {
		errors.add(String.format("%d-%d: %s", errorStart, errorEnd, errorMessage));
	}
	
	@Override public String finish() {
		if (!errors.isEmpty()) {
			a(newlineText).a(newlineText).a(newlineText).a("ERRORS: ").a(newlineText);
			a(Joiner.on(newlineText).join(errors));
			errors.clear();
		}
		return sb.toString();
	}
	
	@Override public void setTimeTaken(long taken) {
	}
	
	@Override public void nameNextElement(String name) {
	}
}
