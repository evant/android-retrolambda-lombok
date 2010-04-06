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
package lombok.ast.ecj;

import java.lang.reflect.Array;

import org.eclipse.jdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.AssertStatement;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.BreakStatement;
import org.eclipse.jdt.internal.compiler.ast.CaseStatement;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.CharLiteral;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Clinit;
import org.eclipse.jdt.internal.compiler.ast.CombinedBinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ExtendedStringLiteral;
import org.eclipse.jdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.FloatLiteral;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.IntLiteralMinValue;
import org.eclipse.jdt.internal.compiler.ast.Javadoc;
import org.eclipse.jdt.internal.compiler.ast.JavadocReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LongLiteral;
import org.eclipse.jdt.internal.compiler.ast.LongLiteralMinValue;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.jdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedSuperReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.StringLiteralConcatenation;
import org.eclipse.jdt.internal.compiler.ast.SubRoutineStatement;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.SynchronizedStatement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;

public class EcjTreePrinter extends EcjTreeVisitor {
	private final StringBuilder output = new StringBuilder();
	private int indent;
	private String rel;
	private final boolean includePositions;
	
	public EcjTreePrinter(boolean includePositions) {
		this.includePositions = includePositions;
	}
	
	@Override
	public String toString() {
		return output.toString();
	}
	
	private void printNode(ASTNode node) {
		if (includePositions) {
			printNode(node == null ? "NULL" :
				String.format("%s (%d-%d)", node.getClass().getSimpleName(), node.sourceStart, node.sourceEnd));
		} else {
			printNode(node == null ? "NULL" : node.getClass().getSimpleName());
		}
		if (node != null) {
			property("bits", node.bits);
			if (node instanceof Expression) {
				property("constant", ((Expression) node).constant);
			}
		}
	}
	
	private void printNode(String nodeKind) {
		printIndent();
		if (rel != null)
			output.append(rel).append(": ");
		rel = null;
		output.append("[").append(nodeKind).append("]\n");
		indent++;
	}
	
	private void printIndent() {
		for (int i = 0; i < indent; i++) {
			output.append("\t");
		}
	}
	
	private void property(String rel, Object val) {
		printIndent();
		if (rel != null)
			output.append(rel).append(": ");
		if (val instanceof ASTNode)
			output.append("!!ASTN-AS-PROP!!");
		if (val == null) {
			output.append("[NULL]\n");
		} else {
			String content;
			if (val instanceof char[]) {
				content = "= " + new lombok.ast.StringLiteral().setValue(new String((char[])val)).getRawValue();
			} else if (val instanceof char[][]) {
				StringBuilder sb = new StringBuilder();
				for (char[] single : ((char[][])val)) {
					if (sb.length() != 0) {
						sb.append(", ");
					}
					sb.append(new lombok.ast.StringLiteral().setValue(new String((char[])single)).getRawValue());
				}
				content = "= {" + sb.toString() + "}";
			} else if (val instanceof Integer) {
				content = String.format("0x%1$08x %1$d", val);
			} else if (val instanceof Long) {
				content = String.format("0x%1$016x %1$d", val);
			} else {
				content = String.valueOf(val);
			}
			output.append("[").append(val.getClass().getSimpleName()).append(" ").append(content).append("]\n");
		}
	}
	
	private void child(String rel, ASTNode node) {
		this.rel = rel;
		if (node != null) {
			visitEcjNode(node);
		} else {
			printNode("NULL");
			indent--;
		}
	}
	
	private void children(String rel, Object nodes) {
		this.rel = rel;
		
		if (nodes == null) {
//			printNode("ARRAYNULL");
//			indent--;
			return;
		}
		
		if (!nodes.getClass().isArray()) {
			throw new IllegalArgumentException("nodes is not an array but a " + nodes.getClass().getName()); 
		}
		if (!ASTNode.class.isAssignableFrom(nodes.getClass().getComponentType())) {
			throw new IllegalArgumentException("nodes does not contain ASTNode-s but " + nodes.getClass().getComponentType().getName());
		}
		int size = Array.getLength(nodes);
		if (size == 0) {
			printNode("ARRAYEMPTY");
			indent--;
		} else {
			for (int i = 0; i < size; i++) {
				child(String.format("%s[%d]", rel, i), (ASTNode)Array.get(nodes, i));
			}
		}
	}
	
