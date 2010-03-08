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

import java.util.EnumMap;
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
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Name.Table;

/**
 * Turns {@code lombok.ast} based ASTs into javac's {@code JCTree} model.
 */
public class JcTreeBuilder extends ForwardingAstVisitor {

	private final boolean includePositions;
	private final TreeMaker treeMaker;
	private final Table table;
	
	List<? extends JCTree> result = null;
	
	public JcTreeBuilder(Context context, boolean includePositions) {
		this(TreeMaker.instance(context), Name.Table.instance(context), includePositions);
	}
	
	private JcTreeBuilder(TreeMaker treeMaker, Table table, boolean includePositions) {
		this.treeMaker = treeMaker;
		this.table = table;
		this.includePositions = includePositions;
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
		return result.head;
	}
	
	public List<? extends JCTree> getAll() {
		return result;
	}
	
	private void set(Node node, JCTree value) {
		if (result != null) {
			throw new IllegalStateException("result is already set");
		}
		JCTree actualValue = value;
		if (node instanceof Expression) {
			for (int i = 0; i < ((Expression)node).getIntendedParens(); i++) {
				actualValue = treeMaker.Parens((JCExpression)actualValue);
				posParen(actualValue, node, i);
			}
		}
		result = List.of(actualValue);
	}
	
	private void set(List<? extends JCTree> values) {
		if (result != null) throw new IllegalStateException("result is already set");
		result = values;
	}
	
