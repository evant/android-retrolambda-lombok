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
package lombok.ast;

/**
 * Like {@link AstVisitor} which this class extends; the difference is that in this class all methods are non-abstract and forward
 * themselves to the {@link #visitNode(lombok.ast.Node)} which by default returns {@code false}, causing automatic traversal.
 */
public abstract class ForwardingAstVisitor extends AstVisitor {
	public boolean visitNode(Node node) {
		return false;
	}
	
	public void endVisit(Node node) {
	}
	
	//Basics
	@Override public boolean visitTypeReference(TypeReference node) {return visitNode(node);}
	@Override public boolean visitTypeReferencePart(TypeReferencePart node) {return visitNode(node);}
	@Override public boolean visitVariableReference(VariableReference node) {return visitNode(node);}
	@Override public boolean visitIdentifier(Identifier node) {return visitNode(node);}
	@Override public boolean visitIntegralLiteral(IntegralLiteral node) {return visitNode(node);}
	@Override public boolean visitFloatingPointLiteral(FloatingPointLiteral node) {return visitNode(node);}
	@Override public boolean visitBooleanLiteral(BooleanLiteral node) {return visitNode(node);}
	@Override public boolean visitCharLiteral(CharLiteral node) {return visitNode(node);}
	@Override public boolean visitStringLiteral(StringLiteral node) {return visitNode(node);}
	@Override public boolean visitNullLiteral(NullLiteral node) {return visitNode(node);}
	
	//Expressions
	@Override public boolean visitBinaryExpression(BinaryExpression node) {return visitNode(node);}
	@Override public boolean visitUnaryExpression(UnaryExpression node) {return visitNode(node);}
	@Override public boolean visitInlineIfExpression(InlineIfExpression node) {return visitNode(node);}
	@Override public boolean visitCast(Cast node) {return visitNode(node);}
	@Override public boolean visitInstanceOf(InstanceOf node) {return visitNode(node);}
	@Override public boolean visitConstructorInvocation(ConstructorInvocation node) {return visitNode(node);}
	@Override public boolean visitMethodInvocation(MethodInvocation node) {return visitNode(node);}
	@Override public boolean visitSelect(Select node) {return visitNode(node);}
	@Override public boolean visitArrayAccess(ArrayAccess node) {return visitNode(node);}
	@Override public boolean visitArrayCreation(ArrayCreation node) {return visitNode(node);}
	@Override public boolean visitAnnotationValueArray(AnnotationValueArray node) {return visitNode(node);}
	@Override public boolean visitArrayInitializer(ArrayInitializer node) {return visitNode(node);}
	@Override public boolean visitArrayDimension(ArrayDimension node) {return visitNode(node);}
	@Override public boolean visitClassLiteral(ClassLiteral node) {return visitNode(node);}
	@Override public boolean visitSuper(Super node) {return visitNode(node);}
	@Override public boolean visitThis(This node) {return visitNode(node);}
	
	//Statements
	@Override public boolean visitLabelledStatement(LabelledStatement node) {return visitNode(node);}
	@Override public boolean visitExpressionStatement(ExpressionStatement node) {return visitNode(node);}
	@Override public boolean visitIf(If node) {return visitNode(node);}
	@Override public boolean visitFor(For node) {return visitNode(node);}
	@Override public boolean visitForEach(ForEach node) {return visitNode(node);}
	@Override public boolean visitTry(Try node) {return visitNode(node);}
	@Override public boolean visitCatch(Catch node) {return visitNode(node);}
	@Override public boolean visitWhile(While node) {return visitNode(node);}
	@Override public boolean visitDoWhile(DoWhile node) {return visitNode(node);}
	@Override public boolean visitSynchronized(Synchronized node) {return visitNode(node);}
	@Override public boolean visitBlock(Block node) {return visitNode(node);}
	@Override public boolean visitAssert(Assert node) {return visitNode(node);}
	@Override public boolean visitEmptyStatement(EmptyStatement node) {return visitNode(node);}
	@Override public boolean visitSwitch(Switch node) {return visitNode(node);}
	@Override public boolean visitCase(Case node) {return visitNode(node);}
	@Override public boolean visitDefault(Default node) {return visitNode(node);}
	@Override public boolean visitBreak(Break node) {return visitNode(node);}
	@Override public boolean visitContinue(Continue node) {return visitNode(node);}
	@Override public boolean visitReturn(Return node) {return visitNode(node);}
	@Override public boolean visitThrow(Throw node) {return visitNode(node);}
	