	@Override
	public void visitBlock(Block node) {
		printNode(node);
		property("explicitDeclarations", node.explicitDeclarations);
		children("statements", node.statements);
		indent--;
	}
	
	@Override
	public void visitClinit(Clinit node) {
		printNode(node);
		indent--;
	}
	
	@Override
	public void visitCompilationUnitDeclaration(CompilationUnitDeclaration node) {
		printNode(node);
		property("getMainTypeName()", node.getMainTypeName());
		child("currentPackage", node.currentPackage);
		children("imports", node.imports);
		children("types", node.types);
		indent--;
	}
	
	@Override
	public void visitConstructorDeclaration(ConstructorDeclaration node) {
		printNode(node);
		property("modifiers", node.modifiers);
		children("annotations", node.annotations);
		children("typeParameters", node.typeParameters);
		children("arguments", node.arguments);
		children("thrownExceptions", node.thrownExceptions);
		child("constructorCall", node.constructorCall);
		children("statements", node.statements);
		indent--;
	}
	
	@Override
	public void visitTypeDeclaration(TypeDeclaration node) {
		printNode(node);
		property("name", node.name);
		if (node.enclosingType == null) {
			property("enclosingType", null);
		} else {
			property("enclosingType.name", node.enclosingType.name);
			property("enclosingType.className", node.enclosingType.getClass().getSimpleName());
		}
		property("modifiers", node.modifiers);
		child("javadoc", node.javadoc);
		if (node.allocation != null && node.allocation.anonymousType == node) {
			property("allocation", "allocation.anonymousType = this");
		} else {
			child("allocation", node.allocation);
		}
		children("annotations", node.annotations);
		child("superclass", node.superclass);
		children("superInterfaces", node.superInterfaces);
		children("typeParameters", node.typeParameters);
		children("memberTypes", node.memberTypes);
		children("fields", node.fields);
		children("methods", node.methods);
		indent--;
	}
	
	@Override
	public void visitImportReference(ImportReference node) {
		printNode(node);
		property("tokens", node.tokens);
		indent--;
	}
	
	@Override
	public void visitInitializer(Initializer node) {
		printNode(node);
		property("modifiers", node.modifiers);
		property("name", node.name);
		child("block", node.block);
		indent--;
	}
	
	@Override
	public void visitExplicitConstructorCall(ExplicitConstructorCall node) {
		printNode(node);
		property("accessMode", node.accessMode);
		child("qualification", node.qualification);
		children("typeArguments", node.typeArguments);
		children("arguments", node.arguments);
		indent--;
	}
	
	@Override
	public void visitEmptyStatement(EmptyStatement node) {
		printNode(node);
		indent--;
	}
	
	@Override
	public void visitLocalDeclaration(LocalDeclaration node) {
		printNode(node);
		property("modifiers", node.modifiers);
		property("name", node.name);
		children("annotations", node.annotations);
		child("type", node.type);
		child("initialization", node.initialization);
		indent--;
	}
	
	@Override
	public void visitTypeParameter(TypeParameter node) {
		printNode(node);
		property("name", node.name);
		child("type", node.type);
		children("bounds", node.bounds);
		indent--;
	}
	
	@Override
	public void visitQualifiedTypeReference(QualifiedTypeReference node) {
		printNode(node);
		property("tokens", node.tokens);
		indent--;
	}
	
	@Override
	public void visitParameterizedQualifiedTypeReference(ParameterizedQualifiedTypeReference node) {
		printNode(node);
		property("tokens", node.tokens);
		property("dimensions", node.dimensions());
		if (node.typeArguments == null || node.typeArguments.length == 0) {
			children("typeArguments", node.typeArguments);
		} else {
			for (int i = 0; i < node.typeArguments.length; i++) {
				children("typeArguments[" + i + "]", node.typeArguments[i]);
			}
		}
		indent--;
	}
	
