package lombok.ast;

public interface TypeDeclaration extends Node, DescribedNode {
	Node getRawModifiers();
	Modifiers getModifiers();
}
