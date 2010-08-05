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
package lombok.ast.javac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import lombok.ast.ArrayAccess;
import lombok.ast.ArrayCreation;
import lombok.ast.ArrayDimension;
import lombok.ast.ArrayInitializer;
import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.Block;
import lombok.ast.BooleanLiteral;
import lombok.ast.Cast;
import lombok.ast.CharLiteral;
import lombok.ast.ClassDeclaration;
import lombok.ast.ClassLiteral;
import lombok.ast.CompilationUnit;
import lombok.ast.ConstructorInvocation;
import lombok.ast.EmptyDeclaration;
import lombok.ast.EmptyStatement;
import lombok.ast.Expression;
import lombok.ast.ExpressionStatement;
import lombok.ast.FloatingPointLiteral;
import lombok.ast.Identifier;
import lombok.ast.ImportDeclaration;
import lombok.ast.InlineIfExpression;
import lombok.ast.InstanceInitializer;
import lombok.ast.InstanceOf;
import lombok.ast.IntegralLiteral;
import lombok.ast.KeywordModifier;
import lombok.ast.MethodInvocation;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.NormalTypeBody;
import lombok.ast.NullLiteral;
import lombok.ast.PackageDeclaration;
import lombok.ast.Position;
import lombok.ast.RawListAccessor;
import lombok.ast.Select;
import lombok.ast.StaticInitializer;
import lombok.ast.StrictListAccessor;
import lombok.ast.StringLiteral;
import lombok.ast.This;
import lombok.ast.TypeDeclaration;
import lombok.ast.TypeReference;
import lombok.ast.TypeReferencePart;
import lombok.ast.TypeVariable;
import lombok.ast.UnaryExpression;
import lombok.ast.UnaryOperator;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.VariableReference;
import lombok.ast.WildcardKind;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCArrayTypeTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCSkip;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.util.List;

public class JcTreeConverter extends JCTree.Visitor {
	private enum FlagKey {
		BLOCKS_ARE_INITIALIZERS,
		SKIP_IS_DECL,
		VARDEF_IS_DEFINITION,
		TYPE_REFERENCE;
	}
	
	private java.util.List<? extends Node> result;
	private Map<JCTree, Integer> endPosTable;
	
	private JcTreeConverter() {}
	
	private Map<FlagKey, Object> params;
	
	private boolean hasFlag(FlagKey key) {
		return params.containsKey(key);
	}
	
	@SuppressWarnings("unused")
	private Object getFlag(FlagKey key) {
		return params.get(key);
	}
	
	@Override public void visitTree(JCTree node) {
		throw new UnsupportedOperationException("visit" + node.getClass().getSimpleName() + " not implemented");
	}
	