	@Override
	public void visitSingleTypeReference(SingleTypeReference node) {
		printNode(node);
		property("token", node.token);
		indent--;
	}
	
	@Override
	public void visitWildcard(Wildcard node) {
		printNode(node);
		property("kind", node.kind);
		child("bound", node.bound);
		indent--;
	}
	
	@Override
	public void visitIntLiteral(IntLiteral node) {
		printNode(node);
		property("source", node.source());
		property("value", node.value);
		indent--;
	}
	
	@Override
	public void visitIntLiteralMinValue(IntLiteralMinValue node) {
		printNode(node);
		property("source", node.source());
		indent--;
	}
	
	@Override
	public void visitLongLiteral(LongLiteral node) {
		printNode(node);
		property("source", node.source());
		indent--;
	}
	
	@Override
	public void visitLongLiteralMinValue(LongLiteralMinValue node) {
		printNode(node);
		property("source", node.source());
		indent--;
	}
	
	@Override
	public void visitDoubleLiteral(DoubleLiteral node) {
		printNode(node);
		property("source", node.source());
		indent--;
	}
	
	@Override
	public void visitFloatLiteral(FloatLiteral node) {
		printNode(node);
		property("source", node.source());
		indent--;
	}
	
	@Override
	public void visitTrueLiteral(TrueLiteral node) {
		printNode(node);
		indent--;
	}
	
	@Override
	public void visitFalseLiteral(FalseLiteral node) {
		printNode(node);
		indent--;
	}
	
	@Override
	public void visitNullLiteral(NullLiteral node) {
		printNode(node);
		indent--;
	}
	
	@Override public void visitCharLiteral(CharLiteral node) {
		printNode(node);
		property("source", node.source());
		indent--;
	}
	
	@Override public void visitStringLiteral(StringLiteral node) {
		printNode(node);
		property("source", node.source());
		indent--;
	}
	
	@Override
	public void visitSingleNameReference(SingleNameReference node) {
		printNode(node);
		property("token", node.token);
		indent--;
	}
	
	@Override
	public void visitUnaryExpression(UnaryExpression node) {
		printNode(node);
		child("expression", node.expression);
		indent--;
	}
	
	@Override
	public void visitPrefixExpression(PrefixExpression node) {
		printNode(node);
		property("operator", node.operator);
		child("lhs", node.lhs);
		indent--;
	}
	
	@Override
	public void visitPostfixExpression(PostfixExpression node) {
		printNode(node);
		property("operator", node.operator);
		child("lhs", node.lhs);
		indent--;
	}
	
	@Override
	public void visitBinaryExpression(BinaryExpression node) {
		printNode(node);
		property("operatorToString()", node.operatorToString());
		child("lhs", node.left);
		child("rhs", node.right);
		indent--;
	}
	
	@Override
	public void visitCastExpression(CastExpression node) {
		printNode(node);
		child("type", node.type);
		child("expression", node.expression);
		indent--;
	}
	
	@Override
	public void visitAllocationExpression(AllocationExpression node) {
		printNode(node);
		children("typeArguments", node.typeArguments);
		child("type", node.type);
		children("arguments", node.arguments);
		indent--;
	}
	
	@Override
	public void visitQualifiedNameReference(QualifiedNameReference node) {
		printNode(node);
		property("tokens", node.tokens);
		indent--;
	}
	
	//You get one of these if the "optimizeStringLiterals" is true and you write "a" + "b".
	@Override
	public void visitExtendedStringLiteral(ExtendedStringLiteral node) {
		printNode(node);
		property("source", node.source());
		indent--;
	}
	
	//You get one of these if the "optimizeStringLiterals" is false and you write "a" + "b".
	@Override
	public void visitStringLiteralConcatenation(StringLiteralConcatenation node) {
		printNode(node);
		children("literals", node.literals);
		indent--;
	}
	
