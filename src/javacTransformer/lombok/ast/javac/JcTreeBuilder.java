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
package lombok.ast.javac;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

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
import lombok.ast.AstException;
import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
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
import lombok.ast.JavadocContainer;
import lombok.ast.KeywordModifier;
import lombok.ast.LabelledStatement;
import lombok.ast.Literal;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.NullLiteral;
import lombok.ast.PackageDeclaration;
import lombok.ast.Position;
import lombok.ast.Return;
import lombok.ast.Select;
import lombok.ast.Statement;
import lombok.ast.StaticInitializer;
import lombok.ast.StrictListAccessor;
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
import lombok.ast.grammar.Source;
import lombok.ast.grammar.SourceStructure;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Name.Table;

/**
 * Turns {@code lombok.ast} based ASTs into javac's {@code JCTree} model.
 */
public class JcTreeBuilder extends ForwardingAstVisitor {
	private final TreeMaker treeMaker;
	private final Table table;
	private final Map<Node, Collection<SourceStructure>> sourceStructures;
	private final Map<JCTree, Integer> endPosTable;
	
	private List<? extends JCTree> result = null;
	
	public JcTreeBuilder(Source source, Context context) {
		this(source.getSourceStructures(), TreeMaker.instance(context), Name.Table.instance(context), new HashMap<JCTree, Integer>());
	}
	
	private JcTreeBuilder(Map<Node, Collection<SourceStructure>> structures, TreeMaker treeMaker, Table table, Map<JCTree, Integer> endPosTable) {
		this.treeMaker = treeMaker;
		this.table = table;
		this.sourceStructures = structures;
		this.endPosTable = endPosTable;
	}
	
	private Name toName(Identifier identifier) {
		if (identifier == null) return null;
		return table.fromString(identifier.getName());
	}
	
	private JCTree toTree(Node node) {
		if (node == null) return null;
		JcTreeBuilder visitor = create();
		node.accept(visitor);
		try {
			return visitor.get();
		} catch (RuntimeException e) {
			System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
			throw e;
		}
	}
	
	private JCExpression toExpression(Node node) {
		return (JCExpression)toTree(node);
	}
	
	private JCStatement toStatement(Node node) {
		return (JCStatement)toTree(node);
	}
	
