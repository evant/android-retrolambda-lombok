package lombok.ast;

public interface DescribedNode extends Node {
	/**
	 * A very short string that is particularly useful in describing this node.
	 * Can always be {@code null} to indicate this particular instance has no useful name (usually, but not neccessarily, because it is not valid).
	 * 
	 * <strong>NB: This method should never throw an exception!</strong>
	 */
	String getDescription();
}