	private JcTreeBuilder create() {
		return new JcTreeBuilder(treeMaker, table, includePositions);
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
		
		set(node, pos(treeMaker.TopLevel(annotations, pid, imports.appendList(types)), node));
		return true;
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
			name = posStar(treeMaker.Select(name, table.fromString("*")), node);
		}
		set(node, pos(treeMaker.Import(name, node.isStaticImport()), node));
		return true;
	}
	
	@Override
	public boolean visitClassDeclaration(ClassDeclaration node) {
		set(node, posClass(treeMaker.ClassDef(
				(JCModifiers) toTree(node.getModifiers()),
				toName(node.getName()),
				toList(JCTypeParameter.class, node.typeVariables()),
				toTree(node.getExtending()),
				toList(JCExpression.class, node.implementing()),
				node.getBody() == null ? List.<JCTree>nil() : toList(JCTree.class, node.getBody().members())),node)
		);
		return true;
	}
	
	@Override
	public boolean visitInterfaceDeclaration(InterfaceDeclaration node) {
		JCModifiers modifiers = (JCModifiers) toTree(node.getModifiers());
		modifiers.flags |= Flags.INTERFACE;
		set(node, treeMaker.ClassDef(
				modifiers,
				toName(node.getName()),
				toList(JCTypeParameter.class, node.typeVariables()),
				null,
				toList(JCExpression.class, node.extending()),
				node.getBody() == null ? List.<JCTree>nil() : toList(JCTree.class, node.getBody().members())
		));
		return true;
	}
	
	public boolean visitEmptyStatement(EmptyStatement node) {
		set(node, pos(treeMaker.Skip(), node));
		return true;
	}
	
	@Override
	public boolean visitEnumDeclaration(EnumDeclaration node) {
		JCModifiers modifiers = (JCModifiers) toTree(node.getModifiers());
		modifiers.flags |= Flags.ENUM;
		set(node, treeMaker.ClassDef(
				modifiers,
				toName(node.getName()),
				List.<JCTypeParameter>nil(),
				null,
				toList(JCExpression.class, node.implementing()),
				node.getBody() == null ? List.<JCTree>nil() : toList(JCTree.class, node.getBody())
		));
		return true;
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
		set(node, treeMaker.VarDef(
				treeMaker.Modifiers(ENUM_CONSTANT_FLAGS, toList(JCAnnotation.class, node.annotations())),
				toName(node.getName()), 
				parentType, 
				treeMaker.NewClass(
						null, 
						List.<JCExpression>nil(),
						parentType, 
						toList(JCExpression.class, node.arguments()),
						body
				)
		));
		return true;
	}
	
	@Override
	public boolean visitTypeBody(TypeBody node) {
		set(node, treeMaker.ClassDef(treeMaker.Modifiers(0), table.empty,
				List.<JCTypeParameter>nil(), null, List.<JCExpression>nil(), toList(JCTree.class, node.members())));
		return true;
	}
	
	@Override
	public boolean visitExpressionStatement(ExpressionStatement node) {
		set(node, treeMaker.Exec(toExpression(node.getExpression())));
		return true;
	}
	
	@Override
	public boolean visitIntegralLiteral(IntegralLiteral node) {
		if (node.isMarkedAsLong()) {
			set(node, treeMaker.Literal(TypeTags.LONG, node.longValue()));
		}
		else {
			set(node, treeMaker.Literal(TypeTags.INT, node.intValue()));
		}
		return true;
	}
	
	@Override
	public boolean visitFloatingPointLiteral(FloatingPointLiteral node) {
		if (node.isMarkedAsFloat()) {
			set(node, treeMaker.Literal(TypeTags.FLOAT, node.floatValue()));
		}
		else {
			set(node, treeMaker.Literal(TypeTags.DOUBLE, node.doubleValue()));
		}
		return true;
	}
	
	@Override
	public boolean visitBooleanLiteral(BooleanLiteral node) {
		set(node, treeMaker.Literal(TypeTags.BOOLEAN, node.getValue() ? 1 : 0));
		return true;
	}
	
	@Override
	public boolean visitCharLiteral(CharLiteral node) {
		set(node, treeMaker.Literal(TypeTags.CHAR, (int)node.getValue()));
		return true;
	}
	
	@Override
	public boolean visitNullLiteral(NullLiteral node) {
		set(node, treeMaker.Literal(TypeTags.BOT, null));
		return true;
	}
	
	@Override
	public boolean visitStringLiteral(StringLiteral node) {
		set(node, treeMaker.Literal(TypeTags.CLASS, node.getValue()));
		return true;
	}
	
	@Override
	public boolean visitIdentifier(Identifier node) {
		set(node, treeMaker.Ident(toName(node)));
		return true;
	}
	
	@Override
	public boolean visitCast(Cast node) {
		set(node, treeMaker.TypeCast(toTree(node.getRawTypeReference()), toExpression(node.getOperand())));
		return true;
	}
	
	@Override
	public boolean visitConstructorInvocation(ConstructorInvocation node) {
		set(node, treeMaker.NewClass(
				toExpression(node.getQualifier()), 
				toList(JCExpression.class, node.getConstructorTypeArguments()), 
				toExpression(node.getTypeReference()), 
				toList(JCExpression.class, node.arguments()), 
				(JCClassDecl)toTree(node.getAnonymousClassBody())
		));
		return true;
	}
	
	@Override
	public boolean visitSelect(Select node) {
		set(node, treeMaker.Select(toExpression(node.getOperand()), toName(node.getIdentifier())));
		return true;
	}
	
	@Override
	public boolean visitUnaryExpression(UnaryExpression node) {
		Expression operand = node.getOperand();
		UnaryOperator operator = node.getOperator();
		if (operator == UnaryOperator.UNARY_MINUS && operand instanceof Literal && !(operand instanceof CharLiteral)) {
			JCLiteral result = (JCLiteral) toTree(operand);
			result.value = negative(result.value);
			set(node, result);
			return true;
		}
		
		set(node, treeMaker.Unary(UNARY_OPERATORS.get(operator), toExpression(operand)));
		return true;
	}
	
	@Override
	public boolean visitAlternateConstructorInvocation(AlternateConstructorInvocation node) {
		set(node, treeMaker.Exec(treeMaker.Apply(
				toList(JCExpression.class, node.getConstructorTypeArguments()), 
				treeMaker.Ident(table._this),
				toList(JCExpression.class, node.arguments()))
		));
		return true;
	}
	
	@Override
	public boolean visitSuperConstructorInvocation(SuperConstructorInvocation node) {
		JCExpression methodId;
		if (node.getQualifier() == null) {
			methodId = treeMaker.Ident(table._super);
		} else {
			methodId = treeMaker.Select(
					toExpression(node.getQualifier()),
					table._super);
		}
		
		set(node, treeMaker.Exec(treeMaker.Apply(
				toList(JCExpression.class, node.getConstructorTypeArguments()), 
				methodId, 
				toList(JCExpression.class, node.arguments()))
		));
		return true;
	}
	
	@Override
	public boolean visitSuper(Super node) {
		JCTree tree;
		if (node.getQualifier() != null) {
			tree = treeMaker.Select((JCExpression) toTree(node.getQualifier()), table._super);
		} else {
			tree = treeMaker.Ident(table._super);
		}
		set(node, tree);
		return true;
	}
	
	@Override
	public boolean visitBinaryExpression(BinaryExpression node) {
		BinaryOperator operator = node.getOperator();
		JCExpression lhs = toExpression(node.getLeft());
		JCExpression rhs = toExpression(node.getRight());
		
		if (operator == BinaryOperator.ASSIGN) {
			set(node, treeMaker.Assign(lhs, rhs));
			return true;
		}
		
		if (operator.isAssignment()) {
			set(node, treeMaker.Assignop(BINARY_OPERATORS.get(operator), lhs, rhs));
			return true;
		}
		
		if (operator == BinaryOperator.PLUS && lhs instanceof JCLiteral && rhs instanceof JCLiteral) {
			JCLiteral left = (JCLiteral)lhs;
			JCLiteral right = (JCLiteral)rhs;
			if (left.typetag == TypeTags.CLASS && right.typetag == TypeTags.CLASS) {
				set(node, treeMaker.Literal(TypeTags.CLASS, String.valueOf(left.value) + String.valueOf(right.value)));
				return true;
			}
		}
		set(node, treeMaker.Binary(BINARY_OPERATORS.get(operator), lhs, rhs));
		return true;
	}
	
	@Override
	public boolean visitInstanceOf(InstanceOf node) {
		set(node, treeMaker.TypeTest(toExpression(node.getObjectReference()), toExpression(node.getTypeReference())));
		return true;
	}
	
	@Override
	public boolean visitInlineIfExpression(InlineIfExpression node) {
		set(node, treeMaker.Conditional(
				toExpression(node.getCondition()), 
				toExpression(node.getIfTrue()), 
				toExpression(node.getIfFalse())));
		return true;
	}
	
	@Override
	public boolean visitMethodInvocation(MethodInvocation node) {
		JCExpression methodId;
		if (node.getOperand() == null) {
			methodId = treeMaker.Ident(toName(node.getName()));
		} else {
			methodId = treeMaker.Select(
					toExpression(node.getOperand()),
					toName(node.getName()));
		}
		
		set(node, treeMaker.Apply(
				toList(JCExpression.class, node.getMethodTypeArguments()), 
				methodId, 
				toList(JCExpression.class, node.arguments())
		));
		return true;
	}
	
	@Override
	public boolean visitArrayInitializer(ArrayInitializer node) {
		set(node, treeMaker.NewArray(
				null,
				List.<JCExpression>nil(),
				toList(JCExpression.class, node.expressions())
		));
		return true;
	}
	
	@Override
	public boolean visitArrayCreation(ArrayCreation node) {
		int typeTrees = 0;
		List<JCExpression> dims = List.nil();
		for (JCExpression e : toList(JCExpression.class, node.dimensions())) {
			if (e == null) {
				typeTrees++;
			} else {
				dims = dims.append(e);
			}
		}
		
		List<JCExpression> init;
		if (node.getInitializer() == null) {
			init = null;
		} else {
			init = toList(JCExpression.class, node.getInitializer().expressions());
			typeTrees--;  //javac sees this as new TYPE[] {}, with both 'new' and the last [] as structure.
		}
		
		JCExpression elementType = toExpression(node.getComponentTypeReference());
		for (int i = 0; i < typeTrees; i++) {
			elementType = treeMaker.TypeArray(elementType);
		}
		set(node, treeMaker.NewArray(elementType, dims, init));
		return true;
	}
	
	@Override
	public boolean visitArrayDimension(ArrayDimension node) {
		set(node, toTree(node.getDimension()));
		return true;
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
		set(node, treeMaker.Assert(toExpression(node.getAssertion()), toExpression(node.getMessage())));
		return true;
	}
	
	@Override
	public boolean visitBreak(Break node) {
		set(node, treeMaker.Break(toName(node.getLabel())));
		return true;
	}
	
	@Override
	public boolean visitContinue(Continue node) {
		set(node, treeMaker.Continue(toName(node.getLabel())));
		return true;
	}
	
	@Override
	public boolean visitDoWhile(DoWhile node) {
		set(node, treeMaker.DoLoop(toStatement(node.getStatement()), toExpression(node.getCondition())));
		return true;
	}
	
	@Override
	public boolean visitFor(For node) {
		List<JCStatement> inits;
		List<JCExpressionStatement> updates;
		
		if (node.isVariableDeclarationBased()) {
			inits = toList(JCStatement.class, node.getVariableDeclaration());
		} else {
			inits = List.nil();
			for (JCExpression expr : toList(JCExpression.class, node.expressionInits())) {
				inits = inits.append(treeMaker.Exec(expr));
			}
		}
		
		updates = List.nil();
		for (JCExpression expr : toList(JCExpression.class, node.updates())) {
			updates = updates.append(treeMaker.Exec(expr));
		}
		
		set(node, treeMaker.ForLoop(inits, toExpression(node.getCondition()), updates, toStatement(node.getStatement())));
		return true;
	}
	
	@Override
	public boolean visitForEach(ForEach node) {
		set(node, treeMaker.ForeachLoop((JCVariableDecl) toTree(node.getVariable()), toExpression(node.getIterable()), toStatement(node.getStatement())));
		return true;
	}
	
	@Override
	public boolean visitIf(If node) {
		set(node, treeMaker.If(toExpression(node.getCondition()), toStatement(node.getStatement()), toStatement(node.getElseStatement())));
		return true;
	}
	
	@Override
	public boolean visitLabelledStatement(LabelledStatement node) {
		set(node, treeMaker.Labelled(toName(node.getLabel()), toStatement(node.getStatement())));
		return true;
	}
	
	@Override
	public boolean visitModifiers(Modifiers node) {
		if (node.isEmpty()) {
			set(node, treeMaker.Modifiers(0, List.<JCAnnotation>nil()));
		} else {
			set(node, pos(treeMaker.Modifiers(node.getExplicitModifierFlags(), toList(JCAnnotation.class, node.annotations())), node));
		}
		return true;
	}
	
	@Override
	public boolean visitKeywordModifier(KeywordModifier node) {
		set(node, treeMaker.Modifiers(getModifier(node)));
		return true;
	}
	
	@Override
	public boolean visitInstanceInitializer(InstanceInitializer node) {
		set(node, toTree(node.getBody()));
		return true;
	}
	
	@Override
	public boolean visitStaticInitializer(StaticInitializer node) {
		JCBlock block = (JCBlock) toTree(node.getBody());
		block.flags |= Flags.STATIC; 
		set(node, block);
		return true;
	}
	
	@Override
	public boolean visitBlock(Block node) {
		set(node, pos(treeMaker.Block(0, toList(JCStatement.class, node.contents())), node));
		return true;
	}
	
	@Override
	public boolean visitVariableDeclaration(VariableDeclaration node) {
		set(toList(JCVariableDecl.class, node.getDefinition()));
		return true;
	}
	
	@Override
	public boolean visitVariableDefinition(VariableDefinition node) {
		JCModifiers mods = (JCModifiers) toTree(node.getModifiers());
		JCExpression vartype = toExpression(node.getTypeReference());
		
		if (node.isVarargs()) {
			mods.flags |= Flags.VARARGS;
			vartype = addDimensions(vartype, 1);
		}
		
		List<JCVariableDecl> defs = List.nil();
		for (VariableDefinitionEntry e : node.variables()) {
			defs = defs.append(treeMaker.VarDef(mods, toName(e.getName()), addDimensions(vartype, e.getArrayDimensions()), toExpression(e.getInitializer())));
		}
		
		if (defs.isEmpty()) throw new RuntimeException("Empty VariableDefinition node");
		set(defs);
		return true;
	}
	
	@Override
	public boolean visitAnnotationDeclaration(AnnotationDeclaration node) {
		JCModifiers modifiers = (JCModifiers) toTree(node.getModifiers());
		modifiers.flags |= Flags.INTERFACE | Flags.ANNOTATION;
		set(node, treeMaker.ClassDef(
				modifiers,
				toName(node.getName()),
				List.<JCTypeParameter>nil(),
				null,
				List.<JCExpression>nil(),
				node.getBody() == null ? List.<JCTree>nil() : toList(JCTree.class, node.getBody().members())
		));
		return true;
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
		set(node, methodDef);
		return true;
	}
	
	@Override
	public boolean visitClassLiteral(ClassLiteral node) {
		set(node, treeMaker.Select((JCExpression) toTree(node.getTypeReference()), table._class));
		return true;
	}
	
	@Override
	public boolean visitAnnotationElement(AnnotationElement node) {
		JCExpression arg = toExpression(node.getValue());
		if (node.getName() != null) {
			arg = treeMaker.Assign((JCIdent) toTree(node.getName()), arg);
		}
		set(node, arg);
		return true;
	}
	
	@Override public boolean visitAnnotation(Annotation node) {
		set(node, treeMaker.Annotation(toTree(node.getAnnotationTypeReference()), toList(JCExpression.class, node.elements())));
		return true;
	}
	
	@Override
	public boolean visitTypeReference(TypeReference node) {
		WildcardKind wildcard = node.getWildcard();
		if (wildcard == WildcardKind.UNBOUND) {
			set(node, treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.UNBOUND), null));
			return true;
		}
		
		JCExpression result = plainTypeReference(node);
		
		result = addWildcards(result, wildcard);
		result = addDimensions(result, node.getArrayDimensions());
		
		set(node, result);
		return true;
	}
	
	@Override
	public boolean visitArrayAccess(ArrayAccess node) {
		set(node, treeMaker.Indexed(toExpression(node.getOperand()), toExpression(node.getIndexExpression())));
		return true;
	}
	
	private JCExpression addDimensions(JCExpression type, int dimensions) {
		JCExpression resultingType = type;
		for (int i = 0; i < dimensions; i++) {
			resultingType = treeMaker.TypeArray(resultingType);
		}
		return resultingType;
	}
	
	private JCExpression plainTypeReference(TypeReference node) {
		if (node.isPrimitive() || node.parts().size() == 1) {
			Identifier identifier = node.parts().first().getIdentifier();
			int typeTag = primitiveTypeTag(identifier.getName());
			if (typeTag > 0) return treeMaker.TypeIdent(typeTag);
		}
		
		List<JCExpression> list = toList(JCExpression.class, node.parts());
		if (list.size() == 1) return list.head;
		
		JCExpression previous = null;
		for (JCExpression part : list) {
			if (previous == null) {
				previous = part;
			} else {
				if (part instanceof JCIdent) {
					previous = treeMaker.Select(previous, ((JCIdent)part).name);
				} else if (part instanceof JCTypeApply) {
					JCTypeApply apply = (JCTypeApply)part;
					apply.clazz = treeMaker.Select(previous, ((JCIdent)apply.clazz).name);
					previous = apply;
				} else {
					throw new IllegalStateException("Didn't expect a " + part.getClass().getName() + " in " + node);
				}
			}
		}
		return previous;
	}
	
	private JCExpression addWildcards(JCExpression type, WildcardKind wildcardKind) {
		switch (wildcardKind) {
		case NONE:
			return type;
		case EXTENDS:
			return treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.EXTENDS), type);
		case SUPER:
			return treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.SUPER), type);
		default:
			throw new IllegalStateException("Unexpected unbound wildcard: " + wildcardKind);
		}
	}
	
	@Override
	public boolean visitTypeReferencePart(TypeReferencePart node) {
		JCIdent ident = treeMaker.Ident(toName(node.getIdentifier()));
		
		List<JCExpression> typeArguments = toList(JCExpression.class, node.getTypeArguments());
		if (typeArguments.isEmpty()) {
			set(node, ident);
		} else {
			set(node, treeMaker.TypeApply(ident, typeArguments));
		}
		return true;
	}
	
	@Override
	public boolean visitTypeArguments(TypeArguments node) {
		set(toList(JCExpression.class, node.generics()));
		return true;
	}
	
	@Override
	public boolean visitTypeVariable(TypeVariable node) {
		set(node, treeMaker.TypeParameter(toName(node.getName()), toList(JCExpression.class, node.extending())));
		return true;
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
		set(node, methodDef);
		return true;
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
		set(node, constrDef);
		return true;
	}
	
	@Override
	public boolean visitReturn(Return node) {
		set(node, treeMaker.Return(toExpression(node.getValue())));
		return true;
	}
	
	@Override
	public boolean visitSwitch(Switch node) {
		List<JCCase> cases = List.nil();
		
		JCExpression currentPat = null;
		List<JCStatement> stats = null;
		boolean preamble = true;
		
		for (Statement s : node.getBody().contents()) {
			if (s instanceof Case || s instanceof Default) {
				JCExpression newPat = (s instanceof Default) ? null : toExpression(((Case)s).getCondition());
				if (preamble) {
					preamble = false;
				} else {
					cases = cases.append(treeMaker.Case(currentPat, stats));
				}
				stats = List.nil();
				currentPat = newPat;
			} else {
				if (preamble) {
					throw new RuntimeException("switch body does not start with default/case.");
				}
				stats = stats.append(toStatement(s));
			}
		}
		
		if (!preamble) cases = cases.append(treeMaker.Case(currentPat, stats));
		
		set(node, treeMaker.Switch(toExpression(node.getCondition()), cases));
		return true;
	}
	
	@Override
	public boolean visitSynchronized(Synchronized node) {
		set(node, treeMaker.Synchronized(toExpression(node.getLock()), (JCBlock)toTree(node.getBody()))); 
		
		return true;
	}
	
	@Override
	public boolean visitThis(This node) {
		JCTree tree;
		if (node.getQualifier() != null) {
			tree = treeMaker.Select((JCExpression) toTree(node.getQualifier()), table._this);
		} else {
			tree = treeMaker.Ident(table._this);
		}
		set(node, tree);
		return true;
	}
	
	@Override
	public boolean visitTry(Try node) {
		List<JCCatch> catches = toList(JCCatch.class, node.catches());
		
		set(node, treeMaker.Try((JCBlock) toTree(node.getBody()), catches, (JCBlock) toTree(node.getFinally())));
		return true;
	}
	
	@Override
	public boolean visitCatch(Catch node) {
		JCVariableDecl exceptionDeclaration = (JCVariableDecl) toTree(node.getExceptionDeclaration());
		exceptionDeclaration.getModifiers().flags |= Flags.PARAMETER;
		set(node, treeMaker.Catch(exceptionDeclaration, (JCBlock) toTree(node.getBody())));
		return true;
	}
	
	@Override
	public boolean visitThrow(Throw node) {
		set(node, treeMaker.Throw(toExpression(node.getThrowable())));
		return true;
	}
	
	@Override
	public boolean visitWhile(While node) {
		set(node, treeMaker.WhileLoop(toExpression(node.getCondition()), toStatement(node.getStatement())));
		return true;
	}
	
	@Override
	public boolean visitEmptyDeclaration(EmptyDeclaration node) {
		if (node.getParent() instanceof CompilationUnit) {
			set(node, pos(treeMaker.Skip(), node));
		} else {
			set(node, posNone(treeMaker.Block(0, List.<JCStatement>nil()), node));
		}
		return true;
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
				previous = pos(treeMaker.Ident(next), part);
			} else {
				previous = posDot(treeMaker.Select(previous, next), part);
			}
		}
		return previous;
	}
	
	@SuppressWarnings("all")
	private JCTree dummy() {
		return treeMaker.Ident(table.fromString("<dummy>"));
	}
	
	private <T extends JCTree> T pos(T jcTree, Node node) {
		return setPos(jcTree, node.getPosition().getStart());
	}
	
	private <T extends JCTree> T posDot(T jcTree, Node node) {
		return setPos(jcTree, node.getPosition().getStart() - 1);
	}
	
	private <T extends JCTree> T posNone(T jcTree, Node node) {
		return jcTree;
	}
	
	private <T extends JCTree> T posParen(T jcTree, Node node, int iteration) {
		return setPos(jcTree, node.getPosition().getStart() - 1 - iteration);
	}
	
	private JCFieldAccess posStar(JCFieldAccess jcTree, ImportDeclaration node) {
		return setPos(jcTree, node.parts().last().getPosition().getEnd());
	}
	private JCTree posClass(JCClassDecl jcTree, ClassDeclaration node) {
//		return setPos(jcTree, node.);
		if (node.getModifiers().isEmpty()) {
			return pos(jcTree, node);
		}
		return setPos(jcTree, node.getModifiers().getPosition().getEnd() + 1);
	}
	
	private <T extends JCTree> T setPos(T jcTree, int position) {
		if (includePositions) {
			jcTree.pos = position;
		}
		return jcTree;
	}
}
