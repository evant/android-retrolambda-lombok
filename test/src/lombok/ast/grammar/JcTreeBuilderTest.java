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
package lombok.ast.grammar;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import javax.tools.JavaFileManager;
import javax.tools.SimpleJavaFileObject;

import lombok.ast.Node;
import lombok.ast.javac.JcTreeBuilder;
import lombok.ast.javac.JcTreePrinter;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;

@RunWith(DirectoryRunner.class)
public class JcTreeBuilderTest {
	public static File getDirectory() {
		return new File("test/idempotency");
	}
	
	@Test
	public boolean testJavaCompiler(Source source) throws Exception {
		if (source.getName().compareTo("B006") > 0) {
			return false;
		}
		String javacString = convertToString(parseWithJavac(source));
		String lombokString;
		try {
			lombokString = convertToString(parseWithLombok(source));
		} catch (Exception e) {
			System.out.printf("==== Processing %s ====\n", source.getName());
			System.out.println(source.getRawInput());
			System.out.println("=========== Expected ============");
			System.out.println(javacString);
			System.out.printf("======= End of %s =======\n", source.getName());
			throw e;
		}
		
		if (!javacString.equals(lombokString)) {
			System.out.printf("==== Processing %s ====\n", source.getName());
			System.out.println(source.getRawInput());
			System.out.println("=========== Expected ============");
			System.out.println(javacString);
			System.out.println("============ Actual =============");
			System.out.println(lombokString);
			System.out.printf("======= End of %s =======\n", source.getName());
		}
		
		assertEquals(javacString, lombokString);
		return true;
	}
	
	private String convertToString(JCTree tree) {
		JcTreePrinter printer = new JcTreePrinter();
		tree.accept(printer);
		String string = printer.toString();
		return string;
	}
	
	private static JCTree parseWithLombok(Source source) {
		source.parseCompilationUnit();
		List<Node> nodes = source.getNodes();
		assertEquals(1, nodes.size());
		Context context = new Context();
		boolean success = false;
		Throwable failTrace = null;
		try {
			Class<?> dfm = Class.forName("com.sun.tools.javac.util.DefaultFileManager");
			JavaFileManager instance = (JavaFileManager) dfm.getConstructor(Context.class, boolean.class, Charset.class).newInstance(context, true, Charset.forName("UTF-8"));
			context.put(JavaFileManager.class, instance);
			success = true;
		} catch (Throwable t) {
			//Either DFM, or its replacement JFM, exists (or possibly both in odd classpath configurations). If something is wrong, NoMethodDefErrors and the like occur.
			failTrace = t;
		}
		
		try {
			Class<?> jfm = Class.forName("com.sun.tools.javac.util.JavacFileManager");
			JavaFileManager instance = (JavaFileManager) jfm.getConstructor(Context.class, boolean.class, Charset.class).newInstance(context, true, Charset.forName("UTF-8"));
			context.put(JavaFileManager.class, instance);
			success = true;
		} catch (Throwable t) {
			//Either DFM, or its replacement JFM, exists (or possibly both in odd classpath configurations). If something is wrong, NoMethodDefErrors and the like occur.
			failTrace = t;
		}
		
		if (!success) {
			if (failTrace instanceof Error) throw (Error)failTrace;
			throw new RuntimeException("Neither com.sun.tools.javac.util.JavacFileManager nor com.sun.tools.javac.util.DefaultFileManager could be configured", failTrace);
		}
		
		JcTreeBuilder builder = new JcTreeBuilder(context);
		nodes.get(0).accept(builder);
		return builder.get();
	}
	
	public static JCTree parseWithJavac(Source source) throws Exception {
		Context context = new Context();
		JavaCompiler compiler = new JavaCompiler(context);
		return compiler.parse(new TestJavaFileObject(source.getName(), source.getRawInput()));
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
