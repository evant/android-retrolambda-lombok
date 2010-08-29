/*
 * Copyright © 2010 Reinier Zwitserloot, Roel Spilker and Robbert Jan Grootjans.
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

import java.util.List;
import java.util.Map;

import lombok.ast.ArrayAccess;
import lombok.ast.ArrayCreation;
import lombok.ast.ArrayDimension;
import lombok.ast.Assert;
import lombok.ast.BinaryOperator;
import lombok.ast.BooleanLiteral;
import lombok.ast.Break;
import lombok.ast.Cast;
import lombok.ast.ClassLiteral;
import lombok.ast.ConstructorInvocation;
import lombok.ast.Continue;
import lombok.ast.DoWhile;
import lombok.ast.ExpressionStatement;
import lombok.ast.FloatingPointLiteral;
import lombok.ast.For;
import lombok.ast.ForEach;
import lombok.ast.If;
import lombok.ast.InlineIfExpression;
import lombok.ast.InstanceOf;
import lombok.ast.IntegralLiteral;
import lombok.ast.KeywordModifier;
import lombok.ast.LabelledStatement;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.NormalTypeBody;
import lombok.ast.Position;
import lombok.ast.RawListAccessor;
import lombok.ast.Select;
import lombok.ast.Statement;
import lombok.ast.StrictListAccessor;
import lombok.ast.Super;
import lombok.ast.Switch;
import lombok.ast.This;
import lombok.ast.TypeReferencePart;
import lombok.ast.TypeVariable;
import lombok.ast.UnaryOperator;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.VariableReference;
import lombok.ast.WildcardKind;

import org.eclipse.jdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
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
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.CharLiteral;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.CombinedBinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ExtendedStringLiteral;
import org.eclipse.jdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.jdt.internal.compiler.ast.FloatLiteral;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.IntLiteralMinValue;
import org.eclipse.jdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LongLiteral;
import org.eclipse.jdt.internal.compiler.ast.LongLiteralMinValue;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.jdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedSuperReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class EcjTreeConverter extends EcjTreeVisitor {
	private enum FlagKey {
		IMPORTDECLARATION_IS_PACKAGE,
		NAMEREFERENCE_IS_TYPE,
		AS_STATEMENT,
		FOR_EACH_VAR,
	}
	
	private List<? extends Node> result = null;
	private Map<FlagKey, Object> params = ImmutableMap.of();
	
	
	private EcjTreeConverter() {}
	
	private boolean hasFlag(FlagKey key) {
		return params.containsKey(key);
	}
	
	@SuppressWarnings("unused")
	private Object getFlag(FlagKey key) {
		return params.get(key);
	}
	
	List<? extends Node> getAll() {
		return result;
	}
	
	Node get() {
		if (result.isEmpty()) {
			return null;
		}
		if (result.size() == 1) {
			return result.get(0);
		}
		throw new RuntimeException("Expected only one result but got " + result.size());
	}
	
	private void set(ASTNode node, Node value) {
		if (result != null) throw new IllegalStateException("result is already set");
		
		if (value instanceof lombok.ast.Expression && hasFlag(FlagKey.AS_STATEMENT)) {
			ExpressionStatement stat = new ExpressionStatement();
			stat.astExpression((lombok.ast.Expression)value);
			set(node, stat);
			return;
		}
		
		List<Node> result = Lists.newArrayList();
		if (value != null) result.add(value);
		this.result = result;
	}
	
	@SuppressWarnings("unused")
	private void set(ASTNode node, List<? extends Node> values) {
		if (values.isEmpty()) System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
		
		if (result != null) throw new IllegalStateException("result is already set");
		this.result = values;
	}
	
	private Node toTree(ASTNode node, FlagKey... keys) {
		Map<FlagKey, Object> map = Maps.newEnumMap(FlagKey.class);
		for (FlagKey key : keys) map.put(key, key);
		return toTree(node, map);
	}
	
	private Node toTree(ASTNode node, Map<FlagKey, Object> params) {
		if (node == null) return null;
		EcjTreeConverter visitor = new EcjTreeConverter();
		if (params != null) visitor.params = params;
		visitor.visitEcjNode(node);
		try {
			return visitor.get();
		} catch (RuntimeException e) {
			System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
			throw e;
		}
	}
	
	private void fillList(ASTNode[] nodes, RawListAccessor<?, ?> list, FlagKey... keys) {
		if (nodes == null) return;
		
		for (ASTNode node : nodes) list.addToEnd(toTree(node, keys));
	}
	
	public static Node convert(ASTNode node) {
		return new EcjTreeConverter().toTree(node);
	}
	
	private lombok.ast.Identifier toIdentifier(char[] token) {
		return lombok.ast.Identifier.of(token == null ? "" : new String(token));
	}
	
	private lombok.ast.Modifiers toModifiers(int modifiers, Annotation[] annotations) {
		lombok.ast.Modifiers m = new lombok.ast.Modifiers();
		for (KeywordModifier mod : KeywordModifier.fromReflectModifiers(modifiers)) m.astKeywords().addToEnd(mod);
		fillList(annotations, m.rawAnnotations());
		return m;
	}
	
	private void fillDimensions(Expression[] nodes, RawListAccessor<ArrayDimension, lombok.ast.ArrayCreation> list) {
		if (nodes == null) return;
		
		for (Expression node : nodes) list.addToEnd(new ArrayDimension().astDimension((lombok.ast.Expression) toTree(node)));
	}
	
	private void fillIdentifiers(char[][] tokens, StrictListAccessor<lombok.ast.Identifier, ?> list) {
		if (tokens == null) return;
		for (char[] token : tokens) list.addToEnd(toIdentifier(token));
	}

	private <N extends lombok.ast.Node> N setPosition(ASTNode node, N lombokNode) {
		lombokNode.setPosition(new Position(node.sourceStart, node.sourceEnd));
		return lombokNode;
	}
	
	@Override public void visitCompilationUnitDeclaration(CompilationUnitDeclaration node) {
		lombok.ast.CompilationUnit unit = new lombok.ast.CompilationUnit();
		unit.rawPackageDeclaration(toTree(node.currentPackage, FlagKey.IMPORTDECLARATION_IS_PACKAGE));
		fillList(node.imports, unit.rawImportDeclarations());
		fillList(node.types, unit.rawTypeDeclarations());
		set(node, unit);
	}
	
	@Override public void visitImportReference(ImportReference node) {
		if (hasFlag(FlagKey.IMPORTDECLARATION_IS_PACKAGE)) {
			lombok.ast.PackageDeclaration pkg = new lombok.ast.PackageDeclaration();
			fillIdentifiers(node.tokens, pkg.astParts());
			fillList(node.annotations, pkg.rawAnnotations());
			set(node, pkg);
			return;
		}
		
		lombok.ast.ImportDeclaration imp = new lombok.ast.ImportDeclaration();
		fillIdentifiers(node.tokens, imp.astParts());
		imp.astStarImport((node.bits & ASTNode.OnDemand) != 0);
		imp.astStaticImport((node.modifiers & ClassFileConstants.AccStatic) != 0);
		set(node, imp);
	}
	
	@Override public void visitInitializer(Initializer node) {
		if ((node.modifiers & ClassFileConstants.AccStatic) != 0) {
			lombok.ast.StaticInitializer staticInit = new lombok.ast.StaticInitializer();
			lombok.ast.Block b = new lombok.ast.Block();
			staticInit.astBody(b);
			fillList(node.block.statements, b.rawContents());
			set(node, staticInit);
			return;
		}
		else if (node.modifiers == 0) {
			lombok.ast.InstanceInitializer instanceInit = new lombok.ast.InstanceInitializer();
			lombok.ast.Block b = new lombok.ast.Block();
			instanceInit.astBody(b);
			
			fillList(node.block.statements, b.rawContents());
			set(node, instanceInit);
			return;
		}
	}
	
	@Override 
	public void visitTypeDeclaration(TypeDeclaration node) {
		switch (TypeDeclaration.kind(node.modifiers)) {
			case TypeDeclaration.CLASS_DECL: {
				lombok.ast.ClassDeclaration decl = new lombok.ast.ClassDeclaration();
				decl.astModifiers(toModifiers(node.modifiers, node.annotations));
				decl.astName(toIdentifier(node.name));
				fillList(node.typeParameters, decl.rawTypeVariables());
				
				NormalTypeBody body = new NormalTypeBody();
				fillList(node.fields, body.rawMembers());
				fillList(node.memberTypes, body.rawMembers());
				
				decl.astBody(body);
				set(node, decl);
				return;
			}
			case TypeDeclaration.INTERFACE_DECL: {
				visitAny(node);
			}
			case TypeDeclaration.ENUM_DECL: {
				visitAny(node);
			}
			case TypeDeclaration.ANNOTATION_TYPE_DECL: {
				visitAny(node);
			}
		}
	}
	
	@Override public void visitTypeParameter(TypeParameter node) {
		TypeVariable var = new TypeVariable();
		var.astName(toIdentifier(node.name));
		var.astExtending().addToEnd((lombok.ast.TypeReference)toTree(node.type));
		fillList(node.bounds, var.rawExtending());
		
		setPosition(node, var);
		set(node, var);
	}

	@Override public void visitMethodDeclaration(MethodDeclaration node) {
		lombok.ast.MethodDeclaration methodDecl = new lombok.ast.MethodDeclaration();
		methodDecl.astMethodName(toIdentifier(node.selector));
		setPosition(node, methodDecl);
		set(node, methodDecl);
	}
	
	@Override public void visitEmptyStatement(EmptyStatement node) {
		lombok.ast.EmptyStatement statement = new lombok.ast.EmptyStatement();
		setPosition(node, statement);
		set(node, statement);
	}
	
	private lombok.ast.Expression toExpression(Expression expression) {
		Node tree = toTree(expression);
		if (tree instanceof ExpressionStatement) {
			lombok.ast.Expression astExpression = ((ExpressionStatement)tree).astExpression();
			tree.detach(astExpression);
			return astExpression;
		}
		return (lombok.ast.Expression) tree;
	}

	@Override public void visitLocalDeclaration(LocalDeclaration node) {
//		VariableDefinition varDef = new VariableDefinition();
//		varDef.astModifiers(toModifiers(node.modifiers,node.annotations));
//		System.err.println("Type ref: " + node.type.getClass().getName());
//		varDef.astTypeReference((lombok.ast.TypeReference) toTree(node.type));
//		
//		VariableDefinitionEntry varDefEntry = new VariableDefinitionEntry();
//		varDefEntry.astInitializer(toExpression(node.initialization));
//		varDefEntry.astName(toIdentifier(node.name));
//		varDef.astVariables().addToEnd(varDefEntry);
		
		if (hasFlag(FlagKey.FOR_EACH_VAR)) {
			VariableDefinition varDef = new VariableDefinition();
			varDef.astModifiers(toModifiers(node.modifiers,node.annotations));
			System.err.println("Type ref: " + node.type.getClass().getName());
			varDef.astTypeReference((lombok.ast.TypeReference) toTree(node.type));
			
			VariableDefinitionEntry varDefEntry = new VariableDefinitionEntry();
			varDefEntry.astInitializer(toExpression(node.initialization));
			varDefEntry.astName(toIdentifier(node.name));
			varDef.astVariables().addToEnd(varDefEntry);
			set(node, varDef);
			return;
		}
		
		VariableDefinition varDef = new VariableDefinition();
		varDef.astModifiers(toModifiers(node.modifiers,node.annotations));
		System.err.println("Type ref: " + node.type.getClass().getName());
		varDef.astTypeReference((lombok.ast.TypeReference) toTree(node.type));
		
		VariableDefinitionEntry varDefEntry = new VariableDefinitionEntry();
		varDefEntry.astInitializer(toExpression(node.initialization));
		varDefEntry.astName(toIdentifier(node.name));
		varDef.astVariables().addToEnd(varDefEntry);
		
		VariableDeclaration decl = new VariableDeclaration();
		decl.astDefinition(varDef);
		set(node, decl);
	}
	
	@Override public void visitBlock(Block node) {
		
		
		lombok.ast.Block block = new lombok.ast.Block();
		fillList(node.statements, block.rawContents(), FlagKey.AS_STATEMENT);
		
		set(node, setPosition(node, block));
	}
	

	@Override public void visitSingleTypeReference(SingleTypeReference node) {
		lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
		ref.astParts().addToEnd(createSingleTypeReferencePart(node));
		setPosition(node, ref);
		set(node, ref);
	}

	private TypeReferencePart createSingleTypeReferencePart(SingleTypeReference node) {
		TypeReferencePart part = new TypeReferencePart();
		part.astIdentifier(toIdentifier(node.token));
		return part;
	}
	
	private TypeReferencePart createParameterizedSingleTypeReferencePart(ParameterizedSingleTypeReference node) {
		TypeReferencePart part = createTypeReferencePart(node.token, node.typeArguments);
		return part;
	}
	
	private void fillTypeReferenceParts(char[][] tokens, StrictListAccessor<lombok.ast.TypeReferencePart, ?> list) {
		if (tokens == null) return;
		for (char[] token : tokens) {
			TypeReferencePart part = new TypeReferencePart();
			part.astIdentifier(toIdentifier(token));
			list.addToEnd(part);
		}
	}
	
	@Override public void visitQualifiedTypeReference(QualifiedTypeReference node) {
		lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
		fillTypeReferenceParts(node.tokens, ref.astParts());
		set(node, ref);
	}
	
	private void fillTypeReferenceParts(char[][] tokens, TypeReference[][] typeArguments, StrictListAccessor<lombok.ast.TypeReferencePart, ?> list) {
		if (tokens == null) return;
		for (int i = 0; i < typeArguments.length ; i++) {
			TypeReference[] typeReferences = typeArguments[i];
			TypeReferencePart part = createTypeReferencePart(tokens[i], typeReferences);
			list.addToEnd(part);
		}
	}
	
	private TypeReferencePart createTypeReferencePart(char[] token) {
		return createTypeReferencePart(token, null);
	}

	private TypeReferencePart createTypeReferencePart(char[] token, TypeReference[] typeReferences) {
		TypeReferencePart part = new TypeReferencePart();
		part.astIdentifier(toIdentifier(token));
		if (typeReferences != null) fillList(typeReferences, part.rawTypeArguments());
		return part;
	}
	
	@Override public void visitParameterizedQualifiedTypeReference(ParameterizedQualifiedTypeReference node) {
		lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
		ref.astArrayDimensions(node.dimensions());
		fillTypeReferenceParts(node.tokens, node.typeArguments, ref.astParts());
		set(node, setPosition(node, ref));
	}
	
	@Override public void visitWildcard(Wildcard node) {
		
		lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
		
		TypeReferencePart part;
		if (node.bound instanceof ParameterizedSingleTypeReference) {
			part = createParameterizedSingleTypeReferencePart((ParameterizedSingleTypeReference)node.bound);
		}
		else if (node.bound instanceof SingleTypeReference) {
			part = createSingleTypeReferencePart((SingleTypeReference)node.bound);
		}
		else {
			part = new TypeReferencePart();
		}
		
		ref.astParts().addToEnd(part);
		
		switch (node.kind) {
			case Wildcard.UNBOUND: 
				ref.astWildcard(WildcardKind.UNBOUND);
				break;
			case Wildcard.EXTENDS: 
				ref.astWildcard(WildcardKind.EXTENDS);
				break;
			case Wildcard.SUPER:
				ref.astWildcard(WildcardKind.SUPER);
		}
		setPosition(node, ref);
		set(node, ref);
	}
	
	@Override public void visitParameterizedSingleTypeReference(ParameterizedSingleTypeReference node) {
		lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
		TypeReferencePart part = new TypeReferencePart();
		part.astIdentifier(toIdentifier(node.token));
		ref.astParts().addToEnd(part);
		fillList(node.typeArguments, part.rawTypeArguments());
		set(node, ref);
	}
	
//	@Override public void visitArrayTypeReference(ArrayTypeReference node) {
//		System.err.println("ATR: " + String.valueOf(node.token));
//		
//		lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
//		
//		
//		TypeReferencePart part = new TypeReferencePart();
//		part.astIdentifier(toIdentifier(node.token));
//		ref.astParts().addToEnd(part);
//		ref.astArrayDimensions(node.dimensions);
//		set(node, ref);
//	}
	@Override public void visitArrayTypeReference(ArrayTypeReference node) {
		System.err.println("ATR: " + String.valueOf(node.token));
		System.err.println("ATR: token conversion " + toIdentifier(node.token));
		
		lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
		
		
		TypeReferencePart part = new TypeReferencePart();
		part.astIdentifier(toIdentifier(node.token));
		ref.astParts().addToEnd(part);
		ref.astArrayDimensions(node.dimensions);
		set(node, ref);
	}
	
	@Override public void visitArrayQualifiedTypeReference(ArrayQualifiedTypeReference node) {
		lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
		fillTypeReferenceParts(node.tokens, ref.astParts());
		ref.astArrayDimensions(node.dimensions());
		set(node, setPosition(node, ref));
	}
	
	private lombok.ast.Node addUnaryMinusAsParent(boolean condition, lombok.ast.Expression expression) {
		if (condition) {
			return new lombok.ast.UnaryExpression().astOperand(expression).astOperator(UnaryOperator.UNARY_MINUS);
		}
		return expression;
	}
	
	@Override public void visitIntLiteral(IntLiteral node) {
		String rawValue = String.valueOf(node.source());
		boolean negative = rawValue.startsWith("-");
		IntegralLiteral integral = new IntegralLiteral().rawValue(negative ? rawValue.substring(1) : rawValue);
		set(node, setPosition(node, addUnaryMinusAsParent(negative, integral)));
	}
	
	@Override public void visitIntLiteralMinValue(IntLiteralMinValue node) {
		visitIntLiteral(node);
	}
		
	@Override public void visitLongLiteral(LongLiteral node) {
		String rawValue = String.valueOf(node.source());
		boolean negative = rawValue.startsWith("-");
		IntegralLiteral integral = new IntegralLiteral().rawValue(negative ? rawValue.substring(1) : rawValue);
		set(node, setPosition(node, addUnaryMinusAsParent(negative, integral)));
	}
	
	@Override public void visitLongLiteralMinValue(LongLiteralMinValue node) {
		visitLongLiteral(node);
	}
	
	@Override public void visitFloatLiteral(FloatLiteral node) {
		set(node, setPosition(node, new FloatingPointLiteral().rawValue(String.valueOf(node.source()))));
	}
	
	@Override public void visitDoubleLiteral(DoubleLiteral node) {
		set(node, setPosition(node, new FloatingPointLiteral().rawValue(String.valueOf(node.source()))));
	}
	
	@Override public void visitTrueLiteral(TrueLiteral node) {
		set(node, setPosition(node, new BooleanLiteral().astValue(true)));
	}
	
	@Override public void visitFalseLiteral(FalseLiteral node) {
		set(node, setPosition(node, new BooleanLiteral().astValue(false)));
	}
	
	@Override public void visitNullLiteral(NullLiteral node) {
		set(node, setPosition(node, new lombok.ast.NullLiteral()));
	}
	
	@Override public void visitCharLiteral(CharLiteral node) {
		set(node, setPosition(node, new lombok.ast.CharLiteral().rawValue(String.valueOf(node.source()))));
	}
	
	@Override public void visitStringLiteral(StringLiteral node) {
		set(node, setPosition(node, new lombok.ast.StringLiteral().astValue(String.valueOf(node.source()))));
	}
	
	@Override public void visitSingleNameReference(SingleNameReference node) {
		if (hasFlag(FlagKey.NAMEREFERENCE_IS_TYPE)) {
			set(node, setPosition(node, new lombok.ast.TypeReference().astParts().addToEnd(createTypeReferencePart(node.token))));
			return;
		}
		set(node, setPosition(node, new VariableReference().astIdentifier(toIdentifier(node.token))));
	}
	
	@Override public void visitCastExpression(CastExpression node) {
//		System.err.println("node: " + node);
//		System.err.println("node.type: " + node.type);
		System.err.println("node.type: " + node.type.getClass().getName());
		
		Node result = toTree(node.type, FlagKey.NAMEREFERENCE_IS_TYPE);
//		System.err.println("result: " + result);
//		System.err.println("result.class: " + result.getClass().getName());
		
		Cast cast = new Cast().astTypeReference((lombok.ast.TypeReference) result);
		cast.astOperand((lombok.ast.Expression)toTree(node.expression));
		set(node, setPosition(node, cast));
//		toTree(node.type);
//		Cast cast = new Cast().astTypeReference((lombok.ast.TypeReference) toTree(node.type));
//		cast.astOperand((lombok.ast.Expression)toTree(node.expression));
//		set(node, setPosition(node, cast));
//		set(node, (Node)null);
	}
	
	@Override public void visitThisReference(ThisReference node) {
		set(node, setPosition(node, new This()));
	}
	
	@Override public void visitQualifiedThisReference(QualifiedThisReference node) {
		set(node, setPosition(node, new This().astQualifier((lombok.ast.TypeReference) toTree(node.qualification))));
	}
	
	@Override public void visitSuperReference(SuperReference node) {
		set(node, setPosition(node, new Super()));
	}
	
	@Override public void visitQualifiedSuperReference(QualifiedSuperReference node) {
		set(node, setPosition(node, new Super().astQualifier((lombok.ast.TypeReference) toTree(node.qualification))));
	}
	
	@Override public void visitClassLiteralAccess(ClassLiteralAccess node) {
		ClassLiteral literal = new ClassLiteral().astTypeReference((lombok.ast.TypeReference) toTree(node.type));
		set(node, setPosition(node, literal));
	}
	
	@Override public void visitArrayAllocationExpression(ArrayAllocationExpression node) {
		ArrayCreation creation = new ArrayCreation();
		creation.astInitializer((lombok.ast.ArrayInitializer) toTree(node.initializer));
		fillDimensions(node.dimensions, creation.rawDimensions());
		creation.astComponentTypeReference((lombok.ast.TypeReference) toTree(node.type));
		set(node, setPosition(node, creation));
	}
	
	@Override public void visitArrayInitializer(ArrayInitializer node) {
		lombok.ast.ArrayInitializer init = new lombok.ast.ArrayInitializer();
		fillList(node.expressions, init.rawExpressions());
		set(node, setPosition(node, init));
	}
	
	
	@Override public void visitAssignment(Assignment node) {
		ExpressionStatement statement = new ExpressionStatement();
		lombok.ast.BinaryExpression bin = new lombok.ast.BinaryExpression();
		bin.astLeft((lombok.ast.Expression) toTree(node.lhs));
		bin.astRight(toExpression(node.expression));
		bin.astOperator(BinaryOperator.ASSIGN);
		statement.astExpression(bin);
		set(node, statement);
	}
	
	@Override public void visitArrayReference(ArrayReference node) {
		ArrayAccess access = new ArrayAccess();
		access.astOperand((lombok.ast.Expression) toTree(node.receiver));
		access.astIndexExpression((lombok.ast.Expression) toTree(node.position));
		set(node, setPosition(node, access));
	}
	
	@Override public void visitUnaryExpression(UnaryExpression node) {
		lombok.ast.UnaryExpression unary = new lombok.ast.UnaryExpression();
		int operatorId = ((node.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT);
		unary.astOperator(GENERIC_UNARY_OPERATORS.get(operatorId));
		unary.astOperand((lombok.ast.Expression) toTree(node.expression));
		set(node, setPosition(node, unary));
	}
	
	@Override public void visitPrefixExpression(PrefixExpression node) {
		lombok.ast.UnaryExpression unary = fillUnaryOperator(node, new lombok.ast.UnaryExpression());
		unary.astOperand((lombok.ast.Expression) toTree(node.lhs));
		set(node, setPosition(node, unary));
	}
	
	@Override public void visitPostfixExpression(PostfixExpression node) {
		lombok.ast.UnaryExpression unary = fillUnaryOperator(node, new lombok.ast.UnaryExpression());
		unary.astOperand(toExpression(node.lhs));
		set(node, setPosition(node, unary));
	}
	
	@Override public void visitBinaryExpression(BinaryExpression node) {
		lombok.ast.BinaryExpression bin = new lombok.ast.BinaryExpression();
		int operatorId = ((node.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT);
		bin.astOperator(GENERIC_BINARY_OPERATORS.get(operatorId));
		bin.astLeft(toExpression(node.left));
		bin.astRight(toExpression(node.right));
		set(node, setPosition(node, bin));
	}
	
	@Override public void visitCombinedBinaryExpression(CombinedBinaryExpression node) {
		visitBinaryExpression(node);
	}
	
	@Override public void visitCompoundAssignment(CompoundAssignment node) {
		lombok.ast.BinaryExpression bin = new lombok.ast.BinaryExpression();
		int operatorId = node.operator;
		bin.astOperator(ASSIGN_BINARY_OPERATORS.get(operatorId));
		bin.astLeft((lombok.ast.Expression) toTree(node.lhs));
		bin.astRight((lombok.ast.Expression) toTree(node.expression));
		set(node, setPosition(node, bin));
	}
	
	@Override public void visitEqualExpression(EqualExpression node) {
		visitBinaryExpression(node);
	}

	@Override public void visitInstanceOfExpression(InstanceOfExpression node) {
		InstanceOf instanceOf = new InstanceOf();
		instanceOf.astObjectReference((lombok.ast.Expression) toTree(node.expression));
		instanceOf.astTypeReference((lombok.ast.TypeReference) toTree(node.type));
		set(node, setPosition(node, instanceOf));
	}
	
	@Override public void visitAND_AND_Expression(AND_AND_Expression node) {
		visitBinaryExpression(node);
	}
	
	@Override public void visitOR_OR_Expression(OR_OR_Expression node) {
		visitBinaryExpression(node);
	}
	
	@Override public void visitConditionalExpression(ConditionalExpression node) {
		InlineIfExpression inlineIf = new InlineIfExpression()
			.astCondition((lombok.ast.Expression) toTree(node.condition))
			.astIfTrue((lombok.ast.Expression) toTree(node.valueIfTrue))
			.astIfFalse((lombok.ast.Expression) toTree(node.valueIfFalse));
		
		set(node, setPosition(node, inlineIf));
	}
	
	@Override public void visitAllocationExpression(AllocationExpression node) {
		ConstructorInvocation constr = new ConstructorInvocation();
		constr.astTypeReference((lombok.ast.TypeReference) toTree(node.type));
		set(node, setPosition(node, constr));
	}
	
	
	private lombok.ast.Expression toSelect(char[][] tokens) {
		if (tokens.length < 2) {
			return null;
		}
		
		lombok.ast.Expression current = new VariableReference().astIdentifier(toIdentifier(tokens[0]));
		
		for (int i = 1; i < tokens.length; i++) {
			Select select = new Select().astIdentifier(toIdentifier(tokens[i]));
			select.astOperand(current);
			current = select;
		}
		
		return current;
	}
	
	@Override public void visitQualifiedNameReference(QualifiedNameReference node) {
		System.err.println("QNA: " + node);
		if (hasFlag(FlagKey.NAMEREFERENCE_IS_TYPE)) {
			System.err.println("QNA: " + node);
			lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
			fillTypeReferenceParts(node.tokens, ref.astParts());
			set(node, setPosition(node, ref));
			return;
		}
		lombok.ast.Expression select = toSelect(node.tokens);
		set(node, setPosition(node, select));
	}
	
	
	@Override public void visitExtendedStringLiteral(ExtendedStringLiteral node) {
		/*
		 * TODO: ECJ merge appended string literals, find out if there is a 
		 * way to check the original sources...
		 */
		
		set(node, (Node)null);
	}

	@Override public void visitMessageSend(MessageSend node) {
		MethodInvocation inv = new MethodInvocation();
		fillList(node.arguments, inv.rawArguments());
		fillList(node.typeArguments, inv.rawMethodTypeArguments());
		inv.astOperand(toExpression(node.receiver));
		inv.astName(toIdentifier(node.selector));
		set(node, inv);
	}
	
	@Override public void visitAssertStatement(AssertStatement node) {
		Assert asrt = new Assert();
		asrt.astAssertion(toExpression(node.assertExpression));
		asrt.astMessage(toExpression(node.exceptionArgument));
		set(node, setPosition(node, asrt));
	}
	
	@Override public void visitDoStatement(DoStatement node) {
		DoWhile doWhile = new DoWhile();
		doWhile.astCondition(toExpression(node.condition));
		doWhile.astStatement((Statement)toTree(node.action));
		set(node, setPosition(node, doWhile));
	}
	
	@Override public void visitForeachStatement(ForeachStatement node) {
		ForEach forEach = new ForEach();
		forEach.astIterable(toExpression(node.collection));
		forEach.astVariable((VariableDefinition) toTree(node.elementVariable, FlagKey.FOR_EACH_VAR));
		forEach.astStatement((Statement)toTree(node.action));
		set(node, setPosition(node, forEach));
	}
	
	@Override public void visitIfStatement(IfStatement node) {
		If ifStatement = new lombok.ast.If().astCondition(toExpression(node.condition));
		ifStatement.astStatement((Statement) toTree(node.thenStatement));
		ifStatement.astElseStatement((Statement) toTree(node.elseStatement));
		set(node, setPosition(node, ifStatement));
	}
	
	@Override public void visitForStatement(ForStatement node) {
		For forStat = new lombok.ast.For();
		forStat.astCondition(toExpression(node.condition));
		forStat.astStatement((Statement)toTree(node.action));
		fillList(node.increments, forStat.rawUpdates());
		fillList(node.initializations, forStat.rawExpressionInits());
		//todo... variabledeclaration based...
		//forStat.astVariableDeclaration(node.)
		
		
		set(node, setPosition(node, forStat));
	}
	
	@Override public void visitLabeledStatement(LabeledStatement node) {
		LabelledStatement label = new lombok.ast.LabelledStatement();
		label.astLabel(toIdentifier(node.label));
		label.astStatement((Statement) toTree(node.statement));
		set(node, setPosition(node, label));
	}
	

	@Override public void visitContinueStatement(ContinueStatement node) {
		Continue cnt = new Continue();
		if (node.label != null) cnt.astLabel(toIdentifier(node.label)); 
		set(node, setPosition(node, cnt));
	}
	
	@Override public void visitBreakStatement(BreakStatement node) {
		Break brk = new Break();
		if (node.label != null) brk.astLabel(toIdentifier(node.label)); 
		set(node, setPosition(node, brk));
	}

	
	private lombok.ast.UnaryExpression fillUnaryOperator(CompoundAssignment ecjNode, lombok.ast.UnaryExpression node) {
		if (ecjNode instanceof PrefixExpression) {
			return node.astOperator(UNARY_PREFIX_OPERATORS.get(ecjNode.operator));
		}
		if (ecjNode instanceof PostfixExpression) {
			return node.astOperator(UNARY_POSTFIX_OPERATORS.get(ecjNode.operator));
		}
		return node;
	}
	
	static final Map<Integer, UnaryOperator> UNARY_PREFIX_OPERATORS = Maps.newHashMap();
	static {
		UNARY_PREFIX_OPERATORS.put(OperatorIds.PLUS, UnaryOperator.PREFIX_INCREMENT); 
		UNARY_PREFIX_OPERATORS.put(OperatorIds.MINUS, UnaryOperator.PREFIX_DECREMENT); 
	}
	static final Map<Integer, UnaryOperator> UNARY_POSTFIX_OPERATORS = Maps.newHashMap();
	static {
		UNARY_POSTFIX_OPERATORS.put(OperatorIds.PLUS, UnaryOperator.POSTFIX_INCREMENT); 
		UNARY_POSTFIX_OPERATORS.put(OperatorIds.MINUS, UnaryOperator.POSTFIX_DECREMENT);
	}
	
	static final Map<Integer, UnaryOperator> GENERIC_UNARY_OPERATORS = Maps.newHashMap();
	static {
		GENERIC_UNARY_OPERATORS.put(OperatorIds.TWIDDLE, UnaryOperator.BINARY_NOT);
		GENERIC_UNARY_OPERATORS.put(OperatorIds.NOT, UnaryOperator.LOGICAL_NOT); 
		GENERIC_UNARY_OPERATORS.put(OperatorIds.PLUS, UnaryOperator.UNARY_PLUS);
		GENERIC_UNARY_OPERATORS.put(OperatorIds.MINUS, UnaryOperator.UNARY_MINUS);
	}
	
	static final Map<Integer, BinaryOperator> GENERIC_BINARY_OPERATORS = Maps.newHashMap();
	static {
		GENERIC_BINARY_OPERATORS.put(OperatorIds.OR_OR, BinaryOperator.LOGICAL_OR);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.AND_AND, BinaryOperator.LOGICAL_AND);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.OR, BinaryOperator.BITWISE_OR);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.XOR, BinaryOperator.BITWISE_XOR);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.AND, BinaryOperator.BITWISE_AND);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.EQUAL_EQUAL, BinaryOperator.EQUALS);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.NOT_EQUAL, BinaryOperator.NOT_EQUALS);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.GREATER, BinaryOperator.GREATER);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.GREATER_EQUAL, BinaryOperator.GREATER_OR_EQUAL);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.LESS, BinaryOperator.LESS);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.LESS_EQUAL, BinaryOperator.LESS_OR_EQUAL);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.LEFT_SHIFT, BinaryOperator.SHIFT_LEFT);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.RIGHT_SHIFT, BinaryOperator.SHIFT_RIGHT);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.UNSIGNED_RIGHT_SHIFT, BinaryOperator.BITWISE_SHIFT_RIGHT);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.PLUS, BinaryOperator.PLUS);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.MINUS, BinaryOperator.MINUS);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.MULTIPLY, BinaryOperator.MULTIPLY);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.DIVIDE, BinaryOperator.DIVIDE);
		GENERIC_BINARY_OPERATORS.put(OperatorIds.REMAINDER, BinaryOperator.REMAINDER);
	}
	
	static final Map<Integer, BinaryOperator> ASSIGN_BINARY_OPERATORS = Maps.newHashMap();
	static {
		ASSIGN_BINARY_OPERATORS.put(OperatorIds.PLUS, BinaryOperator.PLUS_ASSIGN);
		ASSIGN_BINARY_OPERATORS.put(OperatorIds.MINUS, BinaryOperator.MINUS_ASSIGN);
		ASSIGN_BINARY_OPERATORS.put(OperatorIds.MULTIPLY, BinaryOperator.MULTIPLY_ASSIGN);
		ASSIGN_BINARY_OPERATORS.put(OperatorIds.DIVIDE, BinaryOperator.DIVIDE_ASSIGN);
		ASSIGN_BINARY_OPERATORS.put(OperatorIds.REMAINDER, BinaryOperator.REMAINDER_ASSIGN);
		ASSIGN_BINARY_OPERATORS.put(OperatorIds.AND, BinaryOperator.AND_ASSIGN);
		ASSIGN_BINARY_OPERATORS.put(OperatorIds.XOR, BinaryOperator.XOR_ASSIGN);
		ASSIGN_BINARY_OPERATORS.put(OperatorIds.OR, BinaryOperator.OR_ASSIGN);
		ASSIGN_BINARY_OPERATORS.put(OperatorIds.LEFT_SHIFT, BinaryOperator.SHIFT_LEFT_ASSIGN);
		ASSIGN_BINARY_OPERATORS.put(OperatorIds.RIGHT_SHIFT, BinaryOperator.SHIFT_RIGHT_ASSIGN);
		ASSIGN_BINARY_OPERATORS.put(OperatorIds.UNSIGNED_RIGHT_SHIFT, BinaryOperator.BITWISE_SHIFT_RIGHT_ASSIGN);
	}
	
	
}