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

import lombok.NonNull;

/**
 * Implement this class and hand yourself to the {@link lombok.ast.Node#accept(AstVisitor)} method to get
 * a specific method called for each type of {@code Node}.
 * <p>
 * For each method, return {@code true} to indicate you've handled the type, and {@code false} to
 * indicate you haven't. The difference is: If you return {@code false} the children of the node
 * you didn't handle get passed to the implementation of this class instead.
 * <p>
 * If you return {@code false}, then after all children visit calls have been performed, the
 * {@link #endVisit(Node)} method is called to signal all children have been visited.
 * 
 * @see ForwardingAstVisitor
 */
public abstract class AstVisitor {
	//Basics
	public abstract boolean visitTypeReference(TypeReference node);
	public abstract boolean visitTypeReferencePart(TypeReferencePart node);
	public abstract boolean visitVariableReference(VariableReference node);
	public abstract boolean visitIdentifier(Identifier node);
	public abstract boolean visitIntegralLiteral(IntegralLiteral node);
	public abstract boolean visitFloatingPointLiteral(FloatingPointLiteral node);
	public abstract boolean visitBooleanLiteral(BooleanLiteral node);
	public abstract boolean visitCharLiteral(CharLiteral node);
	public abstract boolean visitStringLiteral(StringLiteral node);
	public abstract boolean visitNullLiteral(NullLiteral node);
	
	//Expressions
	public abstract boolean visitBinaryExpression(BinaryExpression node);
	public abstract boolean visitUnaryExpression(UnaryExpression node);
	public abstract boolean visitInlineIfExpression(InlineIfExpression node);
	public abstract boolean visitCast(Cast node);
	public abstract boolean visitInstanceOf(InstanceOf node);
	public abstract boolean visitConstructorInvocation(ConstructorInvocation node);
	public abstract boolean visitMethodInvocation(MethodInvocation node);
	public abstract boolean visitSelect(Select node);
	public abstract boolean visitArrayAccess(ArrayAccess node);
	public abstract boolean visitArrayCreation(ArrayCreation node);
	public abstract boolean visitArrayInitializer(ArrayInitializer node);
	public abstract boolean visitAnnotationValueArray(AnnotationValueArray node);
	public abstract boolean visitArrayDimension(ArrayDimension node);
	public abstract boolean visitClassLiteral(ClassLiteral node);
	public abstract boolean visitSuper(Super node);
	public abstract boolean visitThis(This node);
	
	//Statements
	public abstract boolean visitLabelledStatement(LabelledStatement node);
	public abstract boolean visitExpressionStatement(ExpressionStatement node);
	public abstract boolean visitIf(If node);
	public abstract boolean visitFor(For node);
	public abstract boolean visitForEach(ForEach node);
	public abstract boolean visitTry(Try node);
	public abstract boolean visitCatch(Catch node);
	public abstract boolean visitWhile(While node);
	public abstract boolean visitDoWhile(DoWhile node);
	public abstract boolean visitSynchronized(Synchronized node);
	public abstract boolean visitBlock(Block node);
	public abstract boolean visitAssert(Assert node);
	public abstract boolean visitEmptyStatement(EmptyStatement node);
	public abstract boolean visitSwitch(Switch node);
	public abstract boolean visitCase(Case node);
	public abstract boolean visitDefault(Default node);
	public abstract boolean visitBreak(Break node);
	public abstract boolean visitContinue(Continue node);
	public abstract boolean visitReturn(Return node);
	public abstract boolean visitThrow(Throw node);
	
	//Structural
	public abstract boolean visitVariableDeclaration(VariableDeclaration node);
	public abstract boolean visitVariableDefinition(VariableDefinition node);
	public abstract boolean visitVariableDefinitionEntry(VariableDefinitionEntry node);
	public abstract boolean visitTypeVariable(TypeVariable node);
	public abstract boolean visitKeywordModifier(KeywordModifier node);
	public abstract boolean visitModifiers(Modifiers node);
	public abstract boolean visitAnnotation(Annotation node);
	public abstract boolean visitAnnotationElement(AnnotationElement node);
	public abstract boolean visitNormalTypeBody(NormalTypeBody node);
	public abstract boolean visitEnumTypeBody(EnumTypeBody enumTypeBody);
	public abstract boolean visitEmptyDeclaration(EmptyDeclaration node);
	
