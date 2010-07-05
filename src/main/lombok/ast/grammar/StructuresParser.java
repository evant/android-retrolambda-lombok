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
import org.parboiled.annotations.SuppressSubnodes;

public class StructuresParser extends BaseParser<Node> {
	final ParserGroup group;
	final StructuresActions actions;
	
	public StructuresParser(ParserGroup group) {
		this.actions = new StructuresActions(group.getSource());
		this.group = group;
	}
	
	public Rule typeBody() {
		return Sequence(
				Ch('{'), group.basics.optWS(),
				typeBodyDeclarations(), set(),
				Ch('}'), group.basics.optWS(),
				set(actions.posify(value())));
	}
	
	Rule typeBodyDeclarations() {
		return Sequence(
				ZeroOrMore(FirstOf(
						anyTypeDeclaration(),
						fieldDeclaration(),
						methodDeclaration(),
						constructorDeclaration(),
						staticInitializer(),
						instanceInitializer(),
						emptyDeclaration()
						).label("member")).label("members"),
				set(actions.createNormalTypeBody(values("members/member"))));
	}
	
	Rule emptyDeclaration() {
		return Sequence(Ch(';'), group.basics.optWS(), set(actions.createEmptyDeclaration()));
	}
	
	public Rule methodArguments() {
		return Sequence(
				Ch('('),
				group.basics.optWS(),
				Optional(Sequence(
						group.expressions.anyExpression(),
						set(),
						ZeroOrMore(Sequence(
								Ch(','),
								group.basics.optWS(),
								group.expressions.anyExpression(), set())))),
				Ch(')'),
				group.basics.optWS(),
				set(actions.createMethodArguments(value("Optional/Sequence"), values("Optional/Sequence/ZeroOrMore/Sequence"))));
	}
	
	public Rule anyTypeDeclaration() {
		return Sequence(
				TestNot(FirstOf(Eoi(), Ch('}'))),
				FirstOf(
						classOrInterfaceDeclaration(),
						enumDeclaration(),
						annotationDeclaration(),
						emptyDeclaration()));
	}
	
	public Rule classOrInterfaceDeclaration() {
		return Sequence(
				typeDeclarationModifiers().label("modifiers"),
				FirstOf(String("class"), String("interface")).label("kind"),
				group.basics.testLexBreak(), group.basics.optWS(),
				group.basics.identifier().label("typeName"),
				group.types.typeVariables().label("typeParameters"),
				ZeroOrMore(FirstOf(
						extendsClause(),
						implementsClause()).label("addon")).label("addons"),
				typeBody().label("body"),
				set(actions.createTypeDeclaration(text("kind"), value("modifiers"), value("typeName"), value("typeParameters"), value("body"), values("addons/addon"))));
	}
	
	Rule extendsClause() {
		return Sequence(
				Sequence(String("extends"), group.basics.testLexBreak(), group.basics.optWS()),
				group.types.type().label("head"),
				ZeroOrMore(Sequence(
						Ch(','), group.basics.optWS(),
						group.types.type()).label("tail")),
				set(actions.createExtendsClause(value("head"), values("ZeroOrMore/tail"))));
	}
	
	Rule implementsClause() {
		return Sequence(
				Sequence(String("implements"), group.basics.testLexBreak(), group.basics.optWS()),
				group.types.type().label("head"),
				ZeroOrMore(Sequence(
						Ch(','), group.basics.optWS(),
						group.types.type()).label("tail")),
				set(actions.createImplementsClause(value("head"), values("ZeroOrMore/tail"))));
	}
	
	public Rule enumDeclaration() {
		return Sequence(
				typeDeclarationModifiers().label("modifiers"),
				String("enum"), group.basics.testLexBreak(), group.basics.optWS(),
				group.basics.identifier().label("typeName"),
				ZeroOrMore(FirstOf(
						extendsClause(),
						implementsClause()).label("addon")).label("addons"),
				enumBody().label("body"),
				set(actions.createEnumDeclaration(value("modifiers"), value("typeName"), value("body"), values("addons/addon"))));
	}
	
