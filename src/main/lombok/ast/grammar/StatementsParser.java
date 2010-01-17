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
		return labelledStatement();
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.2
	 */
	public Rule blockStatement() {
		return sequence(
				ch('{'), group.basics.optWS(),
				zeroOrMore(anyStatement()),
				ch('}'), group.basics.optWS(),
				SET(actions.createBlock(VALUES("zeroOrMore/anyStatement"))));
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
				zeroOrMore(group.structures.anyModifier()),
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
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.6
	 */
	private Rule emptyStatement() {
		return sequence(
				ch(';'),
				group.basics.optWS(),
				SET(actions.createEmptyStatement()));
	}
	
	/**
	 * Labels aren't statements; instead they can prefix any statement. Something like {@code if (1 == 1) foo: a();} is legal.
	 * Multiple labels for the same statement is also legal.
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.7
	 */
	public Rule labelledStatement() {
		return sequence(
				zeroOrMore(sequence(
						group.basics.identifier().label("labelName"),
						ch(':'),
						group.basics.optWS())),
				firstOf(
						blockStatement(),
						localClassDeclaration(),
						localVariableDeclaration(),
						emptyStatement(),
						expressionStatement(),
						ifStatement(),
						assertStatement(),
						switchStatement(),
						caseStatement(),
						defaultStatement(),
						whileStatement(),
						doWhileStatement()
				).label("statement"),
				SET(actions.createLabelledStatement(VALUES("zeroOrMore/sequence/labelName"), VALUE("statement"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.8
	 */
	public Rule expressionStatement() {
		return sequence(group.expressions.statementExpression(), ch(';'), group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.9
	 */
	public Rule ifStatement() {
		return sequence(
				string("if"), group.basics.testLexBreak(), group.basics.optWS(),
				ch('('), group.basics.optWS(),
				group.expressions.anyExpression().label("condition"),
				ch(')'), group.basics.optWS(),
				anyStatement(), SET(),
				optional(sequence(
						string("else"), group.basics.testLexBreak(), group.basics.optWS(),
						anyStatement()).label("else")),
				SET(actions.createIfStatement(VALUE("condition"), VALUE(), VALUE("optional/else"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.10
	 */
	public Rule assertStatement() {
		return sequence(
				string("assert"), group.basics.testLexBreak(), group.basics.optWS(),
				group.expressions.anyExpression(),
				SET(),
				optional(sequence(
						ch(':'), group.basics.optWS(),
						group.expressions.anyExpression(), SET())),
				ch(';'), group.basics.optWS(),
				SET(actions.createAssertStatement(VALUE(), VALUE("optional/sequence"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.11
	 */
	public Rule switchStatement() {
		return sequence(
				string("switch"), group.basics.testLexBreak(), group.basics.optWS(),
				ch('('), group.basics.optWS(),
				group.expressions.anyExpression(), SET(),
				ch(')'), group.basics.optWS(),
				blockStatement(),
				SET(actions.createSwitchStatement(VALUE(), LAST_VALUE())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.11
	 */
	public Rule caseStatement() {
		return sequence(
				string("case"), group.basics.testLexBreak(), group.basics.optWS(),
				group.expressions.anyExpression(), SET(),
				ch(':'), group.basics.optWS(),
				SET(actions.createCaseStatement(VALUE())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.11
	 */
	public Rule defaultStatement() {
		return sequence(
				string("default"), group.basics.testLexBreak(), group.basics.optWS(),
				ch(':'), group.basics.optWS(),
				SET(actions.createDefaultStatement()));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.12
	 */
	public Rule whileStatement() {
		return sequence(
				string("while"), group.basics.testLexBreak(), group.basics.optWS(),
				ch('('), group.basics.optWS(),
				group.expressions.anyExpression().label("condition"),
				ch(')'), group.basics.optWS(),
				anyStatement(), SET(),
				SET(actions.createWhileStatement(VALUE("condition"), VALUE())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.13
	 */
	public Rule doWhileStatement() {
		return sequence(
				string("do"), group.basics.testLexBreak(), group.basics.optWS(),
				anyStatement(), SET(),
				string("while"), group.basics.testLexBreak(), group.basics.optWS(),
				ch('('), group.basics.optWS(),
				group.expressions.anyExpression().label("condition"),
				ch(')'), group.basics.optWS(),
				ch(';'), group.basics.optWS(),
				SET(actions.createDoStatement(VALUE("condition"), VALUE())));
	}
}
