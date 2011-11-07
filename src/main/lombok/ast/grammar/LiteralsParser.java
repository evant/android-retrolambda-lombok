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
import org.parboiled.annotations.SuppressSubnodes;

public class LiteralsParser extends BaseParser<Node> {
	final ParserGroup group;
	final LiteralsActions actions;
	
	public LiteralsParser(ParserGroup group) {
		this.actions = new LiteralsActions(group.getSource());
		this.group = group;
	}
	
	public Rule anyLiteral() {
		return FirstOf(
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
		return Sequence(
			Sequence(
					String("null"),
					group.basics.testLexBreak()),
			set(actions.createNullLiteral(lastText())),
			group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.5
	 */
	public Rule stringLiteral() {
		return Sequence(
				stringLiteralRaw(),
				set(actions.createStringLiteral(lastText())),
				group.basics.optWS());
	}
	
	@SuppressSubnodes
	Rule stringLiteralRaw() {
		return Sequence(
				Ch('"'),
				ZeroOrMore(FirstOf(
						stringEscape(),
						Sequence(TestNot(CharSet("\"\r\n")), Any()))),
				Ch('"'));
	}
	
	Rule stringEscape() {
		return Sequence(
				Ch('\\'),
				FirstOf(
						Sequence(Optional(CharRange('0', '3')), Optional(CharRange('0', '7')), CharRange('0', '7')),
						Sequence(TestNot("\r\n"), Any())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.4
	 */
	public Rule charLiteral() {
		return Sequence(
				Sequence(
						Ch('\''),
						FirstOf(
								Sequence(escapedSequence(), Ch('\'')),
								Sequence(
										ZeroOrMore(Sequence(TestNot(
												FirstOf(Ch('\''), group.basics.lineTerminator())), Any())),
										Ch('\'')),
								Any())),
				set(actions.createCharLiteral(lastText())),
				group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.6
	 */
	Rule escapedSequence() {
		return Sequence(Ch('\\'),
				FirstOf(
						Sequence(Optional(zeroToThree()), octalDigit(), Optional(octalDigit())),
						Any()));
	}
	
	Rule zeroToThree() {
		return FirstOf(Ch('0'), Ch('1'), Ch('2'), Ch('3'));
	}
	
	Rule octalDigit() {
		return FirstOf(Ch('0'), Ch('1'), Ch('2'), Ch('3'), Ch('4'), Ch('5'), Ch('6'), Ch('7'));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.3
	 */
	public Rule booleanLiteral() {
		return Sequence(
				Sequence(
						FirstOf(String("true"), String("false")),
						group.basics.testLexBreak()),
				set(actions.createBooleanLiteral(lastText())),
				group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.1
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	public Rule numberLiteral() {
		return Sequence(
				Test(Sequence(Optional(Ch('.')), CharRange('0', '9'))),
				FirstOf(hexLiteral(), fpLiteral()),
				set(lastValue()),
				group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.1
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	Rule fpLiteral() {
		return Sequence(
				Sequence(
						FirstOf(
								Sequence(OneOrMore(digit()), Optional(Sequence(Ch('.'), ZeroOrMore(digit())))),
								Sequence(Ch('.'), OneOrMore(digit()))),
						Optional(
								Sequence(
										CharIgnoreCase('e'),
										Optional(FirstOf(Ch('+'), Ch('-'))),
										OneOrMore(digit()))),
						numberTypeSuffix()),
				set(actions.createNumberLiteral(lastText())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.1
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	Rule hexLiteral() {
		return Sequence(
				Sequence(
						Sequence(Ch('0'), CharIgnoreCase('x')),
						FirstOf(
								hexFP(),
								Sequence(OneOrMore(hexDigit()), numberTypeSuffix())
								)),
				set(actions.createNumberLiteral(lastText())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	Rule hexFP() {
		return Sequence(
				FirstOf(
						Sequence(Ch('.'), OneOrMore(hexDigit())),
						Sequence(
								OneOrMore(hexDigit()),
								Optional(Sequence(Ch('.'), ZeroOrMore(hexDigit()))))),
				Sequence(
						CharIgnoreCase('p'),
						Optional(FirstOf(Ch('+'), Ch('-'))),
						OneOrMore(digit())),
				numberTypeSuffix());
	}
	
	Rule numberTypeSuffix() {
		return Optional(FirstOf(CharIgnoreCase('d'), CharIgnoreCase('f'), CharIgnoreCase('l')));
	}
	
	Rule digit() {
		return FirstOf(Ch('0'), Ch('1'), Ch('2'), Ch('3'), Ch('4'), Ch('5'), Ch('6'), Ch('7'), Ch('8'), Ch('9'), Ch('0'));
	}
	
	Rule hexDigit() {
		return FirstOf(digit(),
				CharIgnoreCase('a'), CharIgnoreCase('b'), CharIgnoreCase('c'), CharIgnoreCase('d'), CharIgnoreCase('e'), CharIgnoreCase('f'));
	}
}