	@Override
	public void visitInstanceOfExpression(InstanceOfExpression node) {
		printNode(node);
		child("expression", node.expression);
		child("type", node.type);
		indent--;
	}
	
	@Override
	public void visitEqualExpression(EqualExpression node) {
		printNode(node);
		property("operatorToString()", node.operatorToString());
		child("left", node.left);
		child("right", node.right);
		indent--;
	}
	
	@Override
	public void visitAND_AND_Expression(AND_AND_Expression node) {
		printNode(node);
		property("operatorToString()", node.operatorToString());
		child("left", node.left);
		child("right", node.right);
		indent--;
	}
	
	@Override
	public void visitOR_OR_Expression(OR_OR_Expression node) {
		printNode(node);
		property("operatorToString()", node.operatorToString());
		child("left", node.left);
		child("right", node.right);
		indent--;
	}
	
	@Override
	public void visitConditionalExpression(ConditionalExpression node) {
		printNode(node);
		child("condition", node.condition);
		child("valueIfTrue", node.valueIfTrue);
		child("valueIfFalse", node.valueIfFalse);
		indent--;
	}
	
	@Override
	public void visitAssignment(Assignment node) {
		printNode(node);
		child("lhs", node.lhs);
		child("expression", node.expression);
		indent--;
	}
	
	@Override
	public void visitCompoundAssignment(CompoundAssignment node) {
		printNode(node);
		property("operator", node.operator);
		child("lhs", node.lhs);
		child("expression", node.expression);
		indent--;
	}
	
	@Override
	public void visitCombinedBinaryExpression(CombinedBinaryExpression node) {
		printNode(node);
		property("arity", node.arity);
		property("arityMax", node.arityMax);
		children("referencesTable", node.referencesTable);
		child("left", node.left);
		child("right", node.right);
		indent--;
	}
	
	@Override
	public void visitMessageSend(MessageSend node) {
		printNode(node);
		property("selector", node.selector);
		child("receiver", node.receiver);
		children("typeArguments", node.typeArguments);
		children("arguments", node.arguments);
		indent--;
	}
	
	@Override
	public void visitArrayInitializer(ArrayInitializer node) {
		printNode(node);
		children("expressions", node.expressions);
		indent--;
	}
	
	@Override
	public void visitArrayAllocationExpression(ArrayAllocationExpression node) {
		printNode(node);
		child("type", node.type);
		children("dimensions", node.dimensions);
		child("initializer", node.initializer);
		indent--;
	}
	
	@Override
	public void visitThisReference(ThisReference node) {
		printNode(node);
		indent--;
	}
	
	@Override
	public void visitQualifiedThisReference(QualifiedThisReference node) {
		printNode(node);
		child("qualification", node.qualification);
		indent--;
	}
	
	@Override
	public void visitClassLiteralAccess(ClassLiteralAccess node) {
		printNode(node);
		child("type", node.type);
		indent--;
	}
	
	@Override
	public void visitArrayReference(ArrayReference node) {
		printNode(node);
		child("receiver", node.receiver);
		child("position", node.position);
		indent--;
	}
	
	@Override
	public void visitAssertStatement(AssertStatement node) {
		printNode(node);
		child("assertExpression", node.assertExpression);
		child("exceptionArgument", node.exceptionArgument);
		indent--;
	}
	
	@Override
	public void visitDoStatement(DoStatement node) {
		printNode(node);
		child("action", node.action);
		child("condition", node.condition);
		indent--;
	}
	
	@Override
	public void visitContinueStatement(ContinueStatement node) {
		printNode(node);
		property("label", node.label);
		indent--;
	}
	
	@Override
	public void visitBreakStatement(BreakStatement node) {
		printNode(node);
		property("label", node.label);
		indent--;
	}
	
	@Override
	public void visitForeachStatement(ForeachStatement node) {
		printNode(node);
		child("elementVariable", node.elementVariable);
		child("collection", node.collection);
		child("action", node.action);
		indent--;
	}
	
