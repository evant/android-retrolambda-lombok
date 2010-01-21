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

import org.parboiled.Actions;
import org.parboiled.BaseParser;
import org.parboiled.Rule;

public class JavaParser extends BaseParser<Node, Actions<Node>> {
	private final ParserGroup group;
	
	/*
	 * TODO:
	 * package
	 * import
	 * CU
	 * integrate backslash-u parsing
	 */
	
	public JavaParser(ParserGroup group) {
		this.group = group;
		
	}
	
	public Rule compilationUnit() {
		return enforcedSequence(
				optional(packageDeclaration()),
				zeroOrMore(importDeclaration()),
				zeroOrMore(typeDeclaration()),
				eoi()
		);
	}
	
	public Rule testRules() {
		return sequence(
				zeroOrMore(group.statements.anyStatement()),
				eoi());
	}
	
	@Override protected Rule fromCharLiteral(char c) {
		return enforcedSequence(ch(c), group.basics.optWS());
	}
	
	@Override protected Rule fromStringLiteral(String string) {
		return enforcedSequence(string(string), group.basics.optWS());
	}
	
	public Rule typeDeclaration() {
		return sequence(zeroOrMore(typeModifier()), string("class"), group.basics.mandatoryWS(), group.basics.identifier(), '{', '}');
	}
	
	public Rule typeModifier() {
		return enforcedSequence(firstOf(string("public"), string("protected"), string("private"), string("static"), string("abstract"), string("strictfp")), group.basics.mandatoryWS());
	}
	
	public Rule importDeclaration() {
		return enforcedSequence(
				sequence(
					string("import"),
					group.basics.testLexBreak(),
					group.basics.optWS()),
				optional(sequence(
						string("static"),
						group.basics.testLexBreak(),
						group.basics.optWS())).label("static"),
				sequenceOfIdentifiers(),
				optional(sequence(
						ch('.'), group.basics.optWS(), ch('*'), group.basics.optWS())).label("starImport"),
				ch(';'),
				group.basics.optWS());
	}
	
	Rule sequenceOfIdentifiers() {
		return sequence(
				group.basics.identifier(),
				zeroOrMore(sequence(
						ch('.'),
						group.basics.optWS(),
						group.basics.identifier())));
	}
	
	public Rule packageDeclaration() {
		return enforcedSequence(
				sequence(
						string("package"),
						group.basics.testLexBreak(),
						group.basics.optWS()),
				sequenceOfIdentifiers(),
				ch(';'),
				group.basics.optWS());
	}
}