	public Rule annotationDeclaration() {
		return Sequence(
				typeDeclarationModifiers().label("modifiers"),
				Ch('@'), group.basics.optWS(),
				String("interface"), group.basics.testLexBreak(), group.basics.optWS(),
				group.basics.identifier().label("name"),
				Ch('{').label("typeOpen"), group.basics.optWS(),
				ZeroOrMore(annotationElementDeclaration().label("member")).label("members"),
				Ch('}').label("typeClose"), group.basics.optWS(),
				set(actions.createAnnotationDeclaration(value("modifiers"), value("name"), values("members/member"), node("typeOpen"), node("typeClose"))));
	}
	
	Rule annotationElementDeclaration() {
		return FirstOf(
				annotationMethodDeclaration(),
				fieldDeclaration(),
				classOrInterfaceDeclaration(),
				enumDeclaration(),
				annotationDeclaration(),
				Sequence(Ch(';'), group.basics.optWS())
				);
	}
	
	Rule enumBody() {
		return Sequence(
				Ch('{'), group.basics.optWS(),
				Optional(Sequence(
						enumConstant().label("head"),
						ZeroOrMore(Sequence(
								Ch(','), group.basics.optWS(),
								enumConstant()).label("tail")),
						Optional(Sequence(Ch(','), group.basics.optWS())))).label("constants"),
				Optional(Sequence(
						Ch(';'), group.basics.optWS(),
						typeBodyDeclarations())).label("typeBodyDeclarations"),
				Ch('}'), group.basics.optWS(),
				set(actions.createEnumBody(value("constants/Sequence/head"), values("constants/Sequence/ZeroOrMore/tail"), value("typeBodyDeclarations"))));
	}
	
	Rule enumConstant() {
		return Sequence(
				ZeroOrMore(annotation().label("annotation")).label("annotations"),
				group.basics.identifier().label("name"),
				Optional(methodArguments()).label("arguments"),
				Optional(typeBody()).label("body"),
				set(actions.createEnumConstant(values("annotations/annotation"), value("name"), value("arguments"), value("body"))));
	}
	
	public Rule constructorDeclaration() {
		return Sequence(
				methodDeclarationModifiers().label("modifiers"),
				group.types.typeVariables().label("typeParameters"),
				group.basics.identifier().label("typeName"),
				methodParameters().label("params"),
				Optional(Sequence(
						Sequence(String("throws"), group.basics.testLexBreak(), group.basics.optWS()),
						group.types.type().label("throwsHead"),
						ZeroOrMore(Sequence(Ch(','), group.basics.optWS(), group.types.type()).label("throwsTail"))
						)).label("throwsClause"),
				FirstOf(
						Sequence(Ch(';'), group.basics.optWS()),
						group.statements.blockStatement()).label("body"),
				set(actions.createConstructorDeclaration(value("modifiers"), value("typeParameters"), value("typeName"), value("params"), 
						value("throwsClause/Sequence/throwsHead"), values("throwsClause/Sequence/ZeroOrMore/throwsTail"),
						value("body"))));
	}
	
	public Rule annotationMethodDeclaration() {
		return Sequence(
				methodDeclarationModifiers().label("modifiers"),
				group.types.type().label("resultType"),
				group.basics.identifier().label("methodName"),
				Ch('('), group.basics.optWS(),
				Ch(')'), group.basics.optWS(),
				ZeroOrMore(Sequence(Ch('['), group.basics.optWS(), Ch(']'), group.basics.optWS()).label("dim")).label("dims"),
				Optional(Sequence(
						Sequence(String("default"), group.basics.testLexBreak(), group.basics.optWS()),
						annotationElementValue())).label("defaultValue"),
				Ch(';'), group.basics.optWS(),
				set(actions.createAnnotationMethodDeclaration(value("modifiers"), value("resultType"), value("methodName"), nodes("dims/dim"), value("defaultValue"))));
	}
	
	public Rule methodDeclaration() {
		return Sequence(
				methodDeclarationModifiers().label("modifiers"),
				group.types.typeVariables().label("typeParameters"),
				group.types.type().label("resultType"),
				group.basics.identifier().label("methodName"),
				methodParameters().label("params"),
				ZeroOrMore(Sequence(Ch('['), group.basics.optWS(), Ch(']'), group.basics.optWS()).label("dim")).label("dims"),
				Optional(Sequence(
						Sequence(String("throws"), group.basics.testLexBreak(), group.basics.optWS()),
						group.types.type().label("throwsHead"),
						ZeroOrMore(Sequence(Ch(','), group.basics.optWS(), group.types.type()).label("throwsTail"))
						)).label("throwsClause"),
				FirstOf(
						Sequence(Ch(';'), group.basics.optWS()),
						group.statements.blockStatement()).label("body"),
				set(actions.createMethodDeclaration(value("modifiers"), value("typeParameters"), value("resultType"), value("methodName"), value("params"), 
						nodes("dims/dim"), value("throwsClause/Sequence/throwsHead"), values("throwsClause/Sequence/ZeroOrMore/throwsTail"),
						value("body"))));
	}
	
