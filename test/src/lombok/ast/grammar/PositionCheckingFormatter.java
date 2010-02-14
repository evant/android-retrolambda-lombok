package lombok.ast.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import lombok.ast.AstException;
import lombok.ast.Node;
import lombok.ast.Position;
import lombok.ast.printer.TextFormatter;

public class PositionCheckingFormatter extends TextFormatter {
	private Stack<Node> nodeStack = new Stack<Node>();
	private List<AstException> problems = new ArrayList<AstException>();
	
	public PositionCheckingFormatter(String rawSource) {
		super(rawSource);
	}
	
	public List<AstException> getProblems() {
		List<AstException> p = new ArrayList<AstException>(problems);
		if (!nodeStack.isEmpty()) p.add(new AstException(nodeStack.peek(), "This node was never closed"));
		return p;
	}
	
	@Override public void buildBlock(Node node) {
		super.buildBlock(node);
		registerNode(node);
	}
	
	private void registerNode(Node node) {
		nodeStack.add(node);
		if (node == null) return;
		
		Position p = node.getPosition();
		if (p.isUnplaced()) problems.add(new AstException(node, "this node is unplaced"));
		else {
			int delta = p.getStart() - getCurrentPosition(true);
			if (delta != 0) {
				int actualPos = getCurrentPosition(true);
				int repPos = p.getStart();
				reportError(node, "start", actualPos, repPos);
			}
		}
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
		String raw = getRawSource();
		String actualPrefix = getCharsBeforePosition(actualPos, raw);
		String actualPostfix = getCharsAfterPosition(actualPos, raw);
		String reportedPrefix = getCharsBeforePosition(repPos, raw);
		String reportedPostfix = getCharsAfterPosition(repPos, raw);
//		if (node instanceof BinaryExpression) System.err.println(
//				((IntegralLiteral)
//				((BinaryExpression)node).getLeft()).intValue());
		problems.add(new AstException(node, String.format(
			"this node's " + description + " is misreported with %+d\nactual: \"%s⊚%s\"\nreported: \"%s⊚%s\"",
			delta, actualPrefix, actualPostfix, reportedPrefix, reportedPostfix)));
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
