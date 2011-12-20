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

import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.Cached;

public class ExpressionsParser extends BaseParser<Node> {
	final ParserGroup group;
	final ExpressionsActions actions;
	
	public ExpressionsParser(ParserGroup group) {
		this.actions = new ExpressionsActions(group.getSource());
		this.group = group;
	}
	
	/**
	 * P0
	 */
	public Rule primaryExpression() {
		return FirstOf(
				parenGrouping(),
				group.literals.anyLiteral(),
				unqualifiedThisOrSuperLiteral(),
				arrayCreationExpression(),
				unqualifiedConstructorInvocation(),
				qualifiedClassOrThisOrSuperLiteral(),
				identifierExpression());
	}
	
	Rule parenGrouping() {
		return Sequence(
				Ch('('), group.basics.optWS(),
				anyExpression(), set(),
				Ch(')'), set(actions.addParens(value())),
				group.basics.optWS());
	}
	
	Rule unqualifiedThisOrSuperLiteral() {
		return Sequence(
				FirstOf(String("this"), String("super")).label("thisOrSuper"),
				group.basics.testLexBreak(),
				group.basics.optWS(),
				TestNot(Ch('(')),
				set(actions.createThisOrSuperOrClass(null, text("thisOrSuper"), null)));
	}
	
	/**
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.8.2">JLS section 15.8.2</a>
	 */
	Rule qualifiedClassOrThisOrSuperLiteral() {
		return Sequence(
				group.types.type().label("type"),
				Ch('.').label("dot"), group.basics.optWS(),
				FirstOf(String("this"), String("super"), String("class")).label("thisOrSuperOrClass"),
				group.basics.testLexBreak(),
				group.basics.optWS(),
				set(actions.createThisOrSuperOrClass(node("dot"), text("thisOrSuperOrClass"), value("type"))));
	}
	
	Rule unqualifiedConstructorInvocation() {
		return Sequence(
				String("new"), group.basics.testLexBreak(), group.basics.optWS(),
				group.types.typeArguments().label("constructorTypeArgs"),
				group.types.type().label("type"),
				group.structures.methodArguments().label("args"),
				Optional(group.structures.typeBody()).label("classBody"),
				set(actions.createUnqualifiedConstructorInvocation(value("constructorTypeArgs"), value("type"), value("args"), value("classBody"))));
	}
	
	/**
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/arrays.html#10.3">JLS section 10.3</a>
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.10">JLS section 15.10</a>
	 */
	Rule arrayCreationExpression() {
		return Sequence(
				String("new"), group.basics.testLexBreak(), group.basics.optWS(),
				group.types.nonArrayType().label("type"),
				OneOrMore(Sequence(
						Ch('[').label("openArray"), group.basics.optWS(),
						Optional(anyExpression()).label("dimension"), Ch(']'), group.basics.optWS(),
						set(actions.createDimension(value("dimension"), node("openArray"))))),
				Optional(arrayInitializer()).label("initializer"),
				set(actions.createArrayCreationExpression(value("type"), values("OneOrMore/Sequence"), value("initializer"))));
	}
	
	public Rule arrayInitializer() {
		return Sequence(
				Ch('{'), group.basics.optWS(),
				Optional(Sequence(
						FirstOf(arrayInitializer(), anyExpression()).label("head"),
						ZeroOrMore(Sequence(
								Ch(','), group.basics.optWS(),
								FirstOf(arrayInitializer(), anyExpression()).label("tail"))),
						Optional(Ch(',')),
						group.basics.optWS())),
				Ch('}'), group.basics.optWS(),
				set(actions.createArrayInitializerExpression(value("Optional/Sequence/head"), values("Optional/Sequence/ZeroOrMore/Sequence/tail"))));
	}
	
	Rule identifierExpression() {
		return Sequence(
				group.basics.identifier(),
				set(),
				Optional(Sequence(group.structures.methodArguments(), set()).label("methodArgs")),
				set(actions.createPrimary(value(), value("Optional/methodArgs"))));
	}
	
	public Rule anyExpression() {
		return assignmentExpressionChaining();
	}
	
	/**
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/statements.html#14.8">JLS section 14.8</a>
	 */
	public Rule statementExpression() {
		return FirstOf(
				assignmentExpression(),
				postfixIncrementExpression(),
				prefixIncrementExpression(),
				Sequence(dotNewExpressionChaining(), set(), actions.checkIfMethodOrConstructorInvocation(value())));
	}
	
