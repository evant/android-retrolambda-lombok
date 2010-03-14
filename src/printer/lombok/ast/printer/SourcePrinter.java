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
package lombok.ast.printer;

import static lombok.ast.printer.SourceFormatter.FAIL;
import lombok.ast.AlternateConstructorInvocation;
import lombok.ast.Annotation;
import lombok.ast.AnnotationDeclaration;
import lombok.ast.AnnotationElement;
import lombok.ast.AnnotationMethodDeclaration;
import lombok.ast.ArrayAccess;
import lombok.ast.ArrayCreation;
import lombok.ast.ArrayDimension;
import lombok.ast.ArrayInitializer;
import lombok.ast.Assert;
import lombok.ast.BinaryExpression;
import lombok.ast.Block;
import lombok.ast.BooleanLiteral;
import lombok.ast.Break;
import lombok.ast.Case;
import lombok.ast.Cast;
import lombok.ast.Catch;
import lombok.ast.CharLiteral;
import lombok.ast.ClassDeclaration;
import lombok.ast.ClassLiteral;
import lombok.ast.Comment;
import lombok.ast.CompilationUnit;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.ConstructorInvocation;
import lombok.ast.Continue;
import lombok.ast.Default;
import lombok.ast.DoWhile;
import lombok.ast.EmptyDeclaration;
import lombok.ast.EmptyStatement;
import lombok.ast.EnumConstant;
import lombok.ast.EnumDeclaration;
import lombok.ast.EnumTypeBody;
import lombok.ast.Expression;
import lombok.ast.ExpressionStatement;
import lombok.ast.FloatingPointLiteral;
import lombok.ast.For;
import lombok.ast.ForEach;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Identifier;
import lombok.ast.If;
import lombok.ast.ImportDeclaration;
import lombok.ast.InlineIfExpression;
import lombok.ast.InstanceInitializer;
import lombok.ast.InstanceOf;
import lombok.ast.IntegralLiteral;
import lombok.ast.InterfaceDeclaration;
import lombok.ast.KeywordModifier;
import lombok.ast.LabelledStatement;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.NullLiteral;
import lombok.ast.PackageDeclaration;
import lombok.ast.RawListAccessor;
import lombok.ast.Return;
import lombok.ast.Select;
import lombok.ast.StaticInitializer;
import lombok.ast.StringLiteral;
import lombok.ast.Super;
import lombok.ast.SuperConstructorInvocation;
import lombok.ast.Switch;
import lombok.ast.Synchronized;
import lombok.ast.This;
import lombok.ast.Throw;
import lombok.ast.Try;
import lombok.ast.TypeArguments;
import lombok.ast.TypeBody;
import lombok.ast.TypeReference;
import lombok.ast.TypeReferencePart;
import lombok.ast.TypeVariable;
import lombok.ast.UnaryExpression;
import lombok.ast.UnaryOperator;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.While;
import lombok.ast.WildcardKind;

public class SourcePrinter extends ForwardingAstVisitor {
	private final SourceFormatter formatter;
	
	public SourcePrinter(SourceFormatter formatter) {
		this.formatter = formatter;
	}
	
	//Private utility methods
	private void visit(Node node) {
		if (node != null) node.accept(this);
	}
	
	@Override public boolean visitNode(Node node) {
		formatter.buildBlock(node);
		formatter.fail("NOT_IMPLEMENTED: " + node.getClass().getSimpleName());
		formatter.closeBlock();
		return false;
	}
	
	private void append(String text) {
		StringBuilder sb = new StringBuilder();
		for (char c : text.toCharArray()) {
			if (c == '\n') {
				if (sb.length() > 0) formatter.append(sb.toString());
				sb.setLength(0);
				formatter.verticalSpace();
			} else if (c == ' ') {
				if (sb.length() > 0) formatter.append(sb.toString());
				sb.setLength(0);
				formatter.space();
			} else sb.append(c);
		}
		if (sb.length() > 0) formatter.append(sb.toString());
	}
	
	private void visitAll(String relation, RawListAccessor<?, ?> nodes, String separator, String prefix, String suffix) {
		if (nodes.isEmpty()) return;
		append(prefix);
		boolean first = true;
		for (Node n : nodes) {
			if (!first) {
				append(separator);
			}
			first = false;
			formatter.nameNextElement(relation);
			visit(n);
		}
		append(suffix);
	}
	