	//Structural
	@Override public boolean visitVariableDeclaration(VariableDeclaration node) {return visitNode(node);}
	@Override public boolean visitVariableDefinition(VariableDefinition node) {return visitNode(node);}
	@Override public boolean visitVariableDefinitionEntry(VariableDefinitionEntry node) {return visitNode(node);}
	@Override public boolean visitTypeVariable(TypeVariable node) {return visitNode(node);}
	@Override public boolean visitKeywordModifier(KeywordModifier node) {return visitNode(node);}
	@Override public boolean visitModifiers(Modifiers node) {return visitNode(node);}
	@Override public boolean visitAnnotation(Annotation node) {return visitNode(node);}
	@Override public boolean visitAnnotationElement(AnnotationElement node) {return visitNode(node);}
	@Override public boolean visitNormalTypeBody(NormalTypeBody node) {return visitNode(node);}
	@Override public boolean visitEnumTypeBody(EnumTypeBody node) {return visitNode(node);}
	@Override public boolean visitEmptyDeclaration(EmptyDeclaration node) {return visitNode(node);}
	
	//Class Bodies
	@Override public boolean visitMethodDeclaration(MethodDeclaration node) {return visitNode(node);}
	@Override public boolean visitConstructorDeclaration(ConstructorDeclaration node) {return visitNode(node);}
	@Override public boolean visitSuperConstructorInvocation(SuperConstructorInvocation node) {return visitNode(node);}
	@Override public boolean visitAlternateConstructorInvocation(AlternateConstructorInvocation node) {return visitNode(node);}
	@Override public boolean visitInstanceInitializer(InstanceInitializer node) {return visitNode(node);}
	@Override public boolean visitStaticInitializer(StaticInitializer node) {return visitNode(node);}
	@Override public boolean visitClassDeclaration(ClassDeclaration node) {return visitNode(node);}
	@Override public boolean visitInterfaceDeclaration(InterfaceDeclaration node) {return visitNode(node);}
	@Override public boolean visitEnumDeclaration(EnumDeclaration node) {return visitNode(node);}
	@Override public boolean visitEnumConstant(EnumConstant node) {return visitNode(node);}
	@Override public boolean visitAnnotationDeclaration(AnnotationDeclaration node) {return visitNode(node);}
	@Override public boolean visitAnnotationMethodDeclaration(AnnotationMethodDeclaration node) {return visitNode(node);}
	@Override public boolean visitCompilationUnit(CompilationUnit node) {return visitNode(node);}
	@Override public boolean visitPackageDeclaration(PackageDeclaration node) {return visitNode(node);}
	@Override public boolean visitImportDeclaration(ImportDeclaration node) {return visitNode(node);}
	
	//Various
	@Override public boolean visitParseArtefact(Node node) {return visitNode(node);}
	@Override public boolean visitComment(Comment node) {return visitNode(node);}
	
	// Post traversal visit methods
	
	//Basics
	@Override public void afterVisitTypeReference(TypeReference node) {}
	@Override public void afterVisitTypeReferencePart(TypeReferencePart node) {}
	@Override public void afterVisitVariableReference(VariableReference node) {}
	@Override public void afterVisitIdentifier(Identifier node) {}
	@Override public void afterVisitIntegralLiteral(IntegralLiteral node) {}
	@Override public void afterVisitFloatingPointLiteral(FloatingPointLiteral node) {}
	@Override public void afterVisitBooleanLiteral(BooleanLiteral node) {}
	@Override public void afterVisitCharLiteral(CharLiteral node) {}
	@Override public void afterVisitStringLiteral(StringLiteral node) {}
	@Override public void afterVisitNullLiteral(NullLiteral node) {}
	
