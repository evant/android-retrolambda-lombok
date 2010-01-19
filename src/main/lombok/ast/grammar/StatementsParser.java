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
	Rule emptyStatement() {
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
						doWhileStatement(),
						basicForStatement(),
						enhancedForStatement()
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
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.14.1.1
	 */
	public Rule basicForStatement() {
		return sequence(
				string("for"), group.basics.testLexBreak(), group.basics.optWS(),
				ch('('), group.basics.optWS(),
				forInit().label("init"),
				ch(';'), group.basics.optWS(),
				optional(group.expressions.anyExpression()).label("condition"),
				ch(';'), group.basics.optWS(),
				forUpdate().label("update"),
				ch(')'), group.basics.optWS(),
				anyStatement().label("statement"),
				SET(actions.createBasicFor(VALUE("init"), VALUE("condition"), VALUE("update"), VALUE("statement"))));
	}
	
	Rule forInit() {
		return optional(firstOf(
				localVariableDeclaration(),
				statementExpressionList()));
	}
	
	Rule forUpdate() {
		return optional(statementExpressionList());
	}
	
	Rule statementExpressionList() {
		return sequence(
				group.expressions.statementExpression().label("head"),
				zeroOrMore(sequence(
						ch(','), group.basics.optWS(),
						group.expressions.statementExpression()).label("tail")),
				SET(actions.createStatementExpressionList(VALUE("head"), VALUES("zeroOrMore/tail"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.14.2
	 * @see http://bugs.sun.com/view_bug.do?bug_id=1699917
	 */
	public Rule enhancedForStatement() {
		return sequence(
				string("for"), group.basics.testLexBreak(), group.basics.optWS(),
				ch('('), group.basics.optWS(),
				zeroOrMore(group.structures.anyModifier().label("modifier")),
				group.types.type().label("type"),
				group.basics.identifier().label("varName"),
				zeroOrMore(sequence(ch('['), group.basics.optWS(), ch(']'), group.basics.optWS()).label("dim")).label("dims"),
				ch(':'), group.basics.optWS(),
				group.expressions.anyExpression().label("iterable"),
				ch(')'), group.basics.optWS(),
				anyStatement().label("statement"),
				SET(actions.createEnhancedFor(VALUES("zeroOrMore/modifier"), VALUE("type"), VALUE("varName"), TEXTS("dims/dim"), VALUE("iterable"), VALUE("statement"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.15
	 */
	public Rule breakStatement() {
		return sequence(
				string("break"), group.basics.testLexBreak(), group.basics.optWS(),
				optional(group.basics.identifier()).label("identifier"),
				ch(';'), group.basics.optWS(),
				SET(actions.createBreak(VALUE("identifier"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.16
	 */
	public Rule continueStatement() {
		return sequence(
				string("continue"), group.basics.testLexBreak(), group.basics.optWS(),
				optional(group.basics.identifier()).label("identifier"),
				ch(';'), group.basics.optWS(),
				SET(actions.createContinue(VALUE("identifier"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.16
	 */
	public Rule returnStatement() {
		return sequence(
				string("return"), group.basics.testLexBreak(), group.basics.optWS(),
				optional(group.expressions.anyExpression()).label("value"),
				ch(';'), group.basics.optWS(),
				SET(actions.createReturn(VALUE("value"))));
	}
	
	public Rule throwStatement() {
		return sequence(
				string("throw"), group.basics.testLexBreak(), group.basics.optWS(),
				group.expressions.anyExpression().label("throwable"),
				ch(';'), group.basics.optWS(),
				SET(actions.createThrow(VALUE("throwable"))));
	}
}
