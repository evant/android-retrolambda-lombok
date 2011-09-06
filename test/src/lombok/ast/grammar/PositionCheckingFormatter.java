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

import java.util.List;
import java.util.Stack;

import com.google.common.collect.Lists;

import lombok.ast.AstException;
import lombok.ast.Node;
import lombok.ast.Position;
import lombok.ast.printer.TextFormatter;

public class PositionCheckingFormatter extends TextFormatter {
	private Stack<Node> nodeStack = new Stack<Node>();
	private List<AstException> problems = Lists.newArrayList();
	private Source source;
	
	public PositionCheckingFormatter(Source source) {
		this.source = source;
		if ("\r\n".equals(System.getProperty("line.separator", "\n")) && source.getRawInput().contains("\r\n")) {
			setNewlineText("\r\n");
		}
	}
	
	public List<AstException> getProblems() {
		List<AstException> p = Lists.newArrayList(problems);
		if (!nodeStack.isEmpty()) p.add(new AstException(nodeStack.peek(), "This node was never closed"));
		return p;
	}
	
	private void registerNode(Node node) {
		nodeStack.add(node);
		if (node == null) return;
		
		Position p = node.getPosition();
		if (p.isUnplaced()) problems.add(new AstException(node, String.format(
				"this node is unplaced [%s]", node.getClass().getSimpleName())));
		else {
			int delta = p.getStart() - getCurrentPosition(true);
			if (delta != 0) {
				int actualPos = getCurrentPosition(true);
				int repPos = p.getStart();
				reportError(node, "start", actualPos, repPos);
			}
		}
	}
	
	@Override public void buildBlock(Node node) {
		super.buildBlock(node);
		registerNode(node);
	}
	
	@Override public void buildInline(Node node) {
		super.buildInline(node);
		registerNode(node);
	}
	
	@Override public void closeBlock() {
		super.closeBlock();
		checkNodeClose();
	}
	
	@Override public void closeInline() {
		super.closeInline();
		checkNodeClose();
	}
	
	private void checkNodeClose() {
		Node node = nodeStack.pop();
		if (node == null) return;
		Position p = node.getPosition();
		if (p.isUnplaced()) return;	//opener already generated error.
		if (p.getStart() > p.getEnd()) problems.add(new AstException(node, "Node ends before it starts."));
		int delta = p.getEnd() - Math.max(getCurrentPosition(false), p.getStart());
		if (delta != 0) {
			int actualPos = getCurrentPosition(false);
			int repPos = p.getEnd();
			reportError(node, "end", actualPos, repPos);
		}
	}
	
	private static final int RANGE = 12;
	private void reportError(Node node, String description, int actualPos, int repPos) {
		int delta = repPos - actualPos;
		String raw = source.getRawInput();
		String actualPrefix = getCharsBeforePosition(actualPos, raw);
		String actualPostfix = getCharsAfterPosition(actualPos, raw);
		String reportedPrefix = getCharsBeforePosition(repPos, raw);
		String reportedPostfix = getCharsAfterPosition(repPos, raw);
		problems.add(new AstException(node, String.format(
			"this[%s] node's " + description + " is misreported with %+d\nactual(%d): \"%s⊚%s\"\nreported(%d): \"%s⊚%s\"\n%s",
			node.getClass().getSimpleName(),
			delta, actualPos, actualPrefix, actualPostfix, repPos, reportedPrefix, reportedPostfix, node)));
	}
	
	private String getCharsAfterPosition(int pos, String raw) {
		int max = raw.length();
		if (pos < 0) pos = 0;
		return pos >= max ? "" : raw.substring(pos, Math.min(pos + RANGE, max));
	}
	
	private String getCharsBeforePosition(int pos, String raw) {
		int max = raw.length();
		return pos > RANGE ? raw.substring(
				Math.min(max, pos-RANGE), Math.min(max, pos)) :
				raw.substring(0, Math.min(max, pos));
	}
}
