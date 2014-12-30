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
package lombok.ast.ecj;

import org.eclipse.jdt.internal.compiler.ast.*;

public abstract class EcjTreeVisitor {
	public void visitEcjNode(ASTNode node) {
		if (node == null) return;
		Class<?> clazz = node.getClass();
		
		if (clazz == Wildcard.class) {
			visitWildcard((Wildcard) node);
			return;
		}
		if (clazz == WhileStatement.class) {
			visitWhileStatement((WhileStatement) node);
			return;
		}
		if (clazz == UnaryExpression.class) {
			visitUnaryExpression((UnaryExpression) node);
			return;
		}
		if (clazz == TypeParameter.class) {
			visitTypeParameter((TypeParameter) node);
			return;
		}
		if (clazz == TypeDeclaration.class) {
			visitTypeDeclaration((TypeDeclaration) node);
			return;
		}
		if (clazz == TryStatement.class) {
			visitTryStatement((TryStatement) node);
			return;
		}
		if (clazz == TrueLiteral.class) {
			visitTrueLiteral((TrueLiteral) node);
			return;
		}
		if (clazz == ThrowStatement.class) {
			visitThrowStatement((ThrowStatement) node);
			return;
		}
		if (clazz == ThisReference.class) {
			visitThisReference((ThisReference) node);
			return;
		}
		if (clazz == SynchronizedStatement.class) {
			visitSynchronizedStatement((SynchronizedStatement) node);
			return;
		}
		if (clazz == SwitchStatement.class) {
			visitSwitchStatement((SwitchStatement) node);
			return;
		}
		if (clazz == SuperReference.class) {
			visitSuperReference((SuperReference) node);
			return;
		}
		if (clazz == StringLiteral.class) {
			visitStringLiteral((StringLiteral) node);
			return;
		}
		if (clazz == SingleTypeReference.class) {
			visitSingleTypeReference((SingleTypeReference) node);
			return;
		}
		if (clazz == SingleNameReference.class) {
			visitSingleNameReference((SingleNameReference) node);
			return;
		}
		if (clazz == SingleMemberAnnotation.class) {
			visitSingleMemberAnnotation((SingleMemberAnnotation) node);
			return;
		}
		if (clazz == ReturnStatement.class) {
			visitReturnStatement((ReturnStatement) node);
			return;
		}
		if (clazz == QualifiedTypeReference.class) {
			visitQualifiedTypeReference((QualifiedTypeReference) node);
			return;
		}
		if (clazz == QualifiedThisReference.class) {
			visitQualifiedThisReference((QualifiedThisReference) node);
			return;
		}
		if (clazz == QualifiedSuperReference.class) {
			visitQualifiedSuperReference((QualifiedSuperReference) node);
			return;
		}
		if (clazz == QualifiedNameReference.class) {
			visitQualifiedNameReference((QualifiedNameReference) node);
			return;
		}
		if (clazz == QualifiedAllocationExpression.class) {
			visitQualifiedAllocationExpression((QualifiedAllocationExpression) node);
			return;
		}
		if (clazz == PrefixExpression.class) {
			visitPrefixExpression((PrefixExpression) node);
			return;
		}
		if (clazz == PostfixExpression.class) {
			visitPostfixExpression((PostfixExpression) node);
			return;
		}
		if (clazz == ParameterizedSingleTypeReference.class) {
			visitParameterizedSingleTypeReference((ParameterizedSingleTypeReference) node);
			return;
		}
		if (clazz == ParameterizedQualifiedTypeReference.class) {
			visitParameterizedQualifiedTypeReference((ParameterizedQualifiedTypeReference) node);
			return;
		}
		if (clazz == OR_OR_Expression.class) {
			visitOR_OR_Expression((OR_OR_Expression) node);
			return;
		}
		if (clazz == NullLiteral.class) {
			visitNullLiteral((NullLiteral) node);
			return;
		}
		if (clazz == NormalAnnotation.class) {
			visitNormalAnnotation((NormalAnnotation) node);
			return;
		}
		if (clazz == StringLiteralConcatenation.class) {
			visitStringLiteralConcatenation((StringLiteralConcatenation) node);
			return;
		}
		if (clazz == MethodDeclaration.class) {
			visitMethodDeclaration((MethodDeclaration) node);
			return;
		}
		if (clazz == MessageSend.class) {
			visitMessageSend((MessageSend) node);
			return;
		}
		if (clazz == MemberValuePair.class) {
			visitMemberValuePair((MemberValuePair) node);
			return;
		}
		if (clazz == MarkerAnnotation.class) {
			visitMarkerAnnotation((MarkerAnnotation) node);
			return;
		}
		if (clazz == LongLiteral.class) {
			visitLongLiteral((LongLiteral) node);
			return;
		}
		if (clazz == LocalDeclaration.class) {
			visitLocalDeclaration((LocalDeclaration) node);
			return;
		}
		if (clazz == LabeledStatement.class) {
			visitLabeledStatement((LabeledStatement) node);
			return;
		}
		if (clazz == IntLiteral.class) {
			visitIntLiteral((IntLiteral) node);
			return;
		}
		if (clazz == InstanceOfExpression.class) {
			visitInstanceOfExpression((InstanceOfExpression) node);
			return;
		}
		if (clazz == Initializer.class) {
			visitInitializer((Initializer) node);
			return;
		}
		if (clazz == ImportReference.class) {
			visitImportReference((ImportReference) node);
			return;
		}
		if (clazz == IfStatement.class) {
			visitIfStatement((IfStatement) node);
			return;
		}
		if (clazz == ForStatement.class) {
			visitForStatement((ForStatement) node);
			return;
		}
		if (clazz == ForeachStatement.class) {
			visitForeachStatement((ForeachStatement) node);
			return;
		}
		if (clazz == FloatLiteral.class) {
			visitFloatLiteral((FloatLiteral) node);
			return;
		}
		if (clazz == FieldReference.class) {
			visitFieldReference((FieldReference) node);
			return;
		}
		if (clazz == FieldDeclaration.class) {
			visitFieldDeclaration((FieldDeclaration) node);
			return;
		}
		if (clazz == FalseLiteral.class) {
			visitFalseLiteral((FalseLiteral) node);
			return;
		}
		if (clazz == ExtendedStringLiteral.class) {
			visitExtendedStringLiteral((ExtendedStringLiteral) node);
			return;
		}
		if (clazz == ExplicitConstructorCall.class) {
			visitExplicitConstructorCall((ExplicitConstructorCall) node);
			return;
		}
		if (clazz == EqualExpression.class) {
			visitEqualExpression((EqualExpression) node);
			return;
		}
		if (clazz == EmptyStatement.class) {
			visitEmptyStatement((EmptyStatement) node);
			return;
		}
		if (clazz == DoubleLiteral.class) {
			visitDoubleLiteral((DoubleLiteral) node);
			return;
		}
		if (clazz == DoStatement.class) {
			visitDoStatement((DoStatement) node);
			return;
		}
		if (clazz == ContinueStatement.class) {
			visitContinueStatement((ContinueStatement) node);
			return;
		}
		if (clazz == ConstructorDeclaration.class) {
			visitConstructorDeclaration((ConstructorDeclaration) node);
			return;
		}
		if (clazz == ConditionalExpression.class) {
			visitConditionalExpression((ConditionalExpression) node);
			return;
		}
		if (clazz == CompoundAssignment.class) {
			visitCompoundAssignment((CompoundAssignment) node);
			return;
		}
		if (clazz == CompilationUnitDeclaration.class) {
			visitCompilationUnitDeclaration((CompilationUnitDeclaration) node);
			return;
		}
		if (clazz == Clinit.class) {
			visitClinit((Clinit) node);
			return;
		}
		if (clazz == ClassLiteralAccess.class) {
			visitClassLiteralAccess((ClassLiteralAccess) node);
			return;
		}
		if (clazz == CharLiteral.class) {
			visitCharLiteral((CharLiteral) node);
			return;
		}
		if (clazz == CastExpression.class) {
			visitCastExpression((CastExpression) node);
			return;
		}
		if (clazz == CaseStatement.class) {
			visitCaseStatement((CaseStatement) node);
			return;
		}
		if (clazz == BreakStatement.class) {
			visitBreakStatement((BreakStatement) node);
			return;
		}
		if (clazz == Block.class) {
			visitBlock((Block) node);
			return;
		}
		if (clazz == BinaryExpression.class) {
			visitBinaryExpression((BinaryExpression) node);
			return;
		}
		if (clazz == Assignment.class) {
			visitAssignment((Assignment) node);
			return;
		}
		if (clazz == AssertStatement.class) {
			visitAssertStatement((AssertStatement) node);
			return;
		}
		if (clazz == ArrayTypeReference.class) {
			visitArrayTypeReference((ArrayTypeReference) node);
			return;
		}
		if (clazz == ArrayReference.class) {
			visitArrayReference((ArrayReference) node);
			return;
		}
		if (clazz == ArrayQualifiedTypeReference.class) {
			visitArrayQualifiedTypeReference((ArrayQualifiedTypeReference) node);
			return;
		}
		if (clazz == ArrayInitializer.class) {
			visitArrayInitializer((ArrayInitializer) node);
			return;
		}
		if (clazz == ArrayAllocationExpression.class) {
			visitArrayAllocationExpression((ArrayAllocationExpression) node);
			return;
		}
		if (clazz == Argument.class) {
			visitArgument((Argument) node);
			return;
		}
		if (clazz == AnnotationMethodDeclaration.class) {
			visitAnnotationMethodDeclaration((AnnotationMethodDeclaration) node);
			return;
		}
		if (clazz == AND_AND_Expression.class) {
			visitAND_AND_Expression((AND_AND_Expression) node);
			return;
		}
		if (clazz == AllocationExpression.class) {
			visitAllocationExpression((AllocationExpression) node);
			return;
		}
		if (clazz == CombinedBinaryExpression.class) {
			visitCombinedBinaryExpression((CombinedBinaryExpression) node);
			return;
		}
		if (clazz == IntLiteralMinValue.class) {
			visitIntLiteralMinValue((IntLiteralMinValue) node);
			return;
		}
		if (clazz == LongLiteralMinValue.class) {
			visitLongLiteralMinValue((LongLiteralMinValue) node);
			return;
		}
		if (clazz == Javadoc.class) {
			visitJavadoc((Javadoc) node);
			return;
		}
		if (clazz == UnionTypeReference.class) {
			visitUnionTypeReference((UnionTypeReference) node);
			return;
		}
		
		visitOther(node);
	}
	
