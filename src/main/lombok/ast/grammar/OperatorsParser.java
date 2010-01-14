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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lombok.ast.Node;

import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;

public class OperatorsParser extends BaseParser<Node, OperatorsActions>{
	private final ParserGroup group;
	
	public OperatorsParser(ParserGroup group) {
		super(Parboiled.createActions(OperatorsActions.class));
		this.group = group;
	}
	
	/*
	 * level 0: paren grouping, primitive
	 * level 1: array index, method call, member access
	 * level 2: increment pre/post, unary +-, bit/logical not, any cast, new.
	 */
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	private static @interface PrecedencePlayer {
		int value();
	}
	
	@PrecedencePlayer(0)
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
		return assignmentOperation();
	}
	
	/**
	 * P2''
	 * 
	 * This is the relational new operator; not just 'new', but new with context, so: "a.new InnerClass(params)". It is grouped with P2, but for some reason has higher precedence
	 * in all java parsers, and so we give it its own little precedence group here.
	 */
	public Rule dotNewOperation() {
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
	 * Technically, postfix increment operations are in group 2 along with all the unary operators like ~ and !, as well as typecasts.
	 * However, because ALL of the group 2 operations are right-associative, the postfix operators can be considered as a higher level of precedence.
	 */
	public Rule postfixIncrementOperation() {
		return sequence(
				dotNewOperation(), SET(),
				zeroOrMore(sequence(
						firstOf(string("++"), string("--")).label("operator"),
						group.basics.optWS()).label("operatorCt")),
				SET(actions.createUnaryPostfixOperation(VALUE(), TEXTS("zeroOrMore/operatorCt/operator"))));
	}
	
	/**
	 * P2
	 */
	public Rule level2Operation() {
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
				postfixIncrementOperation(), SET(),
				SET(actions.createUnaryPrefixOperation(VALUE(), NODES("zeroOrMore/operatorCt/operator"), TEXTS("zeroOrMore/operatorCt/operator"))));
	}
	
	/**
	 * P3
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.17
	 */
	public Rule multiplicativeOperation() {
		return forBinaryOperation(firstOf(ch('*'), solitarySymbol('/'), ch('%')), level2Operation(), true);
	}
	
	/**
	 * P4
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.18
	 */
	public Rule additiveOperation() {
		return forBinaryOperation(firstOf(solitarySymbol('+'), solitarySymbol('-')), multiplicativeOperation(), true);
	}
	
	/**
	 * P5
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.19
	 */
	public Rule shiftOperation() {
		return forBinaryOperation(firstOf(string(">>>"), string("<<<"), string("<<"), string(">>")), additiveOperation(), true);
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
	public Rule relationalOperation() {
		return sequence(
				forBinaryOperation(firstOf(string("<="), string(">="), solitarySymbol('<'), solitarySymbol('>')), shiftOperation(), true),
				SET(),
				optional(enforcedSequence(
						sequence(string("instanceof"), group.basics.testLexBreak(), group.basics.optWS()),
						group.types.type(),
						UP(UP(SET(actions.createInstanceOf(VALUE(), LAST_VALUE())))))));
	}
	
	/**
	 * P7
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.21
	 */
	public Rule equalityOperation() {
		return forBinaryOperation(firstOf(string("==="), string("!=="), string("=="), string("!=")), relationalOperation(), true);
	}
	
	/**
	 * P8
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.22
	 */
	public Rule bitwiseAndOperation() {
		return forBinaryOperation(solitarySymbol('&'), equalityOperation(), true);
	}
	
	/**
	 * P9
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.22
	 */
	public Rule bitwiseXorOperation() {
		return forBinaryOperation(solitarySymbol('^'), bitwiseAndOperation(), true);
	}
	
	/**
	 * P10
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.22
	 */
	public Rule bitwiseOrOperation() {
		return forBinaryOperation(solitarySymbol('|'), bitwiseXorOperation(), true);
	}
	
	/**
	 * P11
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.23
	 */
	@PrecedencePlayer(1100)
	public Rule conditionalAndOperation() {
		return forBinaryOperation(string("&&"), bitwiseOrOperation(), true);
	}
	
	/**
	 * P12'
	 * 
	 * This is not a legal operator; however, it is entirely imaginable someone presumes it does exist.
	 * It also has no other sensible meaning, so we will parse it and flag it as a syntax error in AST phase.
	 */
	public Rule conditionalXorOperation() {
		return forBinaryOperation(string("^^"), conditionalAndOperation(), true);
	}
	
	/**
	 * P12
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.24
	 */
	public Rule conditionalOrOperation() {
		return forBinaryOperation(string("||"), conditionalXorOperation(), true);
	}
	
	/**
	 * P13
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.25
	 */
	public Rule inlineIfOperation() {
		return sequence(
				conditionalOrOperation(),
				SET(),
				zeroOrMore(
						sequence(
								sequence(ch('?'), testNot(firstOf(ch('.'), ch(':'), ch('?')))).label("operator1"),
								group.basics.optWS(),
								conditionalOrOperation().label("tail1"),
								ch(':').label("operator2"),
								group.basics.optWS(),
								conditionalOrOperation().label("tail2")
								)),
				SET(actions.createInlineIfOperation(VALUE(),
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
	public Rule assignmentOperation() {
		return forBinaryOperation(firstOf(
				solitarySymbol('='),
				string("*="), string("/="), string("+="), string("-="), string("%="),
				string(">>>="), string("<<<="), string("<<="), string(">>="),
				string("&="), string("^="), string("|="),
				string("&&="), string("^^="), string("||=")), inlineIfOperation(), false);
	}
	
	private Rule forBinaryOperation(Rule operator, Rule nextHigher, boolean leftAssociative) {
		return sequence(
				nextHigher, SET(),
				group.basics.optWS(),
				zeroOrMore(sequence(
						operator.label("operator"),
						group.basics.optWS(),
						nextHigher.label("tail"),
						group.basics.optWS())),
				SET(leftAssociative ?
						actions.createLeftAssociativeBinaryOperation(VALUE(), TEXTS("zeroOrMore/sequence/operator"), VALUES("zeroOrMore/sequence/tail")) :
						actions.createRightAssociativeBinaryOperation(VALUE(), TEXTS("zeroOrMore/sequence/operator"), VALUES("zeroOrMore/sequence/tail"))
						),
				group.basics.optWS());
	}
	
	private Rule solitarySymbol(char c) {
		return sequence(ch(c), testNot(ch(c)));
	}
}
