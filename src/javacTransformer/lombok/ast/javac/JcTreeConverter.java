/*
 * Copyright © 2010-2011 Reinier Zwitserloot, Roel Spilker and Robbert Jan Grootjans.
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
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
import lombok.ast.Comment;
import lombok.ast.CompilationUnit;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.ConstructorInvocation;
import lombok.ast.Continue;
import lombok.ast.ConversionPositionInfo;
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
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.NormalTypeBody;
import lombok.ast.NullLiteral;
import lombok.ast.PackageDeclaration;
import lombok.ast.Position;
import lombok.ast.RawListAccessor;
import lombok.ast.Return;
import lombok.ast.Select;
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
import lombok.ast.While;
import lombok.ast.WildcardKind;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCArrayTypeTree;
import com.sun.tools.javac.tree.JCTree.JCAssert;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCSkip;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCSynchronized;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;
import com.sun.tools.javac.util.List;

import static lombok.ast.ConversionPositionInfo.setConversionPositionInfo;

public class JcTreeConverter {
	private enum FlagKey {
		BLOCKS_ARE_INITIALIZERS,
		SKIP_IS_DECL,
		VARDEF_IS_DEFINITION,
		NO_VARDECL_FOLDING,
		CONTAINING_TYPE_NAME,
		TYPE_REFERENCE,
		METHODS_ARE_ANNMETHODS;
	}
	
	private java.util.List<? extends Node> result;
	private Map<JCTree, Integer> endPosTable;
	private ConvertingVisitor visitor = new ConvertingVisitor();
	private Map<FlagKey, Object> params;
	
	private static final Field JCWILDCARD_KIND, JCTREE_TAG;
	private static final Method JCTREE_GETTAG;
	static {
		Field f;
		Method m;
		
		f = null;
		try {
			f = JCWildcard.class.getDeclaredField("kind");
		} catch (NoSuchFieldException e) {}
		JCWILDCARD_KIND = f;
		
		f = null;
		try {
			f = JCTree.class.getDeclaredField("tag");
		} catch (NoSuchFieldException e) {}
		JCTREE_TAG = f;
		
		m = null;
		try {
			m = JCTree.class.getDeclaredMethod("getTag");
		} catch (NoSuchMethodException e) {}
		JCTREE_GETTAG = m;
	}
	
	public JcTreeConverter() {
		this.params = ImmutableMap.of();
	}
	
	public JcTreeConverter(Map<JCTree, Integer> endPosTable, Map<FlagKey, Object> params) {
		this.endPosTable = endPosTable;
		this.params = params == null ? ImmutableMap.<FlagKey, Object>of() : params;
	}
	
	private boolean hasFlag(FlagKey key) {
		return params.containsKey(key);
	}
	
	private Object getFlag(FlagKey key) {
		return params.get(key);
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
		JcTreeConverter newConverter = new JcTreeConverter(endPosTable, params);
		node.accept(newConverter.visitor);
		try {
			return newConverter.get();
		} catch (RuntimeException e) {
			System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
			throw e;
		}
	}
	
	private void addJavadoc(JavadocContainer container, JCModifiers mods) {
		if ((mods.flags & Flags.DEPRECATED) != 0) {
			container.astJavadoc(new Comment().astBlockComment(true).astContent("*\n * @deprecated\n "));
		}
	}
	
	private Node toVariableDefinition(java.util.List<JCVariableDecl> decls, FlagKey... keys) {
		Map<FlagKey, Object> map = Maps.newEnumMap(FlagKey.class);
		for (FlagKey key : keys) map.put(key, key);
		return toVariableDefinition(decls, map);
	}
	
	private Position getPosition(JCTree node) {
		if (node == null) return Position.UNPLACED;
		int start = node.pos;
		Integer end_ = null;
		if (endPosTable != null) end_ = endPosTable.get(node);
		int end = end_ == null ? node.getEndPosition(endPosTable) : end_;
		return new Position(start, end);
	}
	
	private Node toVariableDefinition(java.util.List<JCVariableDecl> decls, Map<FlagKey, Object> keys) {
		boolean createDeclaration = !keys.containsKey(FlagKey.VARDEF_IS_DEFINITION);
		
		if (decls == null || decls.isEmpty()) {
			VariableDefinition def = new VariableDefinition();
			return createDeclaration ? new VariableDeclaration().astDefinition(def) : def;
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
		setPos(decls.get(decls.size()-1), def);
		if (decls.size() > 1) def.setPosition(new Position(startPosFirst, def.getPosition().getEnd()));
		int baseDims = countDims(baseType);
		if ((first.mods.flags & Flags.VARARGS) != 0) {
			def.astVarargs(true);
			setConversionPositionInfo(def, "...", getPosition(baseType));
			if (baseType instanceof JCArrayTypeTree) baseType = ((JCArrayTypeTree) baseType).elemtype;
		}
		def.rawTypeReference(toTree(baseType, FlagKey.TYPE_REFERENCE));
		def.astVarargs((first.mods.flags & Flags.VARARGS) != 0);
		
		for (JCVariableDecl varDecl : decls) {
			int extraDims = countDims(varDecl.vartype) - baseDims;
			VariableDefinitionEntry entry = new VariableDefinitionEntry();
			entry.astArrayDimensions(extraDims);
			entry.astName(setPos(varDecl, new Identifier().astValue(varDecl.name.toString())));
			entry.rawInitializer(toTree(varDecl.init));
			setPos(varDecl, entry);
			if (extraDims > 0) {
				JCArrayTypeTree arrayType = (JCArrayTypeTree) varDecl.vartype;
				for (int i = 0; i < extraDims; i++) {
					if (arrayType != null) setConversionPositionInfo(entry, "[]" + (extraDims - i - 1), getPosition(arrayType));
					arrayType = arrayType.elemtype instanceof JCArrayTypeTree ? (JCArrayTypeTree) arrayType.elemtype : null;
				}
				}
			def.astVariables().addToEnd(entry);
		}
		
		if (createDeclaration) {
			VariableDeclaration decl = new VariableDeclaration().astDefinition(def);
			decl.setPosition(def.getPosition());
			addJavadoc(decl, first.mods);
			return decl;
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
	
	private void fillList(java.util.List<? extends JCTree> nodes, RawListAccessor<?, ?> list, FlagKey... keys) {
		Map<FlagKey, Object> map = Maps.newEnumMap(FlagKey.class);
		for (FlagKey key : keys) map.put(key, key);
		fillList(nodes, list, map);
	}
	
	private void fillList(java.util.List<? extends JCTree> nodes, RawListAccessor<?, ?> list, Map<FlagKey, Object> keys) {
		if (nodes == null) return;
		
		// int i, j; is represented with multiple JCVariableDeclarations, but in lombok.ast, it's 1 node. We need to
		// gather up sequential JCVD nodes, check if their modifier objects are == equal, and call a special method
		// to convert them.
		java.util.List<JCVariableDecl> varDeclQueue = new ArrayList<JCVariableDecl>();
		
		boolean fold = !keys.containsKey(FlagKey.NO_VARDECL_FOLDING);
		
		for (JCTree node : nodes) {
			if (node instanceof JCVariableDecl) {
				if (fold && (varDeclQueue.isEmpty() || varDeclQueue.get(0).mods == ((JCVariableDecl) node).mods)) {
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
	
	private static JCTree removeParens(JCTree node) {
		if (!(node instanceof JCParens)) return node;
		return ((JCParens) node).getExpression();
	}
	
	public void visit(JCCompilationUnit cu) {
		visit(cu, cu.endPositions);
	}
	
	public void visit(JCTree node, Map<JCTree, Integer> endPosTable) {
		this.endPosTable = endPosTable;
		node.accept(visitor);
	}
	
	public Node getResult() {
		return get();
	}
	
	private <N extends Node> N setPos(JCTree node, N astNode) {
		if (astNode != null && node != null) {
			int start = node.pos;
			Integer end_ = endPosTable.get(node);
			if (node instanceof JCUnary) end_ = node.getEndPosition(endPosTable);
			int end = end_ == null ? node.getEndPosition(endPosTable) : end_;
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
	
	private static void setConversionStructureInfo(Node node, String key) {
		ConversionPositionInfo.setConversionPositionInfo(node, key, Position.UNPLACED);
	}
	
	private class ConvertingVisitor extends JCTree.Visitor {
		@Override public void visitTree(JCTree node) {
			throw new UnsupportedOperationException("visit" + node.getClass().getSimpleName() + " not implemented");
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
			
			setConversionStructureInfo(unit, "converted");
			set(node, unit);
		}
		
		@Override public void visitImport(JCImport node) {
			ImportDeclaration imp = new ImportDeclaration();
			fillWithIdentifiers(node.getQualifiedIdentifier(), imp.astParts());
			Identifier last = imp.astParts().last();
			if (last != null && "*".equals(last.astValue())) {
				imp.astParts().remove(last);
				imp.astStarImport(true);
				setConversionPositionInfo(imp, ".*", last.getPosition());
			}
			imp.astStaticImport(node.isStatic());
			set(node, imp);
		}
		
		private static final long ENUM_CONSTANT_FLAGS = Flags.PUBLIC | Flags.STATIC | Flags.FINAL | Flags.ENUM;
		
		@Override public void visitClassDef(JCClassDecl node) {
			long flags = node.mods.flags;
			String name = node.getSimpleName().toString();
			TypeDeclaration typeDecl;
			Map<FlagKey, Object> flagKeyMap = Maps.newHashMap();
			flagKeyMap.put(FlagKey.CONTAINING_TYPE_NAME, name);
			flagKeyMap.put(FlagKey.BLOCKS_ARE_INITIALIZERS, FlagKey.BLOCKS_ARE_INITIALIZERS);
			flagKeyMap.put(FlagKey.SKIP_IS_DECL, FlagKey.SKIP_IS_DECL);
			
			if ((flags & (Flags.ENUM | Flags.INTERFACE)) == 0) {
				ClassDeclaration classDecl = new ClassDeclaration();
				typeDecl = classDecl;
				fillList(node.implementing, classDecl.rawImplementing(), FlagKey.TYPE_REFERENCE);
				classDecl.rawExtending(toTree(node.extending, FlagKey.TYPE_REFERENCE));
				fillList(node.typarams, classDecl.rawTypeVariables());
				NormalTypeBody body = new NormalTypeBody();
				fillList(node.defs, body.rawMembers(), flagKeyMap);
				classDecl.astBody(body);
			} else if ((flags & Flags.ANNOTATION) != 0) {
				AnnotationDeclaration annDecl = new AnnotationDeclaration();
				typeDecl = annDecl;
				NormalTypeBody body = new NormalTypeBody();
				flagKeyMap.put(FlagKey.METHODS_ARE_ANNMETHODS, FlagKey.METHODS_ARE_ANNMETHODS);
				fillList(node.defs, body.rawMembers(), flagKeyMap);
				annDecl.astBody(body);
			} else if ((flags & Flags.INTERFACE) != 0) {
				InterfaceDeclaration itfDecl = new InterfaceDeclaration();
				typeDecl = itfDecl;
				fillList(node.typarams, itfDecl.rawTypeVariables());
				fillList(node.implementing, itfDecl.rawExtending(), FlagKey.TYPE_REFERENCE);
				NormalTypeBody body = new NormalTypeBody();
				fillList(node.defs, body.rawMembers(), flagKeyMap);
				itfDecl.astBody(body);
			} else if ((flags & Flags.ENUM) != 0) {
				EnumDeclaration enumDecl = new EnumDeclaration();
				typeDecl = enumDecl;
				EnumTypeBody body = new EnumTypeBody();
				fillList(node.implementing, enumDecl.rawImplementing(), FlagKey.TYPE_REFERENCE);
				java.util.List<JCTree> defs = new ArrayList<JCTree>();
				
				for (JCTree def : node.defs) {
					if (def instanceof JCVariableDecl) {
						JCVariableDecl vd = (JCVariableDecl) def;
						if (vd.mods != null && (vd.mods.flags & ENUM_CONSTANT_FLAGS) == ENUM_CONSTANT_FLAGS) {
							// This is an enum constant, not a field of the enum class.
							EnumConstant ec = new EnumConstant();
							setPos(def, ec);
							ec.astName(new Identifier().astValue(vd.getName().toString()));
							fillList(vd.mods.annotations, ec.rawAnnotations());
							if (vd.init instanceof JCNewClass) {
								JCNewClass init = (JCNewClass) vd.init;
								fillList(init.getArguments(), ec.rawArguments());
								if (init.getClassBody() != null) {
									NormalTypeBody constantBody = setPos(init, new NormalTypeBody());
									fillList(init.getClassBody().getMembers(), constantBody.rawMembers());
									ec.astBody(constantBody);
								}
								setConversionPositionInfo(ec, "newClass", getPosition(init));
							}
							body.astConstants().addToEnd(ec);
							continue;
						}
					}
					
					defs.add(def);
				}
				fillList(defs, body.rawMembers(), flagKeyMap);
				enumDecl.astBody(body);
			} else {
				throw new IllegalStateException("Unknown type declaration: " + node);
			}
			
			typeDecl.astName(new Identifier().astValue(name));
			typeDecl.astModifiers((Modifiers) toTree(node.mods));
			addJavadoc(typeDecl, node.mods);
			set(node, typeDecl);
		}
		
		@Override public void visitModifiers(JCModifiers node) {
			Modifiers m = new Modifiers();
			fillList(node.annotations, m.rawAnnotations());
			for (KeywordModifier mod : KeywordModifier.fromReflectModifiers((int) node.flags)) m.astKeywords().addToEnd(mod);
			setConversionStructureInfo(m, "converted");
			set(node, m);
		}
		
		@Override public void visitBlock(JCBlock node) {
			Node n;
			Block b = new Block();
			fillList(node.stats, b.rawContents());
			setPos(node, b);
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
				This t = new This();
				set(node, t);
				setConversionPositionInfo(t, "this", getPosition(node));
				return;
			}
			
			if ("super".equals(name)) {
				Super s = new Super();
				set(node, s);
				setConversionPositionInfo(s, "super", getPosition(node));
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
				This t = new This();
				setConversionPositionInfo(t, "this", getPosition(node));
				set(node, t.rawQualifier(toTree(node.getExpression(), FlagKey.TYPE_REFERENCE)));
				return;
			}
			
			if ("super".equals(name)) {
				Super s = new Super();
				setConversionPositionInfo(s, "super", getPosition(node));
				set(node, s.rawQualifier(toTree(node.getExpression(), FlagKey.TYPE_REFERENCE)));
				return;
			}
			
			if ("class".equals(name)) {
				ClassLiteral c = new ClassLiteral();
				setConversionPositionInfo(c, "class", getPosition(node));
				set(node, c.rawTypeReference(toTree(node.getExpression(), FlagKey.TYPE_REFERENCE)));
				return;
			}
			
			set(node, new Select().astIdentifier(id).rawOperand(toTree(node.getExpression())));
		}
		
		@Override public void visitTypeApply(JCTypeApply node) {
			TypeReference ref = (TypeReference) toTree(node.clazz, FlagKey.TYPE_REFERENCE);
			TypeReferencePart last = ref.astParts().last();
			fillList(node.arguments, last.rawTypeArguments(), FlagKey.TYPE_REFERENCE);
			setPos(node, ref);
			setConversionPositionInfo(last, "<", getPosition(node));
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
				setConversionPositionInfo(ref, "extends", getTypeBoundKindPosition(node));
				break;
			case SUPER_WILDCARD:
				ref.astWildcard(WildcardKind.SUPER);
				setConversionPositionInfo(ref, "super", getTypeBoundKindPosition(node));
				break;
			}
			set(node, ref);
		}
		
		private Position getTypeBoundKindPosition(JCWildcard node) {
			try {
				Object o = JCWILDCARD_KIND.get(node);
				if (o instanceof TypeBoundKind) {
					return getPosition((TypeBoundKind) o);
				}
			} catch (Exception e) {}
			return Position.UNPLACED;
		}
		
		private int getTag(JCTree node) {
			if (JCTREE_GETTAG != null) {
				try {
					return (Integer) JCTREE_GETTAG.invoke(node);
				} catch (Exception e) {}
			}
			try {
				return (Integer) JCTREE_TAG.get(node);
			} catch (Exception e) {
				throw new IllegalStateException("Can't get node tag");
			}
		}
		
		@Override public void visitTypeParameter(JCTypeParameter node) {
			TypeVariable var = new TypeVariable();
			var.astName(setPos(node, new Identifier().astValue(node.name.toString())));
			fillList(node.bounds, var.rawExtending(), FlagKey.TYPE_REFERENCE);
			set(node, var);
		}
		
		@Override public void visitTypeArray(JCArrayTypeTree node) {
			TypeReference ref = (TypeReference) toTree(node.getType(), FlagKey.TYPE_REFERENCE);
			int currentDim = ref.astArrayDimensions();
			ref.astArrayDimensions(currentDim + 1);
			setConversionPositionInfo(ref, "[]" + currentDim, getPosition(node));
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
			expr.astParensPositions().add(getPosition(node));
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
			expr.astOperator(JcTreeBuilder.UNARY_OPERATORS.inverse().get(getTag(node)));
			set(node, expr);
		}
		
		@Override public void visitBinary(JCBinary node) {
			BinaryExpression expr = new BinaryExpression();
			expr.rawLeft(toTree(node.getLeftOperand()));
			expr.rawRight(toTree(node.getRightOperand()));
			expr.astOperator(JcTreeBuilder.BINARY_OPERATORS.inverse().get(getTag(node)));
			set(node, expr);
		}
		
		@Override public void visitNewClass(JCNewClass node) {
			ConstructorInvocation inv = new ConstructorInvocation();
			fillList(node.getArguments(), inv.rawArguments());
			fillList(node.getTypeArguments(), inv.rawConstructorTypeArguments(), FlagKey.TYPE_REFERENCE);
			inv.rawTypeReference(toTree(node.getIdentifier(), FlagKey.TYPE_REFERENCE));
			inv.rawQualifier(toTree(node.getEnclosingExpression()));
			Node n = toTree(node.getClassBody());
			if (n instanceof TypeDeclaration) {
				NormalTypeBody body = ((ClassDeclaration) n).astBody();
				if (body != null) body.unparent();
				inv.rawAnonymousClassBody(setPos(node.getClassBody(), body));
			}
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
			expr.astOperator(JcTreeBuilder.BINARY_OPERATORS.inverse().get(getTag(node)));
			set(node, expr);
		}
		
		@Override public void visitExec(JCExpressionStatement node) {
			Node expr = toTree(node.getExpression());
			if (expr instanceof SuperConstructorInvocation || expr instanceof AlternateConstructorInvocation) {
				setConversionPositionInfo(expr, "exec", getPosition(node));
				set(node, expr);
				return;
			}
			ExpressionStatement exec = new ExpressionStatement();
			exec.rawExpression(expr);
			set(node, exec);
		}
		
		@Override public void visitApply(JCMethodInvocation node) {
			MethodInvocation inv = new MethodInvocation();
			JCTree sel = node.getMethodSelect();
			Identifier id = new Identifier();
			if (sel instanceof JCIdent) {
				String name = ((JCIdent) sel).getName().toString();
				if ("this".equals(name)) {
					AlternateConstructorInvocation aci = new AlternateConstructorInvocation();
					fillList(node.getTypeArguments(), aci.rawConstructorTypeArguments(), FlagKey.TYPE_REFERENCE);
					fillList(node.getArguments(), aci.rawArguments());
					set(node, aci);
					setConversionPositionInfo(aci, "this", getPosition(sel));
					return;
				}
				
				if ("super".equals(name)) {
					SuperConstructorInvocation sci = new SuperConstructorInvocation();
					fillList(node.getTypeArguments(), sci.rawConstructorTypeArguments(), FlagKey.TYPE_REFERENCE);
					fillList(node.getArguments(), sci.rawArguments());
					set(node, sci);
					setConversionPositionInfo(sci, "super", getPosition(sel));
					return;
				}
				
				setPos(sel, id.astValue(name));
				sel = null;
			} else if (sel instanceof JCFieldAccess) {
				String name = ((JCFieldAccess) sel).getIdentifier().toString();
				if ("super".equals(name)) {
					SuperConstructorInvocation sci = new SuperConstructorInvocation();
					fillList(node.getTypeArguments(), sci.rawConstructorTypeArguments(), FlagKey.TYPE_REFERENCE);
					fillList(node.getArguments(), sci.rawArguments());
					sci.rawQualifier(toTree(((JCFieldAccess) sel).getExpression()));
					set(node, sci);
					setConversionPositionInfo(sci, "super", getPosition(sel));
					return;
				}
				setPos(sel, id.astValue(name));
				sel = ((JCFieldAccess) sel).getExpression();
			}
			inv.astName(id).rawOperand(toTree(sel));
			fillList(node.getTypeArguments(), inv.rawMethodTypeArguments(), FlagKey.TYPE_REFERENCE);
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
			JCTree type = node.getType();
			java.util.List<Position> inits = Lists.newArrayList();
			while (type instanceof JCArrayTypeTree) {
				inits.add(getPosition(type));
				type = ((JCArrayTypeTree) type).getType();
			}
			
			crea.rawComponentTypeReference(toTree(type, FlagKey.TYPE_REFERENCE));
			if (node.getDimensions() != null) for (JCExpression dim : node.getDimensions()) {
				crea.astDimensions().addToEnd(setPos(dim, new ArrayDimension().rawDimension(toTree(dim))));
			}
			
			if (init != null) crea.astDimensions().addToEnd(new ArrayDimension());
			
			// new boolean [][][] {} in javac has one less dimension for some reason.
			for (Position i : inits) {
				ArrayDimension dim = new ArrayDimension();
				dim.setPosition(i);
				crea.astDimensions().addToEnd(dim);
			}
			
			crea.astInitializer(init);
			set(node, crea);
		}
		
		@Override public void visitIndexed(JCArrayAccess node) {
			ArrayAccess aa = new ArrayAccess();
			aa.rawIndexExpression(toTree(node.getIndex()));
			aa.rawOperand(toTree(node.getExpression()));
			set(node, aa);
		}
		
		@Override public void visitAssert(JCAssert node) {
			set(node, new Assert().rawAssertion(toTree(node.getCondition())).rawMessage(toTree(node.getDetail())));
		}
		
		@Override public void visitDoLoop(JCDoWhileLoop node) {
			DoWhile dw = new DoWhile();
			JCExpression cond = node.getCondition();
			setConversionPositionInfo(dw, "()", getPosition(cond));
			set(node, dw.rawCondition(toTree(removeParens(cond))).rawStatement(toTree(node.getStatement())));
		}
		
		@Override public void visitContinue(JCContinue node) {
			Continue c = new Continue();
			if (node.getLabel() != null) {
				c.astLabel(new Identifier().astValue(node.getLabel().toString()));
			}
			set(node, c);
		}
		
		@Override public void visitBreak(JCBreak node) {
			Break b = new Break();
			if (node.getLabel() != null) {
				b.astLabel(new Identifier().astValue(node.getLabel().toString()));
			}
			set(node, b);
		}
		
		@Override public void visitForeachLoop(JCEnhancedForLoop node) {
			ForEach fe = new ForEach();
			fe.rawIterable(toTree(node.getExpression()));
			fe.rawStatement(toTree(node.getStatement()));
			fe.rawVariable(toTree(node.getVariable(), FlagKey.VARDEF_IS_DEFINITION));
			set(node, fe);
		}
		
		@Override public void visitIf(JCIf node) {
			If i = new If();
			JCExpression cond = node.getCondition();
			setConversionPositionInfo(i, "()", getPosition(cond));
			i.rawCondition(toTree(removeParens(cond)));
			i.rawStatement(toTree(node.getThenStatement()));
			i.rawElseStatement(toTree(node.getElseStatement()));
			set(node, i);
		}
		
		@Override public void visitLabelled(JCLabeledStatement node) {
			Identifier lbl = new Identifier().astValue(node.getLabel().toString());
			set(node, new LabelledStatement().rawStatement(toTree(node.getStatement())).astLabel(lbl));
		}
		
		@Override public void visitForLoop(JCForLoop node) {
			For f = new For();
			f.rawCondition(toTree(node.getCondition()));
			f.rawStatement(toTree(node.getStatement()));
			for (JCExpressionStatement upd : node.getUpdate()) {
				Node updateNode = toTree(upd.getExpression());
				setConversionPositionInfo(updateNode, "exec", getPosition(upd));
				f.rawUpdates().addToEnd(updateNode);
			}
			List<JCStatement> initializers = node.getInitializer();
			// Multiple vardefs in a row need to trigger the JCVD version AND be washed through fillList to be turned into 1 VD.
			if (!initializers.isEmpty() && initializers.get(0) instanceof JCVariableDecl) {
				Block tmp = new Block();
				fillList(initializers, tmp.rawContents(), FlagKey.VARDEF_IS_DEFINITION);
				Node varDecl = tmp.rawContents().first();
				if (varDecl != null) varDecl.unparent();
				f.rawVariableDeclaration(varDecl);
			} else {
				for (JCStatement init : initializers) {
					if (init instanceof JCExpressionStatement) {
						Node initNode = toTree(((JCExpressionStatement) init).getExpression());
						setConversionPositionInfo(initNode, "exec", getPosition(init));
						f.rawExpressionInits().addToEnd(initNode);
					} else {
						f.rawExpressionInits().addToEnd(toTree(init));
					}
				}
			}
			set(node, f);
		}
		
		@Override public void visitSwitch(JCSwitch node) {
			Switch s = new Switch();
			JCExpression cond = node.getExpression();
			setConversionPositionInfo(s, "()", getPosition(cond));
			s.rawCondition(toTree(removeParens(cond)));
			Block b = new Block();
			s.astBody(b);
			for (JCCase c : node.getCases()) {
				JCExpression rawExpr = c.getExpression();
				if (rawExpr == null) b.rawContents().addToEnd(setPos(c, new Default()));
				else b.rawContents().addToEnd(setPos(c, new Case().rawCondition(toTree(rawExpr))));
				fillList(c.getStatements(), b.rawContents());
			}
			set(node, s);
		}
		
		@Override public void visitSynchronized(JCSynchronized node) {
			Synchronized s = new Synchronized();
			JCExpression cond = node.getExpression();
			setConversionPositionInfo(s, "()", getPosition(cond));
			set(node, s.rawLock(toTree(removeParens(cond))).rawBody(toTree(node.getBlock())));
		}
		
		@Override public void visitTry(JCTry node) {
			Try t = new Try();
			t.rawBody(toTree(node.getBlock()));
			t.rawFinally(toTree(node.getFinallyBlock()));
			fillList(node.getCatches(), t.rawCatches());
			set(node, t);
		}
		
		@Override public void visitCatch(JCCatch node) {
			set(node, new Catch()
					.rawExceptionDeclaration(toTree(node.getParameter(), FlagKey.VARDEF_IS_DEFINITION))
					.rawBody(toTree(node.getBlock())));
		}
		
		@Override public void visitThrow(JCThrow node) {
			set(node, new Throw().rawThrowable(toTree(node.getExpression())));
		}
		
		@Override public void visitWhileLoop(JCWhileLoop node) {
			While w = new While();
			JCExpression cond = node.getCondition();
			setConversionPositionInfo(w, "()", getPosition(cond));
			set(node, w.rawCondition(toTree(removeParens(cond))).rawStatement(toTree(node.getStatement())));
		}
		
		@Override public void visitReturn(JCReturn node) {
			set(node, new Return().rawValue(toTree(node.getExpression())));
		}
		
		@Override public void visitMethodDef(JCMethodDecl node) {
			String name = node.getName() == null ? null : node.getName().toString();
			if ("<init>".equals(name)) {
				ConstructorDeclaration cd = new ConstructorDeclaration();
				cd.astModifiers((Modifiers) toTree(node.getModifiers()));
				cd.rawBody(toTree(node.getBody()));
				fillList(node.getThrows(), cd.rawThrownTypeReferences(), FlagKey.TYPE_REFERENCE);
				fillList(node.getTypeParameters(), cd.rawTypeVariables());
				fillList(node.getParameters(), cd.rawParameters(), FlagKey.NO_VARDECL_FOLDING, FlagKey.VARDEF_IS_DEFINITION);
				String typeName = (String) getFlag(FlagKey.CONTAINING_TYPE_NAME);
				cd.astTypeName(setPos(node, new Identifier().astValue(typeName)));
				addJavadoc(cd, node.mods);
				set(node, cd);
				return;
			}
			
			if (hasFlag(FlagKey.METHODS_ARE_ANNMETHODS)) {
				AnnotationMethodDeclaration md = new AnnotationMethodDeclaration();
				md.astModifiers((Modifiers) toTree(node.getModifiers()));
				md.astMethodName(setPos(node, new Identifier().astValue(name)));
				md.rawReturnTypeReference(toTree(node.getReturnType(), FlagKey.TYPE_REFERENCE));
				md.rawDefaultValue(toTree(node.getDefaultValue()));
				addJavadoc(md, node.mods);
				set(node, md);
				return;
			}
			
			MethodDeclaration md = new MethodDeclaration();
			md.rawBody(toTree(node.getBody()));
			md.astModifiers((Modifiers) toTree(node.getModifiers()));
			md.astMethodName(setPos(node, new Identifier().astValue(name)));
			fillList(node.getThrows(), md.rawThrownTypeReferences(), FlagKey.TYPE_REFERENCE);
			fillList(node.getTypeParameters(), md.rawTypeVariables());
			fillList(node.getParameters(), md.rawParameters(), FlagKey.NO_VARDECL_FOLDING, FlagKey.VARDEF_IS_DEFINITION);
			md.rawReturnTypeReference(toTree(node.getReturnType(), FlagKey.TYPE_REFERENCE));
			addJavadoc(md, node.mods);
			set(node, md);
		}
		
		@Override public void visitAnnotation(JCAnnotation node) {
			Annotation a = new Annotation();
			a.rawAnnotationTypeReference(toTree(node.getAnnotationType(), FlagKey.TYPE_REFERENCE));
			for (JCExpression elem : node.getArguments()) {
				AnnotationElement e = new AnnotationElement();
				if (elem instanceof JCAssign) {
					JCExpression rawName = ((JCAssign) elem).getVariable();
					if (rawName instanceof JCIdent) e.astName(setPos(rawName, new Identifier().astValue(((JCIdent)rawName).getName().toString())));
					elem = ((JCAssign) elem).getExpression();
				}
				e.rawValue(toTree(elem));
				a.astElements().addToEnd(e);
			}
			set(node, a);
		}
	}
}
