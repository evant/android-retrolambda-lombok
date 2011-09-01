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

import lombok.ast.javac.JcTreeBuilder;
import lombok.ast.javac.JcTreeConverter;
import lombok.ast.javac.JcTreePrinter;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.OptionName;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

/**
 * Compares a JCTree AST parsed by javac with a JCTree AST parsed by javac, converted to lombok.ast, and converted back.
 */
@RunWith(RunForEachFileInDirRunner.class)
public class JcTreeConverterType2Test extends TreeBuilderRunner<JCTree> {
	public JcTreeConverterType2Test() {
		super(true);
	}
	
	@Test
	public boolean testJcTreeConverter(Source source) throws Exception {
		return testCompiler(source);
	}
	
	protected String convertToString(JCTree tree) {
		JcTreePrinter printer = new JcTreePrinter(true);
		printer.visit(tree);
		String string = printer.toString();
		return string;
	}
	
	protected boolean checkForLombokAstParseFailure() {
		return false;
	}
	
	protected JCTree parseWithLombok(Source source) {
		Context context = new Context();
		
		Options.instance(context).put(OptionName.ENCODING, "UTF-8");
		
		JavaCompiler compiler = new JavaCompiler(context);
		compiler.genEndPos = true;
		compiler.keepComments = true;
		
		JCCompilationUnit cu = compiler.parse(new ContentBasedJavaFileObject(source.getName(), source.getRawInput()));
		JcTreeConverter converter = new JcTreeConverter();
		JcTreeBuilder builder = new JcTreeBuilder();
		converter.visit(cu);
		builder.visit(converter.getResult());
		return builder.get();
	}
	
	protected JCTree parseWithTargetCompiler(Source source) {
		Context context = new Context();
		
		Options.instance(context).put(OptionName.ENCODING, "UTF-8");
		
		JavaCompiler compiler = new JavaCompiler(context);
		compiler.genEndPos = true;
		compiler.keepComments = true;
		
		JCCompilationUnit cu = compiler.parse(new ContentBasedJavaFileObject(source.getName(), source.getRawInput()));
		return cu;
	}
}
