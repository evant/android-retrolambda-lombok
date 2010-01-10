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

import org.parboiled.Actions;
import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;

public class TypesParser extends BaseParser<Object, Actions<Object>> {
	private final BasicsParser basics = Parboiled.createParser(BasicsParser.class);
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.2
	 */
	public Rule type() {
		return firstOf(arrayType(), primitiveType(), referenceType());
	}
	
	public Rule arrayType() {
		return sequence(
				firstOf(primitiveType(), referenceType()),
				oneOrMore(
						sequence(ch('['), basics.optWS(), ch(']'), basics.optWS())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.2
	 */
	public Rule primitiveType() {
		return enforcedSequence(
				firstOf(
						sequence(string("boolean"), basics.testLexBreak()),
						sequence(string("int"), basics.testLexBreak()),
						sequence(string("long"), basics.testLexBreak()),
						sequence(string("double"), basics.testLexBreak()),
						sequence(string("float"), basics.testLexBreak()),
						sequence(string("short"), basics.testLexBreak()),
						sequence(string("char"), basics.testLexBreak()),
						sequence(string("byte"), basics.testLexBreak())),
						sequence(string("void"), basics.testLexBreak()),
				basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.3
	 */
	public Rule referenceType() {
		return sequence(
				basics.identifier(), optional(typeArguments()), basics.optWS(),
				zeroOrMore(enforcedSequence(
						ch('.'),
						basics.optWS(),
						basics.identifier(), optional(typeArguments()), basics.optWS())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.5
	 */
	private Rule typeArguments() {
		return optional(enforcedSequence(
				ch('<'),
				basics.optWS(),
				optional(enforcedSequence(
						genericsParam(),
						zeroOrMore(enforcedSequence(
								ch(','),
								basics.optWS(),
								genericsParam())))),
				ch('>'),
				basics.optWS()));
	}
	
	public Rule genericsParam() {
		return sequence(
				firstOf(
						enforcedSequence(
								sequence(ch('?'), basics.optWS(), firstOf(string("extends"), string("super")), basics.mandatoryWS()),
								type()),
						sequence(ch('?'), basics.optWS(), test(firstOf(ch(','), ch('>')))),
						type()
						),
				basics.optWS());
	}
}
