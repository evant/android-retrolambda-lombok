package lombok.ast.grammar;

import lombok.ast.Node;

import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;

public class StructuresParser extends BaseParser<Node, StructuresAction> {
	private final ParserGroup group;
	
	public StructuresParser(ParserGroup group) {
		super(Parboiled.createActions(StructuresAction.class));
		this.group = group;
	}
	
	public Rule classBody() {
		return enforcedSequence(
				ch('{'), group.basics.optWS(), ch('}'), group.basics.optWS());
	}
	
	public Rule methodArguments() {
		return enforcedSequence(
				ch('('),
				group.basics.optWS(),
				optional(sequence(
						group.operators.anyExpression(),
						zeroOrMore(sequence(
								ch(','),
								group.basics.optWS(),
								group.operators.anyExpression())))),
				ch(')'),
				group.basics.optWS());
	}
}