	//Class Bodies
	public abstract boolean visitMethodDeclaration(MethodDeclaration node);
	public abstract boolean visitConstructorDeclaration(ConstructorDeclaration node);
	public abstract boolean visitSuperConstructorInvocation(SuperConstructorInvocation node);
	public abstract boolean visitAlternateConstructorInvocation(AlternateConstructorInvocation node);
	public abstract boolean visitInstanceInitializer(InstanceInitializer node);
	public abstract boolean visitStaticInitializer(StaticInitializer node);
	public abstract boolean visitClassDeclaration(ClassDeclaration node);
	public abstract boolean visitInterfaceDeclaration(InterfaceDeclaration node);
	public abstract boolean visitEnumDeclaration(EnumDeclaration node);
	public abstract boolean visitEnumConstant(EnumConstant node);
	public abstract boolean visitAnnotationDeclaration(AnnotationDeclaration node);
	public abstract boolean visitAnnotationMethodDeclaration(AnnotationMethodDeclaration node);
	public abstract boolean visitCompilationUnit(CompilationUnit node);
	public abstract boolean visitPackageDeclaration(PackageDeclaration node);
	public abstract boolean visitImportDeclaration(ImportDeclaration node);
	
	//Various
	public abstract boolean visitParseArtefact(Node node);
	public abstract boolean visitComment(Comment node);
	
	/**
	 * If a <em>visitX<em> method return {@code false}, then first all children are visited, and then this {@code endVisit} method is called.
	 * <p>
	 * NB: If {@code true} is returned from a <em>visitX</em> method, no {@code endVisit} call is made for that {@code Node}.
	 */
	public abstract void endVisit(Node node);
	
	// Post traversal visit methods
	
	//Basics
	public abstract void afterVisitTypeReference(@NonNull TypeReference node);
	public abstract void afterVisitTypeReferencePart(@NonNull TypeReferencePart node);
	public abstract void afterVisitVariableReference(@NonNull VariableReference node);
	public abstract void afterVisitIdentifier(@NonNull Identifier node);
	public abstract void afterVisitIntegralLiteral(@NonNull IntegralLiteral node);
	public abstract void afterVisitFloatingPointLiteral(@NonNull FloatingPointLiteral node);
	public abstract void afterVisitBooleanLiteral(@NonNull BooleanLiteral node);
	public abstract void afterVisitCharLiteral(@NonNull CharLiteral node);
	public abstract void afterVisitStringLiteral(@NonNull StringLiteral node);
	public abstract void afterVisitNullLiteral(@NonNull NullLiteral node);
	
	//Expressions
	public abstract void afterVisitBinaryExpression(@NonNull BinaryExpression node);
	public abstract void afterVisitUnaryExpression(@NonNull UnaryExpression node);
	public abstract void afterVisitInlineIfExpression(@NonNull InlineIfExpression node);
	public abstract void afterVisitCast(@NonNull Cast node);
	public abstract void afterVisitInstanceOf(@NonNull InstanceOf node);
	public abstract void afterVisitConstructorInvocation(@NonNull ConstructorInvocation node);
	public abstract void afterVisitMethodInvocation(@NonNull MethodInvocation node);
	public abstract void afterVisitSelect(@NonNull Select node);
	public abstract void afterVisitArrayAccess(@NonNull ArrayAccess node);
	public abstract void afterVisitArrayCreation(@NonNull ArrayCreation node);
	public abstract void afterVisitArrayInitializer(@NonNull ArrayInitializer node);
	public abstract void afterVisitAnnotationValueArray(@NonNull AnnotationValueArray node);
	public abstract void afterVisitArrayDimension(@NonNull ArrayDimension node);
	public abstract void afterVisitClassLiteral(@NonNull ClassLiteral node);
	public abstract void afterVisitSuper(@NonNull Super node);
	public abstract void afterVisitThis(@NonNull This node);
	
