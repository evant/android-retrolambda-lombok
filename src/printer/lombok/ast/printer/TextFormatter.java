package lombok.ast.printer;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import lombok.ast.Node;

import org.apache.commons.lang.StringUtils;

public class TextFormatter implements SourceFormatter {
	private static final String INDENT = "    ";
	private final StringBuilder sb = new StringBuilder();
	private final List<String> errors = new ArrayList<String>();
	private int indent;
	private boolean suppress, newline;
	
	private TextFormatter a(String text) {
		if (text.length() == 0) return this;
		if (newline) printIndent();
		newline = false;
		sb.append(text);
		return this;
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
		if (sb.length() > 0) sb.append("\n");
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
			a("\n\n\nERRORS: \n");
			a(StringUtils.join(errors, '\n'));
			errors.clear();
		}
		return sb.toString();
	}
	
	@Override public void setTimeTaken(long taken) {
	}
	
	@Override public void nameNextElement(String name) {
	}
}
