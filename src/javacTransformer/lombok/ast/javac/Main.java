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

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaFileManager;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;

import lombok.ast.ClassDeclaration;
import lombok.ast.CompilationUnit;
import lombok.ast.Identifier;
import lombok.ast.ImportDeclaration;
import lombok.ast.KeywordModifier;
import lombok.ast.PackageDeclaration;
import lombok.ast.TypeDeclaration;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JavacFileManager;
import com.sun.tools.javac.util.Options;

public class Main {
	public static void main(String[] args) throws Exception {
		JCTree cu = parseWithJavac("X.java", "package thePackage.subPackage;\nimport java.util.*;\nimport static java.util.Collections;public abstract class X {}");
		JcTreePrinter printer = new JcTreePrinter();
		cu.accept(printer);
		String string = printer.toString();
		
		CompilationUnit cu2 = new CompilationUnit().setPackageDeclaration(new PackageDeclaration().parts().addToEnd(new Identifier().setName("thePackage"), new Identifier().setName("subPackage")));
		cu2.importDeclarations().addToEnd(new ImportDeclaration().parts().addToEnd(new Identifier().setName("java"), new Identifier().setName("util")).setStarImport(true));
		cu2.importDeclarations().addToEnd(new ImportDeclaration().parts().addToEnd(new Identifier().setName("java"), new Identifier().setName("util"), new Identifier().setName("Collections")).setStaticImport(true));
		cu2.typeDeclarations().addToEndRaw(new ClassDeclaration().setName(new Identifier().setName("X")).getModifiers().keywords().addToEnd(new KeywordModifier().setName("public"), new KeywordModifier().setName("abstract")).getParent());
		Context context = new Context();
		
		context.put(JavaFileManager.class, new JavacFileManager(context, true, Charset.forName("UTF-8")));
		
		JcTreeBuilder builder = new JcTreeBuilder(context);
		cu2.accept(builder);

		JcTreePrinter printer2 = new JcTreePrinter();
		builder.get().accept(printer2);
		String string2 = printer2.toString();
		
		System.out.println(string.equals(string2));
		System.out.println(string);
		System.out.println("========================");
		System.out.println(string2);
		
	}
	
	@SuppressWarnings("deprecation")
	public static JCTree parseWithJavac(String name, String content) throws Exception {
		Context context = new Context();
		JavaCompiler compiler = new JavaCompiler(context);
		return compiler.parse(new TestJavaFileObject(name, content));
	}
	
	
	private static class TestJavaFileObject extends SimpleJavaFileObject {
		private final String content;

		protected TestJavaFileObject(String name, String content) {
			super(URI.create(name), Kind.SOURCE);
			this.content = content;
		}
		
		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			return content;
		}
	}
}