	@Override
	public void visitIfStatement(IfStatement node) {
		printNode(node);
		child("condition", node.condition);
		child("thenStatement", node.thenStatement);
		child("elseStatement", node.elseStatement);
		indent--;
	}
	
	@Override
	public void visitLabeledStatement(LabeledStatement node) {
		printNode(node);
		property("label", node.label);
		child("statement", node.statement);
		indent--;
	}
	
	@Override
	public void visitForStatement(ForStatement node) {
		printNode(node);
		children("initializations", node.initializations);
		child("condition", node.condition);
		children("increments", node.increments);
		child("action", node.action);
		indent--;
	}
	
	@Override
	public void visitSwitchStatement(SwitchStatement node) {
		printNode(node);
		property("explicitDeclarations", node.explicitDeclarations);
		child("expression", node.expression);
		children("statements", node.statements);
		indent--;
	}
	
	@Override
	public void visitCaseStatement(CaseStatement node) {
		printNode(node);
		child("constantExpression", node.constantExpression);
		indent--;
	}
	
	@Override
	public void visitSynchronizedStatement(SynchronizedStatement node) {
		printNode(node);
		child("expression", node.expression);
		child("block", node.block);
		indent--;
	}
	
	@Override
	public void visitTryStatement(TryStatement node) {
		printNode(node);
		child("tryBlock", node.tryBlock);
		children("catchArguments", node.catchArguments);
		children("catchBlocks", node.catchBlocks);
		child("finallyBlock", node.finallyBlock);
		indent--;
	}
	
	@Override
	public void visitWhileStatement(WhileStatement node) {
		printNode(node);
		child("condition", node.condition);
		child("action", node.action);
		indent--;
	}
	
	@Override
	public void visitArgument(Argument node) {
		printNode(node);
		property("modifiers", node.modifiers);
		children("annotations", node.annotations);
		child("type", node.type);
		indent--;
	}
	
	@Override
	public void visitThrowStatement(ThrowStatement node) {
		printNode(node);
		child("exception", node.exception);
		indent--;
	}
	
	@Override
	public void visitMethodDeclaration(MethodDeclaration node) {
		printNode(node);
		property("modifiers", node.modifiers);
		children("annotations", node.annotations);
		children("typeParameters", node.typeParameters);
		child("returnType", node.returnType);
		children("arguments", node.arguments);
		children("thrownExceptions", node.thrownExceptions);
		children("statements", node.statements);
		indent--;
	}
	
	@Override
	public void visitReturnStatement(ReturnStatement node) {
		printNode(node);
		child("expression", node.expression);
		indent--;
	}
	
	@Override
	public void visitParameterizedSingleTypeReference(ParameterizedSingleTypeReference node) {
		printNode(node);
		property("token", node.token);
		property("dimensions", node.dimensions);
		children("typeArguments", node.typeArguments);
		indent--;
	}
	
	@Override
	public void visitArrayTypeReference(ArrayTypeReference node) {
		printNode(node);
		property("token", node.token);
		property("dimensions", node.dimensions);
		indent--;
	}
	
	@Override public void visitArrayQualifiedTypeReference(ArrayQualifiedTypeReference node) {
		printNode(node);
		property("tokens", node.tokens);
		property("dimensions", node.dimensions());
		indent--;
	}
	
	@Override
	public void visitSingleMemberAnnotation(SingleMemberAnnotation node) {
		printNode(node);
		child("type", node.type);
		child("memberValue", node.memberValue);
		indent--;
	}
	
	@Override
	public void visitNormalAnnotation(NormalAnnotation node) {
		printNode(node);
		child("type", node.type);
		children("memberValuePairs", node.memberValuePairs);
		indent--;
	}
	
	@Override
	public void visitMemberValuePair(MemberValuePair node) {
		printNode(node);
		property("name", node.name);
		child("value", node.value);
		indent--;
	}
	
