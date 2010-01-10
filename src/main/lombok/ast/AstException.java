package lombok.ast;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=false)
public class AstException extends RuntimeException {
	private final Node problemNode;
	
	public AstException(Node problemNode, String message) {
		super(message);
		this.problemNode = problemNode;
	}
	
	@Override public String toString() {
		if (problemNode == null && getMessage() == null) return "AstException (unknown cause)";
		if (problemNode == null) return "AstException: " + getMessage();
		if (getMessage() == null) return "AstException at " + problemNode;
		return String.format("AstException: %s (at %s)", getMessage(), problemNode);
	}
}
