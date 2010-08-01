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

import java.util.Map;

import lombok.ast.Block;
import lombok.ast.ClassDeclaration;
import lombok.ast.CompilationUnit;
import lombok.ast.EmptyDeclaration;
import lombok.ast.EmptyStatement;
import lombok.ast.Identifier;
import lombok.ast.ImportDeclaration;
import lombok.ast.InstanceInitializer;
import lombok.ast.KeywordModifier;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.NormalTypeBody;
import lombok.ast.PackageDeclaration;
import lombok.ast.Position;
import lombok.ast.RawListAccessor;
import lombok.ast.StaticInitializer;
import lombok.ast.StrictListAccessor;
import lombok.ast.TypeDeclaration;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCSkip;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;

public class JcTreeConverter extends JCTree.Visitor {
	private enum FlagKey {
		BLOCKS_ARE_INITIALIZERS,
		SKIP_IS_DECL;
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
	
	private void set(JCTree node, java.util.List<? extends Node> values) {
		if (values.isEmpty()) System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
		
		for (Node v : values) if (v.getPosition().isUnplaced()) setPos(node, v);
		
		if (result != null) throw new IllegalStateException("result is already set");
		this.result = values;
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
	
	private void fillList(List<? extends JCTree> nodes, RawListAccessor<?, ?> list, FlagKey... keys) {
		if (nodes == null) return;
		
		for (JCTree node : nodes) list.addToEnd(toTree(node, keys));
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
			fillList(node.implementing, classDecl.rawImplementing());
			classDecl.rawExtending(toTree(node.extending));
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
		// In order to handle int i, j; and friends:
		//   - somehow get all consecutive JCVD objects in a list.
		//   - For any given Element, the type is either (A) a backref, or (B) any number of TypeArray wraps around a backref.
		//     * Therefore the 'base' type is whatever you find that is backreffed. It can be a type wrapped by X TypeArrays in the first.
		//   - Check the modifier; it will be a backref for subsequent JCVDs.
		//   - positions for individual parts go from name to the following comma, unless its the last one, in which case it goes to the end of the NAME.
		//   - to separate int a[][], int[] a[] and int[][] a;, there's only one way: Check positions.
		visitTree(node);
	}
}
