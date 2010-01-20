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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.ast.Node;

import org.parboiled.BaseParser;
import org.parboiled.MatcherContext;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.matchers.AbstractMatcher;
import org.parboiled.support.Characters;

/**
 * Contains the basics of java parsing: Whitespace and comment handling, as well as applying backslash-u escapes.
 */
public class BasicsParser extends BaseParser<Node, BasicsActions> {
	@SuppressWarnings("unused")
	private final ParserGroup group;
	
	public BasicsParser(ParserGroup group) {
		super(Parboiled.createActions(BasicsActions.class));
		this.group = group;
	}
	/**
	 * Eats up any whitespace and comments at the current position.
	 */
	public Rule optWS() {
		return zeroOrMore(firstOf(comment(), whitespaceChar()));
	}
	
	/**
	 * Eats up any whitespace and comments at the current position,
	 * but only matches if there is at least one comment or whitespace character to gobble up.
	 */
	public Rule mandatoryWS() {
		return oneOrMore(firstOf(comment(), whitespaceChar()));
	}
	
	public Rule testLexBreak() {
		return testNot(identifierPart());
	}
	
	public Rule identifier() {
		return sequence(
				sequence(identifierStart(), zeroOrMore(identifierPart())).label("identifier"),
				actions.checkIfKeyword(TEXT("identifier")),
				SET(actions.createIdentifier(TEXT("identifier"))),
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
	
	public Rule identifierStart() {
		return new IdentifierMatcher(true);
	}
	
	public Rule identifierPart() {
		return new IdentifierMatcher(false);
	}
	
	private static class IdentifierMatcher extends AbstractMatcher<Node> {
		private final boolean start;
		
		public IdentifierMatcher(boolean start) {
			this.start = start;
		}
		
		@Override public Characters getStarterChars() {
			Characters c = Characters.NONE;
			
			for (char a = 'a'; a <= 'z'; a++) c = c.add(a);
			for (char a = 'A'; a <= 'Z'; a++) c = c.add(a);
			if (!start) for (char a = '0'; a <= '9'; a++) c = c.add(a);
			c = c.add('$').add('_');
			
			return c;
		}
		
		@Override public boolean match(MatcherContext<Node> context, boolean enforced) {
			char current = context.getCurrentLocation().currentChar;
			if (start ? Character.isJavaIdentifierStart(current) : Character.isJavaIdentifierPart(current)) {
				context.advanceInputLocation();
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
				string("//"),
				zeroOrMore(sequence(testNot(lineTerminator()), any())).label("comment"),
				lineTerminator(),
				SET(actions.createLineComment(TEXT("comment"))));
	}
	
	Rule blockComment() {
		return enforcedSequence(
				string("/*"),
				zeroOrMore(sequence(testNot(string("*/")), any())).label("comment"),
				string("*/"),
				SET(actions.createBlockComment(TEXT("comment"))));
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
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.3
	 */
	public void applyBackslashU(Reader rawIn, Writer rawOut) throws IOException {
		StringBuilder buffer = new StringBuilder();
		
		Reader in = (rawIn instanceof BufferedReader) ? rawIn : new BufferedReader(rawIn);
		Writer out = (rawOut instanceof BufferedWriter) ? rawOut : new BufferedWriter(rawOut);
		
		int state = 0;
		int backslashCount = 0;
		
		while (true) {
			int c = in.read();
			if (c == -1) break;
			switch (state) {
			case 0:	//normal mode. Anything that isn't a backslash is not interesting.
				if (c != '\\') {
					out.write(c);
					break;
				}
				
				buffer.append((char)c);
				backslashCount++;
				state = 1;
				break;
			case 1:	//Last character read is a backslash.
				if ((backslashCount % 2 == 0) || c != 'u') {
					out.write(buffer.toString());
					if (c != '\\') {
						buffer.setLength(0);
						backslashCount = 0;
						out.write(c);
					} else {
						backslashCount++;
					}
				} else {
					//Backslash u escape that needs to be applied found. Start gobbling up characters.
					state = 2;
					buffer.append("u");
				}
				break;
			default:
				//Gobbling hex digits. state-2 is our current position. We want 4.
				buffer.append(c);
				if (c == 'u') {
					//JLS Puzzler: backslash-u-u-u-u-u-u-u-u-u-4hexdigits means the same thing as just 1 u.
					break;
				}
				if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
					state++;
					if (state == 6) {
						//We've got our 4 hex digits.
						buffer.setLength(0);
						out.write(Integer.parseInt(buffer.substring(buffer.length()-4), 0x10));
						//We don't have to check if this char is a backslash and set state to 1; JLS says backslash-u is not recursively applied.
						state = 0;
					}
				} else {
					//Invalid unicode escape.
					//TODO
				}
				break;
			}
		}
	}
}
