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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.ast.Node;

import org.parboiled.BaseParser;
import org.parboiled.MatcherContext;
import org.parboiled.Rule;
import org.parboiled.matchers.AbstractMatcher;
import org.parboiled.support.Characters;
import org.parboiled.support.InputLocation;

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
		return zeroOrMore(firstOf(comment(), whitespaceChar())).label("optWS");
	}
	
	/**
	 * Eats up any whitespace and comments at the current position,
	 * but only matches if there is at least one comment or whitespace character to gobble up.
	 */
	public Rule mandatoryWS() {
		return oneOrMore(firstOf(comment(), whitespaceChar())).label("optWS");
	}
	
	public Rule testLexBreak() {
		return testNot(identifierPart());
	}
	
	public Rule identifier() {
		return sequence(
				identifierRaw().label("identifier"),
				actions.checkIfKeyword(TEXT("identifier")),
				SET(actions.createIdentifier(TEXT("identifier"), NODE("identifier"))),
				optWS()).label("identifier");
	}
	
	public Rule dotIdentifier() {
		return sequence(
				ch('.'), optWS(),
				identifierRaw().label("identifier"),
				actions.checkIfKeyword(TEXT("identifier")),
				SET(actions.createIdentifier(TEXT("identifier"), NODE("identifier"))),
				optWS()).label("identifier");
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
	
	public Rule identifierRaw() {
		return new IdentifierMatcher();
	}
	
	public Rule identifierPart() {
		return new TestBreakMatcher();
	}
	
	private static class TestBreakMatcher extends AbstractMatcher<Node> {
		@Override public Characters getStarterChars() {
			Characters c = Characters.NONE;
			
			for (char a = 'a'; a <= 'z'; a++) c = c.add(a);
			for (char a = 'A'; a <= 'Z'; a++) c = c.add(a);
			for (char a = '0'; a <= '9'; a++) c = c.add(a);
			c = c.add('$').add('_');
			
			return c;
		}
		
		@Override public boolean match(MatcherContext<Node> context) {
			char current = context.getCurrentLocation().currentChar;
			if (Character.isJavaIdentifierPart(current)) {
				context.advanceInputLocation();
				context.createNode();
				context.createNode();
				return true;
			}
			return false;
		}
	}
	
	private static class LineCommentMatcher extends AbstractMatcher<Node> {
		@Override public Characters getStarterChars() {
			return Characters.of('/');
		}
		
		@Override public boolean match(MatcherContext<Node> context) {
			InputLocation start = context.getCurrentLocation();
			if (context.getCurrentLocation().currentChar != '/') return false;
			context.advanceInputLocation();
			if (context.getCurrentLocation().currentChar != '/') {
				context.setCurrentLocation(start);
				return false;
			}
			
			int pos = context.getCurrentLocation().index;
			while (true) {
				context.advanceInputLocation();
				int newPos = context.getCurrentLocation().index;
				if (pos == newPos) {
					//We're at the end of the file.
					context.createNode();
					return true;
				}
				pos = newPos;
				char c = context.getCurrentLocation().currentChar;
				if (c == '\n') {
					context.advanceInputLocation();
					context.createNode();
					return true;
				}
				if (c == '\r') {
					context.advanceInputLocation();
					if (context.getCurrentLocation().currentChar == '\n') {
						context.advanceInputLocation();
					}
					context.createNode();
					return true;
				}
			}
		}
	}
	
	private static class BlockCommentMatcher extends AbstractMatcher<Node> {
		@Override public Characters getStarterChars() {
			return Characters.of('/');
		}
		
		@Override public boolean match(MatcherContext<Node> context) {
			InputLocation start = context.getCurrentLocation();
			if (context.getCurrentLocation().currentChar != '/') return false;
			context.advanceInputLocation();
			if (context.getCurrentLocation().currentChar != '*') {
				context.setCurrentLocation(start);
				return false;
			}
			int pos = context.getCurrentLocation().index;
			boolean star = false;
			while (true) {
				context.advanceInputLocation();
				int newPos = context.getCurrentLocation().index;
				if (pos == newPos) {
					//We're at the end of the file.
					context.createNode();
					return true;
				}
				pos = newPos;
				char c = context.getCurrentLocation().currentChar;
				if (c == '/' && star) {
					context.advanceInputLocation();
					context.createNode();
					return true;
				}
				star = c == '*';
			}
		}
	}
	
	private static class IdentifierMatcher extends AbstractMatcher<Node> {
		@Override public Characters getStarterChars() {
			Characters c = Characters.NONE;
			
			for (char a = 'a'; a <= 'z'; a++) c = c.add(a);
			for (char a = 'A'; a <= 'Z'; a++) c = c.add(a);
			c = c.add('$').add('_');
			
			return c;
		}
		
		@Override public boolean match(MatcherContext<Node> context) {
			char current = context.getCurrentLocation().currentChar;
			if (Character.isJavaIdentifierStart(current)) {
				context.advanceInputLocation();
				while (Character.isJavaIdentifierPart(context.getCurrentLocation().currentChar)) {
					int pos = context.getCurrentLocation().index;
					context.advanceInputLocation();
					if (pos == context.getCurrentLocation().index) break;
				}
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
		return firstOf(
				lineComment(),
				blockComment());
	}
	
	Rule lineComment() {
		return enforcedSequence(
				new LineCommentMatcher(),
				actions.logLineComment(LAST_TEXT()));
	}
	
	Rule blockComment() {
		return enforcedSequence(
				new BlockCommentMatcher(),
				actions.logBlockComment(LAST_TEXT()));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.4
	 */
	Rule whitespaceChar() {
		return firstOf(ch(' '), ch('\t'), ch('\f'), lineTerminator());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.6
	 */
	public Rule lineTerminator() {
		return firstOf(string("\r\n"), ch('\r'), ch('\n'));
	}
}
