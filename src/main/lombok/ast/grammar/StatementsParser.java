/*
 * Copyright (C) 2010 The Project Lombok Authors.
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
		return Sequence(
				TestNot(Ch('}')),
				labelledStatement());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.2
	 */
	public Rule blockStatement() {
		return Sequence(
				Ch('{'), group.basics.optWS(),
				ZeroOrMore(anyStatement().label("statement")),
				Ch('}'), group.basics.optWS(),
				set(actions.createBlock(values("ZeroOrMore/statement"))));
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
		return Sequence(
				group.structures.variableDefinitionModifiers().label("modifiers"),
				group.structures.variableDefinition(), set(), set(actions.posify(value())),
				set(actions.addLocalVariableModifiers(value(), value("modifiers"))));
	}
	
	public Rule localVariableDeclaration() {
		return Sequence(
				variableDefinition().label("definition"),
				Ch(';'), group.basics.optWS(),
				set(actions.createVariableDeclaration(value("definition"))));
	}
	
	public Rule explicitAlternateConstructorInvocation() {
		return Sequence(
				group.types.typeArguments().label("typeArgs"),
				String("this"), group.basics.testLexBreak(), group.basics.optWS(),
				group.structures.methodArguments().label("arguments"),
				Ch(';'), group.basics.optWS(),
				set(actions.createAlternateConstructorInvocation(value("typeArgs"), value("arguments"))));
	}
	
	public Rule explicitSuperConstructorInvocation() {
		return Sequence(
				Optional(Sequence(group.expressions.allPrimaryExpressions(), Ch('.').label("dot"), group.basics.optWS())).label("qualifier"),
				group.types.typeArguments().label("typeArgs"),
				String("super"), group.basics.testLexBreak(), group.basics.optWS(),
				group.structures.methodArguments().label("arguments"),
				Ch(';'), group.basics.optWS(),
				set(actions.createSuperConstructorInvocation(node("qualifier/Sequence/dot"), value("qualifier"), value("typeArgs"), value("arguments"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.6
	 */
	Rule emptyStatement() {
		return Sequence(
				Ch(';'),
				group.basics.optWS(),
				set(actions.createEmptyStatement()));
	}
	
	/**
	 * Labels aren't statements; instead they can prefix any statement. Something like {@code if (1 == 1) foo: a();} is legal.
	 * Multiple labels for the same statement is also legal.
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.7
	 */
	public Rule labelledStatement() {
		return Sequence(
				ZeroOrMore(Sequence(
						group.basics.identifier().label("labelName"),
						Ch(':'),
						group.basics.optWS())),
				FirstOf(
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
				set(actions.createLabelledStatement(values("ZeroOrMore/Sequence/labelName"), value("statement"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.8
	 */
	public Rule expressionStatement() {
		return Sequence(
				group.expressions.statementExpression().label("expression"),
				Ch(';'), group.basics.optWS(),
				set(actions.createExpressionStatement(value("expression"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.9
	 */
	public Rule ifStatement() {
		return Sequence(
				String("if"), group.basics.testLexBreak(), group.basics.optWS(),
				Ch('('), group.basics.optWS(),
				group.expressions.anyExpression().label("condition"),
				Ch(')'), group.basics.optWS(),
				anyStatement(), set(),
				Optional(Sequence(
						String("else"), group.basics.testLexBreak(), group.basics.optWS(),
						anyStatement()).label("else")),
				set(actions.createIfStatement(value("condition"), value(), value("Optional/else"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.10
	 */
	public Rule assertStatement() {
		return Sequence(
				String("assert"), group.basics.testLexBreak(), group.basics.optWS(),
				group.expressions.anyExpression(),
				set(),
				Optional(Sequence(
						Ch(':'), group.basics.optWS(),
						group.expressions.anyExpression(), set())),
				Ch(';'), group.basics.optWS(),
				set(actions.createAssertStatement(value(), value("Optional/Sequence"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.11
	 */
	public Rule switchStatement() {
		return Sequence(
				String("switch"), group.basics.testLexBreak(), group.basics.optWS(),
				Ch('('), group.basics.optWS(),
				group.expressions.anyExpression(), set(),
				Ch(')'), group.basics.optWS(),
				blockStatement(),
				set(actions.createSwitchStatement(value(), lastValue())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.11
	 */
	public Rule caseStatement() {
		return Sequence(
				String("case"), group.basics.testLexBreak(), group.basics.optWS(),
				group.expressions.anyExpression(), set(),
				Ch(':'), group.basics.optWS(),
				set(actions.createCaseStatement(value())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.11
	 */
	public Rule defaultStatement() {
		return Sequence(
				String("default").label("defaultKeyword"), group.basics.testLexBreak(), group.basics.optWS(),
				Ch(':'), group.basics.optWS(),
				set(actions.createDefaultStatement(node("defaultKeyword"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.12
	 */
	public Rule whileStatement() {
		return Sequence(
				String("while"), group.basics.testLexBreak(), group.basics.optWS(),
				Ch('('), group.basics.optWS(),
				group.expressions.anyExpression().label("condition"),
				Ch(')'), group.basics.optWS(),
				anyStatement(), set(),
				set(actions.createWhileStatement(value("condition"), value())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.13
	 */
	public Rule doWhileStatement() {
		return Sequence(
				String("do"), group.basics.testLexBreak(), group.basics.optWS(),
				anyStatement(), set(),
				String("while"), group.basics.testLexBreak(), group.basics.optWS(),
				Ch('('), group.basics.optWS(),
				group.expressions.anyExpression().label("condition"),
				Ch(')'), group.basics.optWS(),
				Ch(';'), group.basics.optWS(),
				set(actions.createDoStatement(value("condition"), value())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.14.1.1
	 */
	public Rule basicForStatement() {
		return Sequence(
				String("for"), group.basics.testLexBreak(), group.basics.optWS(),
				Ch('('), group.basics.optWS(),
				forInit().label("init"),
				Ch(';'), group.basics.optWS(),
				Optional(group.expressions.anyExpression()).label("condition"),
				Ch(';'), group.basics.optWS(),
				forUpdate().label("update"),
				Ch(')'), group.basics.optWS(),
				anyStatement().label("statement"),
				set(actions.createBasicFor(value("init"), value("condition"), value("update"), value("statement"))));
	}
	
	Rule forInit() {
		return Optional(FirstOf(
				variableDefinition(),
				statementExpressionList()));
	}
	
	Rule forUpdate() {
		return Optional(statementExpressionList());
	}
	
	Rule statementExpressionList() {
		return Sequence(
				group.expressions.statementExpression().label("head"),
				ZeroOrMore(Sequence(
						Ch(','), group.basics.optWS(),
						group.expressions.statementExpression()).label("tail")),
				set(actions.createStatementExpressionList(value("head"), values("ZeroOrMore/tail"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.14.2
	 * @see http://bugs.sun.com/view_bug.do?bug_id=1699917
	 */
	public Rule enhancedForStatement() {
		return Sequence(
				String("for"), group.basics.testLexBreak(), group.basics.optWS(),
				Ch('('), group.basics.optWS(),
				group.structures.variableDefinitionModifiers().label("modifiers"),
				group.types.type().label("type"),
				group.structures.variableDefinitionPartNoAssign().label("varDeclPart"),
				Ch(':'), group.basics.optWS(),
				group.expressions.anyExpression().label("iterable"),
				Ch(')'), group.basics.optWS(),
				anyStatement().label("statement"),
				set(actions.createEnhancedFor(node("modifiers"), value("type"), node("varDeclPart"), value("iterable"), value("statement"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.15
	 */
	public Rule breakStatement() {
		return Sequence(
				String("break"), group.basics.testLexBreak(), group.basics.optWS(),
				Optional(group.basics.identifier()).label("identifier"),
				Ch(';'), group.basics.optWS(),
				set(actions.createBreak(value("identifier"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.16
	 */
	public Rule continueStatement() {
		return Sequence(
				String("continue"), group.basics.testLexBreak(), group.basics.optWS(),
				Optional(group.basics.identifier()).label("identifier"),
				Ch(';'), group.basics.optWS(),
				set(actions.createContinue(value("identifier"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.17
	 */
	public Rule returnStatement() {
		return Sequence(
				String("return"), group.basics.testLexBreak(), group.basics.optWS(),
				Optional(group.expressions.anyExpression()).label("value"),
				Ch(';'), group.basics.optWS(),
				set(actions.createReturn(value("value"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.18
	 */
	public Rule throwStatement() {
		return Sequence(
				String("throw"), group.basics.testLexBreak(), group.basics.optWS(),
				group.expressions.anyExpression().label("throwable"),
				Ch(';'), group.basics.optWS(),
				set(actions.createThrow(value("throwable"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.19
	 */
	public Rule synchronizedStatement() {
		return Sequence(
				String("synchronized"), group.basics.testLexBreak(), group.basics.optWS(),
				Ch('('), group.basics.optWS(),
				group.expressions.anyExpression().label("lock"),
				Ch(')'), group.basics.optWS(),
				blockStatement().label("body"),
				set(actions.createSynchronizedStatement(value("lock"), value("body"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.20
	 */
	public Rule tryStatement() {
		return Sequence(
				String("try"), group.basics.testLexBreak(), group.basics.optWS(),
				blockStatement().label("body"),
				ZeroOrMore(catchBlock().label("catchBlock")),
				Optional(Sequence(
						String("finally"), group.basics.testLexBreak(), group.basics.optWS(),
						blockStatement().label("finallyBody"))),
				set(actions.createTryStatement(value("body"), values("ZeroOrMore/catchBlock"), value("Optional/Sequence/finallyBody"))));
	}
	
	Rule catchBlock() {
		return Sequence(
				String("catch"), group.basics.testLexBreak(), group.basics.optWS(),
				Ch('('), group.basics.optWS(),
				group.structures.variableDefinitionModifiers().label("modifiers"),
				group.types.type().label("type"),
				group.basics.identifier().label("varName"),
				Ch(')'), group.basics.optWS(),
				blockStatement().label("body"),
				set(actions.createCatch(value("modifiers"), value("type"), value("varName"), value("body"))));
	}
}
