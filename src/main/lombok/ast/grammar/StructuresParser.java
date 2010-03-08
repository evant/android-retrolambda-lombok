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

import org.parboiled.BaseParser;
import org.parboiled.Rule;

public class StructuresParser extends BaseParser<Node> {
	final ParserGroup group;
	final StructuresActions actions;
	
	public StructuresParser(ParserGroup group) {
		this.actions = new StructuresActions(group.getSource());
		this.group = group;
	}
	
	public Rule typeBody() {
		return enforcedSequence(
				ch('{'), group.basics.optWS(),
				typeBodyDeclarations(), SET(),
				ch('}'), group.basics.optWS(),
				SET(actions.posify(VALUE())));
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
						emptyDeclaration()
						).label("member")).label("members"),
				SET(actions.createTypeBody(VALUES("members/member"))));
	}
	
	Rule emptyDeclaration() {
		return sequence(ch(';'), group.basics.optWS(), SET(actions.createEmptyDeclaration()));
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
				annotationDeclaration(),
				emptyDeclaration());
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
				sequence(string("implements"), group.basics.testLexBreak(), group.basics.optWS()),
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
				ch('{').label("typeOpen"), group.basics.optWS(),
				zeroOrMore(annotationElementDeclaration().label("member")).label("members"),
				ch('}').label("typeClose"), group.basics.optWS(),
				SET(actions.createAnnotationDeclaration(VALUE("modifiers"), VALUE("name"), VALUES("members/member"), NODE("typeOpen"), NODE("typeClose"))));
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
				ch('{'), group.basics.optWS(),
				optional(sequence(
						enumConstant().label("head"),
						zeroOrMore(sequence(
								ch(','), group.basics.optWS(),
								enumConstant()).label("tail")),
						optional(sequence(ch(','), group.basics.optWS())))).label("constants"),
				optional(sequence(
						ch(';'), group.basics.optWS(),
						typeBodyDeclarations())).label("typeBodyDeclarations"),
				ch('}'), group.basics.optWS(),
				SET(actions.createEnumBody(VALUE("constants/sequence/head"), VALUES("constants/sequence/zeroOrMore/tail"), VALUE("typeBodyDeclarations"))));
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
				methodParameters().label("params"),
				optional(enforcedSequence(
						sequence(string("throws"), group.basics.testLexBreak(), group.basics.optWS()),
						group.types.type().label("throwsHead"),
						zeroOrMore(sequence(ch(','), group.basics.optWS(), group.types.type()).label("throwsTail"))
						)).label("throwsClause"),
				firstOf(
						sequence(ch(';'), group.basics.optWS()),
						group.statements.blockStatement()).label("body"),
				SET(actions.createConstructorDeclaration(VALUE("modifiers"), VALUE("typeParameters"), VALUE("typeName"), VALUE("params"), 
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
				ch(';'), group.basics.optWS(),
				SET(actions.createAnnotationMethodDeclaration(VALUE("modifiers"), VALUE("resultType"), VALUE("methodName"), VALUE("defaultValue"))));
	}
	
	public Rule methodDeclaration() {
		return sequence(
				methodDeclarationModifiers().label("modifiers"),
				group.types.typeVariables().label("typeParameters"),
				group.types.type().label("resultType"),
				group.basics.identifier().label("methodName"),
				methodParameters().label("params"),
				zeroOrMore(enforcedSequence(ch('['), group.basics.optWS(), ch(']'), group.basics.optWS()).label("dim")).label("dims"),
				optional(enforcedSequence(
						sequence(string("throws"), group.basics.testLexBreak(), group.basics.optWS()),
						group.types.type().label("throwsHead"),
						zeroOrMore(sequence(ch(','), group.basics.optWS(), group.types.type()).label("throwsTail"))
						)).label("throwsClause"),
				firstOf(
						sequence(ch(';'), group.basics.optWS()),
						group.statements.blockStatement()).label("body"),
				SET(actions.createMethodDeclaration(VALUE("modifiers"), VALUE("typeParameters"), VALUE("resultType"), VALUE("methodName"), VALUE("params"), 
						TEXTS("dims/dim"), VALUE("throwsClause/enforcedSequence/throwsHead"), VALUES("throwsClause/enforcedSequence/zeroOrMore/throwsTail"),
						VALUE("body"))));
	}
	
	Rule methodParameters() {
		return sequence(
				ch('('), group.basics.optWS(),
				optional(sequence(
						methodParameter().label("head"),
						zeroOrMore(sequence(
								ch(','), group.basics.optWS(),
								methodParameter().label("tail"))))),
				ch(')'), group.basics.optWS(),
				SET(actions.createMethodParameters(VALUE("optional/sequence/head"), VALUES("optional/sequence/zeroOrMore/sequence/tail"))));
	}
	
	Rule methodParameter() {
		return sequence(
				variableDefinitionModifiers().label("modifiers"),
				group.types.type().label("type"),
				optional(sequence(string("..."), group.basics.optWS())).label("varargs"),
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
				variableDefinition(), SET(), SET(actions.posify(VALUE())),
				ch(';'), group.basics.optWS(),
				SET(actions.createFieldDeclaration(VALUE(), VALUE("modifiers"))));
	}
	
	/**
	 * Add your own modifiers!
	 */
	Rule variableDefinition() {
		return sequence(
				group.types.type().label("type"),
				variableDefinitionPart().label("head"),
				zeroOrMore(sequence(
						ch(','), group.basics.optWS(),
						variableDefinitionPart()).label("tail")),
				SET(actions.createVariableDefinition(VALUE("type"), VALUE("head"), VALUES("zeroOrMore/tail"))));
	}
	
	Rule variableDefinitionPartNoAssign() {
		return sequence(
				group.basics.identifier().label("varName"),
				zeroOrMore(enforcedSequence(ch('['), group.basics.optWS(), ch(']'), group.basics.optWS()).label("dim")).label("dims"),
				SET(actions.createVariableDefinitionPart(VALUE("varName"), TEXTS("dims/dim"), null)));
	}
	
	Rule variableDefinitionPart() {
		return sequence(
				group.basics.identifier().label("varName"),
				zeroOrMore(enforcedSequence(ch('['), group.basics.optWS(), ch(']'), group.basics.optWS()).label("dim")).label("dims"),
				optional(sequence(
						ch('='), group.basics.optWS(),
						firstOf(
								group.expressions.arrayInitializer(),
								group.expressions.anyExpression()))).label("initializer"),
				SET(actions.createVariableDefinitionPart(VALUE("varName"), TEXTS("dims/dim"), VALUE("initializer"))));
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
	
	Rule anyKeyword() {
		return firstOf("final", "strictfp", "abstract", "transient", "volatile",
				"public", "protected", "private", "synchronized", "static", "native");

	}
	
	public Rule keywordModifier() {
		return sequence(
				anyKeyword().label("keyword"),
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
	
	public Rule variableDefinitionModifiers() {
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
	
	public Rule compilationUnitEoi() {
		return enforcedSequence(compilationUnit(), eoi());
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