	//Expressions
	@Override public void afterVisitBinaryExpression(BinaryExpression node) {}
	@Override public void afterVisitUnaryExpression(UnaryExpression node) {}
	@Override public void afterVisitInlineIfExpression(InlineIfExpression node) {}
	@Override public void afterVisitCast(Cast node) {}
	@Override public void afterVisitInstanceOf(InstanceOf node) {}
	@Override public void afterVisitConstructorInvocation(ConstructorInvocation node) {}
	@Override public void afterVisitMethodInvocation(MethodInvocation node) {}
	@Override public void afterVisitSelect(Select node) {}
	@Override public void afterVisitArrayAccess(ArrayAccess node) {}
	@Override public void afterVisitArrayCreation(ArrayCreation node) {}
	@Override public void afterVisitAnnotationValueArray(AnnotationValueArray node) {}
	@Override public void afterVisitArrayInitializer(ArrayInitializer node) {}
	@Override public void afterVisitArrayDimension(ArrayDimension node) {}
	@Override public void afterVisitClassLiteral(ClassLiteral node) {}
	@Override public void afterVisitSuper(Super node) {}
	@Override public void afterVisitThis(This node) {}
	
	//Statements
	@Override public void afterVisitLabelledStatement(LabelledStatement node) {}
	@Override public void afterVisitExpressionStatement(ExpressionStatement node) {}
	@Override public void afterVisitIf(If node) {}
	@Override public void afterVisitFor(For node) {}
	@Override public void afterVisitForEach(ForEach node) {}
	@Override public void afterVisitTry(Try node) {}
	@Override public void afterVisitCatch(Catch node) {}
	@Override public void afterVisitWhile(While node) {}
	@Override public void afterVisitDoWhile(DoWhile node) {}
	@Override public void afterVisitSynchronized(Synchronized node) {}
	@Override public void afterVisitBlock(Block node) {}
	@Override public void afterVisitAssert(Assert node) {}
	@Override public void afterVisitEmptyStatement(EmptyStatement node) {}
	@Override public void afterVisitSwitch(Switch node) {}
	@Override public void afterVisitCase(Case node) {}
	@Override public void afterVisitDefault(Default node) {}
	@Override public void afterVisitBreak(Break node) {}
	@Override public void afterVisitContinue(Continue node) {}
	@Override public void afterVisitReturn(Return node) {}
	@Override public void afterVisitThrow(Throw node) {}
	
	//Structural
	@Override public void afterVisitVariableDeclaration(VariableDeclaration node) {}
	@Override public void afterVisitVariableDefinition(VariableDefinition node) {}
	@Override public void afterVisitVariableDefinitionEntry(VariableDefinitionEntry node) {}
	@Override public void afterVisitTypeVariable(TypeVariable node) {}
	@Override public void afterVisitKeywordModifier(KeywordModifier node) {}
	@Override public void afterVisitModifiers(Modifiers node) {}
	@Override public void afterVisitAnnotation(Annotation node) {}
	@Override public void afterVisitAnnotationElement(AnnotationElement node) {}
	@Override public void afterVisitNormalTypeBody(NormalTypeBody node) {}
	@Override public void afterVisitEnumTypeBody(EnumTypeBody node) {}
	@Override public void afterVisitEmptyDeclaration(EmptyDeclaration node) {}
	
	//Class Bodies
	@Override public void afterVisitMethodDeclaration(MethodDeclaration node) {}
	@Override public void afterVisitConstructorDeclaration(ConstructorDeclaration node) {}
	@Override public void afterVisitSuperConstructorInvocation(SuperConstructorInvocation node) {}
	@Override public void afterVisitAlternateConstructorInvocation(AlternateConstructorInvocation node) {}
	@Override public void afterVisitInstanceInitializer(InstanceInitializer node) {}
	@Override public void afterVisitStaticInitializer(StaticInitializer node) {}
	@Override public void afterVisitClassDeclaration(ClassDeclaration node) {}
	@Override public void afterVisitInterfaceDeclaration(InterfaceDeclaration node) {}
	@Override public void afterVisitEnumDeclaration(EnumDeclaration node) {}
	@Override public void afterVisitEnumConstant(EnumConstant node) {}
	@Override public void afterVisitAnnotationDeclaration(AnnotationDeclaration node) {}
	@Override public void afterVisitAnnotationMethodDeclaration(AnnotationMethodDeclaration node) {}
	@Override public void afterVisitCompilationUnit(CompilationUnit node) {}
	@Override public void afterVisitPackageDeclaration(PackageDeclaration node) {}
	@Override public void afterVisitImportDeclaration(ImportDeclaration node) {}
	
	//Various
	@Override public void afterVisitParseArtefact(Node node) {}
	@Override public void afterVisitComment(Comment node) {}

}
