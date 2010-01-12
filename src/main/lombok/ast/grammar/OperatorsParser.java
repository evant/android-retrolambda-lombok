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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import lombok.Lombok;
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
		return firstOf(
				assignmentOperation(),
				inlineIfOperation(),
				conditionalOrOperation(),
				conditionalXorOperation(),
				conditionalAndOperation(),
				bitwiseOrOperation(),
				bitwiseXorOperation(),
				bitwiseAndOperation(),
				equalityOperation(),
				relationalOperation(),
				shiftOperation(),
				additiveOperation(),
				multiplicativeOperation(),
				level2Operation(),
				postfixIncrementOperation(),
				dotNewOperation(),
				primaryExpression());
	}
	
	@PrecedencePlayer(101)
	public Rule dotNewOperation() {
		return enforcedSequence(
				sequence(
						higher("dotNewOperation"),
						ch('.'),
						group.basics.optWS(),
						string("new"),
						group.basics.testLexBreak(),
						group.basics.optWS()),
				group.types.typeArguments(),
				group.basics.identifier(),
				group.types.typeArguments(),
				group.basics.optWS(),
				group.structures.methodArguments(),
				group.basics.optWS(),
				optional(group.structures.classBody()));
	}
	
	//Technically, postfix increment operations are in group 2 along with all the unary operators like ~ and !, as well as typecasts, and targeted new expressions.
	//However, because ALL of the group 2 operations are right-associative, the postfix operators can be considered as a higher level of precedence.
	@PrecedencePlayer(190)
	public Rule postfixIncrementOperation() {
		return sequence(
				higher("postfixIncrementOperation"),
				SET(),
				group.basics.optWS(),
				zeroOrMore(sequence(
						firstOf(string("++"), string("--")).label("postfixOperator"), group.basics.optWS())),
				SET(actions.createPostfixOperation(VALUE(), TEXTS("zeroOrMore/sequence/postfixOperator"))));
	}
	
	@PrecedencePlayer(200)
	public Rule level2Operation() {
		return firstOf(
				numericUnaryOperation(),
				castExpression());
	}
	
	private Rule castExpression() {
		return sequence(
				ch('('),
				group.basics.optWS(),
				group.types.type(),
				ch(')'),
				firstOf(level2Operation(), higher("level2Operation")).label("operand"),
				SET(actions.createTypeCastExpression(VALUE("type"), VALUE("operand"))));
	}
	
	private Rule numericUnaryOperation() {
		return sequence(
				firstOf(string("++"), string("--"), ch('!'), ch('~'), solitarySymbol('+'), solitarySymbol('-')).label("operator"),
				group.basics.optWS(),
				firstOf(level2Operation(), higher("level2Operation")).label("operand"),
				group.basics.optWS(),
				SET(actions.createUnaryOperation(TEXT("operator"), VALUE("operand"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.17
	 */
	@PrecedencePlayer(300)
	public Rule multiplicativeOperation() {
		return forBinaryOperation(firstOf(ch('*'), solitarySymbol('/'), ch('%')), true);
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.18
	 */
	@PrecedencePlayer(400)
	public Rule additiveOperation() {
		return forBinaryOperation(firstOf(solitarySymbol('+'), solitarySymbol('-')), true);
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.19
	 */
	@PrecedencePlayer(500)
	public Rule shiftOperation() {
		return forBinaryOperation(firstOf(string(">>>"), string("<<<"), string("<<"), string(">>")), true);
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.20
	 */
	@PrecedencePlayer(600)
	public Rule relationalOperation() {
		return forBinaryOperation(firstOf(string("<="), string(">="), solitarySymbol('<'), solitarySymbol('>'), sequence(string("instanceof"), group.basics.testLexBreak())), true);
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.21
	 */
	@PrecedencePlayer(700)
	public Rule equalityOperation() {
		return forBinaryOperation(firstOf(string("==="), string("!=="), string("=="), string("!=")), true);
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.22
	 */
	@PrecedencePlayer(800)
	public Rule bitwiseAndOperation() {
		return forBinaryOperation(solitarySymbol('&'), true);
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.22
	 */
	@PrecedencePlayer(900)
	public Rule bitwiseXorOperation() {
		return forBinaryOperation(solitarySymbol('^'), true);
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.22
	 */
	@PrecedencePlayer(1000)
	public Rule bitwiseOrOperation() {
		return forBinaryOperation(solitarySymbol('|'), true);
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.23
	 */
	@PrecedencePlayer(1100)
	public Rule conditionalAndOperation() {
		return forBinaryOperation(string("&&"), true);
	}
	
	@PrecedencePlayer(1150)
	public Rule conditionalXorOperation() {
		return forBinaryOperation(string("^^"), true);
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.24
	 */
	@PrecedencePlayer(1200)
	public Rule conditionalOrOperation() {
		return forBinaryOperation(string("||"), true);
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.25
	 */
	@PrecedencePlayer(1300)
	public Rule inlineIfOperation() {
		return sequence(
				higher("inlineIfOperation"),
				SET(),
				group.basics.optWS(),
				oneOrMore(
						sequence(
								sequence(ch('?'), testNot(firstOf(ch('.'), ch(':'), ch('?')))).label("operator1"),
								group.basics.optWS(),
								higher("inlineIfOperation").label("tail1"),
								group.basics.optWS(),
								ch(':').label("operator2"),
								group.basics.optWS(),
								higher("inlineIfOperation").label("tail2")
								)),
				SET(actions.createInlineIfOperation(VALUE(),
						TEXTS("oneOrMore/sequence/operator1"), TEXTS("oneOrMore/sequence/operator2"),
						VALUES("oneOrMore/sequence/tail1"), VALUES("oneOrMore/sequence/tail2"))),
				group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#15.26
	 */
	@PrecedencePlayer(1400)
	public Rule assignmentOperation() {
		return forBinaryOperation(firstOf(
				solitarySymbol('='),
				string("*="), string("/="), string("+="), string("-="), string("%="),
				string(">>>="), string("<<<="), string("<<="), string(">>="),
				string("&="), string("^="), string("|="),
				string("&&="), string("^^="), string("||=")), false);
	}
	
	private List<Method> precedenceList;
	
	private void initReflectionMagic() {
		if (precedenceList != null) return;
		
		TreeMap<Integer, Method> precedenceOperations = new TreeMap<Integer, Method>();
		
		for (Method m : OperatorsParser.class.getDeclaredMethods()) {
			PrecedencePlayer pp = m.getAnnotation(PrecedencePlayer.class);
			if (pp == null) continue;
			
			int order = pp.value();
			
			if (precedenceOperations.get(order) != null) throw new IllegalStateException(String.format(
					"You have 2 @PrecedencePlayer methods that have the same value: %s and %s are both %d",
					m.getName(), precedenceOperations.get(order).getName(), order));
			
			precedenceOperations.put(order, m);
		}
		
		this.precedenceList = new ArrayList<Method>(precedenceOperations.values());
	}
	
	private Rule forBinaryOperation(Rule operator, boolean leftAssociative) {
		StackTraceElement callerFrame = Thread.currentThread().getStackTrace()[2];
		String methodName = callerFrame.getMethodName();
		
		return sequence(
				higher(methodName).label("head"),
				group.basics.optWS(),
				oneOrMore(
						sequence(
								operator.label("operator"),
								group.basics.optWS(),
								higher(methodName).label("tail"),
								group.basics.optWS())),
				SET(leftAssociative ?
						actions.createLeftAssociativeBinaryOperation(VALUE("head"), TEXTS("oneOrMore/sequence/operator"), VALUES("oneOrMore/sequence/tail")) :
						actions.createRightAssociativeBinaryOperation(VALUE("head"), TEXTS("oneOrMore/sequence/operator"), VALUES("oneOrMore/sequence/tail"))
						),
				group.basics.optWS());
	}
	
	private Rule higher(String methodName) {
		initReflectionMagic();
		
		List<Rule> firstOfParts = new ArrayList<Rule>();
		
		for (Method m : precedenceList) {
			if (m.getName().equals(methodName)) {
				break;
			}
			firstOfParts.add(invokeRuleMethod(m));
		}
		
		Collections.reverse(firstOfParts);
		
		switch (firstOfParts.size()) {
		case 0:
			return empty();
		case 1:
			return firstOfParts.get(0);
		default:
			Object[] extras = new Rule[firstOfParts.size() -2];
			for (int i = 2; i < firstOfParts.size(); i++) {
				extras[i-2] = firstOfParts.get(i);
			}
			return firstOf(firstOfParts.get(0), firstOfParts.get(1), extras);
		}
	}
	
	private Rule invokeRuleMethod(Method m) {
		try {
			return (Rule)m.invoke(this);
		} catch (Exception e) {
			throw Lombok.sneakyThrow(e);
		}
	}
	
	private Rule solitarySymbol(char c) {
		return sequence(ch(c), testNot(ch(c)));
	}
}