	Rule methodParameters() {
		return Sequence(
				Ch('('), group.basics.optWS(),
				Optional(Sequence(
						methodParameter().label("head"),
						ZeroOrMore(Sequence(
								Ch(','), group.basics.optWS(),
								methodParameter().label("tail"))))),
				Ch(')'), group.basics.optWS(),
				set(actions.createMethodParameters(value("Optional/Sequence/head"), values("Optional/Sequence/ZeroOrMore/Sequence/tail"))));
	}
	
	Rule methodParameter() {
		return Sequence(
				variableDefinitionModifiers().label("modifiers"),
				group.types.type().label("type"),
				Optional(Sequence(String("..."), group.basics.optWS())).label("varargs"),
				group.basics.identifier().label("name"),
				ZeroOrMore(Sequence(Ch('[').label("open"), group.basics.optWS(), Ch(']').label("closed"), group.basics.optWS()).label("dim")).label("dims"),
				set(actions.createMethodParameter(value("modifiers"), value("type"), text("varargs"), value("name"), nodes("dims/dim/open"), nodes("dims/dim/closed"))));
	}
	
	public Rule instanceInitializer() {
		return Sequence(
				group.statements.blockStatement().label("initializer"),
				set(actions.createInstanceInitializer(value("initializer"))));
	}
	
	public Rule staticInitializer() {
		return Sequence(
				String("static"), group.basics.testLexBreak(), group.basics.optWS(),
				group.statements.blockStatement().label("initializer"),
				set(actions.createStaticInitializer(value("initializer"))));
	}
	
	public Rule fieldDeclaration() {
		return Sequence(
				fieldDeclarationModifiers().label("modifiers"),
				variableDefinition(), set(), set(actions.posify(value())),
				Ch(';'), group.basics.optWS(),
				set(actions.createFieldDeclaration(value(), value("modifiers"))));
	}
	
	/**
	 * Add your own modifiers!
	 */
	Rule variableDefinition() {
		return Sequence(
				group.types.type().label("type"),
				variableDefinitionPart().label("head"),
				ZeroOrMore(Sequence(
						Ch(','), group.basics.optWS(),
						variableDefinitionPart()).label("tail")),
				set(actions.createVariableDefinition(value("type"), value("head"), values("ZeroOrMore/tail"))));
	}
	
	Rule variableDefinitionPartNoAssign() {
		return Sequence(
				group.basics.identifier().label("varName"),
				ZeroOrMore(Sequence(Ch('['), group.basics.optWS(), Ch(']'), group.basics.optWS()).label("dim")).label("dims"),
				set(actions.createVariableDefinitionPart(value("varName"), texts("dims/dim"), null)));
	}
	
	Rule variableDefinitionPart() {
		return Sequence(
				group.basics.identifier().label("varName"),
				ZeroOrMore(Sequence(Ch('['), group.basics.optWS(), Ch(']'), group.basics.optWS()).label("dim")).label("dims"),
				Optional(Sequence(
						Ch('='), group.basics.optWS(),
						FirstOf(
								group.expressions.arrayInitializer(),
								group.expressions.anyExpression()))).label("initializer"),
				set(actions.createVariableDefinitionPart(value("varName"), texts("dims/dim"), value("initializer"))));
	}
	
	public Rule annotation() {
		return Sequence(
				Ch('@'), group.basics.optWS(),
				group.types.plainReferenceType().label("annotationType"),
				Optional(Sequence(
						Ch('('), group.basics.optWS(),
						Optional(FirstOf(
								annotationElements(),
								Sequence(annotationElementValue(),
										set(actions.createAnnotationFromElement(lastValue()))))),
						Ch(')'), group.basics.optWS())).label("content"),
				set(actions.createAnnotation(value("annotationType"), value("content"))));
	}
	
	Rule annotationElements() {
		return Sequence(
				annotationElement().label("head"),
				ZeroOrMore(Sequence(
						Ch(','), group.basics.optWS(),
						annotationElement()).label("tail")),
				set(actions.createAnnotationFromElements(value("head"), values("ZeroOrMore/tail"))));
	}
	