	public Rule allPrimaryExpressions() {
		return Sequence(level1ExpressionChaining(), Empty());
	}
	
	/**
	 * P1
	 */
	Rule level1ExpressionChaining() {
		return Sequence(
				primaryExpression().label("head"), set(),
				ZeroOrMore(FirstOf(
						arrayAccessOperation().label("arrayAccess"),
						methodInvocationWithTypeArgsOperation().label("methodInvocation"),
						select().label("select"))),
				set(actions.createLevel1Expression(node("head"), nodes("ZeroOrMore/FirstOf"))));
	}
	
	Rule arrayAccessOperation() {
		return Sequence(
				Ch('['), group.basics.optWS(),
				anyExpression(), set(), Ch(']'), group.basics.optWS(),
				set(actions.createArrayAccessOperation(value())));
	}
	
	Rule methodInvocationWithTypeArgsOperation() {
		return Sequence(
				Ch('.').label("dot"), group.basics.optWS(),
				group.types.typeArguments().label("typeArguments"),
				group.basics.identifier().label("name"),
				group.structures.methodArguments().label("methodArguments"),
				set(actions.createMethodInvocationOperation(node("dot"), value("typeArguments"), value("name"), value("methodArguments"))));
	}
	
	Rule select() {
		return Sequence(
				group.basics.dotIdentifier().label("identifier"),
				TestNot(Ch('(')),
				set(actions.createSelectOperation(value("identifier"))));
	}
	
	/**
	 * P2''
	 * 
	 * This is the relational new operator; not just 'new', but new with context, so: "a.new InnerClass(params)". It is grouped with P2, but for some reason has higher precedence
	 * in all java parsers, and so we give it its own little precedence group here.
	 */
	Rule dotNewExpressionChaining() {
		return Sequence(
				level1ExpressionChaining().label("head"), set(),
				ZeroOrMore(Sequence(
						Sequence(
								Ch('.'),
								group.basics.optWS(),
								String("new"),
								group.basics.testLexBreak(),
								group.basics.optWS()),
						group.types.typeArguments().label("constructorTypeArgs"),
						group.basics.identifier().label("innerClassName"),
						group.types.typeArguments().label("classTypeArgs"),
						group.structures.methodArguments().label("methodArguments"),
						Optional(group.structures.typeBody()).label("classBody"),
						set(actions.createQualifiedConstructorInvocation(value("constructorTypeArgs"), node("innerClassName"), node("classTypeArgs"), value("methodArguments"), value("classBody"))))),
				set(actions.createChainOfQualifiedConstructorInvocations(node("head"), nodes("ZeroOrMore/Sequence"))));
	}
	
	/**
	 * P2'
	 * Technically, postfix increment operations are in P2 along with all the unary operators like ~ and !, as well as typecasts.
	 * However, because ALL of the P2 expression are right-associative, the postfix operators can be considered as a higher level of precedence.
	 */
	Rule postfixIncrementExpressionChaining() {
		return Sequence(
				dotNewExpressionChaining(), set(),
				ZeroOrMore(Sequence(
						FirstOf(String("++"), String("--")).label("operator"),
						group.basics.optWS()).label("operatorCt")),
				set(actions.createUnaryPostfixExpression(value(), nodes("ZeroOrMore/operatorCt/operator"), texts("ZeroOrMore/operatorCt/operator"))));
	}
	
	Rule postfixIncrementExpression() {
		return Sequence(
				dotNewExpressionChaining(), set(),
				OneOrMore(Sequence(
						FirstOf(String("++"), String("--")).label("operator"),
						group.basics.optWS()).label("operatorCt")),
				set(actions.createUnaryPostfixExpression(value(), nodes("OneOrMore/operatorCt/operator"), texts("OneOrMore/operatorCt/operator"))));
	}
	
	Rule prefixIncrementExpression() {
		return Sequence(
				OneOrMore(Sequence(
						FirstOf(String("++"), String("--")).label("operator"),
						group.basics.optWS()).label("operatorCt")),
						postfixIncrementExpressionChaining().label("operand"), set(),
				set(actions.createUnaryPrefixExpressions(node("operand"), nodes("OneOrMore/operatorCt/operator"), texts("OneOrMore/operatorCt/operator"))));
	}
	
