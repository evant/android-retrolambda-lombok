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
import org.parboiled.Parboiled;
import org.parboiled.Rule;

public class ExpressionsParser extends BaseParser<Node, ExpressionsActions>{
	private final ParserGroup group;
	
	public ExpressionsParser(ParserGroup group) {
		super(Parboiled.createActions(ExpressionsActions.class));
		this.group = group;
	}
	
	/*
	 * level 0: paren grouping, primitive (=literal, identifier, type literal)
	 * level 1: array index, method call, member access, [non-dot constructor]
	 */
	
	public Rule primaryExpression() {
		return firstOf(
				group.literals.anyLiteral(),
				identifierExpression());
	}
	
	private Rule identifierExpression() {
		return sequence(
				group.basics.identifier(),
				SET(actions.createIdentifierExpression(LAST_VALUE())));
	}
	
	public Rule anyExpression() {
		return assignmentExpression();
	}
	
	/**
	 * P2''
	 * 
	 * This is the relational new operator; not just 'new', but new with context, so: "a.new InnerClass(params)". It is grouped with P2, but for some reason has higher precedence
	 * in all java parsers, and so we give it its own little precedence group here.
	 */
	public Rule dotNewExpression() {
		return sequence(
				primaryExpression(),
				zeroOrMore(enforcedSequence(
						sequence(
								ch('.'),
								group.basics.optWS(),
								string("new"),
								group.basics.testLexBreak(),
								group.basics.optWS()),
						group.types.typeArguments(),
						group.basics.identifier(),
						group.types.typeArguments(),
						group.structures.methodArguments(),
						optional(group.structures.classBody()))));
	}
	
	/**
	 * P2'
	 * Technically, postfix increment operations are in P2 along with all the unary operators like ~ and !, as well as typecasts.
	 * However, because ALL of the P2 expression are right-associative, the postfix operators can be considered as a higher level of precedence.
	 */
	public Rule postfixIncrementExpression() {
		return sequence(
				dotNewExpression(), SET(),
				zeroOrMore(sequence(
						firstOf(string("++"), string("--")).label("operator"),
						group.basics.optWS()).label("operatorCt")),
				SET(actions.createUnaryPostfixExpression(VALUE(), TEXTS("zeroOrMore/operatorCt/operator"))));
	}
	
	/**
	 * P2
	 */
	public Rule level2Expression() {
		return sequence(
				zeroOrMore(sequence(
						firstOf(
								string("++"), string("--"),
								ch('!'), ch('~'),
								solitarySymbol('+'), solitarySymbol('-'),
								sequence(
										ch('('), group.basics.optWS(),
										group.types.type(),
										ch(')')).label("cast")
								).label("operator"),
						group.basics.optWS()).label("operatorCt")),
				postfixIncrementExpression(), SET(),
				SET(actions.createUnaryPrefixExpression(VALUE(), NODES("zeroOrMore/operatorCt/operator"), TEXTS("zeroOrMore/operatorCt/operator"))));
	}
	
	/**
	 * P3
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.17
	 */
	public Rule multiplicativeExpression() {
		return forBinaryExpression(firstOf(ch('*'), solitarySymbol('/'), ch('%')), level2Expression(), true);
	}
	
	/**
	 * P4
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.18
	 */
	public Rule additiveExpression() {
		return forBinaryExpression(firstOf(solitarySymbol('+'), solitarySymbol('-')), multiplicativeExpression(), true);
	}
	
	/**
	 * P5
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.19
	 */
	public Rule shiftExpression() {
		return forBinaryExpression(firstOf(string(">>>"), string("<<<"), string("<<"), string(">>")), additiveExpression(), true);
	}
	
	/**
	 * P6
	 * 
	 * Technically 'instanceof' is on equal footing with the other operators, but practically speaking this doesn't hold;
	 * for starters, the RHS of instanceof is a Type and not an expression, and the inevitable type of an instanceof expression (boolean) is
	 * not compatible as LHS to *ANY* of the operators in this class, including instanceof itself. Therefore, pragmatically speaking, there can only
	 * be one instanceof, and it has to appear at the end of the chain.
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.20
	 */
	public Rule relationalExpression() {
		return sequence(
				forBinaryExpression(firstOf(string("<="), string(">="), solitarySymbol('<'), solitarySymbol('>')), shiftExpression(), true),
				SET(),
				optional(enforcedSequence(
						sequence(string("instanceof"), group.basics.testLexBreak(), group.basics.optWS()),
						group.types.type(),
						UP(UP(SET(actions.createInstanceOfExpression(VALUE(), LAST_VALUE())))))));
	}
	