	java.util.List<? extends Node> getAll() {
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
	
	private void set(JCTree node, Node value) {
		if (result != null) throw new IllegalStateException("result is already set");
		
		if (value != null && value.getPosition().isUnplaced()) setPos(node, value);
		
		java.util.List<Node> result = Lists.newArrayList();
		if (value != null) result.add(value);
		this.result = result;
	}
	
	private Node toTree(JCTree node, FlagKey... keys) {
		Map<FlagKey, Object> map = Maps.newEnumMap(FlagKey.class);
		for (FlagKey key : keys) map.put(key, key);
		return toTree(node, map);
	}
	
	private Node toTree(JCTree node, Map<FlagKey, Object> params) {
		if (node == null) return null;
		JcTreeConverter visitor = new JcTreeConverter();
		visitor.endPosTable = endPosTable;
		if (params != null) visitor.params = params;
		node.accept(visitor);
		try {
			return visitor.get();
		} catch (RuntimeException e) {
			System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
			throw e;
		}
	}
	
	private Node toVariableDefinition(java.util.List<JCVariableDecl> decls, FlagKey... keys) {
		boolean createDeclaration = true;
		for (FlagKey key : keys) if (key == FlagKey.VARDEF_IS_DEFINITION) createDeclaration = false;
		
		if (decls == null || decls.isEmpty()) {
			return createDeclaration ? new VariableDeclaration() : new VariableDefinition();
		}
		
		JCVariableDecl first = decls.get(0);
		int startPosFirst = first.pos;
		
		JCExpression baseType = first.vartype;
		while (baseType instanceof JCArrayTypeTree) {
			// if written as int[] a[], b; then the base Type is *NOT* a's type, but a's type with any number of JCATT's dewrapped.
			// The only way to tell the difference is by checking positions, unfortunately: If end pos of type is after start pos of decl, it's a split.
			int endPosType = baseType.getEndPosition(endPosTable);
			if (endPosType != -1 && startPosFirst != -1 && endPosType > startPosFirst) {
				baseType = ((JCArrayTypeTree) baseType).elemtype;
			} else {
				break;
			}
		}
		
		VariableDefinition def = new VariableDefinition();
		def.astModifiers((Modifiers) toTree(first.mods));
		def.rawTypeReference(toTree(baseType, FlagKey.TYPE_REFERENCE));
		def.astVarargs((first.mods.flags & Flags.VARARGS) != 0);
		
		int baseDims = countDims(baseType);
		
		for (JCVariableDecl varDecl : decls) {
			int extraDims = countDims(varDecl.vartype) - baseDims;
			VariableDefinitionEntry entry = new VariableDefinitionEntry();
			entry.astArrayDimensions(extraDims);
			entry.astName(setPos(varDecl, new Identifier().astValue(varDecl.name.toString())));
			entry.rawInitializer(toTree(varDecl.init));
			def.astVariables().addToEnd(entry);
		}
		
		if (createDeclaration) {
			 return new VariableDeclaration().astDefinition(def);
		}
		
		return def;
	}
	
	private static int countDims(JCExpression type) {
		int dims = 0;
		while (type instanceof JCArrayTypeTree) {
			type = ((JCArrayTypeTree) type).elemtype;
			dims++;
		}
		return dims;
	}
	
	private void fillList(List<? extends JCTree> nodes, RawListAccessor<?, ?> list, FlagKey... keys) {
		if (nodes == null) return;
		
		// int i, j; is represented with multiple JCVariableDeclarations, but in lombok.ast, it's 1 node. We need to
		// gather up sequential JCVD nodes, check if their modifier objects are == equal, and call a special method
		// to convert them.
		java.util.List<JCVariableDecl> varDeclQueue = new ArrayList<JCVariableDecl>();
		
		for (JCTree node : nodes) {
			if (node instanceof JCVariableDecl) {
				if (varDeclQueue.isEmpty() || varDeclQueue.get(0).mods == ((JCVariableDecl) node).mods) {
					varDeclQueue.add((JCVariableDecl) node);
					continue;
				} else {
					if (!varDeclQueue.isEmpty()) list.addToEnd(toVariableDefinition(varDeclQueue, keys));
					varDeclQueue.clear();
					varDeclQueue.add((JCVariableDecl) node);
					continue;
				}
			}
			
			if (!varDeclQueue.isEmpty()) list.addToEnd(toVariableDefinition(varDeclQueue, keys));
			varDeclQueue.clear();
			list.addToEnd(toTree(node, keys));
		}
		
		if (!varDeclQueue.isEmpty()) list.addToEnd(toVariableDefinition(varDeclQueue, keys));
	}
	
	public static Node convert(JCCompilationUnit cu) {
		return convert(cu, cu.endPositions);
	}
	
	public static Node convert(JCTree node, Map<JCTree, Integer> endPosTable) {
		JcTreeConverter converter = new JcTreeConverter();
		converter.endPosTable = endPosTable;
		node.accept(converter);
		return converter.get();
	}
	
	private <N extends Node> N setPos(JCTree node, N astNode) {
		if (astNode != null && node != null) {
			int start = node.pos;
			int end = node.getEndPosition(endPosTable);
			if (start != com.sun.tools.javac.util.Position.NOPOS && end != com.sun.tools.javac.util.Position.NOPOS) {
				astNode.setPosition(new Position(start, end));
			}
		}
		return astNode;
	}
	
	private void fillWithIdentifiers(JCTree node, StrictListAccessor<Identifier, ?> list) {
		if (node instanceof JCIdent) {
			JCIdent id = (JCIdent) node;
			list.addToEnd(setPos(node, new Identifier().astValue(id.name.toString())));
		} else if (node instanceof JCFieldAccess) {
			JCFieldAccess sel = (JCFieldAccess) node;
			fillWithIdentifiers(sel.selected, list);
			list.addToEnd(setPos(node, new Identifier().astValue(sel.name.toString())));
		}
	}
	
	@Override public void visitTopLevel(JCCompilationUnit node) {
		CompilationUnit unit = new CompilationUnit();
		if (node.pid != null) {
			PackageDeclaration pkg = new PackageDeclaration();
			fillWithIdentifiers(node.pid, pkg.astParts());
			unit.astPackageDeclaration(setPos(node.pid, pkg));
			fillList(node.packageAnnotations, pkg.rawAnnotations());
		}
		
		for (JCTree def : node.defs) {
			if (def instanceof JCImport) {
				unit.rawImportDeclarations().addToEnd(toTree(def));
			} else {
				unit.rawTypeDeclarations().addToEnd(toTree(def, FlagKey.SKIP_IS_DECL));
			}
		}
		
		set(node, unit);
	}
	
	@Override public void visitImport(JCImport node) {
		ImportDeclaration imp = new ImportDeclaration();
		fillWithIdentifiers(node.getQualifiedIdentifier(), imp.astParts());
		Identifier last = imp.astParts().last();
		if (last != null && "*".equals(last.astValue())) {
			imp.astParts().remove(last);
			imp.astStarImport(true);
		}
		imp.astStaticImport(node.isStatic());
		set(node, imp);
	}
	
	@Override public void visitClassDef(JCClassDecl node) {
		long flags = node.mods.flags;
		TypeDeclaration typeDecl;
		if ((flags & (Flags.ENUM | Flags.INTERFACE)) == 0) {
			ClassDeclaration classDecl = new ClassDeclaration();
			typeDecl = classDecl;
			fillList(node.implementing, classDecl.rawImplementing(), FlagKey.TYPE_REFERENCE);
			classDecl.rawExtending(toTree(node.extending, FlagKey.TYPE_REFERENCE));
			fillList(node.typarams, classDecl.rawTypeVariables());
			NormalTypeBody body = new NormalTypeBody();
			fillList(node.defs, body.rawMembers(), FlagKey.BLOCKS_ARE_INITIALIZERS, FlagKey.SKIP_IS_DECL);
			classDecl.astBody(body);
		} else {
			visitTree(node);
			return;
		}
		
		typeDecl.astName(new Identifier().astValue(node.name.toString()));
		typeDecl.astModifiers((Modifiers) toTree(node.mods));
		set(node, typeDecl);
	}
	
	@Override public void visitModifiers(JCModifiers node) {
		Modifiers m = new Modifiers();
		fillList(node.annotations, m.rawAnnotations());
		for (KeywordModifier mod : KeywordModifier.fromReflectModifiers((int) node.flags)) m.astKeywords().addToEnd(mod);
		set(node, m);
	}
	
	@Override public void visitBlock(JCBlock node) {
		Node n;
		Block b = new Block();
		fillList(node.stats, b.rawContents());
		if (hasFlag(FlagKey.BLOCKS_ARE_INITIALIZERS)) {
			if ((node.flags & Flags.STATIC) != 0) {
				n = setPos(node, new StaticInitializer().astBody(b));
			} else {
				// For some strange reason, solitary ; in a type body are represented not as JCSkips, but as JCBlocks with no endpos. Don't ask me why!
				if (b.rawContents().isEmpty() && node.endpos == -1) {
					n = setPos(node, new EmptyDeclaration());
				} else {
					n = setPos(node, new InstanceInitializer().astBody(b));
				}
			}
		} else {
			n = b;
		}
		set(node, n);
	}
	
	@Override public void visitSkip(JCSkip node) {
		if (hasFlag(FlagKey.SKIP_IS_DECL)) {
			set(node, new EmptyDeclaration());
		} else {
			set(node, new EmptyStatement());
		}
	}
	
	@Override public void visitVarDef(JCVariableDecl node) {
		if (hasFlag(FlagKey.VARDEF_IS_DEFINITION)) {
			set(node, toVariableDefinition(Collections.singletonList(node), FlagKey.VARDEF_IS_DEFINITION));
		} else {
			set(node, toVariableDefinition(Collections.singletonList(node)));
		}
	}
	
	@Override public void visitTypeIdent(JCPrimitiveTypeTree node) {
		String primitiveType = JcTreeBuilder.PRIMITIVES.inverse().get(node.typetag);
		
		if (primitiveType == null) throw new IllegalArgumentException("Uknown primitive type tag: " + node.typetag);
		
		TypeReferencePart part = setPos(node, new TypeReferencePart().astIdentifier(setPos(node, new Identifier().astValue(primitiveType))));
		
		set(node, new TypeReference().astParts().addToEnd(part));
	}
	
	@Override public void visitIdent(JCIdent node) {
		String name = node.getName().toString();
		
		if ("this".equals(name)) {
			set(node, new This());
			return;
		}
		
		Identifier id = setPos(node, new Identifier().astValue(name));
		
		if (hasFlag(FlagKey.TYPE_REFERENCE)) {
			TypeReferencePart part = setPos(node, new TypeReferencePart().astIdentifier(id));
			set(node, new TypeReference().astParts().addToEnd(part));
			return;
		}
		
		set(node, new VariableReference().astIdentifier(id));
	}
	
	@Override public void visitSelect(JCFieldAccess node) {
		String name = node.getIdentifier().toString();
		
		Identifier id = setPos(node, new Identifier().astValue(name));
		Node selected = toTree(node.selected, params);
		
		if (hasFlag(FlagKey.TYPE_REFERENCE)) {
			TypeReference parent = (TypeReference) selected;
			parent.astParts().addToEnd(setPos(node, new TypeReferencePart().astIdentifier(id)));
			set(node, parent);
			return;
		}
		
		if ("this".equals(name)) {
			set(node, new This().rawQualifier(toTree(node.getExpression(), FlagKey.TYPE_REFERENCE)));
			return;
		}
		
		if ("class".equals(name)) {
			set(node, new ClassLiteral().rawTypeReference(toTree(node.getExpression(), FlagKey.TYPE_REFERENCE)));
			return;
		}
		
		set(node, new Select().astIdentifier(id).rawOperand(toTree(node.getExpression())));
	}
	
	@Override public void visitTypeApply(JCTypeApply node) {
		TypeReference ref = (TypeReference) toTree(node.clazz, FlagKey.TYPE_REFERENCE);
		TypeReferencePart last = ref.astParts().last();
		fillList(node.arguments, last.rawTypeArguments(), FlagKey.TYPE_REFERENCE);
		set(node, ref);
	}
	
	@Override public void visitWildcard(JCWildcard node) {
		TypeReference ref = (TypeReference) toTree(node.getBound(), FlagKey.TYPE_REFERENCE);
		if (ref == null) ref = new TypeReference();
		switch (node.getKind()) {
		case UNBOUNDED_WILDCARD:
			ref.astWildcard(WildcardKind.UNBOUND);
			break;
		case EXTENDS_WILDCARD:
			ref.astWildcard(WildcardKind.EXTENDS);
			break;
		case SUPER_WILDCARD:
			ref.astWildcard(WildcardKind.SUPER);
			break;
		}
		set(node, ref);
	}
	
	@Override public void visitTypeParameter(JCTypeParameter node) {
		TypeVariable var = new TypeVariable();
		var.astName(setPos(node, new Identifier().astValue(node.name.toString())));
		fillList(node.bounds, var.rawExtending(), FlagKey.TYPE_REFERENCE);
		set(node, var);
	}
	
	@Override public void visitTypeArray(JCArrayTypeTree node) {
		TypeReference ref = (TypeReference) toTree(node.getType(), FlagKey.TYPE_REFERENCE);
		ref.astArrayDimensions(ref.astArrayDimensions() + 1);
		set(node, ref);
	}
	
	@Override public void visitLiteral(JCLiteral node) {
		Object val = node.getValue();
		boolean negative = false;
		Expression literal = null;
		switch (node.getKind()) {
		case INT_LITERAL:
			int intValue = ((Number)val).intValue();
			negative = intValue < 0;
			if (intValue == Integer.MIN_VALUE) literal = new IntegralLiteral().astIntValue(Integer.MIN_VALUE);
			else if (negative) literal = new IntegralLiteral().astIntValue(-intValue);
			else literal = new IntegralLiteral().astIntValue(intValue);
			break;
		case LONG_LITERAL:
			long longValue = ((Number)val).longValue();
			negative = longValue < 0;
			if (longValue == Long.MIN_VALUE) literal = new IntegralLiteral().astLongValue(Long.MIN_VALUE);
			else if (negative) literal = new IntegralLiteral().astLongValue(-longValue);
			else literal = new IntegralLiteral().astLongValue(longValue);
			break;
		case FLOAT_LITERAL:
			set(node, new FloatingPointLiteral().astFloatValue(((Number)val).floatValue()));
			return;
		case DOUBLE_LITERAL:
			set(node, new FloatingPointLiteral().astDoubleValue(((Number)val).doubleValue()));
			return;
		case BOOLEAN_LITERAL:
			set(node, new BooleanLiteral().astValue((Boolean)val));
			return;
		case CHAR_LITERAL:
			set(node, new CharLiteral().astValue((Character)val));
			return;
		case STRING_LITERAL:
			set(node, new StringLiteral().astValue(val == null ? "" : val.toString()));
			return;
		case NULL_LITERAL:
			set(node, new NullLiteral());
			return;
		}
		
		if (literal != null) {
			if (negative) set(node, new UnaryExpression().astOperand(setPos(node, literal)).astOperator(UnaryOperator.UNARY_MINUS));
			else set(node, literal);
		} else {
			throw new IllegalArgumentException("Unknown JCLiteral type tag:" + node.typetag);
		}
	}
	
	@Override public void visitParens(JCParens node) {
		Expression expr = (Expression) toTree(node.getExpression());
		expr.astParensPositions().add(new Position(node.pos, node.getEndPosition(endPosTable)));
		set(node, expr);
	}
	
	@Override public void visitTypeCast(JCTypeCast node) {
		Cast cast = new Cast();
		cast.rawOperand(toTree(node.getExpression()));
		cast.rawTypeReference(toTree(node.getType(), FlagKey.TYPE_REFERENCE));
		set(node, cast);
	}
	
	@Override public void visitUnary(JCUnary node) {
		UnaryExpression expr = new UnaryExpression();
		expr.rawOperand(toTree(node.getExpression()));
		expr.astOperator(JcTreeBuilder.UNARY_OPERATORS.inverse().get(node.getTag()));
		set(node, expr);
	}
	
	@Override public void visitBinary(JCBinary node) {
		BinaryExpression expr = new BinaryExpression();
		expr.rawLeft(toTree(node.getLeftOperand()));
		expr.rawRight(toTree(node.getRightOperand()));
		expr.astOperator(JcTreeBuilder.BINARY_OPERATORS.inverse().get(node.getTag()));
		set(node, expr);
	}
	
	@Override public void visitNewClass(JCNewClass node) {
		ConstructorInvocation inv = new ConstructorInvocation();
		fillList(node.getArguments(), inv.rawArguments());
		fillList(node.getTypeArguments(), inv.rawConstructorTypeArguments());
		inv.rawTypeReference(toTree(node.getIdentifier(), FlagKey.TYPE_REFERENCE));
		inv.rawQualifier(toTree(node.getEnclosingExpression()));
		inv.rawAnonymousClassBody(toTree(node.getClassBody()));
		set(node, inv);
	}
	
	@Override public void visitTypeTest(JCInstanceOf node) {
		InstanceOf io = new InstanceOf();
		io.rawTypeReference(toTree(node.getType(), FlagKey.TYPE_REFERENCE));
		io.rawObjectReference(toTree(node.getExpression()));
		set(node, io);
	}
	
	@Override public void visitConditional(JCConditional node) {
		InlineIfExpression iie = new InlineIfExpression();
		iie.rawCondition(toTree(node.getCondition()));
		iie.rawIfTrue(toTree(node.getTrueExpression()));
		iie.rawIfFalse(toTree(node.getFalseExpression()));
		set(node, iie);
	}
	
	@Override public void visitAssign(JCAssign node) {
		BinaryExpression expr = new BinaryExpression();
		expr.rawRight(toTree(node.getExpression()));
		expr.rawLeft(toTree(node.getVariable()));
		expr.astOperator(BinaryOperator.ASSIGN);
		set(node, expr);
	}
	
	@Override public void visitAssignop(JCAssignOp node) {
		BinaryExpression expr = new BinaryExpression();
		expr.rawRight(toTree(node.getExpression()));
		expr.rawLeft(toTree(node.getVariable()));
		expr.astOperator(JcTreeBuilder.BINARY_OPERATORS.inverse().get(node.getTag()));
		set(node, expr);
	}
	
	@Override public void visitExec(JCExpressionStatement node) {
		ExpressionStatement exec = new ExpressionStatement();
		exec.rawExpression(toTree(node.getExpression()));
		set(node, exec);
	}
	
	@Override public void visitApply(JCMethodInvocation node) {
		MethodInvocation inv = new MethodInvocation();
		JCTree sel = node.getMethodSelect();
		Identifier id = new Identifier();
		if (sel instanceof JCIdent) {
			setPos(sel, id.astValue(((JCIdent) sel).getName().toString()));
			sel = null;
		} else if (sel instanceof JCFieldAccess) {
			setPos(sel, id.astValue(((JCFieldAccess) sel).getIdentifier().toString()));
			sel = ((JCFieldAccess) sel).getExpression();
		}
		inv.astName(id).rawOperand(toTree(sel));
		fillList(node.getTypeArguments(), inv.rawMethodTypeArguments());
		fillList(node.getArguments(), inv.rawArguments());
		set(node, inv);
	}
	
	@Override public void visitNewArray(JCNewArray node) {
		ArrayInitializer init = null;
		
		if (node.getInitializers() != null) {
			init = setPos(node, new ArrayInitializer());
			fillList(node.getInitializers(), init.rawExpressions());
		}
		
		if (node.getType() == null) {
			set(node, init == null ? new ArrayInitializer() : init);
			return;
		}
		
		ArrayCreation crea = new ArrayCreation();
		int inits = 0;
		JCTree type = node.getType();
		while (type instanceof JCArrayTypeTree) {
			inits++;
			type = ((JCArrayTypeTree) type).getType();
		}
		
		crea.rawComponentTypeReference(toTree(type, FlagKey.TYPE_REFERENCE));
		if (node.getDimensions() != null) for (JCExpression dim : node.getDimensions()) {
			crea.astDimensions().addToEnd(new ArrayDimension().rawDimension(toTree(dim)));
		}
		// new boolean [][][] {} in javac has one less dimension for some reason.
		for (int i = 0; i < inits || (i == inits && init != null); i++) crea.astDimensions().addToEnd(new ArrayDimension());
		crea.astInitializer(init);
		set(node, crea);
	}
	
	@Override public void visitIndexed(JCArrayAccess node) {
		ArrayAccess aa = new ArrayAccess();
		aa.rawIndexExpression(toTree(node.getIndex()));
		aa.rawOperand(toTree(node.getExpression()));
		set(node, aa);
	}
}
