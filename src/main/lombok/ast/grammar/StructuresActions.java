package lombok.ast.grammar;

import java.util.List;

import lombok.ast.KeywordModifier;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;

import org.parboiled.BaseActions;

public class StructuresActions extends BaseActions<Node> {
	public Node createMethodArguments(Node head, List<Node> tail) {
		MethodInvocation mi = new MethodInvocation();
		if (head != null) mi.arguments().addToEndRaw(head);
		if (tail != null) for (Node n : tail) mi.arguments().addToEndRaw(n);
		return mi;
	}
	
	public Node createKeywordModifier(String text) {
		return new KeywordModifier().setName(text);
	}
}
