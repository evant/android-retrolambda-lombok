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

public class TypesParser extends BaseParser<Node> {
	final ParserGroup group;
	final TypesActions actions;
	
	public TypesParser(ParserGroup group) {
		actions = new TypesActions(group.getSource());
		this.group = group;
	}
	
	public Rule nonArrayType() {
		return firstOf(primitiveType(), referenceType());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.2
	 */
	public Rule type() {
		return sequence(
				nonArrayType(),
				SET(),
				zeroOrMore(sequence(
						ch('['), group.basics.optWS(), ch(']'), group.basics.optWS())),
				SET(actions.setArrayDimensionsOfType(VALUE(), TEXTS("zeroOrMore/sequence")))).label("type");
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.2
	 */
	public Rule primitiveType() {
		return sequence(
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
				zeroOrMore(dotReferenceTypePart().label("tail")),
				SET(actions.createReferenceType(VALUE("head"), VALUES("zeroOrMore/tail"))));
	}
	
	Rule dotReferenceTypePart() {
		return sequence(
				ch('.'), group.basics.optWS(),
				group.basics.identifier().label("partName"),
				optional(typeArguments()),
				SET(actions.createTypeReferencePart(NODE("partName"), VALUE("optional/typeArguments"))),
				group.basics.optWS());
	}
	
	Rule referenceTypePart() {
		return sequence(
				group.basics.identifier().label("partName"),
				optional(typeArguments()),
				SET(actions.createTypeReferencePart(NODE("partName"), VALUE("optional/typeArguments"))),
				group.basics.optWS());
	}
	
	public Rule plainReferenceType() {
		return sequence(
				plainReferenceTypePart().label("head"),
				zeroOrMore(dotPlainReferenceTypePart().label("tail")),
				SET(actions.createReferenceType(VALUE("head"), VALUES("zeroOrMore/tail"))));
	}
	
	Rule plainReferenceTypePart() {
		return sequence(
				group.basics.identifier().label("partName"),
				SET(actions.createTypeReferencePart(NODE("partName"), null)),
				group.basics.optWS());
	}
	
	Rule dotPlainReferenceTypePart() {
		return sequence(
				ch('.'), group.basics.optWS(),
				group.basics.identifier().label("partName"),
				SET(actions.createTypeReferencePart(NODE("partName"), null)),
				group.basics.optWS());
	}
	
	public Rule typeVariables() {
		return optional(sequence(
				ch('<'),
				group.basics.optWS(),
				optional(sequence(
						typeVariable().label("head"),
						zeroOrMore(sequence(
								ch(','),
								group.basics.optWS(),
								typeVariable().label("tail"))))),
				ch('>'),
				SET(actions.createTypeVariables(VALUE("optional/sequence/head"), VALUES("optional/sequence/zeroOrMore/sequence/tail"))),
				group.basics.optWS()));
	}
	
	Rule typeVariable() {
		return sequence(
				group.basics.identifier(),
				optional(sequence(
						sequence(
							string("extends"),
							group.basics.testLexBreak(),
							group.basics.optWS()),
						type(),
						zeroOrMore(sequence(
								ch('&'), group.basics.optWS(),
								type())))),
				SET(actions.createTypeVariable(VALUE("identifier"), VALUE("optional/sequence/type"), VALUES("optional/sequence/zeroOrMore/sequence/type"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.5
	 */
	public Rule typeArguments() {
		return optional(sequence(
				ch('<'),
				group.basics.optWS(),
				optional(sequence(
						typeArgument().label("head"),
						zeroOrMore(sequence(
								ch(','),
								group.basics.optWS(),
								typeArgument().label("tail"))))),
				ch('>'),
				SET(actions.createTypeArguments(VALUE("optional/sequence/head"), VALUES("optional/sequence/zeroOrMore/sequence/tail"))),
				group.basics.optWS())).label("typeArguments");
	}
	
	public Rule typeArgument() {
		return firstOf(
				type(),
				sequence(
						ch('?').label("qmark"),
						group.basics.optWS(),
						firstOf(string("extends"), string("super")).label("boundType"),
						group.basics.testLexBreak(),
						group.basics.optWS(),
						type(),
						SET(actions.createWildcardedType(NODE("qmark"), NODE("boundType"), TEXT("boundType"), VALUE("type")))),
				sequence(
						ch('?').label("qmark"),
						SET(actions.createUnboundedWildcardType(NODE("qmark"))),
						group.basics.optWS()));
	}
}