	private void visitAll(RawListAccessor<?, ?> nodes, String separator, String prefix, String suffix) {
		visitAll(null, nodes, separator, prefix, suffix);
	}
	
	private boolean isValidJavaIdentifier(String in) {
		if (in == null || in.length() == 0) return false;
		
		char c = in.charAt(0);
		if (!Character.isJavaIdentifierStart(c)) return false;
		char[] cs = in.toCharArray();
		for (int i = 1; i < cs.length; i++) {
			if (!Character.isJavaIdentifierPart(cs[i])) return false;
		}
		return true;
	}
	
	//Basics
	public boolean visitTypeReference(TypeReference node) {
		WildcardKind kind = node.getWildcard();
		formatter.buildInline(node);
		if (kind == WildcardKind.UNBOUND) {
			formatter.append("?");
			formatter.closeInline();
			return true;
		} else if (kind == WildcardKind.EXTENDS) {
			formatter.append("?");
			formatter.space();
			formatter.keyword("extends");
			formatter.space();
		} else if (kind == WildcardKind.SUPER) {
			formatter.append("?");
			formatter.space();
			formatter.keyword("super");
			formatter.space();
		}
		
		visitAll(node.rawParts(), ".", "", "");
		
		for (int i = 0 ; i < node.getArrayDimensions(); i++)
			formatter.append("[]");
		
		formatter.closeInline();
		return true;
	}
	
	public boolean visitTypeReferencePart(TypeReferencePart node) {
		formatter.buildInline(node);
		visit(node.getRawIdentifier());
		visit(node.getRawTypeArguments());
		formatter.closeInline();
		return true;
	}
	
