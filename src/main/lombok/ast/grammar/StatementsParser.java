/*
 * Copyright © 2010 Reinier Zwitserloot and Roel Spilker.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.ast.grammar;

import lombok.ast.Node;

import org.parboiled.BaseParser;
import org.parboiled.Rule;

public class StatementsParser extends BaseParser<Node> {
	final ParserGroup group;
	final StatementsActions actions;
	
	public StatementsParser(ParserGroup group) {
		this.actions = new StatementsActions(group.getSource());
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
				zeroOrMore(anyStatement().label("statement")),
				ch('}'), group.basics.optWS(),
				SET(actions.createBlock(VALUES("zeroOrMore/statement"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.3
	 */
	public Rule localClassDeclaration() {
		return group.structures.classOrInterfaceDeclaration();
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.4
	 */
	public Rule variableDefinition() {
		return sequence(
				group.structures.variableDefinitionModifiers().label("modifiers"),
				group.structures.variableDefinition(), SET(), SET(actions.posify(VALUE())),
				SET(actions.addLocalVariableModifiers(VALUE(), VALUE("modifiers"))));
	}
	
	public Rule localVariableDeclaration() {
		return sequence(
				variableDefinition().label("definition"),
				ch(';'), group.basics.optWS(),
				SET(actions.createVariableDeclaration(VALUE("definition"))));
	}
	
	public Rule explicitAlternateConstructorInvocation() {
		return sequence(
				group.types.typeArguments().label("typeArgs"),
				string("this"), group.basics.testLexBreak(), group.basics.optWS(),
				group.structures.methodArguments().label("arguments"),
				ch(';'), group.basics.optWS(),
				SET(actions.createAlternateConstructorInvocation(VALUE("typeArgs"), VALUE("arguments"))));
	}
	
	public Rule explicitSuperConstructorInvocation() {
		return sequence(
				optional(sequence(group.expressions.allPrimaryExpressions(), ch('.').label("dot"), group.basics.optWS())).label("qualifier"),
				group.types.typeArguments().label("typeArgs"),
				string("super"), group.basics.testLexBreak(), group.basics.optWS(),
				group.structures.methodArguments().label("arguments"),
				ch(';'), group.basics.optWS(),
				SET(actions.createSuperConstructorInvocation(NODE("qualifier/sequence/dot"), VALUE("qualifier"), VALUE("typeArgs"), VALUE("arguments"))));
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
						enhancedForStatement(),
						breakStatement(),
						continueStatement(),
						returnStatement(),
						synchronizedStatement(),
						throwStatement(),
						tryStatement(),
						explicitAlternateConstructorInvocation(),
						explicitSuperConstructorInvocation()
				).label("statement"),
				SET(actions.createLabelledStatement(VALUES("zeroOrMore/sequence/labelName"), VALUE("statement"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.8
	 */
	public Rule expressionStatement() {
		return sequence(
				group.expressions.statementExpression().label("expression"),
				ch(';'), group.basics.optWS(),
				SET(actions.createExpressionStatement(VALUE("expression"))));
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
				variableDefinition(),
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
				group.structures.variableDefinitionModifiers().label("modifiers"),
				group.types.type().label("type"),
				group.structures.variableDefinitionPartNoAssign().label("varDeclPart"),
				ch(':'), group.basics.optWS(),
				group.expressions.anyExpression().label("iterable"),
				ch(')'), group.basics.optWS(),
				anyStatement().label("statement"),
				SET(actions.createEnhancedFor(NODE("modifiers"), VALUE("type"), NODE("varDeclPart"), VALUE("iterable"), VALUE("statement"))));
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
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.17
	 */
	public Rule returnStatement() {
		return sequence(
				string("return"), group.basics.testLexBreak(), group.basics.optWS(),
				optional(group.expressions.anyExpression()).label("value"),
				ch(';'), group.basics.optWS(),
				SET(actions.createReturn(VALUE("value"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.18
	 */
	public Rule throwStatement() {
		return sequence(
				string("throw"), group.basics.testLexBreak(), group.basics.optWS(),
				group.expressions.anyExpression().label("throwable"),
				ch(';'), group.basics.optWS(),
				SET(actions.createThrow(VALUE("throwable"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.19
	 */
	public Rule synchronizedStatement() {
		return sequence(
				string("synchronized"), group.basics.testLexBreak(), group.basics.optWS(),
				ch('('), group.basics.optWS(),
				group.expressions.anyExpression().label("lock"),
				ch(')'), group.basics.optWS(),
				blockStatement().label("body"),
				SET(actions.createSynchronizedStatement(VALUE("lock"), VALUE("body"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.20
	 */
	public Rule tryStatement() {
		return sequence(
				string("try"), group.basics.testLexBreak(), group.basics.optWS(),
				blockStatement().label("body"),
				zeroOrMore(catchBlock().label("catchBlock")),
				optional(sequence(
						string("finally"), group.basics.testLexBreak(), group.basics.optWS(),
						blockStatement().label("finallyBody"))),
				SET(actions.createTryStatement(VALUE("body"), VALUES("zeroOrMore/catchBlock"), VALUE("optional/sequence/finallyBody"))));
	}
	
	Rule catchBlock() {
		return sequence(
				string("catch"), group.basics.testLexBreak(), group.basics.optWS(),
				ch('('), group.basics.optWS(),
				group.structures.variableDefinitionModifiers().label("modifiers"),
				group.types.type().label("type"),
				group.basics.identifier().label("varName"),
				ch(')'), group.basics.optWS(),
				blockStatement().label("body"),
				SET(actions.createCatch(VALUE("modifiers"), VALUE("type"), VALUE("varName"), VALUE("body"))));
	}
}