	/**
	 * P2
	 */
	Rule level2ExpressionChaining() {
		return FirstOf(
				Sequence(
						FirstOf(
								String("++"), String("--"),
								Ch('!'), Ch('~'),
								solitarySymbol('+'), solitarySymbol('-'),
								Sequence(
										Ch('('), group.basics.optWS(),
										group.types.type().label("type"),
										Ch(')'),
										TestNot(Sequence(
												actions.typeIsAlsoLegalAsExpression(UP(UP(value("type")))),
												group.basics.optWS(),
												FirstOf(solitarySymbol('+'), solitarySymbol('-'))))).label("cast")
								).label("operator"),
						group.basics.optWS(),
						level2ExpressionChaining().label("operand"), set(),
						set(actions.createUnaryPrefixExpression(value("operand"), node("operator"), text("operator")))),
					Sequence(postfixIncrementExpressionChaining(), set()));
	}
	
	/**
	 * P3
	 * 
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.17">JLS section 15.17</a>
	 */
	Rule multiplicativeExpressionChaining() {
		return forLeftAssociativeBinaryExpression("exprMultiplicative", FirstOf(Ch('*'), solitarySymbol('/'), Ch('%')), level2ExpressionChaining());
	}
	
	/**
	 * P4
	 * 
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.18">JLS section 15.18</a>
	 */
	Rule additiveExpressionChaining() {
		return forLeftAssociativeBinaryExpression("exprAdditive", FirstOf(solitarySymbol('+'), solitarySymbol('-')), multiplicativeExpressionChaining());
	}
	
	/**
	 * P5
	 * 
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.19">JLS section 15.19</a>
	 */
	Rule shiftExpressionChaining() {
		return forLeftAssociativeBinaryExpression("exprShift", FirstOf(String(">>>"), String("<<<"), String("<<"), String(">>")), additiveExpressionChaining());
	}
	
	/**
	 * P6
	 * 
	 * Technically 'instanceof' is on equal footing with the other operators, but practically speaking this doesn't hold;
	 * for starters, the RHS of instanceof is a Type and not an expression, and the inevitable type of an instanceof expression (boolean) is
	 * not compatible as LHS to *ANY* of the operators in this class, including instanceof itself. Therefore, pragmatically speaking, there can only
	 * be one instanceof, and it has to appear at the end of the chain.
	 * 
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.20">JLS section 15.20</a>
	 */
	Rule relationalExpressionChaining() {
		return Sequence(
				forLeftAssociativeBinaryExpression("exprRelational", FirstOf(String("<="), String(">="), solitarySymbol('<'), solitarySymbol('>')), shiftExpressionChaining()),
				set(),
				Optional(Sequence(
						Sequence(String("instanceof"), group.basics.testLexBreak(), group.basics.optWS()),
						group.types.type().label("type")).label("typeCt")).label("instanceof"),
				set(actions.createInstanceOfExpression(value(), value("instanceof/typeCt/type"))));
	}
	
	/**
	 * P7
	 * 
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.21">JLS section 15.21</a>
	 */
	Rule equalityExpressionChaining() {
		return forLeftAssociativeBinaryExpression("exprEquality", FirstOf(String("==="), String("!=="), String("=="), String("!=")), relationalExpressionChaining());
	}
	
	/**
	 * P8
	 * 
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.22">JLS section 15.22</a>
	 */
	Rule bitwiseAndExpressionChaining() {
		return forLeftAssociativeBinaryExpression("exprBitwiseAnd", solitarySymbol('&'), equalityExpressionChaining());
	}
	
	/**
	 * P9
	 * 
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.22">JLS section 15.22</a>
	 */
	Rule bitwiseXorExpressionChaining() {
		return forLeftAssociativeBinaryExpression("exprBitwiseXor", solitarySymbol('^'), bitwiseAndExpressionChaining());
	}
	
	/**
	 * P10
	 * 
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.22">JLS section 15.22</a>
	 */
	Rule bitwiseOrExpressionChaining() {
		return forLeftAssociativeBinaryExpression("exprBitwiseOr", solitarySymbol('|'), bitwiseXorExpressionChaining());
	}
	
	/**
	 * P11
	 * 
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.23">JLS section 15.23</a>
	 */
	Rule conditionalAndExpressionChaining() {
		return forLeftAssociativeBinaryExpression("exprLogicalAnd", String("&&"), bitwiseOrExpressionChaining());
	}
	
