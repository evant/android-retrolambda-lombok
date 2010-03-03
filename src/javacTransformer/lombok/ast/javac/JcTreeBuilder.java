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

import java.util.Map;

import lombok.ast.Annotation;
import lombok.ast.Block;
import lombok.ast.ClassDeclaration;
import lombok.ast.CompilationUnit;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Identifier;
import lombok.ast.ImportDeclaration;
import lombok.ast.InstanceInitializer;
import lombok.ast.KeywordModifier;
import lombok.ast.ListAccessor;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.PackageDeclaration;
import lombok.ast.StaticInitializer;
import lombok.ast.TypeArguments;
import lombok.ast.TypeReference;
import lombok.ast.TypeReferencePart;
import lombok.ast.TypeVariable;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.WildcardKind;

import com.google.common.collect.ImmutableMap;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Name.Table;

public class JcTreeBuilder extends ForwardingAstVisitor {

	private final TreeMaker treeMaker;
	private final Table table;
	
	List<? extends JCTree> result = null;
	
	public JcTreeBuilder(Context context) {
		this(TreeMaker.instance(context), Name.Table.instance(context));
	}

	private JcTreeBuilder(TreeMaker treeMaker, Table table) {
		this.treeMaker = treeMaker;
		this.table = table;
	}

	private Name toName(Identifier identifier) {
		if (identifier == null) return null;
		return table.fromString(identifier.getName());
	}
	
	private JCTree toTree(Node node) {
		if (node == null) return null;
		JcTreeBuilder visitor = create();
		node.accept(visitor);
		return visitor.get();
	}
	