	public void visitOther(ASTNode node) {
		throw new UnsupportedOperationException("Unknown ASTNode child: " + node.getClass().getSimpleName());
	}
	
	public void visitAny(ASTNode node) {
		throw new UnsupportedOperationException("visit" + node.getClass().getSimpleName() + " not implemented");
	}
	
	public void visitWildcard(Wildcard node) {
		visitAny(node);
	}
	
	public void visitWhileStatement(WhileStatement node) {
		visitAny(node);
	}
	
	public void visitUnaryExpression(UnaryExpression node) {
		visitAny(node);
	}
	
	public void visitTypeParameter(TypeParameter node) {
		visitAny(node);
	}
	
	public void visitTypeDeclaration(TypeDeclaration node) {
		visitAny(node);
	}
	
	public void visitTryStatement(TryStatement node) {
		visitAny(node);
	}
	
	public void visitTrueLiteral(TrueLiteral node) {
		visitAny(node);
	}
	
	public void visitThrowStatement(ThrowStatement node) {
		visitAny(node);
	}
	
	public void visitThisReference(ThisReference node) {
		visitAny(node);
	}
	
	public void visitSynchronizedStatement(SynchronizedStatement node) {
		visitAny(node);
	}
	
	public void visitSwitchStatement(SwitchStatement node) {
		visitAny(node);
	}
	