	public boolean visitIdentifier(Identifier node) {
		parensOpen(node);
		String name = node.getName();
		if (name == null) name = FAIL + "NULL_IDENTIFIER" + FAIL;
		else if (name.isEmpty()) name = FAIL + "EMPTY_IDENTIFIER" + FAIL;
		else if (!isValidJavaIdentifier(name)) name = FAIL + "INVALID_IDENTIFIER: " + name + FAIL;
		
		formatter.buildInline(node);
		formatter.append(name);
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitIntegralLiteral(IntegralLiteral node) {
		parensOpen(node);
		String raw = node.getRawValue();
		
		formatter.buildInline(node);
		formatter.append(raw);
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitFloatingPointLiteral(FloatingPointLiteral node) {
		parensOpen(node);
		String raw = node.getRawValue();
		
		formatter.buildInline(node);
		formatter.append(raw);
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitBooleanLiteral(BooleanLiteral node) {
		parensOpen(node);
		String raw = node.getRawValue();
		
		formatter.buildInline(node);
		formatter.append(raw);
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitCharLiteral(CharLiteral node) {
		parensOpen(node);
		String raw = node.getRawValue();
		
		formatter.buildInline(node);
		formatter.append(raw);
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitStringLiteral(StringLiteral node) {
		parensOpen(node);
		String raw = node.getRawValue();
		
		formatter.buildInline(node);
		formatter.append(raw);
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitNullLiteral(NullLiteral node) {
		parensOpen(node);
		formatter.buildInline(node);
		formatter.keyword("null");
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	private void parensOpen(Expression node) {
		for (int i = 0; i < node.getIntendedParens(); i++) formatter.append("(");
	}
	
	private void parensClose(Expression node) {
		for (int i = 0; i < node.getIntendedParens(); i++) formatter.append(")");
	}
	
	//Expressions
	public boolean visitBinaryExpression(BinaryExpression node) {
		parensOpen(node);
		formatter.buildInline(node);
		formatter.nameNextElement("left");
		visit(node.getRawLeft());
		formatter.space();
		try {
			formatter.operator(node.getOperator().getSymbol());
		} catch (Exception e) {
			formatter.operator(node.getRawOperator());
		}
		formatter.space();
		formatter.nameNextElement("right");
		visit(node.getRawRight());
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitUnaryExpression(UnaryExpression node) {
		UnaryOperator op;
		parensOpen(node);
		
		try {
			op = node.getOperator();
			if (op == null) throw new Exception();
		} catch (Exception e) {
			formatter.buildInline(node);
			visit(node.getOperand());
			formatter.closeInline();
			parensClose(node);
			return true;
		}
		formatter.buildInline(node);
		if (!op.isPostfix()) formatter.operator(op.getSymbol());
		visit(node.getOperand());
		if (op.isPostfix()) formatter.operator(op.getSymbol());
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitCast(Cast node) {
		parensOpen(node);
		formatter.buildInline(node);
		formatter.append("(");
		visit(node.getRawTypeReference());
		formatter.append(")");
		formatter.space();
		visit(node.getRawOperand());
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitInlineIfExpression(InlineIfExpression node) {
		parensOpen(node);
		formatter.buildInline(node);
		formatter.nameNextElement("condition");
		visit(node.getRawCondition());
		formatter.space();
		formatter.operator("?");
		formatter.space();
		formatter.nameNextElement("ifTrue");
		visit(node.getRawIfTrue());
		formatter.space();
		formatter.operator(":");
		formatter.space();
		formatter.nameNextElement("ifFalse");
		visit(node.getRawIfFalse());
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitInstanceOf(InstanceOf node) {
		parensOpen(node);
		formatter.buildInline(node);
		formatter.nameNextElement("operand");
		visit(node.getRawObjectReference());
		formatter.space();
		formatter.keyword("instanceof");
		formatter.space();
		formatter.nameNextElement("type");
		visit(node.getRawTypeReference());
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitConstructorInvocation(ConstructorInvocation node) {
		parensOpen(node);
		formatter.buildInline(node);
		if (node.getRawQualifier() != null) {
			formatter.nameNextElement("qualifier");
			visit(node.getRawQualifier());
			formatter.append(".");
		}
		formatter.keyword("new");
		formatter.space();
		visit(node.getRawConstructorTypeArguments());
		formatter.nameNextElement("type");
		visit(node.getRawTypeReference());
		formatter.append("(");
		visitAll(node.rawArguments(), ", ", "", "");
		formatter.append(")");
		if (node.getRawAnonymousClassBody() != null) {
			formatter.space();
			formatter.startSuppressBlock();
			visit(node.getRawAnonymousClassBody());
			formatter.endSuppressBlock();
		}
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitMethodInvocation(MethodInvocation node) {
		parensOpen(node);
		formatter.buildInline(node);
		if (node.getRawOperand() != null) {
			formatter.nameNextElement("operand");
			visit(node.getRawOperand());
			formatter.append(".");
		}
		visit(node.getRawMethodTypeArguments());
		formatter.nameNextElement("methodName");
		visit(node.getRawName());
		formatter.append("(");
		visitAll(node.rawArguments(), ", ", "", "");
		formatter.append(")");
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitSelect(Select node) {
		parensOpen(node);
		formatter.buildInline(node);
		if (node.getRawOperand() != null) {
			formatter.nameNextElement("operand");
			visit(node.getRawOperand());
			formatter.append(".");
		}
		formatter.nameNextElement("selected");
		visit(node.getRawIdentifier());
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitArrayAccess(ArrayAccess node) {
		parensOpen(node);
		formatter.buildInline(node);
		visit(node.getRawOperand());
		formatter.append("[");
		visit(node.getRawIndexExpression());
		formatter.append("]");
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitArrayCreation(ArrayCreation node) {
		parensOpen(node);
		formatter.buildInline(node);
		formatter.keyword("new");
		formatter.space();
		visit(node.getRawComponentTypeReference());
		visitAll(node.rawDimensions(), "", "", "");
		if (node.getRawInitializer() != null) {
			formatter.space();
			visit(node.getRawInitializer());
		}
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitArrayInitializer(ArrayInitializer node) {
		parensOpen(node);
		formatter.buildInline(node);
		formatter.append("{");
		visitAll(node.rawExpressions(), ", ", "", "");
		formatter.append("}");
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitArrayDimension(ArrayDimension node) {
		formatter.buildInline(node);
		formatter.append("[");
		visit(node.getRawDimension());
		formatter.append("]");
		formatter.closeInline();
		return true;
	}
	
	public boolean visitClassLiteral(ClassLiteral node) {
		parensOpen(node);
		formatter.buildInline(node);
		visit(node.getRawTypeReference());
		formatter.append(".");
		formatter.keyword("class");
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitSuper(Super node) {
		parensOpen(node);
		formatter.buildInline(node);
		if (node.getRawQualifier() != null) {
			visit(node.getRawQualifier());
			formatter.append(".");
		}
		formatter.keyword("super");
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	public boolean visitThis(This node) {
		parensOpen(node);
		formatter.buildInline(node);
		if (node.getRawQualifier() != null) {
			visit(node.getRawQualifier());
			formatter.append(".");
		}
		formatter.keyword("this");
		formatter.closeInline();
		parensClose(node);
		return true;
	}
	
	//Statements
	public boolean visitExpressionStatement(ExpressionStatement node) {
		formatter.buildBlock(node);
		visit(node.getRawExpression());
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitLabelledStatement(LabelledStatement node) {
		formatter.buildBlock(node);
		if (node.getRawLabel() != null) {
			formatter.nameNextElement("label");
			visit(node.getRawLabel());
			formatter.append(":");
		}
		visit(node.getRawStatement());
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitIf(If node) {
		formatter.buildBlock(node);
		formatter.keyword("if");
		formatter.space();
		formatter.append("(");
		formatter.nameNextElement("condition");
		visit(node.getRawCondition());
		formatter.append(")");
		formatter.space();
		formatter.startSuppressBlock();
		formatter.nameNextElement("ifTrue");
		visit(node.getRawStatement());
		formatter.endSuppressBlock();
		if (node.getRawElseStatement() != null) {
			formatter.space();
			formatter.keyword("else");
			formatter.space();
			formatter.startSuppressBlock();
			formatter.nameNextElement("ifFalse");
			visit(node.getRawElseStatement());
			formatter.endSuppressBlock();
		}
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitFor(For node) {
		formatter.buildBlock(node);
		formatter.keyword("for");
		formatter.space();
		formatter.append("(");
		if (node.isVariableDeclarationBased()) {
			formatter.nameNextElement("init");
			visit(node.getRawVariableDeclaration());
		} else {
			visitAll("init", node.rawExpressionInits(), ", ", "", "");
		}
		formatter.append(";");
		if (node.getRawCondition() != null) {
			formatter.space();
			formatter.nameNextElement("condition");
			visit(node.getRawCondition());
		}
		formatter.append(";");
		if (!node.rawUpdates().isEmpty()) {
			formatter.space();
			visitAll(node.rawUpdates(), ", ", "", "");
		}
		formatter.append(")");
		formatter.space();
		formatter.startSuppressBlock();
		visit(node.getRawStatement());
		formatter.endSuppressBlock();
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitForEach(ForEach node) {
		formatter.buildBlock(node);
		formatter.keyword("for");
		formatter.space();
		formatter.append("(");
		formatter.nameNextElement("variable");
		visit(node.getRawVariable());
		formatter.space();
		formatter.append(":");
		formatter.space();
		formatter.nameNextElement("iterable");
		visit(node.getRawIterable());
		formatter.append(")");
		formatter.space();
		formatter.startSuppressBlock();
		visit(node.getRawStatement());
		formatter.endSuppressBlock();
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitTry(Try node) {
		formatter.buildBlock(node);
		formatter.keyword("try");
		formatter.space();
		formatter.startSuppressBlock();
		visit(node.getRawBody());
		formatter.endSuppressBlock();
		visitAll(node.rawCatches(), " ", " ", "");
		if (node.getRawFinally() != null) {
			formatter.space();
			formatter.keyword("finally");
			formatter.space();
			formatter.startSuppressBlock();
			visit(node.getRawFinally());
			formatter.endSuppressBlock();
		}
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitCatch(Catch node) {
		formatter.buildInline(node);
		formatter.keyword("catch");
		formatter.space();
		formatter.append("(");
		visit(node.getRawExceptionDeclaration());
		formatter.append(")");
		formatter.space();
		formatter.startSuppressBlock();
		visit(node.getRawBody());
		formatter.endSuppressBlock();
		formatter.closeInline();
		return true;
	}
	
	public boolean visitWhile(While node) {
		formatter.buildBlock(node);
		formatter.keyword("while");
		formatter.space();
		formatter.append("(");
		formatter.nameNextElement("condition");
		visit(node.getRawCondition());
		formatter.append(")");
		formatter.space();
		formatter.startSuppressBlock();
		visit(node.getRawStatement());
		formatter.endSuppressBlock();
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitDoWhile(DoWhile node) {
		formatter.buildBlock(node);
		formatter.keyword("do");
		formatter.space();
		formatter.startSuppressBlock();
		visit(node.getRawStatement());
		formatter.endSuppressBlock();
		formatter.space();
		formatter.keyword("while");
		formatter.space();
		formatter.append("(");
		formatter.nameNextElement("condition");
		visit(node.getRawCondition());
		formatter.append(")");
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitSynchronized(Synchronized node) {
		formatter.buildBlock(node);
		formatter.keyword("synchronized");
		formatter.space();
		formatter.append("(");
		formatter.nameNextElement("lock");
		visit(node.getRawLock());
		formatter.append(")");
		formatter.space();
		formatter.startSuppressBlock();
		visit(node.getBody());
		formatter.endSuppressBlock();
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitBlock(Block node) {
		formatter.buildBlock(node);
		formatter.append("{");
		formatter.buildBlock(null);
		visitAll(node.rawContents(), "", "", "");
		formatter.closeBlock();
		formatter.append("}");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitAssert(Assert node) {
		formatter.buildBlock(node);
		formatter.keyword("assert");
		formatter.space();
		formatter.nameNextElement("assertion");
		visit(node.getRawAssertion());
		if (node.getRawMessage() != null) {
			formatter.append(":");
			formatter.space();
			formatter.nameNextElement("message");
			visit(node.getRawMessage());
		}
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitEmptyStatement(EmptyStatement node) {
		formatter.buildBlock(node);
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitSwitch(Switch node) {
		formatter.buildBlock(node);
		formatter.keyword("switch");
		formatter.space();
		formatter.append("(");
		formatter.nameNextElement("operand");
		visit(node.getRawCondition());
		formatter.append(")");
		formatter.space();
		
		Node body = node.getRawBody();
		if (!(body instanceof Block)) {
			visit(body);
			formatter.closeBlock();
			return true;
		}
		
		formatter.append("{");
		formatter.buildBlock(null);
		
		for (Node child : ((Block)body).rawContents()) {
			if (child instanceof Case || child instanceof Default) {
				formatter.startSuppressIndent();
				visit(child);
				formatter.endSuppressIndent();
			} else {
				visit(child);
			}
		}
		
		formatter.closeBlock();
		formatter.append("}");
		
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitCase(Case node) {
		formatter.buildBlock(node);
		formatter.keyword("case");
		formatter.space();
		formatter.nameNextElement("condition");
		visit(node.getRawCondition());
		formatter.append(":");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitDefault(Default node) {
		formatter.buildBlock(node);
		formatter.keyword("default");
		formatter.append(":");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitBreak(Break node) {
		formatter.buildBlock(node);
		formatter.keyword("break");
		if (node.getRawLabel() != null) {
			formatter.space();
			visit(node.getRawLabel());
		}
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitContinue(Continue node) {
		formatter.buildBlock(node);
		formatter.keyword("continue");
		if (node.getRawLabel() != null) {
			formatter.space();
			visit(node.getRawLabel());
		}
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitReturn(Return node) {
		formatter.buildBlock(node);
		formatter.keyword("return");
		if (node.getRawValue() != null) {
			formatter.space();
			visit(node.getRawValue());
		}
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitThrow(Throw node) {
		formatter.buildBlock(node);
		formatter.keyword("throw");
		formatter.space();
		visit(node.getRawThrowable());
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	//Structural
	public boolean visitVariableDeclaration(VariableDeclaration node) {
		visit(node.getRawJavadoc());
		formatter.buildBlock(node);
		visit(node.getRawDefinition());
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	public boolean visitVariableDefinition(VariableDefinition node) {
		formatter.buildInline(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).rawKeywords().isEmpty()) {
				formatter.space();
			}
		}
		formatter.nameNextElement("type");
		visit(node.getRawTypeReference());
		if (node.isVarargs()) {
			formatter.append("...");
		}
		formatter.space();
		visitAll(node.rawVariables(), ", ", "", "");
		formatter.closeInline();
		
		return true;
	}
	
	public boolean visitVariableDefinitionEntry(VariableDefinitionEntry node) {
		formatter.buildInline(node);
		formatter.nameNextElement("varName");
		visit(node.getRawName());
		for (int i = 0; i < node.getArrayDimensions(); i++)
			formatter.append("[]");
		if (node.getRawInitializer() != null) {
			formatter.space();
			formatter.append("=");
			formatter.space();
			visit(node.getRawInitializer());
		}
		formatter.closeInline();
		
		return true;
	}
	
	public boolean visitTypeArguments(TypeArguments node) {
		formatter.buildInline(node);
		visitAll(node.rawGenerics(), ", ", "<", ">");
		formatter.closeInline();
		return true;
	}
	
	public boolean visitTypeVariable(TypeVariable node) {
		formatter.buildInline(node);
		visit(node.getRawName());
		if (!node.rawExtending().isEmpty()) {
			formatter.space();
			formatter.keyword("extends");
			visitAll(node.rawExtending(), " & ", " ", "");
		}
		formatter.closeInline();
		return true;
	}
	
	public boolean visitKeywordModifier(KeywordModifier node) {
		formatter.buildInline(node);
		if (node.getName() == null || node.getName().isEmpty()) formatter.fail("MISSING_MODIFIER");
		else
			formatter.keyword(node.getName());
		formatter.closeInline();
		return true;
	}
	
	public boolean visitModifiers(Modifiers node) {
		formatter.buildInline(node);
		visitAll(node.rawAnnotations(), "", "", "");
		visitAll(node.rawKeywords(), " ", "", "");
		formatter.closeInline();
		return true;
	}
	
	public boolean visitAnnotation(Annotation node) {
		formatter.buildBlock(node);
		formatter.append("@");
		visit(node.getRawAnnotationTypeReference());
		visitAll(node.rawElements(), ", ", "(", ")");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitAnnotationElement(AnnotationElement node) {
		formatter.buildInline(node);
		if (node.getRawName() != null) {
			formatter.nameNextElement("name");
			visit(node.getRawName());
			formatter.space();
			formatter.append("=");
			formatter.space();
		}
		visit(node.getValue());
		formatter.closeInline();
		return true;
	}
	
	public boolean visitEnumTypeBody(EnumTypeBody node) {
		formatter.buildBlock(node);
		formatter.append("{");
		formatter.buildBlock(null);
		visitAll("constant", node.rawConstants(), ",\n", "", "");
		if (!node.rawMembers().isEmpty()) {
			formatter.append(";");
			formatter.verticalSpace();
		}
		visitAll(node.rawMembers(), "\n", "", "");
		formatter.closeBlock();
		formatter.append("}");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitTypeBody(TypeBody node) {
		formatter.buildBlock(node);
		formatter.append("{");
		formatter.buildBlock(null);
		visitAll(node.rawMembers(), "\n", "", "");
		formatter.closeBlock();
		formatter.append("}");
		formatter.closeBlock();
		return true;
	}
	
	//Class Bodies
	public boolean visitMethodDeclaration(MethodDeclaration node) {
		visit(node.getRawJavadoc());
		formatter.buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).rawKeywords().isEmpty()) {
				formatter.space();
			}
		}
		visitAll(node.rawTypeVariables(), ", ", "<", ">");
		if (!node.rawTypeVariables().isEmpty()) formatter.space();
		formatter.nameNextElement("returnType");
		visit(node.getRawReturnTypeReference());
		formatter.space();
		formatter.nameNextElement("methodName");
		visit(node.getRawMethodName());
		formatter.append("(");
		visitAll("parameter", node.rawParameters(), ", ", "", "");
		formatter.append(")");
		if (!node.rawThrownTypeReferences().isEmpty()) {
			formatter.space();
			formatter.keyword("throws");
			visitAll("throws", node.rawThrownTypeReferences(), ", ", " ", "");
		}
		if (node.getRawBody() == null) {
			formatter.append(";");
		} else {
			formatter.space();
			formatter.startSuppressBlock();
			visit(node.getRawBody());
			formatter.endSuppressBlock();
		}
		formatter.closeBlock();
		
		return true;
	}
	
	public boolean visitConstructorDeclaration(ConstructorDeclaration node) {
		visit(node.getRawJavadoc());
		formatter.buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).rawKeywords().isEmpty()) {
				formatter.space();
			}
		}
		visitAll(node.rawTypeVariables(), ", ", "<", ">");
		if (!node.rawTypeVariables().isEmpty()) formatter.space();
		formatter.nameNextElement("typeName");
		visit(node.getRawTypeName());
		formatter.append("(");
		visitAll("parameter", node.rawParameters(), ", ", "", "");
		formatter.append(")");
		formatter.space();
		if (!node.rawThrownTypeReferences().isEmpty()) {
			formatter.keyword("throws");
			visitAll("throws", node.rawThrownTypeReferences(), ", ", " ", " ");
		}
		formatter.startSuppressBlock();
		visit(node.getRawBody());
		if (node.getRawBody() == null) {
			formatter.append(";");
		}
		formatter.endSuppressBlock();
		formatter.closeBlock();
		
		return true;
	}
	
	public boolean visitSuperConstructorInvocation(SuperConstructorInvocation node) {
		formatter.buildBlock(node);
		if (node.getRawQualifier() != null) {
			formatter.nameNextElement("qualifier");
			visit(node.getRawQualifier());
			formatter.append(".");
		}
		visit(node.getRawConstructorTypeArguments());
		formatter.keyword("super");
		formatter.append("(");
		visitAll(node.rawArguments(), ", ", "", "");
		formatter.append(")");
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitAlternateConstructorInvocation(AlternateConstructorInvocation node) {
		formatter.buildBlock(node);
		visit(node.getRawConstructorTypeArguments());
		formatter.keyword("this");
		formatter.append("(");
		visitAll(node.rawArguments(), ", ", "", "");
		formatter.append(")");
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitInstanceInitializer(InstanceInitializer node) {
		formatter.buildBlock(node);
		formatter.startSuppressBlock();
		visit(node.getRawBody());
		formatter.endSuppressBlock();
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitStaticInitializer(StaticInitializer node) {
		formatter.buildBlock(node);
		formatter.keyword("static");
		formatter.space();
		formatter.startSuppressBlock();
		visit(node.getRawBody());
		formatter.endSuppressBlock();
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitClassDeclaration(ClassDeclaration node) {
		visit(node.getRawJavadoc());
		formatter.buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).rawKeywords().isEmpty()) {
				formatter.space();
			}
		}
		formatter.keyword("class");
		formatter.space();
		formatter.nameNextElement("typeName");
		visit(node.getRawName());
		visitAll(node.rawTypeVariables(), ", ", "<", ">");
		formatter.space();
		if (node.getRawExtending() != null) {
			formatter.keyword("extends");
			formatter.space();
			formatter.nameNextElement("extends");
			visit(node.getRawExtending());
			formatter.space();
		}
		if (!node.rawImplementing().isEmpty()) {
			formatter.keyword("implements");
			visitAll("implements", node.rawImplementing(), ", ", " ", " ");
		}
		formatter.startSuppressBlock();
		visit(node.getRawBody());
		formatter.endSuppressBlock();
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitInterfaceDeclaration(InterfaceDeclaration node) {
		visit(node.getRawJavadoc());
		formatter.buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).rawKeywords().isEmpty()) {
				formatter.space();
			}
		}
		formatter.keyword("interface");
		formatter.space();
		formatter.nameNextElement("typeName");
		visit(node.getRawName());
		visitAll(node.rawTypeVariables(), ", ", "<", ">");
		formatter.space();
		if (!node.rawExtending().isEmpty()) {
			formatter.keyword("extends");
			visitAll("extends", node.rawExtending(), ", ", " ", " ");
		}
		formatter.startSuppressBlock();
		visit(node.getRawBody());
		formatter.endSuppressBlock();
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitEnumDeclaration(EnumDeclaration node) {
		visit(node.getRawJavadoc());
		formatter.buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).rawKeywords().isEmpty()) {
				formatter.space();
			}
		}
		formatter.keyword("enum");
		formatter.space();
		formatter.nameNextElement("typeName");
		visit(node.getRawName());
		formatter.space();
		if (!node.rawImplementing().isEmpty()) {
			formatter.keyword("implements");
			visitAll("implements", node.rawImplementing(), ", ", " ", " ");
		}
		
		formatter.startSuppressBlock();
		visit(node.getRawBody());
		formatter.endSuppressBlock();
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitEnumConstant(EnumConstant node) {
		visit(node.getRawJavadoc());
		formatter.buildInline(node);
		visitAll(node.rawAnnotations(), "", "", "");
		formatter.nameNextElement("name");
		visit(node.getRawName());
		visitAll(node.rawArguments(), ", ", "(", ")");
		if (node.getRawBody() != null) {
			formatter.space();
			formatter.startSuppressBlock();
			visit(node.getRawBody());
			formatter.endSuppressBlock();
		}
		formatter.closeInline();
		return true;
	}
	
	public boolean visitAnnotationDeclaration(AnnotationDeclaration node) {
		visit(node.getRawJavadoc());
		formatter.buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).rawKeywords().isEmpty()) {
				formatter.space();
			}
		}
		formatter.append("@");
		formatter.keyword("interface");
		formatter.space();
		formatter.nameNextElement("constantName");
		visit(node.getRawName());
		formatter.space();
		formatter.startSuppressBlock();
		visit(node.getRawBody());
		formatter.endSuppressBlock();
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitAnnotationMethodDeclaration(AnnotationMethodDeclaration node) {
		formatter.buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).rawKeywords().isEmpty()) {
				formatter.space();
			}
		}
		formatter.nameNextElement("returnType");
		visit(node.getRawReturnTypeReference());
		formatter.space();
		formatter.nameNextElement("methodName");
		visit(node.getRawMethodName());
		formatter.append("(");
		formatter.append(")");
		if (node.getRawDefaultValue() != null) {
			formatter.space();
			formatter.keyword("default");
			formatter.space();
			formatter.nameNextElement("default");
			visit(node.getRawDefaultValue());
		}
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitCompilationUnit(CompilationUnit node) {
		formatter.buildBlock(node);
		if (node.getRawPackageDeclaration() != null) {
			visit(node.getRawPackageDeclaration());
			if (!node.rawTypeDeclarations().isEmpty() || !node.rawImportDeclarations().isEmpty()) formatter.verticalSpace();
		}
		visitAll(node.rawImportDeclarations(), "", "", "");
		if (!node.rawTypeDeclarations().isEmpty() && !node.rawImportDeclarations().isEmpty()) formatter.verticalSpace();
		visitAll(node.rawTypeDeclarations(), "\n", "", "");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitPackageDeclaration(PackageDeclaration node) {
		formatter.buildBlock(node);
		visitAll(node.rawAnnotations(), "", "", "");
		formatter.keyword("package");
		formatter.space();
		visitAll(node.rawParts(), ".", "", "");
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	public boolean visitImportDeclaration(ImportDeclaration node) {
		formatter.buildBlock(node);
		formatter.keyword("import");
		formatter.space();
		if (node.isStaticImport()) {
			formatter.keyword("static");
			formatter.space();
		}
		visitAll(node.rawParts(), ".", "", "");
		if (node.isStarImport()) {
			formatter.append(".*");
		}
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
	
	//Various
	public boolean visitParseArtefact(Node node) {
		formatter.buildInline(node);
		formatter.fail("ARTEFACT: " + node.getClass().getSimpleName());
		formatter.closeInline();
		return true;
	}
	
	public boolean visitComment(Comment node) {
		formatter.buildBlock(node);
		formatter.append(node.isBlockComment() ? "/*" : "//");
		if (node.getContent() == null) formatter.fail("MISSING_COMMENT");
		else
			formatter.append(node.getContent());
		if (node.isBlockComment()) formatter.append("*/");
		formatter.closeBlock();
		return true;
	}

	@Override
	public boolean visitEmptyDeclaration(EmptyDeclaration node) {
		formatter.buildBlock(node);
		formatter.append(";");
		formatter.closeBlock();
		return true;
	}
}
