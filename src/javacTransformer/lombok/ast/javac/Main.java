package lombok.ast.javac;

import java.nio.charset.Charset;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.ToolProvider;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JavacFileManager;

import lombok.ast.CompilationUnit;
import lombok.ast.Identifier;
import lombok.ast.ImportDeclaration;
import lombok.ast.PackageDeclaration;

public class Main {
	public static void main(String[] args) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		
		CompilationUnit cu = new CompilationUnit().setPackageDeclaration(new PackageDeclaration().parts().addToEnd(new Identifier().setName("thePackage"), new Identifier().setName("subPackage")));
		cu.importDeclarations().addToEnd(new ImportDeclaration().parts().addToEnd(new Identifier().setName("java"), new Identifier().setName("util")).setStarImport(true));
		cu.importDeclarations().addToEnd(new ImportDeclaration().parts().addToEnd(new Identifier().setName("java"), new Identifier().setName("util"), new Identifier().setName("Collections")).setStaticImport(true));
		Context context = new Context();
		
		context.put(JavaFileManager.class, new JavacFileManager(context, true, Charset.forName("UTF-8")));
		
		JcTreeBuilder builder = new JcTreeBuilder(context);
		cu.accept(builder);
		System.out.println(builder.get().toString());
	}
}
