package lombok.ast.grammar;

import lombok.ast.Node;

import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;

public class JavaParser extends BaseParser<Node, JavaActions> {
	private BasicsParser basics = Parboiled.createParser(BasicsParser.class);
	private LiteralsParser literals = Parboiled.createParser(LiteralsParser.class);
	private TypesParser types = Parboiled.createParser(TypesParser.class);
	
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
				zeroOrMore(firstOf(
						literals.anyLiteral(),
						types.type())),
				eoi());
	}
	
	@Override protected Rule fromCharLiteral(char c) {
		return enforcedSequence(ch(c), basics.optWS());
	}
	
	@Override protected Rule fromStringLiteral(String string) {
		return enforcedSequence(string(string), basics.optWS());
	}
	
	public Rule typeDeclaration() {
		return sequence(zeroOrMore(typeModifier()), string("class"), basics.mandatoryWS(), basics.identifier(), '{', '}');
	}
	
	public Rule typeModifier() {
		return enforcedSequence(firstOf(string("public"), string("protected"), string("private"), string("static"), string("abstract"), string("strictfp")), basics.mandatoryWS());
	}
	
	public Rule importDeclaration() {
		return enforcedSequence(string("import"), basics.mandatoryWS(), basics.fqn(), ';');
	}
	
	public Rule packageDeclaration() {
		return enforcedSequence(string("package"), basics.mandatoryWS(), basics.fqn(), ';');
	}
	
	public Rule annotation() {
		return enforcedSequence(ch('@'), basics.optWS(), basics.fqn());
	}
}
