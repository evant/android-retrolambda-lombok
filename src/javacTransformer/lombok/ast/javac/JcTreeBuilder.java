package lombok.ast.javac;

import java.util.Arrays;

import lombok.ast.Annotation;
import lombok.ast.CompilationUnit;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Identifier;
import lombok.ast.ImportDeclaration;
import lombok.ast.PackageDeclaration;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCImport;
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