	/**
	 * P7
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.21
	 */
	public Rule equalityExpression() {
		return forBinaryExpression(firstOf(string("==="), string("!=="), string("=="), string("!=")), relationalExpression(), true);
	}
	
	/**
	 * P8
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.22
	 */
	public Rule bitwiseAndExpression() {
		return forBinaryExpression(solitarySymbol('&'), equalityExpression(), true);
	}
	
	/**
	 * P9
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.22
	 */
	public Rule bitwiseXorExpression() {
		return forBinaryExpression(solitarySymbol('^'), bitwiseAndExpression(), true);
	}
	
	/**
	 * P10
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.22
	 */
	public Rule bitwiseOrExpression() {
		return forBinaryExpression(solitarySymbol('|'), bitwiseXorExpression(), true);
	}
	
	/**
	 * P11
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.23
	 */
	public Rule conditionalAndExpression() {
		return forBinaryExpression(string("&&"), bitwiseOrExpression(), true);
	}
	
	/**
	 * P12'
	 * 
	 * This is not a legal operator; however, it is entirely imaginable someone presumes it does exist.
	 * It also has no other sensible meaning, so we will parse it and flag it as a syntax error in AST phase.
	 */
	public Rule conditionalXorExpression() {
		return forBinaryExpression(string("^^"), conditionalAndExpression(), true);
	}
	
	/**
	 * P12
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.24
	 */
	public Rule conditionalOrExpression() {
		return forBinaryExpression(string("||"), conditionalXorExpression(), true);
	}
	
	/**
	 * P13
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.25
	 */
	public Rule inlineIfExpression() {
		return sequence(
				conditionalOrExpression(),
				SET(),
				zeroOrMore(
						sequence(
								sequence(ch('?'), testNot(firstOf(ch('.'), ch(':'), ch('?')))).label("operator1"),
								group.basics.optWS(),
								conditionalOrExpression().label("tail1"),
								ch(':').label("operator2"),
								group.basics.optWS(),
								conditionalOrExpression().label("tail2")
								)),
				SET(actions.createInlineIfExpression(VALUE(),
						TEXTS("zeroOrMore/sequence/operator1"), TEXTS("zeroOrMore/sequence/operator2"),
						VALUES("zeroOrMore/sequence/tail1"), VALUES("zeroOrMore/sequence/tail2"))),
				group.basics.optWS());
	}
	
	/**
	 * P14
	 * 
	 * Not all of the listed operators are actually legal, but if not legal, then they are at least imaginable, so we parse them and flag them as errors in the AST phase.
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.26
	 */
	public Rule assignmentExpression() {
		return forBinaryExpression(firstOf(
				solitarySymbol('='),
				string("*="), string("/="), string("+="), string("-="), string("%="),
				string(">>>="), string("<<<="), string("<<="), string(">>="),
				string("&="), string("^="), string("|="),
				string("&&="), string("^^="), string("||=")), inlineIfExpression(), false);
	}
	
	/**
	 * @param nextHigher Careful; operator has to match _ONLY_ the operator, not any whitespace around it (otherwise we'd have to remove comments from it, which isn't feasible).
	 */
	private Rule forBinaryExpression(Rule operator, Rule nextHigher, boolean leftAssociative) {
		return sequence(
				nextHigher, SET(),
				group.basics.optWS(),
				zeroOrMore(sequence(
						operator.label("operator"),
						group.basics.optWS(),
						nextHigher.label("tail"),
						group.basics.optWS())),
				SET(leftAssociative ?
						actions.createLeftAssociativeBinaryExpression(VALUE(), TEXTS("zeroOrMore/sequence/operator"), VALUES("zeroOrMore/sequence/tail")) :
						actions.createRightAssociativeBinaryExpression(VALUE(), TEXTS("zeroOrMore/sequence/operator"), VALUES("zeroOrMore/sequence/tail"))
						),
				group.basics.optWS());
	}
	
	private Rule solitarySymbol(char c) {
		return sequence(ch(c), testNot(ch(c)));
	}
}