	public void visitSuperReference(SuperReference node) {
		visitAny(node);
	}
	
	public void visitStringLiteral(StringLiteral node) {
		visitAny(node);
	}
	
	public void visitSingleTypeReference(SingleTypeReference node) {
		visitAny(node);
	}
	
	public void visitSingleNameReference(SingleNameReference node) {
		visitAny(node);
	}
	
	public void visitSingleMemberAnnotation(SingleMemberAnnotation node) {
		visitAny(node);
	}
	
	public void visitReturnStatement(ReturnStatement node) {
		visitAny(node);
	}
	
	public void visitQualifiedTypeReference(QualifiedTypeReference node) {
		visitAny(node);
	}
	
	public void visitQualifiedThisReference(QualifiedThisReference node) {
		visitAny(node);
	}
	
	public void visitQualifiedSuperReference(QualifiedSuperReference node) {
		visitAny(node);
	}
	
	public void visitQualifiedNameReference(QualifiedNameReference node) {
		visitAny(node);
	}
	
	public void visitQualifiedAllocationExpression(QualifiedAllocationExpression node) {
		visitAny(node);
	}
	
	public void visitPrefixExpression(PrefixExpression node) {
		visitAny(node);
	}
	
	public void visitPostfixExpression(PostfixExpression node) {
		visitAny(node);
	}
	
	public void visitParameterizedSingleTypeReference(ParameterizedSingleTypeReference node) {
		visitAny(node);
	}
	
	public void visitParameterizedQualifiedTypeReference(ParameterizedQualifiedTypeReference node) {
		visitAny(node);
	}
	
	public void visitOR_OR_Expression(OR_OR_Expression node) {
		visitAny(node);
	}
	
	public void visitNullLiteral(NullLiteral node) {
		visitAny(node);
	}
	
	public void visitNormalAnnotation(NormalAnnotation node) {
		visitAny(node);
	}
	
	public void visitStringLiteralConcatenation(StringLiteralConcatenation node) {
		visitAny(node);
	}
	
	public void visitMethodDeclaration(MethodDeclaration node) {
		visitAny(node);
	}
	
	public void visitMessageSend(MessageSend node) {
		visitAny(node);
	}
	
	public void visitMemberValuePair(MemberValuePair node) {
		visitAny(node);
	}
	
	public void visitMarkerAnnotation(MarkerAnnotation node) {
		visitAny(node);
	}
	