	//Statements
	public abstract void afterVisitLabelledStatement(@NonNull LabelledStatement node);
	public abstract void afterVisitExpressionStatement(@NonNull ExpressionStatement node);
	public abstract void afterVisitIf(@NonNull If node);
	public abstract void afterVisitFor(@NonNull For node);
	public abstract void afterVisitForEach(@NonNull ForEach node);
	public abstract void afterVisitTry(@NonNull Try node);
	public abstract void afterVisitCatch(@NonNull Catch node);
	public abstract void afterVisitWhile(@NonNull While node);
	public abstract void afterVisitDoWhile(@NonNull DoWhile node);
	public abstract void afterVisitSynchronized(@NonNull Synchronized node);
	public abstract void afterVisitBlock(@NonNull Block node);
	public abstract void afterVisitAssert(@NonNull Assert node);
	public abstract void afterVisitEmptyStatement(@NonNull EmptyStatement node);
	public abstract void afterVisitSwitch(@NonNull Switch node);
	public abstract void afterVisitCase(@NonNull Case node);
	public abstract void afterVisitDefault(@NonNull Default node);
	public abstract void afterVisitBreak(@NonNull Break node);
	public abstract void afterVisitContinue(@NonNull Continue node);
	public abstract void afterVisitReturn(@NonNull Return node);
	public abstract void afterVisitThrow(@NonNull Throw node);
	
	//Structural
	public abstract void afterVisitVariableDeclaration(@NonNull VariableDeclaration node);
	public abstract void afterVisitVariableDefinition(@NonNull VariableDefinition node);
	public abstract void afterVisitVariableDefinitionEntry(@NonNull VariableDefinitionEntry node);
	public abstract void afterVisitTypeVariable(@NonNull TypeVariable node);
	public abstract void afterVisitKeywordModifier(@NonNull KeywordModifier node);
	public abstract void afterVisitModifiers(@NonNull Modifiers node);
	public abstract void afterVisitAnnotation(@NonNull Annotation node);
	public abstract void afterVisitAnnotationElement(@NonNull AnnotationElement node);
	public abstract void afterVisitNormalTypeBody(@NonNull NormalTypeBody node);
	public abstract void afterVisitEnumTypeBody(@NonNull EnumTypeBody enumTypeBody);
	public abstract void afterVisitEmptyDeclaration(@NonNull EmptyDeclaration node);
	
	//Class Bodies
	public abstract void afterVisitMethodDeclaration(@NonNull MethodDeclaration node);
	public abstract void afterVisitConstructorDeclaration(@NonNull ConstructorDeclaration node);
	public abstract void afterVisitSuperConstructorInvocation(@NonNull SuperConstructorInvocation node);
	public abstract void afterVisitAlternateConstructorInvocation(@NonNull AlternateConstructorInvocation node);
	public abstract void afterVisitInstanceInitializer(@NonNull InstanceInitializer node);
	public abstract void afterVisitStaticInitializer(@NonNull StaticInitializer node);
	public abstract void afterVisitClassDeclaration(@NonNull ClassDeclaration node);
	public abstract void afterVisitInterfaceDeclaration(@NonNull InterfaceDeclaration node);
	public abstract void afterVisitEnumDeclaration(@NonNull EnumDeclaration node);
	public abstract void afterVisitEnumConstant(@NonNull EnumConstant node);
	public abstract void afterVisitAnnotationDeclaration(@NonNull AnnotationDeclaration node);
	public abstract void afterVisitAnnotationMethodDeclaration(@NonNull AnnotationMethodDeclaration node);
	public abstract void afterVisitCompilationUnit(@NonNull CompilationUnit node);
	public abstract void afterVisitPackageDeclaration(@NonNull PackageDeclaration node);
	public abstract void afterVisitImportDeclaration(@NonNull ImportDeclaration node);
	
	//Various
	public abstract void afterVisitParseArtefact(@NonNull Node node);
	public abstract void afterVisitComment(@NonNull Comment node);
}
