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

public class TypesParser extends BaseParser<Node> {
	final ParserGroup group;
	final TypesActions actions;
	
	public TypesParser(ParserGroup group) {
		actions = new TypesActions(group.getSource());
		this.group = group;
	}
	
	public Rule nonArrayType() {
		return FirstOf(primitiveType(), referenceType());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.2
	 */
	public Rule type() {
		return Sequence(
				nonArrayType(),
				set(),
				ZeroOrMore(Sequence(
						Ch('['), group.basics.optWS(), Ch(']'), group.basics.optWS())),
				set(actions.setArrayDimensionsOfType(value(), texts("ZeroOrMore/Sequence")))).label("type");
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.2
	 */
	public Rule primitiveType() {
		return Sequence(
				rawPrimitiveType(),
				set(actions.createPrimitiveType(lastText())),
				group.basics.optWS());
	}
	
	@SuppressSubnodes
	Rule rawPrimitiveType() {
		return Sequence(
				FirstOf("boolean", "int", "long", "double", "float", "short", "char", "byte", "void"),
				group.basics.testLexBreak());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.3
	 */
	public Rule referenceType() {
		return Sequence(
				referenceTypePart().label("head"),
				ZeroOrMore(dotReferenceTypePart().label("tail")),
				set(actions.createReferenceType(value("head"), values("ZeroOrMore/tail"))));
	}
	
	Rule dotReferenceTypePart() {
		return Sequence(
				Ch('.'), group.basics.optWS(),
				group.basics.identifier().label("partName"),
				Optional(typeArguments()),
				set(actions.createTypeReferencePart(node("partName"), value("Optional/typeArguments"))),
				group.basics.optWS());
	}
	
	Rule referenceTypePart() {
		return Sequence(
				group.basics.identifier().label("partName"),
				Optional(typeArguments()),
				set(actions.createTypeReferencePart(node("partName"), value("Optional/typeArguments"))),
				group.basics.optWS());
	}
	
	public Rule plainReferenceType() {
		return Sequence(
				plainReferenceTypePart().label("head"),
				ZeroOrMore(dotPlainReferenceTypePart().label("tail")),
				set(actions.createReferenceType(value("head"), values("ZeroOrMore/tail"))));
	}
	
	Rule plainReferenceTypePart() {
		return Sequence(
				group.basics.identifier().label("partName"),
				set(actions.createTypeReferencePart(node("partName"), null)),
				group.basics.optWS());
	}
	
	Rule dotPlainReferenceTypePart() {
		return Sequence(
				Ch('.'), group.basics.optWS(),
				group.basics.identifier().label("partName"),
				set(actions.createTypeReferencePart(node("partName"), null)),
				group.basics.optWS());
	}
	
	public Rule typeVariables() {
		return Optional(Sequence(
				Ch('<'),
				group.basics.optWS(),
				Optional(Sequence(
						typeVariable().label("head"),
						ZeroOrMore(Sequence(
								Ch(','),
								group.basics.optWS(),
								typeVariable().label("tail"))))),
				Ch('>'),
				set(actions.createTypeVariables(value("Optional/Sequence/head"), values("Optional/Sequence/ZeroOrMore/Sequence/tail"))),
				group.basics.optWS()));
	}
	
	Rule typeVariable() {
		return Sequence(
				group.basics.identifier(),
				Optional(Sequence(
						Sequence(
							String("extends"),
							group.basics.testLexBreak(),
							group.basics.optWS()),
						type(),
						ZeroOrMore(Sequence(
								Ch('&'), group.basics.optWS(),
								type())))),
				set(actions.createTypeVariable(value("identifier"), value("Optional/Sequence/type"), values("Optional/Sequence/ZeroOrMore/Sequence/type"))));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#4.5
	 */
	public Rule typeArguments() {
		return Optional(Sequence(
				Ch('<'),
				group.basics.optWS(),
				Optional(Sequence(
						typeArgument().label("head"),
						ZeroOrMore(Sequence(
								Ch(','),
								group.basics.optWS(),
								typeArgument().label("tail"))))),
				Ch('>'),
				set(actions.createTypeArguments(value("Optional/Sequence/head"), values("Optional/Sequence/ZeroOrMore/Sequence/tail"))),
				group.basics.optWS())).label("typeArguments");
	}
	
	public Rule typeArgument() {
		return FirstOf(
				type(),
				Sequence(
						Ch('?').label("qmark"),
						group.basics.optWS(),
						FirstOf(String("extends"), String("super")).label("boundType"),
						group.basics.testLexBreak(),
						group.basics.optWS(),
						type(),
						set(actions.createWildcardedType(node("qmark"), node("boundType"), text("boundType"), value("type")))),
				Sequence(
						Ch('?').label("qmark"),
						set(actions.createUnboundedWildcardType(node("qmark"))),
						group.basics.optWS()));
	}
}
