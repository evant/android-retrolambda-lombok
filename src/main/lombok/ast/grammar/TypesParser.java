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
	private final ParserGroup group;
	
	public TypesParser(ParserGroup group) {
		super(Parboiled.createActions(TypesActions.class));
		this.group = group;
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.2
	 */
	public Rule type() {
		return sequence(
				firstOf(primitiveType(), referenceType()),
				zeroOrMore(sequence(
						ch('['), group.basics.optWS(), ch(']'), group.basics.optWS())),
				SET(actions.addArrayDimensionsToType(VALUE("firstOf"), TEXTS("zeroOrMore/sequence")))).label("type");
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.2
	 */
	public Rule primitiveType() {
		return enforcedSequence(
				firstOf(
						sequence(string("boolean"), group.basics.testLexBreak()),
						sequence(string("int"), group.basics.testLexBreak()),
						sequence(string("long"), group.basics.testLexBreak()),
						sequence(string("double"), group.basics.testLexBreak()),
						sequence(string("float"), group.basics.testLexBreak()),
						sequence(string("short"), group.basics.testLexBreak()),
						sequence(string("char"), group.basics.testLexBreak()),
						sequence(string("byte"), group.basics.testLexBreak()),
						sequence(string("void"), group.basics.testLexBreak())),
				SET(actions.createPrimitiveType(TEXT("firstOf/sequence"))),
				group.basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.3
	 */
	public Rule referenceType() {
		return sequence(
				referenceTypePart().label("head"),
				zeroOrMore(sequence(
						ch('.'),
						group.basics.optWS(),
						referenceTypePart().label("tail"))),
				SET(actions.createReferenceType(VALUE("head"), VALUES("zeroOrMore/sequence/tail"))));
	}
	
	private Rule referenceTypePart() {
		return sequence(
				group.basics.identifier().label("partName"),
				optional(typeArguments()),
				SET(actions.createTypeReferencePart(VALUE("partName"), VALUE("optional/typeArguments"))),
				group.basics.optWS());
	}
	
	public Rule typeVariables() {
		return optional(enforcedSequence(
				ch('<'),
				group.basics.optWS(),
				optional(enforcedSequence(
						typeVariable().label("head"),
						zeroOrMore(enforcedSequence(
								ch(','),
								group.basics.optWS(),
								typeVariable().label("tail"))))),
				ch('>'),
				SET(actions.createTypeVariables(VALUE("optional/enforcedSequence/head"), VALUES("optional/enforcedSequence/zeroOrMore/enforcedSequence/tail"))),
				group.basics.optWS()));
	}
	
	private Rule typeVariable() {
		return sequence(
				group.basics.identifier(),
				optional(enforcedSequence(
						sequence(
							string("extends"),
							group.basics.testLexBreak(),
							group.basics.optWS()),
						type(),
						zeroOrMore(sequence(
								ch('&'), group.basics.optWS(),
								type())))),
				SET(actions.createTypeVariable(VALUE("identifier"), VALUE("optional/enforcedSequence/type"), VALUES("optional/enforcedSequence/zeroOrMore/sequence/type"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.5
	 */
	public Rule typeArguments() {
		return optional(enforcedSequence(
				ch('<'),
				group.basics.optWS(),
				optional(enforcedSequence(
						typeArgument().label("head"),
						zeroOrMore(enforcedSequence(
								ch(','),
								group.basics.optWS(),
								typeArgument().label("tail"))))),
				ch('>'),
				SET(actions.createTypeArguments(VALUE("optional/enforcedSequence/head"), VALUES("optional/enforcedSequence/zeroOrMore/enforcedSequence/tail"))),
				group.basics.optWS())).label("typeArguments");
	}
	
	public Rule typeArgument() {
		return firstOf(
				type(),
				sequence(
						ch('?'),
						group.basics.optWS(),
						firstOf(string("extends"), string("super")),
						group.basics.testLexBreak(),
						group.basics.optWS(),
						type(),
						SET(actions.createWildcardedType(TEXT("firstOf"), VALUE("type")))),
				sequence(
						ch('?'),
						SET(actions.createUnboundedWildcardType()),
						group.basics.optWS()));
	}
}
