package lombok.ast.grammar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.ast.Node;

import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;

public class StructuresParser extends BaseParser<Node, StructuresActions> {
	private final ParserGroup group;
	
	public StructuresParser(ParserGroup group) {
		super(Parboiled.createActions(StructuresActions.class));
		this.group = group;
	}
	
	public Rule classBody() {
		//TODO dummy
		return enforcedSequence(
				ch('{'), group.basics.optWS(), ch('}'), group.basics.optWS());
	}
	
	public Rule methodArguments() {
		return enforcedSequence(
				ch('('),
				group.basics.optWS(),
				optional(sequence(
						group.expressions.anyExpression(),
						SET(),
						zeroOrMore(sequence(
								ch(','),
								group.basics.optWS(),
								group.expressions.anyExpression(), SET())))),
				ch(')'),
				group.basics.optWS(),
				SET(actions.createMethodArguments(VALUE("optional/sequence"), VALUES("optional/sequence/zeroOrMore/sequence"))));
	}
	
	public Rule classDeclaration() {
		//TODO dummy
		return sequence(
				string("class"),
				group.basics.testLexBreak(),
				group.basics.optWS(),
				group.basics.identifier(),
				classBody());
	}
	
	public Rule annotation() {
		//TODO dummy
		return sequence(ch('@'), group.basics.optWS(), group.types.type());
	}
	
	private static final List<String> MODIFIER_KEYWORDS = Collections.unmodifiableList(Arrays.asList(
			"final", "strictfp", "abstract", "transient", "volatile",
			"public", "protected", "private", "static", "native"
			));
	
	public Rule keywordModifier() {
		Object[] tail = new Object[MODIFIER_KEYWORDS.size() - 2];
		for (int i = 2; i < MODIFIER_KEYWORDS.size(); i++) tail[i-2] = string(MODIFIER_KEYWORDS.get(i));
		
		return sequence(
				firstOf(string(MODIFIER_KEYWORDS.get(0)), string(MODIFIER_KEYWORDS.get(1)), tail).label("keyword"),
				group.basics.testLexBreak(),
				SET(actions.createKeywordModifier(TEXT("keyword"))),
				group.basics.optWS());
	}
	
	public Rule anyModifier() {
		return firstOf(annotation(), keywordModifier());
	}
}