	public void visitLongLiteral(LongLiteral node) {
		visitAny(node);
	}
	
	public void visitLocalDeclaration(LocalDeclaration node) {
		visitAny(node);
	}
	
	public void visitLabeledStatement(LabeledStatement node) {
		visitAny(node);
	}
	
	public void visitIntLiteral(IntLiteral node) {
		visitAny(node);
	}
	
	public void visitInstanceOfExpression(InstanceOfExpression node) {
		visitAny(node);
	}
	
	public void visitInitializer(Initializer node) {
		visitAny(node);
	}
	
	public void visitImportReference(ImportReference node) {
		visitAny(node);
	}
	
	public void visitIfStatement(IfStatement node) {
		visitAny(node);
	}
	
	public void visitForStatement(ForStatement node) {
		visitAny(node);
	}
	
	public void visitForeachStatement(ForeachStatement node) {
		visitAny(node);
	}
	
	public void visitFloatLiteral(FloatLiteral node) {
		visitAny(node);
	}
	
	public void visitFieldReference(FieldReference node) {
		visitAny(node);
	}
	
	public void visitFieldDeclaration(FieldDeclaration node) {
		visitAny(node);
	}
	
	public void visitFalseLiteral(FalseLiteral node) {
		visitAny(node);
	}
	
	public void visitExtendedStringLiteral(ExtendedStringLiteral node) {
		visitAny(node);
	}
	
	public void visitExplicitConstructorCall(ExplicitConstructorCall node) {
		visitAny(node);
	}
	
	public void visitEqualExpression(EqualExpression node) {
		visitAny(node);
	}
	
	public void visitEmptyStatement(EmptyStatement node) {
		visitAny(node);
	}
	
	public void visitDoubleLiteral(DoubleLiteral node) {
		visitAny(node);
	}
	
	public void visitDoStatement(DoStatement node) {
		visitAny(node);
	}
	
	public void visitContinueStatement(ContinueStatement node) {
		visitAny(node);
	}
	
	public void visitConstructorDeclaration(ConstructorDeclaration node) {
		visitAny(node);
	}
	
	public void visitConditionalExpression(ConditionalExpression node) {
		visitAny(node);
	}
	
	public void visitCompoundAssignment(CompoundAssignment node) {
		visitAny(node);
	}
	
	public void visitCompilationUnitDeclaration(CompilationUnitDeclaration node) {
		visitAny(node);
	}
	
	public void visitClinit(Clinit node) {
		visitAny(node);
	}
	
	public void visitClassLiteralAccess(ClassLiteralAccess node) {
		visitAny(node);
	}
	
	public void visitCharLiteral(CharLiteral node) {
		visitAny(node);
	}
	
	public void visitCastExpression(CastExpression node) {
		visitAny(node);
	}
	
	public void visitCaseStatement(CaseStatement node) {
		visitAny(node);
	}
	
	public void visitBreakStatement(BreakStatement node) {
		visitAny(node);
	}
	
	public void visitBlock(Block node) {
		visitAny(node);
	}
	
	public void visitBinaryExpression(BinaryExpression node) {
		visitAny(node);
	}
	
	public void visitAssignment(Assignment node) {
		visitAny(node);
	}
	
	public void visitAssertStatement(AssertStatement node) {
		visitAny(node);
	}
	
	public void visitArrayTypeReference(ArrayTypeReference node) {
		visitAny(node);
	}
	
	public void visitArrayReference(ArrayReference node) {
		visitAny(node);
	}
	
	public void visitArrayQualifiedTypeReference(ArrayQualifiedTypeReference node) {
		visitAny(node);
	}
	
	public void visitArrayInitializer(ArrayInitializer node) {
		visitAny(node);
	}
	
	public void visitArrayAllocationExpression(ArrayAllocationExpression node) {
		visitAny(node);
	}
	
	public void visitArgument(Argument node) {
		visitAny(node);
	}
	
	public void visitAnnotationMethodDeclaration(AnnotationMethodDeclaration node) {
		visitAny(node);
	}
	
	public void visitAND_AND_Expression(AND_AND_Expression node) {
		visitAny(node);
	}
	
	public void visitAllocationExpression(AllocationExpression node) {
		visitAny(node);
	}
	
	public void visitCombinedBinaryExpression(CombinedBinaryExpression node) {
		visitAny(node);
	}
	
	public void visitIntLiteralMinValue(IntLiteralMinValue node) {
		visitAny(node);
	}
	
	public void visitLongLiteralMinValue(LongLiteralMinValue node) {
		visitAny(node);
	}
	
	public void visitJavadoc(Javadoc node) {
		visitAny(node);
	}
	
	public void visitUnionTypeReference(UnionTypeReference node) {
		visitAny(node);
	}
}
