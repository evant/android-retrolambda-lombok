package lombok.ast.grammar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.ast.Node;

import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;

public class StructuresParser extends BaseParser<Node, StructuresActions> {
	private final ParserGroup group;
	
	public StructuresParser(ParserGroup group) {
		super(Parboiled.createActions(StructuresActions.class));
		this.group = group;
	}
	
	public Rule classBody() {
		//TODO dummy
		return enforcedSequence(
				ch('{'), group.basics.optWS(), ch('}'), group.basics.optWS());
	}
	
	public Rule methodArguments() {
		return enforcedSequence(
				ch('('),
				group.basics.optWS(),
				optional(sequence(
						group.expressions.anyExpression(),
						SET(),
						zeroOrMore(sequence(
								ch(','),
								group.basics.optWS(),
								group.expressions.anyExpression(), SET())))),
				ch(')'),
				group.basics.optWS(),
				SET(actions.createMethodArguments(VALUE("optional/sequence"), VALUES("optional/sequence/zeroOrMore/sequence"))));
	}
	
	public Rule classDeclaration() {
		//TODO dummy
		return sequence(
				string("class"),
				group.basics.testLexBreak(),
				group.basics.optWS(),
				group.basics.identifier(),
				classBody());
	}
	
	public Rule constructorDeclaration() {
		return sequence(
				methodDeclarationModifiers().label("modifiers"),
				group.types.typeVariables().label("typeParameters"),
				group.basics.identifier().label("typeName"),
				ch('('), group.basics.optWS(),
				zeroOrMore(methodParameter().label("param")).label("params"),
				ch(')'), group.basics.optWS(),
				optional(enforcedSequence(
						sequence(string("throws"), group.basics.testLexBreak(), group.basics.optWS()),
						group.types.type().label("throwsHead"),
						zeroOrMore(sequence(ch(','), group.basics.optWS(), group.types.type()).label("throwsTail"))
						)).label("throwsClause"),
				firstOf(
						sequence(ch(';'), group.basics.optWS()),
						group.statements.blockStatement()).label("body"),
				SET(actions.createConstructorDeclaration(VALUE("modifiers"), VALUE("typeParameters"), VALUE("typeName"), VALUES("params/param"), 
						VALUE("throwsClause/enforcedSequence/throwsHead"), VALUES("throwsClause/enforcedSequence/zeroOrMore/throwsTail"),
						VALUE("body"))));
	}
	
	public Rule methodDeclaration() {
		return sequence(
				methodDeclarationModifiers().label("modifiers"),
				group.types.typeVariables().label("typeParameters"),
				group.types.type().label("resultType"),
				group.basics.identifier().label("methodName"),
				ch('('), group.basics.optWS(),
				zeroOrMore(methodParameter().label("param")).label("params"),
				ch(')'), group.basics.optWS(),
				zeroOrMore(enforcedSequence(ch('['), group.basics.optWS(), ch(']'), group.basics.optWS()).label("dim")).label("dims"),
				optional(enforcedSequence(
						sequence(string("throws"), group.basics.testLexBreak(), group.basics.optWS()),
						group.types.type().label("throwsHead"),
						zeroOrMore(sequence(ch(','), group.basics.optWS(), group.types.type()).label("throwsTail"))
						)).label("throwsClause"),
				firstOf(
						sequence(ch(';'), group.basics.optWS()),
						group.statements.blockStatement()).label("body"),
				SET(actions.createMethodDeclaration(VALUE("modifiers"), VALUE("typeParameters"), VALUE("resultType"), VALUE("methodName"), VALUES("params/param"), 
						TEXTS("dims/dim"), VALUE("throwsClause/enforcedSequence/throwsHead"), VALUES("throwsClause/enforcedSequence/zeroOrMore/throwsTail"),
						VALUE("body"))));
	}
	
	Rule methodParameter() {
		return sequence(
				variableDeclarationModifiers().label("modifiers"),
				group.types.type().label("type"),
				optional(string("...")).label("varargs"),
				group.basics.identifier().label("name"),
				zeroOrMore(sequence(ch('['), group.basics.optWS(), ch(']'), group.basics.optWS()).label("dim")).label("dims"),
				SET(actions.createMethodParameter(VALUE("modifiers"), VALUE("type"), TEXT("varargs"), VALUE("name"), TEXTS("dims/dim"))));
	}
	
	public Rule instanceInitializer() {
		return sequence(
				group.statements.blockStatement().label("initializer"),
				SET(actions.createInstanceInitializer(VALUE("initializer"))));
	}
	
	public Rule staticInitializerBlock() {
		return sequence(
				string("static"), group.basics.testLexBreak(), group.basics.optWS(),
				group.statements.blockStatement().label("initializer"),
				SET(actions.createStaticInitializer(VALUE("initializer"))));
	}
	