	private <T extends JCTree> List<T> toList(Class<T> type, ListAccessor<? extends Node, ?> accessor) {
		List<T> result = List.nil();
		for (Node node : accessor.getContents()) {
			JcTreeBuilder visitor = create();
			node.accept(visitor);
			JCTree value = visitor.get();
			if (value != null && !type.isInstance(value)) {
				throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + type.getName());
			}
			result = result.append(type.cast(value));
		}
		return result;
	}
	
	private List<JCTree> toList(ListAccessor<? extends Node, ?> accessor) {
		return toList(JCTree.class, accessor);
	}
	
	private List<JCTree> toList(Node node) {
		return toList(JCTree.class, node);
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
	
	private void set(JCTree... value) {
		result = List.from(value);
	}
	
	private void set(List<? extends JCTree> values) {
		result = values;
	}
	
	private JcTreeBuilder create() {
		return new JcTreeBuilder(treeMaker, table);
	}
	
	@Override
	public boolean visitCompilationUnit(CompilationUnit node) {
		JCExpression pkg = (JCExpression) toTree(node.getPackageDeclaration());
		
		List<JCTree> imports = toList(node.importDeclarations());
		List<JCTree> types = toList(node.typeDeclarations());

		set(treeMaker.TopLevel(List.<JCAnnotation>nil(), pkg, imports.appendList(types)));
		return true;
	}
	
	@Override
	public boolean visitPackageDeclaration(PackageDeclaration node) {
		JCExpression pkg = chain(node.parts().getContents());
		
		for (Annotation annotation : node.annotations().getContents()){
			// TODO Add implementation
		}
		
		set(pkg);
		return true;
	}
	
	@Override
	public boolean visitImportDeclaration(ImportDeclaration node) {
		JCExpression name = chain(node.parts().getContents());
		if (node.isStarImport()) {
			name = treeMaker.Select(name, table.fromString("*"));
		}
		set(treeMaker.Import(name, node.isStaticImport()));
		return true;
	}
	
	
	@Override
	public boolean visitClassDeclaration(ClassDeclaration node) {
		set(treeMaker.ClassDef(
				(JCModifiers) toTree(node.getModifiers()),
				toName(node.getName()),
				toList(JCTypeParameter.class, node.typeVariables()),
				toTree(node.getExtending()),
				toList(JCExpression.class, node.implementing()),
				node.getBody() == null ? List.<JCTree>nil() : toList(node.getBody().members())
		));
		return true;
	}
	
	@Override
	public boolean visitModifiers(Modifiers node) {
		set(treeMaker.Modifiers(node.getExplicitModifierFlags(), toList(JCAnnotation.class, node.annotations())));
		return true;
	}
	
	@Override
	public boolean visitKeywordModifier(KeywordModifier node) {
		set(treeMaker.Modifiers(getModifier(node)));
		return true;
	}
	
	@Override
	public boolean visitInstanceInitializer(InstanceInitializer node) {
		set(toTree(node.getBody()));
		return true;
	}
	
	@Override
	public boolean visitStaticInitializer(StaticInitializer node) {
		JCBlock block = (JCBlock) toTree(node.getBody());
		block.flags |= Flags.STATIC; 
		set(block);
		return true;
	}
	
	@Override
	public boolean visitBlock(Block node) {
		set(treeMaker.Block(0, toList(JCStatement.class, node.contents())));
		return true;
	}
	
	@Override
	public boolean visitVariableDeclaration(VariableDeclaration node) {
		set(toTree(node.getDefinition()));
		return true;
	}
	
	@Override
	public boolean visitVariableDefinition(VariableDefinition node) {
		JCModifiers mods = (JCModifiers) toTree(node.getModifiers());
		JCExpression vartype = (JCExpression) toTree(node.getTypeReference());
		JCExpression init = null;
		for (VariableDefinitionEntry e : node.variables().getContents()){
			set(treeMaker.VarDef(mods, toName(e.getName()), addDimensions(vartype, e.getArrayDimensions()), init));
			return true;
			// TODO add multiple entries
		}
		throw new RuntimeException("expected some definitions...");
	}
	
	@Override
	public boolean visitTypeReference(TypeReference node) {
		WildcardKind wildcard = node.getWildcard();
		if (wildcard == WildcardKind.UNBOUND) {
			set(treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.UNBOUND), null));
			return true;
		}
		
		JCExpression result = plainTypeReference(node);
		
		result = addWildcards(result, wildcard);
		result = addDimensions(result, node.getArrayDimensions());
		
		set(result);
		return true;
	}
	
	private JCExpression addDimensions(JCExpression type, int dimensions) {
		for (int i = 0; i < dimensions; i++) {
			type = treeMaker.TypeArray(type);
		}
		return type;
	}
	
	private JCExpression plainTypeReference(TypeReference node) {
		if (node.isPrimitive() || node.parts().size() == 1) {
			Identifier identifier = node.parts().first().getIdentifier();
			int typeTag = primitiveTypeTag(identifier.getName());
			if (typeTag > 0) return treeMaker.TypeIdent(typeTag);
		}
		
		List<JCExpression> list = toList(JCExpression.class, node.parts());
		if (list.size() == 1) {
			return list.head;
		}
		
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
				// TODO Handle type parameters somewhere in the middle
			}
		}
		return previous;	
	}

	private JCExpression addWildcards(JCExpression type, WildcardKind wildcardKind) {
		if (wildcardKind == WildcardKind.NONE) {
			return type;
		}
		if (wildcardKind == WildcardKind.EXTENDS) {
			return treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.EXTENDS), type);
		}
		if (wildcardKind == WildcardKind.SUPER) {
			return treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.SUPER), type);
		}
		throw new IllegalStateException("Unexpected unbound wildcard");
	}
	
	@Override
	public boolean visitTypeReferencePart(TypeReferencePart node) {
		JCIdent ident = treeMaker.Ident(toName(node.getIdentifier()));
		
		List<JCExpression> typeArguments = toList(JCExpression.class, node.getTypeArguments());
		if (typeArguments.isEmpty()) {
			set(ident);
		} else {
			set(treeMaker.TypeApply(ident, typeArguments));
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
		set(treeMaker.TypeParameter(toName(node.getName()), toList(JCExpression.class, node.extending())));
		return true;
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
				previous = treeMaker.Ident(next);
			} else {
				previous = treeMaker.Select(previous, next);
			}
		}
		return previous;
	}
}
