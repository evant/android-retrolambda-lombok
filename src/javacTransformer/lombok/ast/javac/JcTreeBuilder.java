package lombok.ast.javac;

import java.util.Arrays;

import lombok.ast.Annotation;
import lombok.ast.ClassDeclaration;
import lombok.ast.CompilationUnit;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Identifier;
import lombok.ast.ImportDeclaration;
import lombok.ast.KeywordModifier;
import lombok.ast.Modifiers;
import lombok.ast.PackageDeclaration;
import lombok.ast.TypeDeclaration;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Name.Table;

public class JcTreeBuilder extends ForwardingAstVisitor {

	private final TreeMaker treeMaker;
	private final Table table;
	
	java.util.List<? extends JCTree> result = null;
	
	public JcTreeBuilder(Context context) {
		this(TreeMaker.instance(context), Name.Table.instance(context));
	}

	private JcTreeBuilder(TreeMaker treeMaker, Table table) {
		this.treeMaker = treeMaker;
		this.table = table;
	}

	JCTree get() {
		return result.get(0);
	}
	
	java.util.List<? extends JCTree> getAll() {
		return result;
	}
	
	private void set(JCTree... value) {
		result = Arrays.asList(value);
	}
	
	private JcTreeBuilder create() {
		return new JcTreeBuilder(treeMaker, table);
	}
	
	@Override
	public boolean visitCompilationUnit(CompilationUnit node) {
		JcTreeBuilder pkgVisitor = create();
		node.getPackageDeclaration().accept(pkgVisitor);
		
		List<JCTree> defs = List.nil();
		for (ImportDeclaration importDecl : node.importDeclarations().getContents()) {
			JcTreeBuilder impVisitor = create();
			importDecl.accept(impVisitor);
			defs = defs.append(impVisitor.get());
		}
		for (TypeDeclaration typeDecl : node.typeDeclarations().getContents()) {
			JcTreeBuilder typeVisitor = create();
			typeDecl.accept(typeVisitor);
			defs = defs.append(typeVisitor.get());
		}		
		set(treeMaker.TopLevel(List.<JCAnnotation>nil(), (JCExpression)pkgVisitor.get(), defs));
		return true;
	}
	
	@Override
	public boolean visitPackageDeclaration(PackageDeclaration node) {
//		treeMaker.
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
		JcTreeBuilder modVisitor = create();
		node.getModifiers().accept(modVisitor);
		
		JCModifiers modifiers = (JCModifiers) modVisitor.get();
		set(treeMaker.ClassDef(modifiers, table.fromString(node.getName().getName()), List.<JCTypeParameter>nil(), null, List.<JCExpression>nil(), List.<JCTree>nil()));
		return true;
	}
	
	@Override
	public boolean visitModifiers(Modifiers node) {
		List<JCAnnotation> annotations = List.nil();
		for (Annotation annotation : node.annotations().getContents()) {
			JcTreeBuilder annVisitor = create();
			annotation.accept(annVisitor);
			annotations = annotations.append((JCAnnotation) annVisitor.get());
		}
		
		set(treeMaker.Modifiers(node.asReflectModifiers(), annotations));
		return true;
	}
	
	private long getModifier(KeywordModifier keyword) {
		return keyword.asReflectModifiers();
	}

	@Override
	public boolean visitKeywordModifier(KeywordModifier node) {
		set(treeMaker.Modifiers(getModifier(node)));
		return true;
	}
	
	private JCExpression chain(Iterable<Identifier> parts) {
		JCExpression previous = null;
		for (Identifier part : parts) {
			Name next = table.fromString(part.getName());
			if (previous == null) {
				previous = treeMaker.Ident(next);
			} else {
				previous = treeMaker.Select(previous, next);
			}
		}
		return previous;
	}
}
