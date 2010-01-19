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
package lombok.ast;

public interface ASTVisitor {
	public boolean visitIf(If node);
	public boolean visitFor(For node);
	public boolean visitForEach(ForEach node);
	public boolean visitTry(Try node);
	public boolean visitCatch(Catch node);
	public boolean visitWhile(While node);
	public boolean visitDoWhile(DoWhile node);
	public boolean visitSynchronized(Synchronized node);
	public boolean visitBlock(Block node);
	public boolean visitAssert(Assert node);
	public boolean visitVariableDeclaration(VariableDeclaration node);
	public boolean visitVariableDeclarationEntry(VariableDeclarationEntry node);
	public boolean visitTypeReference(TypeReference node);
	public boolean visitIdentifier(Identifier node);
	public boolean visitIntegralLiteral(IntegralLiteral node);
	public boolean visitFloatingPointLiteral(FloatingPointLiteral node);
	public boolean visitBooleanLiteral(BooleanLiteral node);
	public boolean visitCharLiteral(CharLiteral node);
	public boolean visitStringLiteral(StringLiteral node);
	public boolean visitNullLiteral(NullLiteral node);
	public boolean visitBinaryExpression(BinaryExpression node);
	public boolean visitUnaryExpression(UnaryExpression node);
	public boolean visitInlineIfExpression(InlineIfExpression node);
	public boolean visitIncrementExpression(IncrementExpression node);
	public boolean visitTypeReferencePart(TypeReferencePart node);
	public boolean visitCast(Cast node);
	public boolean visitIdentifierExpression(IdentifierExpression node);
	public boolean visitTypeVariable(TypeVariable node);
	public boolean visitParseArtefact(Node node);
	public boolean visitInstanceOf(InstanceOf node);
	public boolean visitTypeArguments(TypeArguments node);
	public boolean visitClassBody(ClassBody node);
	public boolean visitConstructorInvocation(ConstructorInvocation node);
	public boolean visitMethodInvocation(MethodInvocation node);
	public boolean visitSelect(Select node);
	public boolean visitArrayAccess(ArrayAccess node);
	public boolean visitArrayCreation(ArrayCreation node);
	public boolean visitArrayInitializer(ArrayInitializer node);
	public boolean visitArrayDimension(ArrayDimension node);
	public boolean visitSuper(Super node);
	public boolean visitClassLiteral(ClassLiteral node);
	public boolean visitThis(This node);
	public boolean visitKeywordModifier(KeywordModifier node);
	public boolean visitEmptyStatement(EmptyStatement node);
	public boolean visitLabelledStatement(LabelledStatement node);
	public boolean visitSwitch(Switch node);
	public boolean visitCase(Case node);
	public boolean visitDefault(Default node);
	public boolean visitModifiers(Modifiers node);
	public boolean visitAnnotation(Annotation node);
	public boolean visitBreak(Break node);
	public boolean visitContinue(Continue node);
	public boolean visitReturn(Return node);
	public boolean visitThrow(Throw node);
}
