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

public class TypesParser extends BaseParser<Node, TypesActions> {
	private final BasicsParser basics = Parboiled.createParser(BasicsParser.class);
	
	public TypesParser() {
		super(Parboiled.createActions(TypesActions.class));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.2
	 */
	public Rule type() {
		return sequence(
				firstOf(primitiveType(), referenceType()),
				zeroOrMore(sequence(
						ch('['), basics.optWS(), ch(']'), basics.optWS())),
				SET(actions.addArrayDimensionsToType(VALUE("firstOf"), TEXTS("zeroOrMore/sequence")))).label("type");
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
						sequence(string("byte"), basics.testLexBreak()),
						sequence(string("void"), basics.testLexBreak())),
				SET(actions.createPrimitiveType(TEXT("firstOf/sequence"))),
				basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.3
	 */
	public Rule referenceType() {
		return sequence(
				referenceTypePart().label("head"),
				zeroOrMore(sequence(
						ch('.'),
						basics.optWS(),
						referenceTypePart().label("tail"))),
				SET(actions.createReferenceType(VALUE("head"), VALUES("zeroOrMore/sequence/tail"))));
	}
	
	private Rule referenceTypePart() {
		return sequence(
				basics.identifier().label("partName"),
				optional(typeArguments().label("partArgs")),
				SET(actions.createTypePart(VALUE("partName"), VALUE("optional/partArgs"))),
				basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.5
	 */
	private Rule typeArguments() {
		return optional(enforcedSequence(
				ch('<'),
				basics.optWS(),
				optional(enforcedSequence(
						genericsParam().label("head"),
						zeroOrMore(enforcedSequence(
								ch(','),
								basics.optWS(),
								genericsParam().label("tail"))))),
				ch('>'),
				SET(actions.createTypeArguments(VALUE("optional/enforcedSequence/head"), VALUES("optional/enforcedSequence/zeroOrMore/enforcedSequence/tail"))),
				basics.optWS()));
	}
	
	public Rule genericsParam() {
		return firstOf(
				type(),
				sequence(
						ch('?'),
						basics.optWS(),
						firstOf(string("extends"), string("super")),
						basics.testLexBreak(),
						basics.optWS(),
						type(),
						SET(actions.createWildcardedType(TEXT("firstOf"), VALUE("type")))),
				sequence(
						ch('?'),
						SET(actions.createUnboundedWildcardType()),
						basics.optWS()));
	}
}
