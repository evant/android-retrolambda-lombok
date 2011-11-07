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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.ast.Node;

import org.parboiled.BaseParser;
import org.parboiled.MatcherContext;
import org.parboiled.Rule;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.matchers.CharSetMatcher;
import org.parboiled.support.Characters;

/**
 * Contains the basics of java parsing: Whitespace and comment handling, as well as applying backslash-u escapes.
 */
public class BasicsParser extends BaseParser<Node> {
	final ParserGroup group;
	final BasicsActions actions;
	
	public BasicsParser(ParserGroup group) {
		this.group = group;
		this.actions = new BasicsActions(group.getSource());
	}
	
	/**
	 * Eats up any whitespace and comments at the current position.
	 */
	public Rule optWS() {
		return ZeroOrMore(FirstOf(comment(), whitespaceChar())).label("ws");
	}
	
	/**
	 * Eats up any whitespace and comments at the current position,
	 * but only matches if there is at least one comment or whitespace character to gobble up.
	 */
	public Rule mandatoryWS() {
		return OneOrMore(FirstOf(comment(), whitespaceChar())).label("ws");
	}
	
	public Rule testLexBreak() {
		return TestNot(identifierPart());
	}
	
	public Rule identifier() {
		return Sequence(
				identifierRaw().label("identifier"),
				actions.checkIfKeyword(text("identifier")),
				set(actions.createIdentifier(text("identifier"), node("identifier"))),
				optWS());
	}
	
	public Rule dotIdentifier() {
		return Sequence(
				Ch('.'), optWS(),
				identifierRaw().label("identifier"),
				actions.checkIfKeyword(text("identifier")),
				set(actions.createIdentifier(text("identifier"), node("identifier"))),
				optWS());
	}
	
	/**
	 * Technically {@code null}, {@code true} and {@code false} aren't keywords but specific literals, but, from a parser point of view they are keywords.
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.9
	 */
	static final List<String> KEYWORDS = Collections.unmodifiableList(Arrays.asList(
			"abstract", "class", "interface", "enum", "static", "final", "volatile", "transient", "strictfp", "native",
			"boolean", "byte", "short", "char", "int", "long", "float", "double", "void",
			"null", "this", "super", "true", "false",
			"continue", "break", "goto", "case", "default", "instanceof",
			"if", "do", "while", "for", "else", "synchronized", "switch", "assert", "throw", "try", "catch", "finally", "new", "return",
			"throws", "extends", "implements",
			"import", "package", "const",
			"public", "private", "protected"
	));
	
	@SuppressSubnodes
	public Rule identifierRaw() {
		return Sequence(new JavaIdentifierStartMatcher(), ZeroOrMore(new JavaIdentifierPartMatcher()));
	}
	
	public Rule identifierPart() {
		return new JavaIdentifierPartMatcher();
	}
	
	private static class JavaIdentifierPartMatcher extends CharSetMatcher<Node> {
		public JavaIdentifierPartMatcher() {
			super(Characters.of("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_$"));
		}
		
		@Override public boolean match(MatcherContext<Node> context) {
			char current = context.getCurrentChar();
			if (Character.isJavaIdentifierPart(current)) {
				context.advanceIndex();
				context.createNode();
				return true;
			}
			return false;
		}
	}
	
	private static class JavaIdentifierStartMatcher extends CharSetMatcher<Node> {
		public JavaIdentifierStartMatcher() {
			super(Characters.of("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_$"));
		}
		
		@Override public boolean match(MatcherContext<Node> context) {
			char current = context.getCurrentChar();
			if (Character.isJavaIdentifierStart(current)) {
				context.advanceIndex();
				context.createNode();
				return true;
			}
			return false;
		}
	}
	
	
	/**
	 * Any comment (block, line, or javadoc)
	 * 
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.7
	 */
	public Rule comment() {
		return Sequence(
				FirstOf(lineComment(), blockComment()),
				actions.logComment(lastText()));
	}
	
	@SuppressSubnodes
	Rule lineComment() {
		return Sequence(String("//"), ZeroOrMore(Sequence(TestNot(CharSet("\r\n")), Any())), FirstOf(String("\r\n"), Ch('\r'), Ch('\n'), Test(Eoi())));
	}
	
	@SuppressSubnodes
	Rule blockComment() {
		return Sequence("/*", ZeroOrMore(Sequence(TestNot("*/"), Any())), "*/");
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.4
	 */
	@SuppressSubnodes
	Rule whitespaceChar() {
		return FirstOf(Ch(' '), Ch('\t'), Ch('\f'), lineTerminator());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.6
	 */
	@SuppressSubnodes
	public Rule lineTerminator() {
		return FirstOf(String("\r\n").label("\\r\\n"), Ch('\r').label("\\r"), Ch('\n').label("\\n"));
	}
}