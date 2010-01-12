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

public class LiteralsParser extends BaseParser<Node, LiteralsActions> {
	private final ParserGroup group;
	
	public LiteralsParser(ParserGroup group) {
		super(Parboiled.createActions(LiteralsActions.class));
		this.group = group;
	}
	
	public Rule anyLiteral() {
		return firstOf(
				nullLiteral(),
				booleanLiteral(),
				numberLiteral(),
				charLiteral(),
				stringLiteral());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.7
	 */
	public Rule nullLiteral() {
		return enforcedSequence(
			sequence(
					string("null"),
					group.basics.testLexBreak()),
			SET(actions.createNullLiteral(LAST_TEXT())),
			group.basics.optWS());
	}
	
	public Rule thisLiteral() {
		return enforcedSequence(
				sequence(
						string("this"), group.basics.testLexBreak(), group.basics.optWS(), testNot(ch('('))),
				SET(actions.createThisLiteral(LAST_TEXT())),
				group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.5
	 */
	public Rule stringLiteral() {
		return enforcedSequence(
				enforcedSequence(
						ch('"'),
						zeroOrMore(
								sequence(
										testNot(firstOf(ch('"'), group.basics.lineTerminator())),
										firstOf(
												escapedSequence(),
												any()))),
						ch('"')),
				SET(actions.createStringLiteral(LAST_TEXT())),
				group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.4
	 */
	public Rule charLiteral() {
		return enforcedSequence(
				enforcedSequence(
						ch('\''),
						firstOf(
								enforcedSequence(escapedSequence(), ch('\'')),
								sequence(
										zeroOrMore(sequence(testNot(
												firstOf(ch('\''), group.basics.lineTerminator())), any())),
										ch('\'')),
								any())),
				SET(actions.createCharLiteral(LAST_TEXT())),
				group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.6
	 */
	private Rule escapedSequence() {
		return enforcedSequence(ch('\\'),
				firstOf(
						sequence(optional(zeroToThree()), octalDigit(), optional(octalDigit())),
						any()));
	}
	
	private Rule zeroToThree() {
		return firstOf(ch('0'), ch('1'), ch('2'), ch('3'));
	}
	
	private Rule octalDigit() {
		return firstOf(ch('0'), ch('1'), ch('2'), ch('3'), ch('4'), ch('5'), ch('6'), ch('7'));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.3
	 */
	public Rule booleanLiteral() {
		return enforcedSequence(
				sequence(
						firstOf(string("true"), string("false")),
						group.basics.testLexBreak()),
				SET(actions.createBooleanLiteral(LAST_TEXT())),
				group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.1
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	public Rule numberLiteral() {
		return enforcedSequence(
				test(sequence(optional(ch('.')), charRange('0', '9'))),
				firstOf(hexLiteral(), fpLiteral()),
				SET(LAST_VALUE()),
				group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.1
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	private Rule fpLiteral() {
		return enforcedSequence(
				sequence(
						firstOf(
								enforcedSequence(oneOrMore(digit()), optional(sequence(ch('.'), zeroOrMore(digit())))),
								enforcedSequence(ch('.'), oneOrMore(digit()))),
						optional(
								enforcedSequence(
										charIgnoreCase('e'),
										optional(firstOf(ch('+'), ch('-'))),
										oneOrMore(digit()))),
						numberTypeSuffix()),
				SET(actions.createNumberLiteral(LAST_TEXT())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.1
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	private Rule hexLiteral() {
		return enforcedSequence(
				enforcedSequence(
						sequence(ch('0'), charIgnoreCase('x')),
						firstOf(
								hexFP(),
								sequence(oneOrMore(hexDigit()), numberTypeSuffix())
								)),
				SET(actions.createNumberLiteral(LAST_TEXT())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	private Rule hexFP() {
		return sequence(
				firstOf(
						enforcedSequence(ch('.'), oneOrMore(hexDigit())),
						enforcedSequence(
								oneOrMore(hexDigit()),
								optional(enforcedSequence(ch('.'), zeroOrMore(hexDigit()))))),
				enforcedSequence(
						charIgnoreCase('p'),
						optional(firstOf(ch('+'), ch('-'))),
						oneOrMore(digit())),
				numberTypeSuffix());
	}
	
	private Rule numberTypeSuffix() {
		return optional(firstOf(charIgnoreCase('d'), charIgnoreCase('f'), charIgnoreCase('l')));
	}
	
	private Rule digit() {
		return firstOf(ch('0'), ch('1'), ch('2'), ch('3'), ch('4'), ch('5'), ch('6'), ch('7'), ch('8'), ch('9'), ch('0'));
	}
	
	private Rule hexDigit() {
		return firstOf(digit(),
				charIgnoreCase('a'), charIgnoreCase('b'), charIgnoreCase('c'), charIgnoreCase('d'), charIgnoreCase('e'), charIgnoreCase('f'));
	}
}