	public Rule fieldDeclaration() {
		return sequence(
				fieldDeclarationModifiers().label("modifiers"),
				variableDeclaration(), SET(),
				SET(actions.addFieldModifiers(VALUE(), VALUE("modifiers"))));
	}
	
	/**
	 * Add your own modifiers!
	 */
	Rule variableDeclaration() {
		return sequence(
				group.types.type().label("type"),
				variableDeclarationPart().label("head"),
				zeroOrMore(sequence(
						ch(','), group.basics.optWS(),
						variableDeclarationPart()).label("tail")),
				ch(';'), group.basics.optWS(),
				SET(actions.createVariableDeclaration(VALUE("type"), VALUE("head"), VALUES("zeroOrMore/tail"))));
	}
	
	Rule variableDeclarationPart() {
		return sequence(
				group.basics.identifier().label("varName"),
				zeroOrMore(enforcedSequence(ch('['), group.basics.optWS(), ch(']'), group.basics.optWS()).label("dim")).label("dims"),
				optional(sequence(
						ch('='), group.basics.optWS(),
						firstOf(
								group.expressions.arrayInitializer(),
								group.expressions.anyExpression()))).label("initializer"),
				SET(actions.createVariableDelarationPart(VALUE("varName"), TEXTS("dims/dim"), VALUE("initializer"))));
	}
	
	public Rule annotation() {
		return sequence(
				ch('@'), group.basics.optWS(),
				group.types.type().label("annotationType"),
				optional(enforcedSequence(
						ch('('), group.basics.optWS(),
						optional(firstOf(
								annotationElements(),
								sequence(annotationElementValue(),
										SET(actions.createAnnotationFromElement(LAST_VALUE()))))),
						ch(')'), group.basics.optWS())).label("content"),
				SET(actions.createAnnotation(VALUE("annotationType"), VALUE("content"))));
	}
	
	Rule annotationElements() {
		return sequence(
				annotationElement().label("head"),
				zeroOrMore(sequence(
						ch(','), group.basics.optWS(),
						annotationElement()).label("tail")),
				SET(actions.createAnnotationFromElements(VALUE("head"), VALUES("zeroOrMore/tail"))));
	}
	
	Rule annotationElement() {
		return sequence(
				group.basics.identifier().label("name"),
				ch('='), group.basics.optWS(),
				annotationElementValue().label("value"),
				SET(actions.createAnnotationElement(VALUE("name"), VALUE("value"))));
	}
	
	Rule annotationElementValue() {
		return firstOf(
				annotation(),
				enforcedSequence(
						ch('{'), group.basics.optWS(),
						optional(sequence(
								annotationElementValue().label("head"),
								zeroOrMore(sequence(
										ch(','), group.basics.optWS(),
										annotationElementValue()).label("tail")),
								optional(sequence(ch(','), group.basics.optWS())))),
						ch('}'), group.basics.optWS(),
						SET(actions.createAnnotationElementValueArrayInitializer(VALUE("optional/sequence/head"), VALUES("optional/sequence/zeroOrMore/tail")))),
				group.expressions.inlineIfExpressionChaining());
	}
	
	private static final List<String> MODIFIER_KEYWORDS = Collections.unmodifiableList(Arrays.asList(
			"final", "strictfp", "abstract", "transient", "volatile",
			"public", "protected", "private", "static", "native"
			));
	
	public Rule keywordModifier() {
		Object[] tail = new Object[MODIFIER_KEYWORDS.size() - 2];
		for (int i = 2; i < MODIFIER_KEYWORDS.size(); i++) tail[i-2] = string(MODIFIER_KEYWORDS.get(i));
		
		return sequence(
				firstOf(string(MODIFIER_KEYWORDS.get(0)), string(MODIFIER_KEYWORDS.get(1)), tail).label("keyword"),
				group.basics.testLexBreak(),
				SET(actions.createKeywordModifier(TEXT("keyword"))),
				group.basics.optWS());
	}
	
	public Rule methodDeclarationModifiers() {
		return sequence(
				zeroOrMore(anyModifier().label("modifier")),
				SET(actions.createModifiers(VALUES("zeroOrMore/modifier"))));
	}
	
	public Rule fieldDeclarationModifiers() {
		return sequence(
				zeroOrMore(anyModifier().label("modifier")),
				SET(actions.createModifiers(VALUES("zeroOrMore/modifier"))));
	}
	
	public Rule variableDeclarationModifiers() {
		return sequence(
				zeroOrMore(anyModifier().label("modifier")),
				SET(actions.createModifiers(VALUES("zeroOrMore/modifier"))));
	}
	
	public Rule anyModifier() {
		return firstOf(annotation(), keywordModifier());
	}
}