	Rule annotationElement() {
		return Sequence(
				group.basics.identifier().label("name"),
				Ch('='), group.basics.optWS(),
				annotationElementValue().label("value"),
				set(actions.createAnnotationElement(value("name"), value("value"))));
	}
	
	Rule annotationElementValue() {
		return FirstOf(
				annotation(),
				Sequence(
						Ch('{'), group.basics.optWS(),
						Optional(Sequence(
								annotationElementValue().label("head"),
								ZeroOrMore(Sequence(
										Ch(','), group.basics.optWS(),
										annotationElementValue()).label("tail")),
								Optional(Sequence(Ch(','), group.basics.optWS())))),
						Ch('}'), group.basics.optWS(),
						set(actions.createAnnotationElementValueArrayInitializer(value("Optional/Sequence/head"), values("Optional/Sequence/ZeroOrMore/tail")))),
				group.expressions.inlineIfExpressionChaining());
	}
	
	@SuppressSubnodes
	Rule anyKeyword() {
		return FirstOf("final", "strictfp", "abstract", "transient", "volatile",
				"public", "protected", "private", "synchronized", "static", "native");
	}
	
	public Rule keywordModifier() {
		return Sequence(
				anyKeyword().label("keyword"),
				group.basics.testLexBreak(),
				set(actions.createKeywordModifier(text("keyword"))),
				group.basics.optWS());
	}
	
	public Rule typeDeclarationModifiers() {
		return Sequence(
				TestNot(Ch('}')),
				ZeroOrMore(anyModifier().label("modifier")),
				set(actions.createModifiers(values("ZeroOrMore/modifier"))));
	}
	
	public Rule methodDeclarationModifiers() {
		return Sequence(
				TestNot(Ch('}')),
				ZeroOrMore(anyModifier().label("modifier")),
				set(actions.createModifiers(values("ZeroOrMore/modifier"))));
	}
	
	public Rule fieldDeclarationModifiers() {
		return Sequence(
				TestNot(Ch('}')),
				ZeroOrMore(anyModifier().label("modifier")),
				set(actions.createModifiers(values("ZeroOrMore/modifier"))));
	}
	
	public Rule variableDefinitionModifiers() {
		return Sequence(
				TestNot(Ch('}')),
				ZeroOrMore(anyModifier().label("modifier")),
				set(actions.createModifiers(values("ZeroOrMore/modifier"))));
	}
	
	public Rule anyModifier() {
		return FirstOf(annotation(), keywordModifier());
	}
	
	public Rule packageDeclaration() {
		return Sequence(
				Sequence(
						ZeroOrMore(annotation().label("annotation")).label("annotations"),
						String("package"), group.basics.testLexBreak(), group.basics.optWS()),
				group.basics.identifier().label("head"),
				ZeroOrMore(group.basics.dotIdentifier().label("tail")),
				Ch(';'), group.basics.optWS(),
				set(actions.createPackageDeclaration(values("Sequence/annotations/annotation"), value("head"), values("ZeroOrMore/tail"))));
	}
	
	public Rule importDeclaration() {
		return Sequence(
				Sequence(String("import"), group.basics.testLexBreak(), group.basics.optWS()),
				Optional(Sequence(String("static"), group.basics.testLexBreak(), group.basics.optWS())).label("static"),
				group.basics.identifier().label("head"),
				ZeroOrMore(group.basics.dotIdentifier().label("tail")),
				Optional(Sequence(
						Ch('.'), group.basics.optWS(),
						Ch('*'), group.basics.optWS())).label("dotStar"),
				Ch(';'), group.basics.optWS(),
				set(actions.createImportDeclaration(text("static"), value("head"), values("ZeroOrMore/tail"), text("dotStar"))));
	}
	
	public Rule compilationUnitEoi() {
		return Sequence(compilationUnit(), Eoi());
	}
	
	public Rule compilationUnit() {
		return Sequence(
				group.basics.optWS(),
				Optional(packageDeclaration()).label("package"),
				ZeroOrMore(importDeclaration().label("import")).label("imports"),
				ZeroOrMore(anyTypeDeclaration().label("type")).label("types"),
				set(actions.createCompilationUnit(value("package"), values("imports/import"), values("types/type"))));
	}
}
