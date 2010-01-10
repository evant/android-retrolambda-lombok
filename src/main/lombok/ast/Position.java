package lombok.ast;

import lombok.Data;

/**
 * The position information which lets you find the raw characters that represent this node in a source file.
 * For generated nodes, {@code generatedBy} is set to non-{@code null} and the {@code start} and {@code end}
 * refer to the places where the node would have appeared if it wasn't generated.
 */
@Data
public class Position {
	private int start, end;
	private Node generatedBy;
	
	public Position(int start, int end) {
		this.start = start;
		this.end = end;
		this.generatedBy = null;
	}
	
	public Position(int start, int end, Node generatedBy) {
		this.start = start;
		this.end = end;
		this.generatedBy = generatedBy;
	}
}
