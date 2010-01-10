package lombok.ast.grammar;

import lombok.ast.Node;

import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;

public class LiteralsParser extends BaseParser<Node, LiteralsActions> {
	private BasicsParser basics = Parboiled.createParser(BasicsParser.class);
	
	public LiteralsParser() {
		super(Parboiled.createActions(LiteralsActions.class));
	}
	
	public Rule anyLiteral() {
		return firstOf(
				nullLiteral(),
				booleanLiteral(),
				numberLiteral(),
				charLiteral(),
				stringLiteral());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.7
	 */
	public Rule nullLiteral() {
		return enforcedSequence(
			enforcedSequence(
					string("null"),
					basics.testLexBreak()),
			SET(actions.createNullLiteral(LAST_TEXT())),
			basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.5
	 */
	public Rule stringLiteral() {
		return enforcedSequence(
				enforcedSequence(
						ch('"'),
						zeroOrMore(
								sequence(
										testNot(firstOf(ch('"'), basics.lineTerminator())),
										firstOf(
												escapedSequence(),
												any()))),
						ch('"')),
				SET(actions.createStringLiteral(LAST_TEXT())),
				basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.4
	 */
	public Rule charLiteral() {
		return enforcedSequence(
				enforcedSequence(
						ch('\''),
						firstOf(
								enforcedSequence(escapedSequence(), ch('\'')),
								sequence(
										zeroOrMore(sequence(testNot(
												firstOf(ch('\''), basics.lineTerminator())), any())),
										ch('\'')),
								any())),
				SET(actions.createCharLiteral(LAST_TEXT())),
				basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.6
	 */
	private Rule escapedSequence() {
		return enforcedSequence(ch('\\'),
				firstOf(
						sequence(optional(zeroToThree()), octalDigit(), optional(octalDigit())),
						any()));
	}
	
	private Rule zeroToThree() {
		return firstOf(ch('0'), ch('1'), ch('2'), ch('3'));
	}
	
	private Rule octalDigit() {
		return firstOf(ch('0'), ch('1'), ch('2'), ch('3'), ch('4'), ch('5'), ch('6'), ch('7'));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.3
	 */
	public Rule booleanLiteral() {
		return enforcedSequence(
				enforcedSequence(
						firstOf(string("true"), string("false")),
						basics.testLexBreak()),
				SET(actions.createBooleanLiteral(LAST_TEXT())),
				basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.1
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	public Rule numberLiteral() {
		return enforcedSequence(
				test(sequence(optional(ch('.')), charRange('0', '9'))),
				firstOf(hexLiteral(), fpLiteral()),
				SET(LAST_VALUE()),
				basics.optWS());
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.1
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	private Rule fpLiteral() {
		return enforcedSequence(
				sequence(
						firstOf(
								enforcedSequence(oneOrMore(digit()), optional(sequence(ch('.'), zeroOrMore(digit())))),
								enforcedSequence(ch('.'), oneOrMore(digit()))),
						optional(
								enforcedSequence(
										charIgnoreCase('e'),
										optional(firstOf(ch('+'), ch('-'))),
										oneOrMore(digit()))),
						numberTypeSuffix()),
				SET(actions.createNumberLiteral(LAST_TEXT())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.1
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	private Rule hexLiteral() {
		return enforcedSequence(
				enforcedSequence(
						sequence(ch('0'), charIgnoreCase('x')),
						firstOf(
								hexFP(),
								sequence(oneOrMore(hexDigit()), numberTypeSuffix())
								)),
				SET(actions.createNumberLiteral(LAST_TEXT())));
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2
	 */
	private Rule hexFP() {
		return sequence(
				firstOf(
						enforcedSequence(ch('.'), oneOrMore(hexDigit())),
						enforcedSequence(
								oneOrMore(hexDigit()),
								optional(enforcedSequence(ch('.'), zeroOrMore(hexDigit()))))),
				enforcedSequence(
						charIgnoreCase('p'),
						optional(firstOf(ch('+'), ch('-'))),
						oneOrMore(digit())),
				numberTypeSuffix());
	}
	
	private Rule numberTypeSuffix() {
		return optional(firstOf(charIgnoreCase('d'), charIgnoreCase('f'), charIgnoreCase('l')));
	}
	
	private Rule digit() {
		return firstOf(ch('0'), ch('1'), ch('2'), ch('3'), ch('4'), ch('5'), ch('6'), ch('7'), ch('8'), ch('9'), ch('0'));
	}
	
	private Rule hexDigit() {
		return firstOf(digit(),
				charIgnoreCase('a'), charIgnoreCase('b'), charIgnoreCase('c'), charIgnoreCase('d'), charIgnoreCase('e'), charIgnoreCase('f'));
	}
}
