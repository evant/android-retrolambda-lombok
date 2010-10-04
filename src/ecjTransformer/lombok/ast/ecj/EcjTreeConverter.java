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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.ast.BinaryOperator;
import lombok.ast.KeywordModifier;
import lombok.ast.Node;
import lombok.ast.Position;
import lombok.ast.RawListAccessor;
import lombok.ast.StrictListAccessor;
import lombok.ast.UnaryOperator;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
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
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
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
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.StringLiteralConcatenation;
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
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class EcjTreeConverter {
	private enum FlagKey {
		IMPORTDECLARATION_IS_PACKAGE,
		NAMEREFERENCE_IS_TYPE,
		AS_STATEMENT,
		AS_DEFINITION,
		AS_ENUM,
	}
	
	private List<? extends Node> result = null;
	private Map<FlagKey, Object> params = ImmutableMap.of();
	private Map<PosInfoKey, Position> positionInfo = Maps.newHashMap();
	
	private static final Comparator<ASTNode> ASTNODE_ORDER = new Comparator<ASTNode>() {
		@Override public int compare(ASTNode nodeOne, ASTNode nodeTwo) {
			return nodeOne.sourceStart - nodeTwo.sourceStart;
		}
	};
	
	private boolean hasFlag(FlagKey key) {
		return params.containsKey(key);
	}
	
	@SuppressWarnings("unused")
	private Object getFlag(FlagKey key) {
		return params.get(key);
	}
	
	public List<? extends Node> getAll() {
		return result;
	}
	
	public Node get() {
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
			lombok.ast.ExpressionStatement stat = new lombok.ast.ExpressionStatement();
			stat.astExpression((lombok.ast.Expression)value);
			int start = node.sourceStart;
			int end = node.sourceEnd;
			try {
				end = (Integer)node.getClass().getField("statementEnd").get(node);
			} catch (Exception e) {
				// Not all these classes may have a statementEnd.
			}
			
			set(node, stat.setPosition(new Position(start, end + 1)));
			return;
		}
		
		if (value instanceof lombok.ast.Expression) {
			int parenCount = (node.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
			for (int i = 0; i < parenCount; i++) {
				((lombok.ast.Expression) value).astParensPositions().add(value.getPosition());
			}
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
		EcjTreeConverter newConverter = new EcjTreeConverter();
		if (params != null) newConverter.params = params;
		newConverter.positionInfo = positionInfo;
		newConverter.visit(node);
		try {
			return newConverter.get();
		} catch (RuntimeException e) {
			System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
			throw e;
		}
	}
	
	private void setPosInfo(Node lombokNode, String key, Position info) {
		positionInfo.put(new PosInfoKey(lombokNode, key), info);
	}
	
	public void transferPositionInfo(EcjTreeBuilder builder) {
		builder.setEcjTreeConverterPositionInfo(positionInfo);
	}
	
	private void fillList(ASTNode[] nodes, RawListAccessor<?, ?> list, FlagKey... keys) {
		if (nodes == null) return;
		
		for (ASTNode node : nodes) list.addToEnd(toTree(node, keys));
	}
	
	private void fillUtilityList(List<ASTNode> list, ASTNode... nodes) {
		if (nodes == null || nodes.length == 0) return;
		for (ASTNode statement : nodes) if (statement != null) list.add(statement);
	}
	
	public void visit(ASTNode node) {
		visitor.visitEcjNode(node);
	}
	
	private lombok.ast.Identifier toIdentifier(char[] token, long pos) {
		return toIdentifier(token, toPosition(pos));
	}
	
	private lombok.ast.Identifier toIdentifier(char[] token, int start, int end) {
		return toIdentifier(token, toPosition(start, end));
	}
	
	private lombok.ast.Identifier toIdentifier(char[] token, Position pos) {
		lombok.ast.Identifier id = lombok.ast.Identifier.of(token == null ? "" : new String(token));
		id.setPosition(pos);
		return id;
	}
	
	private Position toPosition(int start, int end) {
		return new Position(start, end + 1);
	}
	
	private Position toPosition(long pos) {
		return new Position((int) (pos >> 32), (int) (pos & 0xFFFFFFFFL) + 1);
	}
	
	private long toLong(int start, int end) {
		return (((long) start) << 32) | (0xFFFFFFFFL & end);
	}
	
	private lombok.ast.Modifiers toModifiers(int modifiers, Annotation[] annotations, int start, int end) {
		lombok.ast.Modifiers m = new lombok.ast.Modifiers();
		for (KeywordModifier mod : KeywordModifier.fromReflectModifiers(modifiers)) m.astKeywords().addToEnd(mod);
		fillList(annotations, m.rawAnnotations());
		m.setPosition(new Position(start, end));
		return m;
	}
	
	private lombok.ast.Block toBlock(Statement[] statements) {
		lombok.ast.Block block = new lombok.ast.Block();
		fillList(statements, block.rawContents(), FlagKey.AS_STATEMENT);
		return block;
	}
	
	private void fillDimensions(Expression[] nodes, RawListAccessor<lombok.ast.ArrayDimension, lombok.ast.ArrayCreation> list) {
		if (nodes == null) return;
		
		for (Expression node : nodes) list.addToEnd(new lombok.ast.ArrayDimension().astDimension((lombok.ast.Expression) toTree(node)));
	}
	
	private void fillIdentifiers(char[][] tokens, long[] positions, StrictListAccessor<lombok.ast.Identifier, ?> list) {
		if (tokens == null) return;
		if (positions.length != tokens.length) throw new IllegalStateException("bug");
		
		for (int i = 0; i < positions.length; i++) {
			list.addToEnd(toIdentifier(tokens[i], positions[i]));
		}
	}
	
	private <N extends lombok.ast.Node> N setPosition(ASTNode node, N lombokNode) {
		lombokNode.setPosition(toPosition(node.sourceStart, node.sourceEnd));
		return lombokNode;
	}
	
	private final EcjTreeVisitor visitor = new EcjTreeVisitor() {
		@Override public void visitCompilationUnitDeclaration(CompilationUnitDeclaration node) {
			lombok.ast.CompilationUnit unit = new lombok.ast.CompilationUnit();
			unit.rawPackageDeclaration(toTree(node.currentPackage, FlagKey.IMPORTDECLARATION_IS_PACKAGE));
			fillList(node.imports, unit.rawImportDeclarations());
			
			TypeDeclaration[] newTypes = null;
			if (node.types != null && node.types.length > 0 && CharOperation.equals(EcjTreeBuilder.PACKAGE_INFO, node.types[0].name)) {
				newTypes = new TypeDeclaration[node.types.length - 1];
				System.arraycopy(node.types, 1, newTypes, 0, node.types.length - 1);
			} else {
				newTypes = node.types;
			}
			
			fillList(newTypes, unit.rawTypeDeclarations());
			set(node, unit);
		}
		
		@Override public void visitImportReference(ImportReference node) {
			if (hasFlag(FlagKey.IMPORTDECLARATION_IS_PACKAGE)) {
				lombok.ast.PackageDeclaration pkg = new lombok.ast.PackageDeclaration();
				fillIdentifiers(node.tokens, node.sourcePositions, pkg.astParts());
				fillList(node.annotations, pkg.rawAnnotations());
				pkg.setPosition(toPosition(node.declarationSourceStart, node.declarationSourceEnd));
				set(node, pkg);
				return;
			}
			
			lombok.ast.ImportDeclaration imp = new lombok.ast.ImportDeclaration();
			fillIdentifiers(node.tokens, node.sourcePositions, imp.astParts());
			imp.astStarImport((node.bits & ASTNode.OnDemand) != 0);
			imp.astStaticImport((node.modifiers & ClassFileConstants.AccStatic) != 0);
			imp.setPosition(toPosition(node.declarationSourceStart, node.declarationSourceEnd));
			set(node, imp);
		}
		
		@Override public void visitInitializer(Initializer node) {
			if ((node.modifiers & ClassFileConstants.AccStatic) != 0) {
				lombok.ast.StaticInitializer staticInit = new lombok.ast.StaticInitializer();
				staticInit.astBody((lombok.ast.Block) toTree(node.block));
				staticInit.setPosition(toPosition(node.declarationSourceStart, node.sourceEnd));
				set(node, staticInit);
				return;
			} else {
				lombok.ast.InstanceInitializer instanceInit = new lombok.ast.InstanceInitializer();
				instanceInit.astBody((lombok.ast.Block) toTree(node.block));
				set(node, setPosition(node, instanceInit));
				return;
			}
		}
		
		@Override 
		public void visitTypeDeclaration(TypeDeclaration node) {
			lombok.ast.TypeDeclaration decl = null;
			switch (TypeDeclaration.kind(node.modifiers)) {
				case TypeDeclaration.CLASS_DECL: {
					lombok.ast.ClassDeclaration cDecl = new lombok.ast.ClassDeclaration();
					
					cDecl.rawExtending(toTree(node.superclass));
					cDecl.astBody(createNormalTypeBody(node));
					fillList(node.superInterfaces, cDecl.rawImplementing());
					fillList(node.typeParameters, cDecl.rawTypeVariables());
					
					decl = cDecl;
					break;
				}
				case TypeDeclaration.INTERFACE_DECL: {
					lombok.ast.InterfaceDeclaration iDecl = new lombok.ast.InterfaceDeclaration();
					iDecl.astBody(createNormalTypeBody(node));
					fillList(node.superInterfaces, iDecl.rawExtending());
					fillList(node.typeParameters, iDecl.rawTypeVariables());
					
					decl = iDecl;
					break;
				}
				case TypeDeclaration.ENUM_DECL: {
					lombok.ast.EnumDeclaration eDecl = new lombok.ast.EnumDeclaration();
					lombok.ast.EnumTypeBody enumTypeBody = createEnumTypeBody(node);
					
					fillList(node.superInterfaces, eDecl.rawImplementing());
					eDecl.astBody(enumTypeBody);
					decl = eDecl;
					break;
				}
				case TypeDeclaration.ANNOTATION_TYPE_DECL: {
					lombok.ast.AnnotationDeclaration aDecl = new lombok.ast.AnnotationDeclaration();
					aDecl.astBody(createNormalTypeBody(node));
					
					decl = aDecl;
					break;
				}
			}
			decl.astJavadoc((lombok.ast.Comment) toTree(node.javadoc));
			decl.astModifiers(toModifiers(node.modifiers, node.annotations, node.modifiersSourceStart, node.declarationSourceStart));
			decl.astName(toIdentifier(node.name, node.sourceStart, node.sourceEnd));
			decl.setPosition(toPosition(node.declarationSourceStart, node.declarationSourceEnd));
			
			set(node, decl);
			return;
		}
		
		private lombok.ast.EnumTypeBody createEnumTypeBody(TypeDeclaration node) {
			lombok.ast.EnumTypeBody body = new lombok.ast.EnumTypeBody();
			List<ASTNode> orderedList = createOrderedMemberList(node);
			fillList(orderedList.toArray(new ASTNode[0]), body.rawMembers());
			fillList(node.fields, body.rawConstants(), FlagKey.AS_ENUM);
			body.setPosition(toPosition(node.bodyStart - 1, node.bodyEnd));
			return body;
		}
		
		private List<ASTNode> createOrderedMemberList(TypeDeclaration node) {
			List<ASTNode> orderedList = new ArrayList<ASTNode>();
			fillUtilityList(orderedList, node.fields);
			fillUtilityList(orderedList, node.methods);
			fillUtilityList(orderedList, node.memberTypes);
			Collections.sort(orderedList, ASTNODE_ORDER);
			return orderedList;
		}
		
		private lombok.ast.NormalTypeBody createNormalTypeBody(TypeDeclaration node) {
			lombok.ast.NormalTypeBody body = new lombok.ast.NormalTypeBody();
			List<ASTNode> orderedList = createOrderedMemberList(node);
			fillList(orderedList.toArray(new ASTNode[0]), body.rawMembers());
			body.setPosition(toPosition(node.bodyStart - 1, node.bodyEnd));
			return body;
		}
		
		@Override public void visitTypeParameter(TypeParameter node) {
			lombok.ast.TypeVariable var = new lombok.ast.TypeVariable();
			var.astName(toIdentifier(node.name, node.sourceStart, node.sourceEnd));
			var.astExtending().addToEnd((lombok.ast.TypeReference)toTree(node.type));
			fillList(node.bounds, var.rawExtending());
			
			setPosition(node, var);
			set(node, var);
		}
		
		@Override public void visitEmptyStatement(EmptyStatement node) {
			lombok.ast.EmptyStatement statement = new lombok.ast.EmptyStatement();
			setPosition(node, statement);
			set(node, statement);
		}
		
		@Override public void visitLocalDeclaration(LocalDeclaration node) {
			handleAbstractVariableDefinition(node);
		}
		
		@Override public void visitFieldDeclaration(FieldDeclaration node) {
			if (hasFlag(FlagKey.AS_ENUM) && node.initialization instanceof AllocationExpression) {
				handleEnumConstant(node);
				return;
			}
			if(!hasFlag(FlagKey.AS_ENUM) && !(node.initialization instanceof AllocationExpression)) {
				handleAbstractVariableDefinition(node);
				return;
			}
			/*
			 * Either an enumconstant during fieldparsing, or a field during enumconstant parsing
			 */
			set(node, (Node) null);
		}
		
		@Override public void visitFieldReference(FieldReference node) {
			lombok.ast.Select select = new lombok.ast.Select();
			select.astIdentifier(toIdentifier(node.token, node.sourceStart, node.sourceEnd));
			select.astOperand((lombok.ast.Expression) toTree(node.receiver));
			
			set(node, select);
		}
		
		private void handleEnumConstant(FieldDeclaration node) {
			AllocationExpression init = (AllocationExpression)node.initialization;
			
			lombok.ast.EnumConstant constant = new lombok.ast.EnumConstant();
			constant.astJavadoc((lombok.ast.Comment) toTree(node.javadoc));
			constant.astName(toIdentifier(node.name, node.sourceStart, node.sourceEnd));
			fillList(init.arguments, constant.rawArguments());
			fillList(node.annotations, constant.rawAnnotations());
			
			if (node.initialization instanceof QualifiedAllocationExpression) {
				QualifiedAllocationExpression qualifiedNode = ((QualifiedAllocationExpression)node.initialization);
				lombok.ast.NormalTypeBody body = createNormalTypeBody(qualifiedNode.anonymousType);
				constant.astBody(body);
			}
			
			set(node, constant);
		}
		
		public void handleAbstractVariableDefinition(AbstractVariableDeclaration node) {
			lombok.ast.VariableDefinition varDef = createVariableDefinition(node);
			varDef.setPosition(toPosition(node.declarationSourceStart, node.sourceEnd));
			if (hasFlag(FlagKey.AS_DEFINITION)) {
				set(node, varDef);
				return;
			}
			
			lombok.ast.VariableDeclaration decl = new lombok.ast.VariableDeclaration();
			if (node instanceof FieldDeclaration) {
				decl.astJavadoc((lombok.ast.Comment) toTree(((FieldDeclaration)node).javadoc));
			}
			
			decl.astDefinition(varDef);
			decl.setPosition(toPosition(node.declarationSourceStart, node.declarationSourceEnd));
			
			set(node, decl);
		}
		
		private lombok.ast.VariableDefinition createVariableDefinition(AbstractVariableDeclaration node) {
			lombok.ast.VariableDefinition varDef = new lombok.ast.VariableDefinition();
			varDef.astModifiers(toModifiers(node.modifiers, node.annotations, node.modifiersSourceStart, node.declarationSourceStart));
			varDef.astTypeReference((lombok.ast.TypeReference) toTree(node.type));
			varDef.astVarargs((node.type.bits & ASTNode.IsVarArgs) != 0);
			
			lombok.ast.VariableDefinitionEntry varDefEntry = new lombok.ast.VariableDefinitionEntry();
			varDefEntry.astInitializer((lombok.ast.Expression) toTree(node.initialization));
			varDefEntry.astName(toIdentifier(node.name, node.sourceStart, node.sourceEnd));
			varDef.astVariables().addToEnd(varDefEntry);
			return varDef;
		}
		
		@Override public void visitBlock(Block node) {
			set(node, setPosition(node, toBlock(node.statements)));
		}
		
		@Override public void visitSingleTypeReference(SingleTypeReference node) {
			lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
			ref.astParts().addToEnd(createSingleTypeReferencePart(node));
			setPosition(node, ref);
			set(node, ref);
		}
		
		private lombok.ast.TypeReferencePart createSingleTypeReferencePart(SingleTypeReference node) {
			lombok.ast.TypeReferencePart part = new lombok.ast.TypeReferencePart();
			part.astIdentifier(toIdentifier(node.token, node.sourceStart, node.sourceEnd));
			part.setPosition(part.astIdentifier().getPosition());
			return part;
		}
		
		private lombok.ast.TypeReferencePart createParameterizedSingleTypeReferencePart(ParameterizedSingleTypeReference node) {
			lombok.ast.TypeReferencePart part = createTypeReferencePart(node.token, toLong(node.sourceStart, node.sourceEnd), node.typeArguments);
			return part;
		}
		
		private void fillTypeReferenceParts(char[][] tokens, long[] positions, StrictListAccessor<lombok.ast.TypeReferencePart, ?> list) {
			if (tokens == null) return;
			if (tokens.length != positions.length) throw new IllegalStateException("bug");
			
			for (int i = 0; i < tokens.length; i++) {
				lombok.ast.TypeReferencePart part = new lombok.ast.TypeReferencePart();
				part.astIdentifier(toIdentifier(tokens[i], positions[i]));
				list.addToEnd(part);
			}
		}
		
		@Override public void visitQualifiedTypeReference(QualifiedTypeReference node) {
			lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
			fillTypeReferenceParts(node.tokens, node.sourcePositions, ref.astParts());
			set(node, ref);
		}
		
		private void fillTypeReferenceParts(char[][] tokens, long[] positions, TypeReference[][] typeArguments, StrictListAccessor<lombok.ast.TypeReferencePart, ?> list) {
			if (tokens == null) return;
			if (tokens.length != positions.length) throw new IllegalStateException("bug");
			for (int i = 0; i < typeArguments.length; i++) {
				TypeReference[] typeReferences = typeArguments[i];
				lombok.ast.TypeReferencePart part = createTypeReferencePart(tokens[i], positions[i], typeReferences);
				list.addToEnd(part);
			}
		}
		
		private lombok.ast.TypeReferencePart createTypeReferencePart(char[] token, long pos) {
			return createTypeReferencePart(token, pos, null);
		}
		
		private lombok.ast.TypeReferencePart createTypeReferencePart(char[] token, long pos, TypeReference[] typeReferences) {
			lombok.ast.TypeReferencePart part = new lombok.ast.TypeReferencePart();
			part.astIdentifier(toIdentifier(token, pos));
			if (typeReferences != null) fillList(typeReferences, part.rawTypeArguments());
			part.setPosition(toPosition(pos));
			return part;
		}
		
		@Override public void visitParameterizedQualifiedTypeReference(ParameterizedQualifiedTypeReference node) {
			lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
			ref.astArrayDimensions(node.dimensions());
			fillTypeReferenceParts(node.tokens, node.sourcePositions, node.typeArguments, ref.astParts());
			set(node, setPosition(node, ref));
		}
		
		@Override public void visitWildcard(Wildcard node) {
			lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
			
			lombok.ast.TypeReferencePart part;
			if (node.bound instanceof ParameterizedSingleTypeReference) {
				part = createParameterizedSingleTypeReferencePart((ParameterizedSingleTypeReference)node.bound);
			} else if (node.bound instanceof SingleTypeReference) {
				part = createSingleTypeReferencePart((SingleTypeReference)node.bound);
			} else {
				part = new lombok.ast.TypeReferencePart();
			}
			
			ref.astParts().addToEnd(part);
			
			switch (node.kind) {
			case Wildcard.UNBOUND: 
				ref.astWildcard(lombok.ast.WildcardKind.UNBOUND);
				break;
			case Wildcard.EXTENDS: 
				ref.astWildcard(lombok.ast.WildcardKind.EXTENDS);
				break;
			case Wildcard.SUPER:
				ref.astWildcard(lombok.ast.WildcardKind.SUPER);
			}
			setPosition(node, ref);
			set(node, ref);
		}
		
		@Override public void visitParameterizedSingleTypeReference(ParameterizedSingleTypeReference node) {
			lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
			lombok.ast.TypeReferencePart part = new lombok.ast.TypeReferencePart();
			part.astIdentifier(toIdentifier(node.token, node.sourceStart, node.sourceEnd));
			ref.astParts().addToEnd(part);
			fillList(node.typeArguments, part.rawTypeArguments());
			set(node, ref);
		}
		
		@Override public void visitArrayTypeReference(ArrayTypeReference node) {
			lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
			ref.astArrayDimensions(((node.bits & ASTNode.IsVarArgs) == 0) ? node.dimensions : node.dimensions - 1);
			lombok.ast.TypeReferencePart part = new lombok.ast.TypeReferencePart();
			part.astIdentifier(toIdentifier(node.token, node.sourceStart, node.sourceEnd));
			ref.astParts().addToEnd(part);
			set(node, setPosition(node, ref));
		}
		
		@Override public void visitArrayQualifiedTypeReference(ArrayQualifiedTypeReference node) {
			lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
			fillTypeReferenceParts(node.tokens, node.sourcePositions, ref.astParts());
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
			lombok.ast.IntegralLiteral integral = new lombok.ast.IntegralLiteral().rawValue(negative ? rawValue.substring(1) : rawValue);
			set(node, setPosition(node, addUnaryMinusAsParent(negative, integral)));
		}
		
		@Override public void visitIntLiteralMinValue(IntLiteralMinValue node) {
			visitIntLiteral(node);
		}
		
		@Override public void visitLongLiteral(LongLiteral node) {
			String rawValue = String.valueOf(node.source());
			boolean negative = rawValue.startsWith("-");
			lombok.ast.IntegralLiteral integral = new lombok.ast.IntegralLiteral().rawValue(negative ? rawValue.substring(1) : rawValue);
			set(node, setPosition(node, addUnaryMinusAsParent(negative, integral)));
		}
		
		@Override public void visitLongLiteralMinValue(LongLiteralMinValue node) {
			visitLongLiteral(node);
		}
		
		@Override public void visitFloatLiteral(FloatLiteral node) {
			set(node, setPosition(node, new lombok.ast.FloatingPointLiteral().rawValue(String.valueOf(node.source()))));
		}
		
		@Override public void visitDoubleLiteral(DoubleLiteral node) {
			set(node, setPosition(node, new lombok.ast.FloatingPointLiteral().rawValue(String.valueOf(node.source()))));
		}
		
		@Override public void visitTrueLiteral(TrueLiteral node) {
			set(node, setPosition(node, new lombok.ast.BooleanLiteral().astValue(true)));
		}
		
		@Override public void visitFalseLiteral(FalseLiteral node) {
			set(node, setPosition(node, new lombok.ast.BooleanLiteral().astValue(false)));
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
		
		@Override public void visitStringLiteralConcatenation(StringLiteralConcatenation node) {
			Node lombokAggregator = null;
			
			if (node.literals != null) {
				for (int i = 0; i < node.counter; i++) {
					Node lombokElemNode = toTree(node.literals[i]);
					if (lombokAggregator != null) {
						Position newPos = new Position(lombokAggregator.getPosition().getStart(), lombokElemNode.getPosition().getEnd());
						lombokAggregator = new lombok.ast.BinaryExpression().astOperator(BinaryOperator.PLUS)
								.rawLeft(lombokAggregator).rawRight(lombokElemNode);
						lombokAggregator.setPosition(newPos);
					} else {
						lombokAggregator = lombokElemNode;
					}
				}
			}
			
			set(node, setPosition(node, lombokAggregator));
		}
		
		@Override public void visitExtendedStringLiteral(ExtendedStringLiteral node) {
			// While there's a node for it, this node has no further information about the separate parts,
			// so we are forced to produce a single string literal.
			
			visitStringLiteral(node);
		}
		
		@Override public void visitSingleNameReference(SingleNameReference node) {
			if (hasFlag(FlagKey.NAMEREFERENCE_IS_TYPE)) {
				set(node, setPosition(node, new lombok.ast.TypeReference().astParts().addToEnd(createTypeReferencePart(node.token, toLong(node.sourceStart, node.sourceEnd)))));
				return;
			}
			set(node, setPosition(node, new lombok.ast.VariableReference().astIdentifier(toIdentifier(node.token, node.sourceStart, node.sourceEnd))));
		}
		
		@Override public void visitCastExpression(CastExpression node) {
			Node result = toTree(node.type, FlagKey.NAMEREFERENCE_IS_TYPE);
			lombok.ast.Cast cast = new lombok.ast.Cast().astTypeReference((lombok.ast.TypeReference) result);
			cast.astOperand((lombok.ast.Expression)toTree(node.expression));
			setPosInfo(cast, "type", new Position(node.type.sourceStart, node.type.sourceEnd));
			set(node, setPosition(node, cast));
		}
		
		@Override public void visitThisReference(ThisReference node) {
			set(node, node.isImplicitThis() ? null : setPosition(node, new lombok.ast.This()));
		}
		
		@Override public void visitQualifiedThisReference(QualifiedThisReference node) {
			set(node, setPosition(node, new lombok.ast.This().astQualifier((lombok.ast.TypeReference) toTree(node.qualification))));
		}
		
		@Override public void visitSuperReference(SuperReference node) {
			set(node, setPosition(node, new lombok.ast.Super()));
		}
		
		@Override public void visitQualifiedSuperReference(QualifiedSuperReference node) {
			set(node, setPosition(node, new lombok.ast.Super().astQualifier((lombok.ast.TypeReference) toTree(node.qualification))));
		}
		
		@Override public void visitClassLiteralAccess(ClassLiteralAccess node) {
			lombok.ast.ClassLiteral literal = new lombok.ast.ClassLiteral().astTypeReference((lombok.ast.TypeReference) toTree(node.type));
			set(node, setPosition(node, literal));
		}
		
		@Override public void visitArrayAllocationExpression(ArrayAllocationExpression node) {
			lombok.ast.ArrayCreation creation = new lombok.ast.ArrayCreation();
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
			lombok.ast.BinaryExpression bin = new lombok.ast.BinaryExpression();
			bin.astLeft((lombok.ast.Expression) toTree(node.lhs));
			bin.astRight(((lombok.ast.Expression) toTree(node.expression)));
			bin.astOperator(BinaryOperator.ASSIGN);
			setPosition(node, bin);
			set(node, bin);
		}
		
		@Override public void visitArrayReference(ArrayReference node) {
			lombok.ast.ArrayAccess access = new lombok.ast.ArrayAccess();
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
			unary.astOperand(((lombok.ast.Expression) toTree(node.lhs)));
			set(node, setPosition(node, unary));
		}
		
		@Override public void visitBinaryExpression(BinaryExpression node) {
			lombok.ast.BinaryExpression bin = new lombok.ast.BinaryExpression();
			int operatorId = ((node.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT);
			bin.astOperator(GENERIC_BINARY_OPERATORS.get(operatorId));
			bin.astLeft(((lombok.ast.Expression) toTree(node.left)));
			bin.astRight(((lombok.ast.Expression) toTree(node.right)));
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
			lombok.ast.InstanceOf instanceOf = new lombok.ast.InstanceOf();
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
			lombok.ast.InlineIfExpression inlineIf = new lombok.ast.InlineIfExpression()
				.astCondition((lombok.ast.Expression) toTree(node.condition))
				.astIfTrue((lombok.ast.Expression) toTree(node.valueIfTrue))
				.astIfFalse((lombok.ast.Expression) toTree(node.valueIfFalse));
			
			set(node, setPosition(node, inlineIf));
		}
		
		@Override public void visitAllocationExpression(AllocationExpression node) {
			lombok.ast.ConstructorInvocation constr = new lombok.ast.ConstructorInvocation();
			constr.astTypeReference((lombok.ast.TypeReference) toTree(node.type));
			fillList(node.arguments, constr.rawArguments());
			fillList(node.typeArguments, constr.rawConstructorTypeArguments());
			
			set(node, setPosition(node, constr));
		}
		
		@Override public void visitQualifiedAllocationExpression(QualifiedAllocationExpression node) {
			lombok.ast.ConstructorInvocation constr = new lombok.ast.ConstructorInvocation();
			constr.astTypeReference((lombok.ast.TypeReference) toTree(node.type));
			lombok.ast.NormalTypeBody body = createNormalTypeBody(node.anonymousType);
			constr.astAnonymousClassBody(body);
			
			set(node, setPosition(node, constr));
		}
		
		private lombok.ast.Expression toSelect(char[][] tokens, long[] positions) {
			if (tokens.length < 2) return null;
			if (tokens.length != positions.length) throw new IllegalStateException("bug");
			
			lombok.ast.Identifier current0 = toIdentifier(tokens[0], positions[0]);
			lombok.ast.Expression current = new lombok.ast.VariableReference().astIdentifier(current0);
			current.setPosition(current0.getPosition());
			
			for (int i = 1; i < tokens.length; i++) {
				lombok.ast.Select select = new lombok.ast.Select().astIdentifier(toIdentifier(tokens[i], positions[i]));
				select.astOperand(current);
				current = select;
			}
			
			return current;
		}
		
		@Override public void visitQualifiedNameReference(QualifiedNameReference node) {
			if (hasFlag(FlagKey.NAMEREFERENCE_IS_TYPE)) {
				lombok.ast.TypeReference ref = new lombok.ast.TypeReference();
				fillTypeReferenceParts(node.tokens, node.sourcePositions, ref.astParts());
				set(node, setPosition(node, ref));
				return;
			}
			lombok.ast.Expression select = toSelect(node.tokens, node.sourcePositions);
			set(node, setPosition(node, select));
		}
		
		@Override public void visitMessageSend(MessageSend node) {
			lombok.ast.MethodInvocation inv = new lombok.ast.MethodInvocation();
			fillList(node.arguments, inv.rawArguments());
			fillList(node.typeArguments, inv.rawMethodTypeArguments());
			inv.astOperand(((lombok.ast.Expression) toTree(node.receiver)));
			inv.astName(toIdentifier(node.selector, node.nameSourcePosition));
			setPosition(node, inv);
			set(node, inv);
		}
		
		@Override public void visitAssertStatement(AssertStatement node) {
			lombok.ast.Assert asrt = new lombok.ast.Assert();
			asrt.astAssertion(((lombok.ast.Expression) toTree(node.assertExpression)));
			asrt.astMessage(((lombok.ast.Expression) toTree(node.exceptionArgument)));
			set(node, setPosition(node, asrt));
		}
		
		@Override public void visitDoStatement(DoStatement node) {
			lombok.ast.DoWhile doWhile = new lombok.ast.DoWhile();
			doWhile.astCondition(((lombok.ast.Expression) toTree(node.condition)));
			doWhile.astStatement((lombok.ast.Statement)toTree(node.action, FlagKey.AS_STATEMENT));
			set(node, setPosition(node, doWhile));
		}
		
		@Override public void visitForeachStatement(ForeachStatement node) {
			lombok.ast.ForEach forEach = new lombok.ast.ForEach();
			forEach.astIterable(((lombok.ast.Expression) toTree(node.collection)));
			forEach.astVariable((lombok.ast.VariableDefinition) toTree(node.elementVariable, FlagKey.AS_DEFINITION));
			forEach.astStatement((lombok.ast.Statement)toTree(node.action, FlagKey.AS_STATEMENT));
			set(node, setPosition(node, forEach));
		}
		
		@Override public void visitIfStatement(IfStatement node) {
			lombok.ast.If ifStatement = new lombok.ast.If().astCondition(((lombok.ast.Expression) toTree(node.condition)));
			ifStatement.astStatement((lombok.ast.Statement) toTree(node.thenStatement, FlagKey.AS_STATEMENT));
			ifStatement.astElseStatement((lombok.ast.Statement) toTree(node.elseStatement, FlagKey.AS_STATEMENT));
			set(node, setPosition(node, ifStatement));
		}
		
		@Override public void visitForStatement(ForStatement node) {
			lombok.ast.For forStat = new lombok.ast.For();
			forStat.astCondition(((lombok.ast.Expression) toTree(node.condition)));
			forStat.astStatement((lombok.ast.Statement) toTree(node.action, FlagKey.AS_STATEMENT));
			fillList(node.increments, forStat.rawUpdates());
			fillList(node.initializations, forStat.rawExpressionInits(), FlagKey.AS_DEFINITION);
			
			set(node, setPosition(node, forStat));
		}
		
		@Override public void visitLabeledStatement(LabeledStatement node) {
			lombok.ast.LabelledStatement label = new lombok.ast.LabelledStatement();
			label.astLabel(toIdentifier(node.label, node.sourceStart, node.labelEnd));
			label.astStatement((lombok.ast.Statement) toTree(node.statement, FlagKey.AS_STATEMENT));
			set(node, setPosition(node, label));
		}
		
		@Override public void visitContinueStatement(ContinueStatement node) {
			lombok.ast.Continue cnt = new lombok.ast.Continue();
			if (node.label != null) cnt.astLabel(toIdentifier(node.label, node.sourceStart, node.sourceEnd));
			set(node, setPosition(node, cnt));
		}
		
		@Override public void visitBreakStatement(BreakStatement node) {
			lombok.ast.Break brk = new lombok.ast.Break();
			if (node.label != null) brk.astLabel(toIdentifier(node.label, node.sourceStart, node.sourceEnd));
			set(node, setPosition(node, brk));
		}
		
		@Override public void visitSwitchStatement(SwitchStatement node) {
			lombok.ast.Switch switchStat = new lombok.ast.Switch();
			switchStat.astCondition((lombok.ast.Expression) toTree(node.expression));
			switchStat.astBody(toBlock(node.statements));
			set(node, setPosition(node, switchStat));
		}
		
		@Override public void visitCaseStatement(CaseStatement node) {
			if (node.constantExpression == null) {
				lombok.ast.Default defaultStat = new lombok.ast.Default();
				//TODO still have fix drunken positioning.
				set(node, setPosition(node, defaultStat));
				return;
			}
			lombok.ast.Case caseStat = new lombok.ast.Case();
			caseStat.astCondition((lombok.ast.Expression) toTree(node.constantExpression));
			set(node, setPosition(node, caseStat));
		}
	
		@Override public void visitSynchronizedStatement(SynchronizedStatement node) {
			lombok.ast.Synchronized synch = new lombok.ast.Synchronized();
			synch.astLock((lombok.ast.Expression) toTree(node.expression));
			synch.astBody((lombok.ast.Block) toTree(node.block));
			set(node, setPosition(node, synch));
		}
		
		@Override public void visitTryStatement(TryStatement node) {
			lombok.ast.Try tryStat = new lombok.ast.Try();
			tryStat.astBody((lombok.ast.Block) toTree(node.tryBlock));
			tryStat.astFinally((lombok.ast.Block) toTree(node.finallyBlock));
			
			toCatches(node.catchArguments, node.catchBlocks, tryStat.astCatches());
			set(node, setPosition(node, tryStat));
		}
		
		private void toCatches(Argument[] catchArguments, Block[] catchBlocks, StrictListAccessor<lombok.ast.Catch, lombok.ast.Try> astCatches) {
			if (catchArguments == null || catchBlocks == null || (catchBlocks.length != catchArguments.length)) {
				return;
			}
			
			for (int i = 0; i < catchBlocks.length; i++) {
				lombok.ast.Catch cat = new lombok.ast.Catch();
				cat.astExceptionDeclaration((lombok.ast.VariableDefinition) toTree(catchArguments[i]));
				cat.astBody((lombok.ast.Block) toTree(catchBlocks[i]));
				astCatches.addToEnd(cat);
			}
		}
		
		@Override public void visitArgument(Argument node) {
			lombok.ast.VariableDefinition varDef = createVariableDefinition(node);
			set(node, setPosition(node, varDef));
		}
		
		@Override public void visitThrowStatement(ThrowStatement node) {
			lombok.ast.Throw throwStat = new lombok.ast.Throw();
			throwStat.astThrowable((lombok.ast.Expression) toTree(node.exception));
			set(node, setPosition(node, throwStat));
		}
		
		@Override public void visitWhileStatement(WhileStatement node) {
			lombok.ast.While whileStat = new lombok.ast.While();
			whileStat.astCondition((lombok.ast.Expression) toTree(node.condition));
			whileStat.astStatement((lombok.ast.Statement) toTree(node.action, FlagKey.AS_STATEMENT));
			set(node, setPosition(node, whileStat));
		}
		
		@Override public void visitConstructorDeclaration(ConstructorDeclaration node) {
			if ((node.bits & ASTNode.IsDefaultConstructor) != 0) {
				set(node, (Node)null);
				return;
			}
			
			lombok.ast.ConstructorDeclaration constr = new lombok.ast.ConstructorDeclaration();
			constr.astTypeName(toIdentifier(node.selector, node.sourceStart, node.sourceEnd));
			lombok.ast.Block block = toBlock(node.statements);
			block.astContents().addToEnd((lombok.ast.Statement)toTree(node.constructorCall, FlagKey.AS_STATEMENT));
			constr.astBody(block);
			constr.astJavadoc((lombok.ast.Comment) toTree(node.javadoc));
			constr.astModifiers(toModifiers(node.modifiers, node.annotations, node.modifiersSourceStart, node.declarationSourceStart));
			fillList(node.arguments, constr.rawParameters());
			fillList(node.typeParameters, constr.rawTypeVariables());
			fillList(node.thrownExceptions, constr.rawThrownTypeReferences());
			set(node, setPosition(node, constr));
		}
		
		@Override public void visitExplicitConstructorCall(ExplicitConstructorCall node) {
			if (node.isImplicitSuper()) {
				set(node, (Node)null);	
				return;
			}
			
			if (node.isSuperAccess()) {
				lombok.ast.SuperConstructorInvocation sup = new lombok.ast.SuperConstructorInvocation();
				fillList(node.arguments, sup.rawArguments());
				fillList(node.typeArguments, sup.rawConstructorTypeArguments());
				sup.astQualifier((lombok.ast.Expression) toTree(node.qualification));
				set(node, setPosition(node, sup));
				return;
			}
			
			lombok.ast.AlternateConstructorInvocation inv = new lombok.ast.AlternateConstructorInvocation();
			fillList(node.arguments, inv.rawArguments());
			fillList(node.typeArguments, inv.rawConstructorTypeArguments());
			set(node, setPosition(node, inv));
		}
		
		@Override public void visitMethodDeclaration(MethodDeclaration node) {
			lombok.ast.MethodDeclaration decl = new lombok.ast.MethodDeclaration();
			decl.astMethodName(toIdentifier(node.selector, node.sourceStart, node.sourceEnd));
			decl.astJavadoc((lombok.ast.Comment) toTree(node.javadoc));
			lombok.ast.Modifiers modifiers = toModifiers(node.modifiers, node.annotations, node.modifiersSourceStart, node.declarationSourceStart);
			decl.astModifiers(modifiers);
			decl.astReturnTypeReference((lombok.ast.TypeReference) toTree(node.returnType));
			
			boolean semiColonBody = ((node.modifiers & ExtraCompilerModifiers.AccSemicolonBody) != 0);
			if (!modifiers.isAbstract() && !node.isNative() && !semiColonBody) decl.astBody(toBlock(node.statements));
			fillList(node.arguments, decl.rawParameters());
			fillList(node.typeParameters, decl.rawTypeVariables());
			fillList(node.thrownExceptions, decl.rawThrownTypeReferences());
			
			set(node, setPosition(node, decl));
		}
		
		@Override public void visitAnnotationMethodDeclaration(AnnotationMethodDeclaration node) {
			lombok.ast.AnnotationMethodDeclaration decl = new lombok.ast.AnnotationMethodDeclaration();
			decl.astMethodName(toIdentifier(node.selector, node.sourceStart, node.sourceEnd));
			decl.astJavadoc((lombok.ast.Comment) toTree(node.javadoc));
			decl.astModifiers(toModifiers(node.modifiers, node.annotations, node.modifiersSourceStart, node.declarationSourceStart));
			decl.astReturnTypeReference((lombok.ast.TypeReference) toTree(node.returnType));
			decl.astDefaultValue((lombok.ast.Expression) toTree(node.defaultValue));
			
			set(node, setPosition(node, decl));
		}
		
		@Override public void visitReturnStatement(ReturnStatement node) {
			lombok.ast.Return returnStat = new lombok.ast.Return();
			returnStat.astValue((lombok.ast.Expression) toTree(node.expression));
			set(node, setPosition(node, returnStat));
		}
		
		@Override public void visitClinit(Clinit node) {
			//currently doing nothing...
			set(node, (Node)null);
		}
		
		@Override public void visitMarkerAnnotation(MarkerAnnotation node) {
			set(node, setPosition(node, createAnnotation(node)));
		}
		
		@Override public void visitSingleMemberAnnotation(SingleMemberAnnotation node) {
			lombok.ast.Annotation annot = createAnnotation(node);
			lombok.ast.AnnotationElement element = new lombok.ast.AnnotationElement();
			element.astValue((lombok.ast.AnnotationValue) toTree(node.memberValue));
			annot.astElements().addToEnd(element);
			set(node, setPosition(node, annot));
		}
		
		@Override public void visitNormalAnnotation(NormalAnnotation node) {
			lombok.ast.Annotation annot = createAnnotation(node);
			fillList(node.memberValuePairs, annot.rawElements());
			set(node, setPosition(node, annot));
		}
	
		private lombok.ast.Annotation createAnnotation(Annotation node) {
			lombok.ast.Annotation annotation = new lombok.ast.Annotation();
			annotation.astAnnotationTypeReference((lombok.ast.TypeReference) toTree(node.type));
			return annotation;
		}
		
		@Override public void visitMemberValuePair(MemberValuePair node) {
			lombok.ast.AnnotationElement element = new lombok.ast.AnnotationElement();
			element.astName(toIdentifier(node.name, node.sourceStart, node.sourceEnd));
			element.astValue((lombok.ast.AnnotationValue) toTree(node.value));
			set(node, setPosition(node, element));
		}
		
		@Override public void visitJavadoc(Javadoc node) {
			if (node == null) {
				set(node, (Node)null);
				return;
			}
			
			lombok.ast.Comment comment = new lombok.ast.Comment();
			comment.astContent(node.toString());
			set(node, setPosition(node, comment));
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
	};
	
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
