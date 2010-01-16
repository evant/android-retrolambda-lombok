package lombok.ast.grammar;

import java.util.List;

import lombok.ast.Block;
import lombok.ast.Node;

import org.parboiled.BaseActions;

public class StatementsActions extends BaseActions<Node> {
	public Node createBlock(List<Node> statements) {
		Block b = new Block();
		if (statements != null) for (Node s : statements) {
			if (s != null) b.contents().addToEndRaw(s);
		}
		
		return b;
	}
}
