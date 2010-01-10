package lombok.ast.grammar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import lombok.ast.Node;

import org.parboiled.Actions;
import org.parboiled.BaseParser;
import org.parboiled.MatcherContext;
import org.parboiled.Rule;
import org.parboiled.matchers.AbstractMatcher;
import org.parboiled.support.Characters;

/**
 * Contains the basics of java parsing: Whitespace and comment handling, as well as applying backslash-u escapes.
 */
public class BasicsParser extends BaseParser<Node, Actions<Node>> {
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
	
	public Rule fqn() {
		return enforcedSequence(identifier(), optWS(), zeroOrMore(sequence(ch('.'), optWS(), identifier(), optWS())));
	}
	
	public Rule identifier() {
		return sequence(identifierStart(), zeroOrMore(identifierPart()), optWS());
	}
	
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
			return Characters.ALL_EXCEPT_EMPTY;
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
	
	private Rule lineComment() {
		return enforcedSequence(string("//"), zeroOrMore(sequence(testNot(lineTerminator()), any())), lineTerminator());
	}
	
	private Rule blockComment() {
		return enforcedSequence(string("/*"), zeroOrMore(sequence(testNot(string("*/")), any())), string("*/"));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.4
	 */
	private Rule whitespaceChar() {
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
					//True WTF moment in the JLS: backslash-u-u-u-u-u-u-u-u-u-4hexdigits means the same thing as just 1 u.
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
