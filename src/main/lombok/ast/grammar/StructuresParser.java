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
	
	public Rule typeBody() {
		return enforcedSequence(
				ch('{'), group.basics.optWS(),
				typeBodyDeclarations(),
				ch('}'), group.basics.optWS());
	}
	
	Rule typeBodyDeclarations() {
		return sequence(
				zeroOrMore(firstOf(
						anyTypeDeclaration(),
						fieldDeclaration(),
						methodDeclaration(),
						constructorDeclaration(),
						staticInitializer(),
						instanceInitializer(),
						sequence(ch(';'), group.basics.optWS())
						).label("member")).label("members"),
				SET(actions.createTypeBody(VALUES("members/member"))));
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
	
	public Rule anyTypeDeclaration() {
		return firstOf(
				classOrInterfaceDeclaration(),
				enumDeclaration(),
				annotationDeclaration());
	}
	
	public Rule classOrInterfaceDeclaration() {
		return sequence(
				typeDeclarationModifiers().label("modifiers"),
				firstOf(string("class"), string("interface")).label("kind"),
				group.basics.testLexBreak(), group.basics.optWS(),
				group.basics.identifier().label("typeName"),
				group.types.typeVariables().label("typeParameters"),
				zeroOrMore(firstOf(
						extendsClause(),
						implementsClause()).label("addon")).label("addons"),
				typeBody().label("body"),
				SET(actions.createTypeDeclaration(TEXT("kind"), VALUE("modifiers"), VALUE("typeName"), VALUE("typeParameters"), VALUE("body"), VALUES("addons/addon"))));
	}
	
	Rule extendsClause() {
		return enforcedSequence(
				sequence(string("extends"), group.basics.testLexBreak(), group.basics.optWS()),
				group.types.type().label("head"),
				zeroOrMore(enforcedSequence(
						ch(','), group.basics.optWS(),
						group.types.type()).label("tail")),
				SET(actions.createExtendsClause(VALUE("head"), VALUES("zeroOrMore/tail"))));
	}
	
	Rule implementsClause() {
		return enforcedSequence(
				sequence(string("extends"), group.basics.testLexBreak(), group.basics.optWS()),
				group.types.type().label("head"),
				zeroOrMore(enforcedSequence(
						ch(','), group.basics.optWS(),
						group.types.type()).label("tail")),
				SET(actions.createImplementsClause(VALUE("head"), VALUES("zeroOrMore/tail"))));
	}
	
	public Rule enumDeclaration() {
		return sequence(
				typeDeclarationModifiers().label("modifiers"),
				string("enum"), group.basics.testLexBreak(), group.basics.optWS(),
				group.basics.identifier().label("typeName"),
				zeroOrMore(firstOf(
						extendsClause(),
						implementsClause()).label("addon")).label("addons"),
				enumBody().label("body"),
				SET(actions.createEnumDeclaration(VALUE("modifiers"), VALUE("typeName"), VALUE("body"), VALUES("addons/addon"))));
	}
	
	public Rule annotationDeclaration() {
		return sequence(
				typeDeclarationModifiers().label("modifiers"),
				ch('@'), group.basics.optWS(),
				string("interface"), group.basics.testLexBreak(), group.basics.optWS(),
				group.basics.identifier().label("name"),
				ch('{'), group.basics.optWS(),
				zeroOrMore(annotationElementDeclaration().label("member")).label("members"),
				ch('}'), group.basics.optWS(),
				SET(actions.createAnnotationDeclaration(VALUE("modifiers"), VALUE("name"), VALUES("members/member"))));
	}
	
	Rule annotationElementDeclaration() {
		return firstOf(
				annotationMethodDeclaration(),
				fieldDeclaration(),
				classOrInterfaceDeclaration(),
				enumDeclaration(),
				annotationDeclaration(),
				sequence(ch(';'), group.basics.optWS())
				);
	}
	
	Rule enumBody() {
		return sequence(
				optional(sequence(
						enumConstant().label("head"),
						zeroOrMore(sequence(
								ch(','), group.basics.optWS(),
								enumConstant()).label("tail")),
						optional(sequence(ch(','), group.basics.optWS())))).label("constants"),
				optional(sequence(
						ch(';'), group.basics.optWS(),
						typeBodyDeclarations())).label("typeBodyDeclarations"),
				SET(actions.createEnumFromContents(VALUE("constants/sequence/head"), VALUES("constants/sequence/zeroOrMore/tail"), VALUE("typeBodyDeclarations"))));
	}
	
	Rule enumConstant() {
		return sequence(
				zeroOrMore(annotation().label("annotation")).label("annotations"),
				group.basics.identifier().label("name"),
				optional(methodArguments()).label("arguments"),
				optional(typeBody()).label("body"),
				SET(actions.createEnumConstant(VALUES("annotations/annotation"), VALUE("name"), VALUE("arguments"), VALUE("body"))));
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
	
	public Rule annotationMethodDeclaration() {
		return sequence(
				methodDeclarationModifiers().label("modifiers"),
				group.types.type().label("resultType"),
				group.basics.identifier().label("methodName"),
				ch('('), group.basics.optWS(),
				ch(')'), group.basics.optWS(),
				optional(enforcedSequence(
						sequence(string("default"), group.basics.testLexBreak(), group.basics.optWS()),
						annotationElementValue())).label("defaultValue"),
				SET(actions.createAnnotationMethodDeclaration(VALUE("modifiers"), VALUE("resultType"), VALUE("methodName"), VALUE("defaultValue"))));
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
	
	public Rule staticInitializer() {
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
	
	public Rule typeDeclarationModifiers() {
		return sequence(
				zeroOrMore(anyModifier().label("modifier")),
				SET(actions.createModifiers(VALUES("zeroOrMore/modifier"))));
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
	
	public Rule packageDeclaration() {
		return enforcedSequence(
				sequence(
						zeroOrMore(annotation().label("annotation")).label("annotations"),
						string("package"), group.basics.testLexBreak(), group.basics.optWS()),
				group.basics.identifier().label("head"),
				zeroOrMore(sequence(
						ch('.'), group.basics.optWS(),
						group.basics.identifier()).label("tail")),
				ch(';'), group.basics.optWS(),
				SET(actions.createPackageDeclaration(VALUES("sequence/annotations/annotation"), VALUE("head"), VALUES("zeroOrMore/tail"))));
	}
	
	public Rule importDeclaration() {
		return enforcedSequence(
				sequence(string("import"), group.basics.testLexBreak(), group.basics.optWS()),
				optional(sequence(string("static"), group.basics.testLexBreak(), group.basics.optWS())).label("static"),
				group.basics.identifier().label("head"),
				zeroOrMore(sequence(
						ch('.'), group.basics.optWS(),
						group.basics.identifier()).label("tail")),
				optional(sequence(
						ch('.'), group.basics.optWS(),
						ch('*'), group.basics.optWS())).label("dotStar"),
				ch(';'), group.basics.optWS(),
				SET(actions.createImportDeclaration(TEXT("static"), VALUE("head"), VALUES("zeroOrMore/tail"), TEXT("dotStar"))));
	}
	
	public Rule compilationUnit() {
		return sequence(
				group.basics.optWS(),
				optional(packageDeclaration()).label("package"),
				zeroOrMore(importDeclaration().label("import")).label("imports"),
				zeroOrMore(anyTypeDeclaration().label("type")).label("types"),
				SET(actions.createCompilationUnit(VALUE("package"), VALUES("imports/import"), VALUES("types/type"))));
	}
}
