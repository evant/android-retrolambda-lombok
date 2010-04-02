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

public class LiteralsParser extends BaseParser<Node> {
	final ParserGroup group;
	final LiteralsActions actions;
	
	public LiteralsParser(ParserGroup group) {
		this.actions = new LiteralsActions(group.getSource());
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
		return sequence(
			sequence(
					string("null"),
					group.basics.testLexBreak()),
			SET(actions.createNullLiteral(LAST_TEXT())),
			group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.5
	 */
	public Rule stringLiteral() {
		return sequence(
				sequence(
						ch('"'),
						zeroOrMore(firstOf(
								stringEscape(),
								sequence(testNot(charSet("\"\r\n")), any()))),
						ch('"')),
				SET(actions.createStringLiteral(LAST_TEXT())),
				group.basics.optWS());
	}
	
	Rule stringEscape() {
		return sequence(
				ch('\\'),
				firstOf(
						sequence(optional(charRange('0', '3')), optional(charRange('0', '7')), charRange('0', '7')),
						sequence(testNot("\r\n"), any())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.4
	 */
	public Rule charLiteral() {
		return sequence(
				sequence(
						ch('\''),
						firstOf(
								sequence(escapedSequence(), ch('\'')),
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
	Rule escapedSequence() {
		return sequence(ch('\\'),
				firstOf(
						sequence(optional(zeroToThree()), octalDigit(), optional(octalDigit())),
						any()));
	}
	
	Rule zeroToThree() {
		return firstOf(ch('0'), ch('1'), ch('2'), ch('3'));
	}
	
	Rule octalDigit() {
		return firstOf(ch('0'), ch('1'), ch('2'), ch('3'), ch('4'), ch('5'), ch('6'), ch('7'));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.3
	 */
	public Rule booleanLiteral() {
		return sequence(
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
		return sequence(
				test(sequence(optional(ch('.')), charRange('0', '9'))),
				firstOf(hexLiteral(), fpLiteral()),
				SET(LAST_VALUE()),
				group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.1
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	Rule fpLiteral() {
		return sequence(
				sequence(
						firstOf(
								sequence(oneOrMore(digit()), optional(sequence(ch('.'), zeroOrMore(digit())))),
								sequence(ch('.'), oneOrMore(digit()))),
						optional(
								sequence(
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
	Rule hexLiteral() {
		return sequence(
				sequence(
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
	Rule hexFP() {
		return sequence(
				firstOf(
						sequence(ch('.'), oneOrMore(hexDigit())),
						sequence(
								oneOrMore(hexDigit()),
								optional(sequence(ch('.'), zeroOrMore(hexDigit()))))),
				sequence(
						charIgnoreCase('p'),
						optional(firstOf(ch('+'), ch('-'))),
						oneOrMore(digit())),
				numberTypeSuffix());
	}
	
	Rule numberTypeSuffix() {
		return optional(firstOf(charIgnoreCase('d'), charIgnoreCase('f'), charIgnoreCase('l')));
	}
	
	Rule digit() {
		return firstOf(ch('0'), ch('1'), ch('2'), ch('3'), ch('4'), ch('5'), ch('6'), ch('7'), ch('8'), ch('9'), ch('0'));
	}
	
	Rule hexDigit() {
		return firstOf(digit(),
				charIgnoreCase('a'), charIgnoreCase('b'), charIgnoreCase('c'), charIgnoreCase('d'), charIgnoreCase('e'), charIgnoreCase('f'));
	}
}
