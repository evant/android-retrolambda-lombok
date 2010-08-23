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

import lombok.ast.BooleanLiteral;
import lombok.ast.FloatingPointLiteral;
import lombok.ast.Identifier;
import lombok.ast.IntegralLiteral;
import lombok.ast.KeywordModifier;
import lombok.ast.LiteralType;
import lombok.ast.Node;
import lombok.ast.NormalTypeBody;
import lombok.ast.Position;
import lombok.ast.RawListAccessor;
import lombok.ast.StrictListAccessor;
import lombok.ast.TypeReferencePart;
import lombok.ast.TypeVariable;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.WildcardKind;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.jdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FloatLiteral;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LongLiteral;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class EcjTreeConverter extends EcjTreeVisitor {
	private enum FlagKey {
		IMPORTDECLARATION_IS_PACKAGE;
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
	
	private void fillList(ASTNode[] nodes, RawListAccessor<?, ?> list) {
		if (nodes == null) return;
		
		for (ASTNode node : nodes) list.addToEnd(toTree(node));
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
	
	private void fillIdentifiers(char[][] tokens, StrictListAccessor<lombok.ast.Identifier, ?> list) {
		if (tokens == null) return;
		for (char[] token : tokens) list.addToEnd(toIdentifier(token));
	}

	private void setPosition(ASTNode node, lombok.ast.Node lombokNode) {
		lombokNode.setPosition(new Position(node.sourceStart, node.sourceEnd));
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
				
				NormalTypeBody body = new NormalTypeBody();
				fillList(node.fields, body.rawMembers());
				fillList(node.typeParameters, decl.rawTypeVariables());
				
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
	
	@Override public void visitLocalDeclaration(LocalDeclaration node) {
		VariableDeclaration decl = new VariableDeclaration();
		
		VariableDefinition varDef = new VariableDefinition();
		varDef.astModifiers(toModifiers(node.modifiers,node.annotations));
		varDef.astTypeReference((lombok.ast.TypeReference) toTree(node.type));
		decl.astDefinition(varDef);
		
		VariableDefinitionEntry varDefEntry = new VariableDefinitionEntry();
		varDefEntry.astInitializer((lombok.ast.Expression)toTree(node.initialization));
		varDefEntry.astName(toIdentifier(node.name));
		
		varDef.astVariables().addToEnd(varDefEntry);
		set(node, decl);
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

	private TypeReferencePart createTypeReferencePart(char[] token, TypeReference[] typeReferences) {
		TypeReferencePart part = new TypeReferencePart();
		part.astIdentifier(toIdentifier(token));
		if (typeReferences != null) fillList(typeReferences, part.rawTypeArguments());
		return part;
	}
	
	@Override public void visitParameterizedQualifiedTypeReference(ParameterizedQualifiedTypeReference node) {
		lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
		fillTypeReferenceParts(node.tokens, node.typeArguments, ref.astParts());
		set(node, ref);
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
	
	@Override public void visitArrayTypeReference(ArrayTypeReference node) {
		lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
		TypeReferencePart part = new TypeReferencePart();
		part.astIdentifier(toIdentifier(node.token));
		ref.astParts().addToEnd(part);
		ref.astArrayDimensions(node.dimensions);
		set(node, ref);
	}
	
	@Override public void visitArrayQualifiedTypeReference(ArrayQualifiedTypeReference node) {
		lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
		ref.astArrayDimensions(node.dimensions());
		fillTypeReferenceParts(node.tokens, ref.astParts());
		set(node, ref);
	}
	
	@Override public void visitIntLiteral(IntLiteral node) {
		IntegralLiteral literal = new IntegralLiteral();
		literal.rawValue(String.valueOf(node.source()));
		set(node, literal);
	}
	
	@Override public void visitLongLiteral(LongLiteral node) {
		IntegralLiteral literal = new IntegralLiteral();
		literal.rawValue(String.valueOf(node.source()));
		setPosition(node, literal);
		set(node, literal);
	}
	
	@Override public void visitFloatLiteral(FloatLiteral node) {
		FloatingPointLiteral literal = new FloatingPointLiteral();
		literal.rawValue(String.valueOf(node.source()));
		setPosition(node, literal);
		set(node, literal);
	}
	
	@Override public void visitDoubleLiteral(DoubleLiteral node) {
		FloatingPointLiteral literal = new FloatingPointLiteral();
		literal.rawValue(String.valueOf(node.source()));
		setPosition(node, literal);
		set(node, literal);
	}
	
	@Override public void visitTrueLiteral(TrueLiteral node) {
		BooleanLiteral literal = new BooleanLiteral();
		literal.astValue(true);
		setPosition(node, literal);
		set(node, literal);
	}
	
	@Override public void visitFalseLiteral(FalseLiteral node) {
		BooleanLiteral literal = new BooleanLiteral();
		literal.astValue(false);
		setPosition(node, literal);
		set(node, literal);
	}
	
	@Override public void visitNullLiteral(NullLiteral node) {
		lombok.ast.NullLiteral literal = new lombok.ast.NullLiteral();
		setPosition(node, literal);
		set(node, literal);
	}
}