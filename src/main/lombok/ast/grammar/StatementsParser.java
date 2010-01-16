package lombok.ast.grammar;

import lombok.ast.Node;

import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;

public class StatementsParser extends BaseParser<Node, StatementsActions> {
	private final ParserGroup group;
	
	public StatementsParser(ParserGroup group) {
		super(Parboiled.createActions(StatementsActions.class));
		this.group = group;
	}
	
	public Rule anyStatement() {
		return firstOf(
				blockStatement(),
				localClassDeclaration(),
				localVariableDeclaration());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.2
	 */
	public Rule blockStatement() {
		return sequence(
				ch('{'), group.basics.optWS(),
				zeroOrMore(anyStatement().label("statement")),
				ch('}'), group.basics.optWS(),
				SET(actions.createBlock(VALUES("zeroOrMore/statement"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.3
	 */
	public Rule localClassDeclaration() {
		return group.structures.classDeclaration();
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.4
	 */
	public Rule localVariableDeclaration() {
		return sequence(
				zeroOrMore(group.structures.keywordModifier()),
				group.types.type(),
				localVariableDeclarationPart(),
				zeroOrMore(sequence(
						ch(','), group.basics.optWS(),
						localVariableDeclarationPart())),
				ch(';'), group.basics.optWS());
	}
	
	public Rule localVariableDeclarationPart() {
		return sequence(
				group.basics.identifier(),
				zeroOrMore(enforcedSequence(ch('['), group.basics.optWS(), ch(']'), group.basics.optWS())),
				optional(sequence(
						ch('='), group.basics.optWS(),
						firstOf(
								group.expressions.arrayInitializer(),
								group.expressions.anyExpression()))));
	}
}