	private <T extends JCTree> List<T> toList(Class<T> type, StrictListAccessor<?, ?> accessor) {
		List<T> result = List.nil();
		for (Node node : accessor) {
			JcTreeBuilder visitor = create();
			node.accept(visitor);
			
			List<? extends JCTree> values;
			
			try {
				values = visitor.getAll();
				if (values.size() == 0) throw new RuntimeException();
			} catch (RuntimeException e) {
				System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
				throw e;
			}
			
			for (JCTree value : values) {
				if (value != null && !type.isInstance(value)) {
					throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + type.getName());
				}
				result = result.append(type.cast(value));
			}
		}
		return result;
	}
	
	private <T extends JCTree> List<T> toList(Class<T> type, Node node) {
		if (node == null) return List.nil();
		JcTreeBuilder visitor = create();
		node.accept(visitor);
		@SuppressWarnings("unchecked")
		List<T> all = (List<T>)visitor.getAll();
		return List.<T>nil().appendList(all);
	}
	
	public JCTree get() {
		if (result.size() > 1) {
			throw new RuntimeException("Expected only one result but got " + result.size());
		}
		return result.head;
	}
	
	public List<? extends JCTree> getAll() {
		return result;
	}
	
	private boolean set(Node node, JCTree value) {
		if (result != null) throw new IllegalStateException("result is already set");
		JCTree actualValue = value;
		if (node instanceof Expression) {
			for (int i = 0; i < ((Expression)node).getIntendedParens(); i++) {
				actualValue = treeMaker.Parens((JCExpression)actualValue);
				posParen(node, i, ((Expression)node).getParensPositions(), actualValue);
			}
		}
		result = List.of(actualValue);
		return true;
	}
	
	private void posParen(Node node, int iteration, java.util.List<Position> parenPositions, JCTree jcTree) {
		Position p = null;
		if (parenPositions.size() > iteration) p = parenPositions.get(iteration);
		int start = (p == null || p.isUnplaced() || p.getStart() < 0) ? node.getPosition().getStart() - 1 - iteration : p.getStart();
		int end = (p == null || p.isUnplaced() || p.getEnd() < 0) ? node.getPosition().getEnd() + 1 + iteration : p.getEnd();
		setPos(start, end, jcTree);
	}
	
	private boolean set(List<? extends JCTree> values) {
		if (result != null) throw new IllegalStateException("result is already set");
		result = values;
		return true;
	}
	
	private JcTreeBuilder create() {
		return new JcTreeBuilder(sourceStructures, treeMaker, table, endPosTable);
	}
	
	@Override
	public boolean visitNode(Node node) {
		throw new UnsupportedOperationException(String.format("Unhandled node '%s' (%s)", node, node.getClass().getSimpleName()));
	}
	
	@Override
	public boolean visitCompilationUnit(CompilationUnit node) {
		List<JCTree> preamble = toList(JCTree.class, node.getPackageDeclaration());
		List<JCTree> imports = toList(JCTree.class, node.importDeclarations());
		List<JCTree> types = toList(JCTree.class, node.typeDeclarations());
		
		List<JCAnnotation> annotations = List.nil();
		JCExpression pid = null;
		
		for (JCTree elem : preamble) {
			if (elem instanceof JCAnnotation) {
				annotations = annotations.append((JCAnnotation)elem);
			} else if (elem instanceof JCExpression && pid == null) {
				pid = (JCExpression) elem;
			} else {
				throw new RuntimeException("Unexpected element in preamble: " + elem);
			}
		}
		
		JCCompilationUnit topLevel = treeMaker.TopLevel(annotations, pid, imports.appendList(types));
		topLevel.endPositions = endPosTable;
		int start = Integer.MAX_VALUE;
		int end = node.getPosition().getEnd();
		if (node.getPackageDeclaration() != null) start = Math.min(start, node.getPackageDeclaration().getPosition().getStart());
		if (!node.importDeclarations().isEmpty()) start = Math.min(start, node.rawImportDeclarations().first().getPosition().getStart());
		if (!node.typeDeclarations().isEmpty()) start = Math.min(start, node.rawTypeDeclarations().first().getPosition().getStart());
		if (start == Integer.MAX_VALUE) start = node.getPosition().getStart();
		return set(node, setPos(start, end, topLevel));
	}
	
	@Override
	public boolean visitPackageDeclaration(PackageDeclaration node) {
		List<JCTree> defs = List.nil();
		
		for (Annotation annotation : node.annotations()) {
			defs = defs.append(toTree(annotation));
		}
		
		//Actual package declaration
		defs = defs.append(chain(node.parts()));
		
		set(defs);
		return true;
	}
	
	@Override
	public boolean visitImportDeclaration(ImportDeclaration node) {
		JCExpression name = chain(node.parts());
		if (node.isStarImport()) {
			int start = posOfStructure(node, ".", true);
			int end = posOfStructure(node, "*", false);
			name = setPos(start, end, treeMaker.Select(name, table.asterisk));
		}
		return posSet(node, treeMaker.Import(name, node.isStaticImport()));
	}
	
	@Override
	public boolean visitClassDeclaration(ClassDeclaration node) {
		int start = posOfStructure(node, "class", true);
		int end = node.getPosition().getEnd();
		return set(node, setPos(start, end, treeMaker.ClassDef(
				(JCModifiers) toTree(node.getModifiers()),
				toName(node.getName()),
				toList(JCTypeParameter.class, node.typeVariables()),
				toTree(node.getExtending()),
				toList(JCExpression.class, node.implementing()),
				node.getBody() == null ? List.<JCTree>nil() : toList(JCTree.class, node.getBody().members())
		)));
	}
	
	@Override
	public boolean visitInterfaceDeclaration(InterfaceDeclaration node) {
		JCModifiers modifiers = (JCModifiers) toTree(node.getModifiers());
		modifiers.flags |= Flags.INTERFACE;
		int start = posOfStructure(node, "interface", true);
		int end = node.getPosition().getEnd();
		return set(node, setPos(start, end, treeMaker.ClassDef(
				modifiers,
				toName(node.getName()),
				toList(JCTypeParameter.class, node.typeVariables()),
				null,
				toList(JCExpression.class, node.extending()),
				node.getBody() == null ? List.<JCTree>nil() : toList(JCTree.class, node.getBody().members())
		)));
	}
	
	public boolean visitEmptyStatement(EmptyStatement node) {
		return posSet(node, treeMaker.Skip());
	}
	
	@Override
	public boolean visitEnumDeclaration(EnumDeclaration node) {
		JCModifiers modifiers = (JCModifiers) toTree(node.getModifiers());
		modifiers.flags |= Flags.ENUM;
		int start = posOfStructure(node, "enum", true);
		int end = node.getPosition().getEnd();
		return set(node, setPos(start, end, treeMaker.ClassDef(
				modifiers,
				toName(node.getName()),
				List.<JCTypeParameter>nil(),
				null,
				toList(JCExpression.class, node.implementing()),
				node.getBody() == null ? List.<JCTree>nil() : toList(JCTree.class, node.getBody())
		)));
	}
	
	@Override
	public boolean visitEnumTypeBody(EnumTypeBody node) {
		List<JCTree> constants = toList(JCTree.class, node.constants());
		List<JCTree> members = toList(JCTree.class, node.members());
		
		set(List.<JCTree>nil().appendList(constants).appendList(members));
		return true;
	}
	
	private static final long ENUM_CONSTANT_FLAGS = Flags.PUBLIC | Flags.STATIC | Flags.FINAL | Flags.ENUM;
	
	@Override
	public boolean visitEnumConstant(EnumConstant node) {
		JCIdent parentType = treeMaker.Ident(toName(((EnumDeclaration)node.getParent().getParent()).getName()));
		JCClassDecl body = (JCClassDecl) toTree(node.getBody());
		if (body != null) body.mods.flags |= Flags.STATIC | Flags.ENUM;
		JCNewClass newClass = treeMaker.NewClass(
				null, 
				List.<JCExpression>nil(),
				parentType, 
				toList(JCExpression.class, node.arguments()),
				body
		);
		
		int start = posOfStructure(node, "(", true);
		int end = body != null ? node.getPosition().getEnd() : posOfStructure(node, ")", false);
		if (body != null) body.pos = node.getPosition().getStart();
		if (start != node.getPosition().getStart()) {
			setPos(start, end, newClass);
		} else {
			if (body != null) setPos(node.getBody(), newClass);
		}
		
		return posSet(node, treeMaker.VarDef(
				treeMaker.Modifiers(ENUM_CONSTANT_FLAGS, toList(JCAnnotation.class, node.annotations())),
				toName(node.getName()), 
				parentType, 
				newClass
		));
	}
	
	@Override
	public boolean visitTypeBody(TypeBody node) {
		return posSet(node, treeMaker.ClassDef(treeMaker.Modifiers(0), table.empty,
				List.<JCTypeParameter>nil(), null, List.<JCExpression>nil(), toList(JCTree.class, node.members())));
	}
	
	@Override
	public boolean visitExpressionStatement(ExpressionStatement node) {
		return posSet(node, treeMaker.Exec(toExpression(node.getExpression())));
	}
	
	@Override
	public boolean visitIntegralLiteral(IntegralLiteral node) {
		if (node.isMarkedAsLong()) {
			return posSet(node, treeMaker.Literal(TypeTags.LONG, node.longValue()));
		}
		return posSet(node, treeMaker.Literal(TypeTags.INT, node.intValue()));
	}
	
	@Override
	public boolean visitFloatingPointLiteral(FloatingPointLiteral node) {
		if (node.isMarkedAsFloat()) {
			return posSet(node, treeMaker.Literal(TypeTags.FLOAT, node.floatValue()));
		}
		return posSet(node, treeMaker.Literal(TypeTags.DOUBLE, node.doubleValue()));
	}
	
	@Override
	public boolean visitBooleanLiteral(BooleanLiteral node) {
		return posSet(node, treeMaker.Literal(TypeTags.BOOLEAN, node.getValue() ? 1 : 0));
	}
	
	@Override
	public boolean visitCharLiteral(CharLiteral node) {
		return posSet(node, treeMaker.Literal(TypeTags.CHAR, (int)node.getValue()));
	}
	
	@Override
	public boolean visitNullLiteral(NullLiteral node) {
		return posSet(node, treeMaker.Literal(TypeTags.BOT, null));
	}
	
	@Override
	public boolean visitStringLiteral(StringLiteral node) {
		return posSet(node, treeMaker.Literal(TypeTags.CLASS, node.getValue()));
	}
	
	@Override
	public boolean visitIdentifier(Identifier node) {
		return posSet(node, treeMaker.Ident(toName(node)));
	}
	
	@Override
	public boolean visitCast(Cast node) {
		return posSet(node, treeMaker.TypeCast(toTree(node.getRawTypeReference()), toExpression(node.getOperand())));
	}
	
	@Override
	public boolean visitConstructorInvocation(ConstructorInvocation node) {
		return posSet(node, treeMaker.NewClass(
				toExpression(node.getQualifier()), 
				toList(JCExpression.class, node.getConstructorTypeArguments()), 
				toExpression(node.getTypeReference()), 
				toList(JCExpression.class, node.arguments()), 
				(JCClassDecl)toTree(node.getAnonymousClassBody())
		));
	}
	
	@Override
	public boolean visitSelect(Select node) {
		int start = posOfStructure(node.getIdentifier(), ".", true);
		int end = node.getPosition().getEnd();
		return set(node, setPos(start, end,
				treeMaker.Select(toExpression(node.getOperand()), toName(node.getIdentifier()))));
	}
	
	@Override
	public boolean visitUnaryExpression(UnaryExpression node) {
		Expression operand = node.getOperand();
		UnaryOperator operator = node.getOperator();
		if (operator == UnaryOperator.UNARY_MINUS && operand instanceof Literal && !(operand instanceof CharLiteral)) {
			JCLiteral result = (JCLiteral) toTree(operand);
			result.value = negative(result.value);
			return set(node, setPos(operand, result));
		}
		
		int start = node.getPosition().getStart();
		int end = node.getPosition().getEnd();
		
		/*
		 * The pos of "++x" is the entire thing, but the pos of "x++" is only the symbol.
		 * I guess the javac guys think consistency is overrated :(
		 */
		switch (operator) {
		case POSTFIX_DECREMENT:
		case POSTFIX_INCREMENT:
			start = posOfStructure(node, node.getOperator().getSymbol(), true);
			end = posOfStructure(node, node.getOperator().getSymbol(), false);
		}
		
		return set(node, setPos(start, end, treeMaker.Unary(UNARY_OPERATORS.get(operator), toExpression(operand))));
	}
	
	@Override
	public boolean visitAlternateConstructorInvocation(AlternateConstructorInvocation node) {
		int thisStart = posOfStructure(node, "this", true);
		int thisEnd = posOfStructure(node, "this", false);
		if (node.getConstructorTypeArguments() != null && !node.getConstructorTypeArguments().generics().isEmpty()) {
			thisStart = node.getConstructorTypeArguments().getPosition().getStart();
		}
		JCMethodInvocation invoke = treeMaker.Apply(
				toList(JCExpression.class, node.getConstructorTypeArguments()), 
				setPos(thisStart, thisEnd,
						treeMaker.Ident(table._this)),
				toList(JCExpression.class, node.arguments()));
		int start = posOfStructure(node, "(", true);
		int end = posOfStructure(node, ")", false);
		return posSet(node, treeMaker.Exec(setPos(start, end, invoke)));
	}
	
	@Override
	public boolean visitSuperConstructorInvocation(SuperConstructorInvocation node) {
		JCExpression methodId;
		if (node.getQualifier() == null) {
			methodId = treeMaker.Ident(table._super);
			methodId.pos = posOfStructure(node, "super", true);
		} else {
			methodId = treeMaker.Select(
					toExpression(node.getQualifier()),
					table._super);
			setPos(posOfStructure(node, ".", true), posOfStructure(node, "super", false), methodId);
		}
		
		JCMethodInvocation invoke = treeMaker.Apply(
				toList(JCExpression.class, node.getConstructorTypeArguments()), 
				methodId, 
				toList(JCExpression.class, node.arguments()));
		int start = posOfStructure(node, "(", Integer.MAX_VALUE, true);
		int end = posOfStructure(node, ")", Integer.MAX_VALUE, false);
		return posSet(node, treeMaker.Exec(setPos(start, end, invoke)));
	}
	
	@Override
	public boolean visitSuper(Super node) {
		JCTree tree;
		if (node.getQualifier() != null) {
			tree = treeMaker.Select((JCExpression) toTree(node.getQualifier()), table._super);
			setPos(posOfStructure(node, ".", true), posOfStructure(node, "super", false), tree);
		} else {
			tree = treeMaker.Ident(table._super);
			tree.pos = posOfStructure(node, "super", true);
		}
		return set(node, tree);
	}
	
	
	@Override
	public boolean visitBinaryExpression(BinaryExpression node) {
		BinaryOperator operator = node.getOperator();
		if (posOfStructure(node, node.getRawOperator(), true) == 96 && node.getPosition().getEnd() == 122) {
			System.out.println("ARRIVED");
		}
		int start = posOfStructure(node, node.getRawOperator(), true);
		int end = node.getPosition().getEnd();
		
		if (operator == BinaryOperator.PLUS) {
			if (tryStringCombine(node)) return true;
		}
		
		JCExpression lhs = toExpression(node.getLeft());
		JCExpression rhs = toExpression(node.getRight());
		
		if (operator == BinaryOperator.ASSIGN) {
			return set(node, setPos(start, end, treeMaker.Assign(lhs, rhs)));
		}
		
		if (operator.isAssignment()) {
			return set(node, setPos(start, end, treeMaker.Assignop(BINARY_OPERATORS.get(operator), lhs, rhs)));
		}
		
		return set(node, setPos(start, end, treeMaker.Binary(BINARY_OPERATORS.get(operator), lhs, rhs)));
	}
	
	private boolean tryStringCombine(BinaryExpression node) {
		if (node.getParens() > 0) {
			;
		} else if (node.getParent() instanceof BinaryExpression) {
			try {
				if (!((BinaryExpression)node.getParent()).getOperator().isAssignment()) return false;
			} catch (AstException ignore) {
				return false;
			}
		} else if (node.getParent() instanceof InstanceOf) {
			return false;
		}
		
		java.util.List<String> buffer = new ArrayList<String>();
		BinaryExpression current = node;
		int start = Integer.MAX_VALUE;
		while (true) {
			start = Math.min(start, posOfStructure(current, "+", true));
			if (current.getRawRight() instanceof StringLiteral && current.getRight().getParens() == 0) {
				buffer.add(((StringLiteral)current.getRawRight()).getValue());
			} else {
				return false;
			}
			
			if (current.getRawLeft() instanceof BinaryExpression) {
				current = (BinaryExpression) current.getRawLeft();
				try {
					if (current.getOperator() != BinaryOperator.PLUS || current.getParens() > 0) return false;
				} catch (AstException e) {
					return false;
				}
			} else if (current.getRawLeft() instanceof StringLiteral && current.getLeft().getParens() == 0) {
				buffer.add(((StringLiteral)current.getRawLeft()).getValue());
				break;
			} else {
				return false;
			}
		}
		
		StringBuilder out = new StringBuilder();
		for (int i = buffer.size() - 1; i >= 0; i--) out.append(buffer.get(i));
		int end = node.getPosition().getEnd();
		return set(node, setPos(start, end, treeMaker.Literal(TypeTags.CLASS, out.toString())));
	}
	
	@Override
	public boolean visitInstanceOf(InstanceOf node) {
		int start = posOfStructure(node, "instanceof", true);
		int end = node.getPosition().getEnd();
		return set(node, setPos(start, end,
				treeMaker.TypeTest(
						toExpression(node.getObjectReference()),
						toExpression(node.getTypeReference()))));
	}
	
	@Override
	public boolean visitInlineIfExpression(InlineIfExpression node) {
		int start = posOfStructure(node, "?", true);
		int end = node.getPosition().getEnd();
		return set(node, setPos(start, end, treeMaker.Conditional(
				toExpression(node.getCondition()), 
				toExpression(node.getIfTrue()), 
				toExpression(node.getIfFalse()))));
	}
	
	@Override
	public boolean visitMethodInvocation(MethodInvocation node) {
		JCExpression methodId;
		if (node.getOperand() == null) {
			methodId = (JCExpression) toTree(node.getName());
		} else {
			int start = posOfStructure(node, ".", true);
			int end = node.getName().getPosition().getEnd();
			methodId = setPos(start, end, treeMaker.Select(
					toExpression(node.getOperand()),
					toName(node.getName())));
		}
		
		int start = posOfStructure(node, "(", true);
		int end = node.getPosition().getEnd();
		
		return set(node, setPos(start, end, treeMaker.Apply(
				toList(JCExpression.class, node.getMethodTypeArguments()), 
				methodId, 
				toList(JCExpression.class, node.arguments())
		)));
	}
	
	@Override
	public boolean visitArrayInitializer(ArrayInitializer node) {
		return posSet(node, treeMaker.NewArray(
				null,
				List.<JCExpression>nil(),
				toList(JCExpression.class, node.expressions())
		));
	}
	
	@Override
	public boolean visitArrayCreation(ArrayCreation node) {
		java.util.List<Integer> typeTrees = new ArrayList<Integer>();
		int endPosOfTypeTree = 0;
		List<JCExpression> dims = List.nil();
		for (ArrayDimension dim : node.dimensions()) {
			JCExpression e = toExpression(dim);
			if (e == null) {
				Position p = dim.getPosition();
				typeTrees.add(p.getStart());
				endPosOfTypeTree = Math.max(endPosOfTypeTree, p.getEnd());
			} else {
				dims = dims.append(e);
			}
		}
		
		Collections.reverse(typeTrees);
		
		List<JCExpression> init;
		if (node.getInitializer() == null) {
			init = null;
		} else {
			init = toList(JCExpression.class, node.getInitializer().expressions());
			typeTrees.remove(typeTrees.size()-1); //javac sees this as new TYPE[] {}, with both 'new' and the last [] as structure.
		}
		
		JCExpression elementType = toExpression(node.getComponentTypeReference());
		for (Integer start : typeTrees) {
			elementType = setPos(start, endPosOfTypeTree, treeMaker.TypeArray(elementType));
		}
		return posSet(node, treeMaker.NewArray(elementType, dims, init));
	}
	
	@Override
	public boolean visitArrayDimension(ArrayDimension node) {
		return set(node, toTree(node.getDimension()));
	}
	
	private static Object negative(Object value) {
		Number num = (Number)value;
		if (num instanceof Integer) return -num.intValue();
		if (num instanceof Long) return -num.longValue();
		if (num instanceof Float) return -num.floatValue();
		if (num instanceof Double) return -num.doubleValue();
		
		throw new IllegalArgumentException("value should be an Integer, Long, Float or Double, not a " + value.getClass().getSimpleName());
	}
	
	@Override
	public boolean visitAssert(Assert node) {
		return posSet(node, treeMaker.Assert(toExpression(node.getAssertion()), toExpression(node.getMessage())));
	}
	
	@Override
	public boolean visitBreak(Break node) {
		return posSet(node, treeMaker.Break(toName(node.getLabel())));
	}
	
	@Override
	public boolean visitContinue(Continue node) {
		return posSet(node, treeMaker.Continue(toName(node.getLabel())));
	}
	
	@Override
	public boolean visitDoWhile(DoWhile node) {
		JCExpression expr = toExpression(node.getCondition());
		int start = posOfStructure(node, "(", true);
		int end = posOfStructure(node, ")", false);
		expr = setPos(start, end, treeMaker.Parens(expr));
		return posSet(node, treeMaker.DoLoop(toStatement(node.getStatement()), expr));
	}
	
	@Override
	public boolean visitFor(For node) {
		List<JCStatement> inits;
		List<JCExpressionStatement> updates;
		
		if (node.isVariableDeclarationBased()) {
			inits = toList(JCStatement.class, node.getVariableDeclaration());
		} else {
			inits = List.nil();
			for (Expression init : node.expressionInits()) {
				inits = inits.append(setPos(init, treeMaker.Exec(toExpression(init))));
			}
		}
		
		updates = List.nil();
		for (Expression update : node.updates()) {
			updates = updates.append(setPos(update, treeMaker.Exec(toExpression(update))));
		}
		
		return posSet(node, treeMaker.ForLoop(inits, toExpression(node.getCondition()), updates, toStatement(node.getStatement())));
	}
	
	@Override
	public boolean visitForEach(ForEach node) {
		return posSet(node, treeMaker.ForeachLoop((JCVariableDecl) toTree(node.getVariable()), toExpression(node.getIterable()), toStatement(node.getStatement())));
	}
	
	@Override
	public boolean visitIf(If node) {
		JCExpression expr = toExpression(node.getCondition());
		int start = posOfStructure(node, "(", true);
		int end = posOfStructure(node, ")", false);
		expr = setPos(start, end, treeMaker.Parens(expr));
		return posSet(node, treeMaker.If(expr, toStatement(node.getStatement()), toStatement(node.getElseStatement())));
	}
	
	@Override
	public boolean visitLabelledStatement(LabelledStatement node) {
		return posSet(node, treeMaker.Labelled(toName(node.getLabel()), toStatement(node.getStatement())));
	}
	
	@Override
	public boolean visitModifiers(Modifiers node) {
		JCModifiers mods = treeMaker.Modifiers(node.getExplicitModifierFlags(), toList(JCAnnotation.class, node.annotations()));
		
		Comment javadoc = null;
		
		if (node.getParent() instanceof JavadocContainer) {
			javadoc = ((JavadocContainer)node.getParent()).getJavadoc();
		} else if (node.getParent() instanceof VariableDefinition && node.getParent().getParent() instanceof VariableDeclaration) {
			javadoc = ((VariableDeclaration)node.getParent().getParent()).getJavadoc();
		}
		
		if (javadoc != null && javadoc.isMarkedDeprecated()) mods.flags |= Flags.DEPRECATED;
		
		if (node.isEmpty()) {
			//Workaround for a javac bug; start (but not end!) gets set of an empty modifiers object,
			//but only if these represent the modifiers of a constructor or method that has type variables.
			if (
					(node.getParent() instanceof MethodDeclaration && ((MethodDeclaration)node.getParent()).typeVariables().size() > 0) ||
					(node.getParent() instanceof ConstructorDeclaration && ((ConstructorDeclaration)node.getParent()).typeVariables().size() > 0)) {
				
				mods.pos = node.getParent().getPosition().getStart();
			}
			return set(node, mods);
		} else {
			return posSet(node, mods);
		}
	}
	
	@Override
	public boolean visitKeywordModifier(KeywordModifier node) {
		return set(node, treeMaker.Modifiers(getModifier(node)));
	}
	
	@Override
	public boolean visitInstanceInitializer(InstanceInitializer node) {
		return set(node, toTree(node.getBody()));
	}
	
	@Override
	public boolean visitStaticInitializer(StaticInitializer node) {
		JCBlock block = (JCBlock) toTree(node.getBody());
		block.flags |= Flags.STATIC; 
		return posSet(node, block);
	}
	
	@Override
	public boolean visitBlock(Block node) {
		return posSet(node, treeMaker.Block(0, toList(JCStatement.class, node.contents())));
	}
	
	@Override
	public boolean visitVariableDeclaration(VariableDeclaration node) {
		List<JCVariableDecl> list = toList(JCVariableDecl.class, node.getDefinition());
		JCVariableDecl last = list.get(list.size() -1);
		endPosTable.put(last, node.getPosition().getEnd());
		return set(list);
	}
	
	@Override
	public boolean visitVariableDefinition(VariableDefinition node) {
		JCModifiers mods = (JCModifiers) toTree(node.getModifiers());
		JCExpression vartype = toExpression(node.getTypeReference());
		
		if (node.isVarargs()) {
			mods.flags |= Flags.VARARGS;
			vartype = addDimensions(node, vartype, 1);
			setPos(posOfStructure(node, "...", true), posOfStructure(node, "...", false), vartype);
		}
		
		List<JCVariableDecl> defs = List.nil();
		for (VariableDefinitionEntry e : node.variables()) {
			defs = defs.append(setPos(
					e,
					treeMaker.VarDef(mods, toName(e.getName()),
							addDimensions(e, vartype, e.getArrayDimensions()), toExpression(e.getInitializer()))));
		}
		
		/* the endpos when multiple nodes are generated is after the comma for all but the last item, for some reason. */ {
			for (int i = 0; i < defs.size() -1; i++) {
				endPosTable.put(defs.get(i), posOfStructure(node, ",", i, false));
			}
		}
		
		if (defs.isEmpty()) throw new RuntimeException("Empty VariableDefinition node");
		set(defs);
		return true;
	}
	
	@Override
	public boolean visitAnnotationDeclaration(AnnotationDeclaration node) {
		JCModifiers modifiers = (JCModifiers) toTree(node.getModifiers());
		modifiers.flags |= Flags.INTERFACE | Flags.ANNOTATION;
		int start = posOfStructure(node, "interface", true);
		int end = node.getPosition().getEnd();
		if (modifiers.pos == -1) modifiers.pos = posOfStructure(node, "@", true);
		endPosTable.put(modifiers, posOfStructure(node, "@", false));
			
		return set(node, setPos(start, end, treeMaker.ClassDef(
				modifiers,
				toName(node.getName()),
				List.<JCTypeParameter>nil(),
				null,
				List.<JCExpression>nil(),
				node.getBody() == null ? List.<JCTree>nil() : toList(JCTree.class, node.getBody().members())
		)));
	}
	
	@Override
	public boolean visitAnnotationMethodDeclaration(AnnotationMethodDeclaration node) {
		JCMethodDecl methodDef = treeMaker.MethodDef(
				(JCModifiers)toTree(node.getModifiers()), 
				toName(node.getMethodName()), 
				toExpression(node.getReturnTypeReference()), 
				List.<JCTypeParameter>nil(),
				List.<JCVariableDecl>nil(),
				List.<JCExpression>nil(),
				null,
				toExpression(node.getDefaultValue())
		);
		
		int start = node.getMethodName().getPosition().getStart();
		int end = node.getPosition().getEnd();
		
		return set(node, setPos(start, end, methodDef));
	}
	
	@Override
	public boolean visitClassLiteral(ClassLiteral node) {
		int start = posOfStructure(node, ".", true);
		int end = node.getPosition().getEnd();
		return set(node, setPos(start, end,
				treeMaker.Select((JCExpression) toTree(node.getTypeReference()), table._class)));
	}
	
	@Override
	public boolean visitAnnotationElement(AnnotationElement node) {
		JCExpression arg = toExpression(node.getValue());
		if (node.getName() != null) {
			arg = setPos(node.getValue(), treeMaker.Assign((JCIdent) toTree(node.getName()), arg));
		}
		return set(node, arg);
	}
	
	@Override public boolean visitAnnotation(Annotation node) {
		int start = node.getPosition().getStart();
		int end = node.getPosition().getEnd();
		return set(node, setPos(start, end,
				treeMaker.Annotation(toTree(node.getAnnotationTypeReference()), toList(JCExpression.class, node.elements()))));
	}
	
	@Override
	public boolean visitTypeReference(TypeReference node) {
		WildcardKind wildcard = node.getWildcard();
		if (wildcard == WildcardKind.UNBOUND) {
			return posSet(node, treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.UNBOUND), null));
		}
		
		JCExpression result = plainTypeReference(node);
		
		result = addWildcards(node, result, wildcard);
		result = addDimensions(node, result, node.getArrayDimensions());
		
		return set(node, result);
	}
	
	@Override
	public boolean visitArrayAccess(ArrayAccess node) {
		int start = posOfStructure(node, "[", true);
		int end = node.getPosition().getEnd();
		return set(node, setPos(start, end,
				treeMaker.Indexed(toExpression(node.getOperand()), toExpression(node.getIndexExpression()))));
	}
	
	private JCExpression addDimensions(Node node, JCExpression type, int dimensions) {
		JCExpression resultingType = type;
		for (int i = 0; i < dimensions; i++) {
			int start = posOfStructure(node, "[", dimensions - i - 1, true);
			int end = posOfStructure(node, "]", false);
			resultingType = setPos(start, end, treeMaker.TypeArray(resultingType));
		}
		return resultingType;
	}
	
	private JCExpression plainTypeReference(TypeReference node) {
		if (node.isPrimitive() || node.parts().size() == 1) {
			int end = node.getPosition().getEnd();
			if (node.getArrayDimensions() > 0) {
				end = node.parts().last().getPosition().getEnd();
			}
			if (end == node.getPosition().getStart()) end = node.getPosition().getEnd();
			
			Identifier identifier = node.parts().first().getIdentifier();
			int typeTag = primitiveTypeTag(identifier.getName());
			if (typeTag > 0) return setPos(node.getPosition().getStart(), end, treeMaker.TypeIdent(typeTag));
		}
		
		JCExpression current = null;
		for (TypeReferencePart part : node.parts()) {
			JCExpression expr = (JCExpression) toTree(part);
			if (current == null) {
				current = expr;
				continue;
			}
			if (expr instanceof JCIdent) {
				current = treeMaker.Select(current, ((JCIdent)expr).name);
				setPos(posOfStructure(part, ".", true), part.getPosition().getEnd(), current);
			} else if (expr instanceof JCTypeApply) {
				JCTypeApply apply = (JCTypeApply)expr;
				apply.clazz = treeMaker.Select(current, ((JCIdent)apply.clazz).name);
				setPos(posOfStructure(part, ".", true), part.getIdentifier().getPosition().getEnd(), apply.clazz);
				current = apply;
			} else {
				throw new IllegalStateException("Didn't expect a " + expr.getClass().getName() + " in " + node);
			}
		}
		
		return current;
	}
	
	private JCExpression addWildcards(Node node, JCExpression type, WildcardKind wildcardKind) {
		TypeBoundKind typeBoundKind;
		switch (wildcardKind) {
		case NONE:
			return type;
		case EXTENDS:
			typeBoundKind = treeMaker.TypeBoundKind(BoundKind.EXTENDS);
			setPos(posOfStructure(node, "extends", true), posOfStructure(node, "extends", false), typeBoundKind);
			return setPos(type.pos, endPosTable.get(type), treeMaker.Wildcard(typeBoundKind, type));
		case SUPER:
			typeBoundKind = treeMaker.TypeBoundKind(BoundKind.SUPER);
			setPos(posOfStructure(node, "super", true), posOfStructure(node, "super", false), typeBoundKind);
			return setPos(type.pos, endPosTable.get(type), treeMaker.Wildcard(typeBoundKind, type));
		default:
			throw new IllegalStateException("Unexpected unbound wildcard: " + wildcardKind);
		}
	}
	
	@Override
	public boolean visitTypeReferencePart(TypeReferencePart node) {
		JCIdent ident = (JCIdent) toTree(node.getIdentifier());
		
		List<JCExpression> typeArguments = toList(JCExpression.class, node.getTypeArguments());
		if (typeArguments.isEmpty()) {
			return set(node, ident);
		} else {
			JCTypeApply typeApply = treeMaker.TypeApply(ident, typeArguments);
			return set(node, setPos(node.getTypeArguments(), typeApply));
		}
	}
	
	@Override
	public boolean visitTypeArguments(TypeArguments node) {
		set(toList(JCExpression.class, node.generics()));
		return true;
	}
	
	@Override
	public boolean visitTypeVariable(TypeVariable node) {
		return posSet(node, treeMaker.TypeParameter(toName(node.getName()), toList(JCExpression.class, node.extending())));
	}
	
	@Override
	public boolean visitMethodDeclaration(MethodDeclaration node) {
		JCMethodDecl methodDef = treeMaker.MethodDef(
				(JCModifiers)toTree(node.getModifiers()), 
				toName(node.getMethodName()), 
				toExpression(node.getReturnTypeReference()), 
				toList(JCTypeParameter.class, node.typeVariables()), 
				toList(JCVariableDecl.class, node.parameters()), 
				toList(JCExpression.class, node.thrownTypeReferences()), 
				(JCBlock)toTree(node.getBody()), 
				null
		);
		for (JCVariableDecl decl : methodDef.params) {
			decl.mods.flags |= Flags.PARAMETER;
		}
		
		int start = node.getMethodName().getPosition().getStart();
		int end = node.getPosition().getEnd();
		
		return set(node, setPos(start, end, methodDef));
	}
	
	@Override
	public boolean visitConstructorDeclaration(ConstructorDeclaration node) {
		JCMethodDecl constrDef = treeMaker.MethodDef(
				(JCModifiers)toTree(node.getModifiers()), 
				table.init, null,
				toList(JCTypeParameter.class, node.typeVariables()), 
				toList(JCVariableDecl.class, node.parameters()), 
				toList(JCExpression.class, node.thrownTypeReferences()), 
				(JCBlock)toTree(node.getBody()), 
				null
		);
		for (JCVariableDecl decl : constrDef.params) {
			decl.mods.flags |= Flags.PARAMETER;
		}
		
		int start = node.getTypeName().getPosition().getStart();
		int end = node.getPosition().getEnd();
		
		return set(node, setPos(start, end, constrDef));
	}
	
	@Override
	public boolean visitReturn(Return node) {
		return posSet(node, treeMaker.Return(toExpression(node.getValue())));
	}
	
	@Override
	public boolean visitSwitch(Switch node) {
		List<JCCase> cases = List.nil();
		
		JCExpression currentPat = null;
		Node currentNode = null;
		List<JCStatement> stats = null;
		boolean preamble = true;
		
		for (Statement s : node.getBody().contents()) {
			if (s instanceof Case || s instanceof Default) {
				JCExpression newPat = (s instanceof Default) ? null : toExpression(((Case)s).getCondition());
				if (preamble) {
					preamble = false;
				} else {
					cases = addCase(cases, currentPat, currentNode, stats);
				}
				stats = List.nil();
				currentPat = newPat;
				currentNode = s;
			} else {
				if (preamble) {
					throw new RuntimeException("switch body does not start with default/case.");
				}
				stats = stats.append(toStatement(s));
			}
		}
		
		if (!preamble) cases = addCase(cases, currentPat, currentNode, stats);
		
		JCExpression expr = toExpression(node.getCondition());
		int start = posOfStructure(node, "(", true);
		int end = posOfStructure(node, ")", false);
		expr = setPos(start, end, treeMaker.Parens(expr));
		return posSet(node, treeMaker.Switch(expr, cases));
	}

	private List<JCCase> addCase(List<JCCase> cases, JCExpression currentPat, Node currentNode, List<JCStatement> stats) {
		JCStatement last = stats.last();
		int start = currentNode.getPosition().getStart();
		int end = last == null ? currentNode.getPosition().getEnd() : endPosTable.get(last);
		cases = cases.append(setPos(start, end, treeMaker.Case(currentPat, stats)));
		return cases;
	}
	
	@Override
	public boolean visitSynchronized(Synchronized node) {
		JCExpression expr = toExpression(node.getLock());
		int start = posOfStructure(node, "(", true);
		int end = posOfStructure(node, ")", false);
		expr = setPos(start, end, treeMaker.Parens(expr));
		return posSet(node, treeMaker.Synchronized(expr, (JCBlock)toTree(node.getBody())));
	}
	
	@Override
	public boolean visitThis(This node) {
		JCTree tree;
		int end = node.getPosition().getEnd(), start;
		if (node.getQualifier() != null) {
			tree = treeMaker.Select((JCExpression) toTree(node.getQualifier()), table._this);
			start = posOfStructure(node, ".", true);
		} else {
			tree = treeMaker.Ident(table._this);
			start = node.getPosition().getStart();
		}
		return set(node, setPos(start, end, tree));
	}
	
	@Override
	public boolean visitTry(Try node) {
		List<JCCatch> catches = toList(JCCatch.class, node.catches());
		
		return posSet(node, treeMaker.Try((JCBlock) toTree(node.getBody()), catches, (JCBlock) toTree(node.getFinally())));
	}
	
	@Override
	public boolean visitCatch(Catch node) {
		JCVariableDecl exceptionDeclaration = (JCVariableDecl) toTree(node.getExceptionDeclaration());
		exceptionDeclaration.getModifiers().flags |= Flags.PARAMETER;
		return posSet(node, treeMaker.Catch(exceptionDeclaration, (JCBlock) toTree(node.getBody())));
	}
	
	@Override
	public boolean visitThrow(Throw node) {
		return posSet(node, treeMaker.Throw(toExpression(node.getThrowable())));
	}
	
	@Override
	public boolean visitWhile(While node) {
		JCExpression expr = toExpression(node.getCondition());
		int start = posOfStructure(node, "(", true);
		int end = posOfStructure(node, ")", false);
		expr = setPos(start, end, treeMaker.Parens(expr));
		return posSet(node, treeMaker.WhileLoop(expr, toStatement(node.getStatement())));
	}
	
	@Override
	public boolean visitEmptyDeclaration(EmptyDeclaration node) {
		if (node.getParent() instanceof CompilationUnit) {
			return posSet(node, treeMaker.Skip());
		} 
		return set(node, posNone(treeMaker.Block(0, List.<JCStatement>nil())));
	}
	
	private static final EnumMap<UnaryOperator, Integer> UNARY_OPERATORS = Maps.newEnumMap(UnaryOperator.class);
	static {
		UNARY_OPERATORS.put(UnaryOperator.BINARY_NOT, JCTree.COMPL);
		UNARY_OPERATORS.put(UnaryOperator.LOGICAL_NOT, JCTree.NOT); 
		UNARY_OPERATORS.put(UnaryOperator.UNARY_PLUS, JCTree.POS);
		UNARY_OPERATORS.put(UnaryOperator.PREFIX_INCREMENT, JCTree.PREINC); 
		UNARY_OPERATORS.put(UnaryOperator.UNARY_MINUS, JCTree.NEG);
		UNARY_OPERATORS.put(UnaryOperator.PREFIX_DECREMENT, JCTree.PREDEC); 
		UNARY_OPERATORS.put(UnaryOperator.POSTFIX_INCREMENT, JCTree.POSTINC); 
		UNARY_OPERATORS.put(UnaryOperator.POSTFIX_DECREMENT, JCTree.POSTDEC);
	}
	
	private static final EnumMap<BinaryOperator, Integer> BINARY_OPERATORS = Maps.newEnumMap(BinaryOperator.class);
	static {
		BINARY_OPERATORS.put(BinaryOperator.PLUS_ASSIGN, JCTree.PLUS_ASG);
		BINARY_OPERATORS.put(BinaryOperator.MINUS_ASSIGN, JCTree.MINUS_ASG);
		BINARY_OPERATORS.put(BinaryOperator.MULTIPLY_ASSIGN, JCTree.MUL_ASG);
		BINARY_OPERATORS.put(BinaryOperator.DIVIDE_ASSIGN, JCTree.DIV_ASG);
		BINARY_OPERATORS.put(BinaryOperator.REMAINDER_ASSIGN, JCTree.MOD_ASG);
		BINARY_OPERATORS.put(BinaryOperator.AND_ASSIGN, JCTree.BITAND_ASG);
		BINARY_OPERATORS.put(BinaryOperator.XOR_ASSIGN, JCTree.BITXOR_ASG);
		BINARY_OPERATORS.put(BinaryOperator.OR_ASSIGN, JCTree.BITOR_ASG);
		BINARY_OPERATORS.put(BinaryOperator.SHIFT_LEFT_ASSIGN, JCTree.SL_ASG);
		BINARY_OPERATORS.put(BinaryOperator.SHIFT_RIGHT_ASSIGN, JCTree.SR_ASG);
		BINARY_OPERATORS.put(BinaryOperator.BITWISE_SHIFT_RIGHT_ASSIGN, JCTree.USR_ASG);
		BINARY_OPERATORS.put(BinaryOperator.LOGICAL_OR, JCTree.OR);
		BINARY_OPERATORS.put(BinaryOperator.LOGICAL_AND, JCTree.AND);
		BINARY_OPERATORS.put(BinaryOperator.BITWISE_OR, JCTree.BITOR);
		BINARY_OPERATORS.put(BinaryOperator.BITWISE_XOR, JCTree.BITXOR);
		BINARY_OPERATORS.put(BinaryOperator.BITWISE_AND, JCTree.BITAND);
		BINARY_OPERATORS.put(BinaryOperator.EQUALS, JCTree.EQ);
		BINARY_OPERATORS.put(BinaryOperator.NOT_EQUALS, JCTree.NE);
		BINARY_OPERATORS.put(BinaryOperator.GREATER, JCTree.GT);
		BINARY_OPERATORS.put(BinaryOperator.GREATER_OR_EQUAL, JCTree.GE);
		BINARY_OPERATORS.put(BinaryOperator.LESS, JCTree.LT);
		BINARY_OPERATORS.put(BinaryOperator.LESS_OR_EQUAL, JCTree.LE);
		BINARY_OPERATORS.put(BinaryOperator.SHIFT_LEFT, JCTree.SL);
		BINARY_OPERATORS.put(BinaryOperator.SHIFT_RIGHT, JCTree.SR);
		BINARY_OPERATORS.put(BinaryOperator.BITWISE_SHIFT_RIGHT, JCTree.USR);
		BINARY_OPERATORS.put(BinaryOperator.PLUS, JCTree.PLUS);
		BINARY_OPERATORS.put(BinaryOperator.MINUS, JCTree.MINUS);
		BINARY_OPERATORS.put(BinaryOperator.MULTIPLY, JCTree.MUL);
		BINARY_OPERATORS.put(BinaryOperator.DIVIDE, JCTree.DIV);
		BINARY_OPERATORS.put(BinaryOperator.REMAINDER, JCTree.MOD);
	}
	
	private static final Map<String, Integer> PRIMITIVES = ImmutableMap.<String, Integer>builder()
		.put("byte", TypeTags.BYTE)
		.put("char", TypeTags.CHAR)
		.put("short", TypeTags.SHORT)
		.put("int", TypeTags.INT)
		.put("long", TypeTags.LONG)
		.put("float", TypeTags.FLOAT)
		.put("double", TypeTags.DOUBLE)
		.put("boolean", TypeTags.BOOLEAN)
		.build();
	
	static int primitiveTypeTag(String typeName) {
		Integer primitive = PRIMITIVES.get(typeName);
		return primitive == null ? 0 : primitive;
	}
	
	private long getModifier(KeywordModifier keyword) {
		return keyword.asReflectModifiers();
	}
	
	private JCExpression chain(Iterable<Identifier> parts) {
		JCExpression previous = null;
		for (Identifier part : parts) {
			Name next = toName(part);
			if (previous == null) {
				previous = setPos(part, treeMaker.Ident(next));
			} else {
				previous = setPos(posOfStructure(part, ".", true), part.getPosition().getEnd(), treeMaker.Select(previous, next));
			}
		}
		return previous;
	}
	
	private int posOfStructure(Node node, String structure, boolean atStart) {
		return posOfStructure(node, structure, atStart ? 0 : Integer.MAX_VALUE, atStart);
	}
	
	private int posOfStructure(Node node, String structure, int idx, boolean atStart) {
		int start = node.getPosition().getStart();
		
		if (sourceStructures != null && sourceStructures.containsKey(node)) {
			for (SourceStructure struct : sourceStructures.get(node)) {
				if (structure.equals(struct.getContent())) {
					start = atStart ? struct.getPosition().getStart() : struct.getPosition().getEnd();
					if (idx-- <= 0) break;
				}
			}
		}
		
		return start;
	}
	
	private boolean posSet(Node node, JCTree jcTree) {
		return set(node, setPos(node, jcTree));
	}
	
	private <T extends JCTree> T posNone(T jcTree) {
		jcTree.pos = -1;
		endPosTable.remove(jcTree);
		return jcTree;
	}
	
	private <T extends JCTree> T setPos(Node node, T jcTree) {
		return setPos(node.getPosition().getStart(), node.getPosition().getEnd(), jcTree);
	}
	
	private <T extends JCTree> T setPos(int start, int end, T jcTree) {
		jcTree.pos = start;
		endPosTable.put(jcTree, end);
		return jcTree;
	}
}