	/**
	 * P12'
	 * 
	 * This is not a legal operator; however, it is entirely imaginable someone presumes it does exist.
	 * It also has no other sensible meaning, so we will parse it and flag it as a syntax error in AST phase.
	 */
	Rule conditionalXorExpressionChaining() {
		return forLeftAssociativeBinaryExpression("exprLogicalXor", String("^^"), conditionalAndExpressionChaining());
	}
	
	/**
	 * P12
	 * 
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.24">JLS section 15.24</a>
	 */
	Rule conditionalOrExpressionChaining() {
		return forLeftAssociativeBinaryExpression("exprLogicalOr", String("||"), conditionalXorExpressionChaining());
	}
	
	/**
	 * P13
	 * 
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.25">JLS section 15.25</a>
	 */
	Rule inlineIfExpressionChaining() {
		return Sequence(
				conditionalOrExpressionChaining().label("head"),
				set(),
				Optional(
						Sequence(
								Sequence(Ch('?'), TestNot(FirstOf(Ch('.'), Ch(':'), Ch('?')))).label("operator1"),
								group.basics.optWS(),
								assignmentExpressionChaining().label("tail1"),
								Ch(':').label("operator2"),
								group.basics.optWS(),
								inlineIfExpressionChaining().label("tail2")
								)),
				set(actions.createInlineIfExpression(value("head"),
						node("Optional/Sequence/operator1"), node("Optional/Sequence/operator2"),
						value("Optional/Sequence/tail1"), value("Optional/Sequence/tail2"))),
				group.basics.optWS());
	}
	
	/**
	 * P14
	 * 
	 * Not all of the listed operators are actually legal, but if not legal, then they are at least imaginable, so we parse them and flag them as errors in the AST phase.
	 * 
	 * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.26">JLS section 15.26</a>
	 */
	Rule assignmentExpressionChaining() {
		return Sequence(
				inlineIfExpressionChaining(), set(),
				Optional(Sequence(
						assignmentOperator().label("operator"),
						group.basics.optWS(),
						assignmentExpressionChaining().label("RHS"))).label("assignment"),
				set(actions.createAssignmentExpression(value(), text("assignment/Sequence/operator"), value("assignment"))));
	}
	
	// TODO add checks to see if an LHS that isn't valid for assignment shows up as a syntax error of some sort, e.g. a.b() = 2;
	
	Rule assignmentExpression() {
		return Sequence(
				assignmentLHS(), set(),
				assignmentOperator().label("operator"),
				group.basics.optWS(),
				assignmentExpressionChaining().label("RHS"),
				set(actions.createAssignmentExpression(value(), text("operator"), lastValue())));
	}
	
	Rule assignmentLHS() {
		return Sequence(
				level1ExpressionChaining(), set(),
				actions.checkIfLevel1ExprIsValidForAssignment(value()));
	}
	
	Rule assignmentOperator() {
		return FirstOf(
				solitarySymbol('='),
				String("*="), String("/="), String("+="), String("-="), String("%="),
				String(">>>="), String("<<<="), String("<<="), String(">>="),
				String("&="), String("^="), String("|="),
				String("&&="), String("^^="), String("||="));
	}
	
	/**
	 * @param operator Careful; operator has to match _ONLY_ the operator, not any whitespace around it (otherwise we'd have to remove comments from it, which isn't feasible).
	 */
	@Cached
	Rule forLeftAssociativeBinaryExpression(String labelName, Rule operator, Rule nextHigher) {
		return Sequence(
				nextHigher.label("head"), new Action<Node>() {
					@Override public boolean run(Context<Node> context) {
						setContext(context);
						return set();
					}
				},
				group.basics.optWS(),
				ZeroOrMore(Sequence(
						operator.label("operator"),
						group.basics.optWS(),
						nextHigher.label("tail"),
						group.basics.optWS())),
				new Action<Node>() {
					@Override public boolean run(Context<Node> context) {
						setContext(context);
						return set(actions.createLeftAssociativeBinaryExpression(
								node("head"),
								nodes("ZeroOrMore/Sequence/operator"), texts("ZeroOrMore/Sequence/operator"),
								nodes("ZeroOrMore/Sequence/tail")));
					}
				},
				group.basics.optWS()).label(labelName);
	}
	
	Rule solitarySymbol(char c) {
		return Sequence(Ch(c), TestNot(Ch(c)));
	}
}