	@Override
	public void visitMarkerAnnotation(MarkerAnnotation node) {
		printNode(node);
		child("type", node.type);
		indent--;
	}
	
	@Override
	public void visitFieldDeclaration(FieldDeclaration node) {
		printNode(node);
		property("modifiers", node.modifiers);
		property("name", node.name);
		children("annotations", node.annotations);
		child("type", node.type);
		child("initialization", node.initialization);
		indent--;
	}
	
	@Override
	public void visitQualifiedAllocationExpression(QualifiedAllocationExpression node) {
		printNode(node);
		child("enclosingInstance", node.enclosingInstance);
		child("anonymousType", node.anonymousType);
		children("typeArguments", node.typeArguments);
		child("type", node.type);
		children("arguments", node.arguments);
		child("anonymousType", node.anonymousType);
		indent--;
	}
	
	@Override
	public void visitFieldReference(FieldReference node) {
		printNode(node);
		property("token", node.token);
		child("receiver", node.receiver);
		indent--;
	}
	
	@Override
	public void visitAnnotationMethodDeclaration(AnnotationMethodDeclaration node) {
		printNode(node);
		property("modifiers", node.modifiers);
		children("annotations", node.annotations);
		child("returnType", node.returnType);
		child("defaultValue", node.defaultValue);
		children("typeParameters", node.typeParameters);
		indent--;
	}
	
	@Override
	public void visitQualifiedSuperReference(QualifiedSuperReference node) {
		printNode(node);
		child("qualification", node.qualification);
		indent--;
	}
	
	@Override
	public void visitSuperReference(SuperReference node) {
		printNode(node);
		indent--;
	}
	
	@Override
	public void visitJavadoc(Javadoc node) {
		printNode(node);
		
		if (node.paramReferences == null) {
			property("paramReferences", null);
		} else {
			int i = 0;
			for (JavadocSingleNameReference s : node.paramReferences) {
				rel = "parameterReferences[" + i + "]";
				printNode(s == null ? "NULL" : s.getClass().getSimpleName());
				property("bits", s.bits);
				property("token", s.token);
				indent--;
			}
		}
		if (node.invalidParameters == null) {
			property("invalidParameters", null);
		} else {
			int i = 0;
			for (JavadocSingleNameReference s : node.invalidParameters) {
				rel = "invalidParameters[" + i + "]";
				printNode(s == null ? "NULL" : s.getClass().getSimpleName());
				property("bits", s.bits);
				property("token", s.token);
				indent--;
			}
		}
		if (node.paramTypeParameters == null) {
			property("paramTypeParameters", null);
		} else {
			int i = 0;
			for (JavadocSingleTypeReference s : node.paramTypeParameters) {
				rel = "paramTypeParameters[" + i + "]";
				printNode(s == null ? "NULL" : s.getClass().getSimpleName());
				property("bits", s.bits);
				property("token", s.token);
				indent--;
			}
		}
		JavadocReturnStatement returnStatement = node.returnStatement;
		if (returnStatement == null) {
			property("returnStatement.expression", null);
		} else {
			child("returnStatement.expression", returnStatement.expression);
		
			SubRoutineStatement[] subroutines = returnStatement.subroutines;
			if (subroutines == null) {
				property("returnStatement.subroutines", null);
			} else {
				int i = 0;
				for (SubRoutineStatement s : subroutines) {
					rel = "returnStatement.subroutines[" + i + "]";
					printNode(s == null ? "NULL" : s.getClass().getSimpleName());
					property("bits", s.bits);
					indent--;
				}
			}
		}
		
		if (node.seeReferences == null) {
			property("seeReferences", null);
		} else {
			int i = 0;
			for (Expression s : node.seeReferences) {
				property("seeReferences[" + i + "]", s.toString());
			}
		}
		if (node.exceptionReferences == null) {
			property("exceptionReferences", null);
		} else {
			int i = 0;
			for (TypeReference s : node.exceptionReferences) {
				property("exceptionReferences[" + i + "]", s.toString());
			}
		}
		indent--;
	}
}
