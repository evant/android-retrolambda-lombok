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

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import lombok.ast.Node;
import lombok.ast.grammar.RunForEachFileInDirRunner.DirDescriptor;
import lombok.ast.javac.JcTreeConverter;
import lombok.ast.printer.SourcePrinter;
import lombok.ast.printer.StructureFormatter;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.OptionName;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

@RunWith(RunForEachFileInDirRunner.class)
public class JcToLombokTest extends TreeBuilderRunner<Node> {
	public JcToLombokTest() {
		super(false);
	}
	
	@Override protected Collection<DirDescriptor> getDirDescriptors() {
		return Arrays.asList(
				DirDescriptor.of(new File("test/resources/idempotency"), true).withInclusion(Pattern.compile("^.*(?:[a-a]\\d{3}_).*\\.java$", Pattern.CASE_INSENSITIVE)));
//		return Arrays.asList(
//				DirDescriptor.of(new File("test/resources/idempotency"), true),
//				DirDescriptor.of(new File("test/resources/alias"), true),
//				DirDescriptor.of(new File("test/resources/special"), true));
	}
	
	@Test
	public boolean testJcTreeConverter(Source source) throws Exception {
		return testCompiler(source);
	}
	
	protected String convertToString(Source source, Node tree) {
		StructureFormatter formatter = StructureFormatter.formatterWithoutPositions();
		tree.accept(new SourcePrinter(formatter));
		return formatter.finish();
	}
	
	protected Node parseWithLombok(Source source) {
		List<Node> nodes = source.getNodes();
		assertEquals(1, nodes.size());
		
		return nodes.get(0);
	}
	
	protected Node parseWithTargetCompiler(Source source) {
		Context context = new Context();
		
		Options.instance(context).put(OptionName.ENCODING, "UTF-8");
		
		JavaCompiler compiler = new JavaCompiler(context);
		compiler.genEndPos = true;
		
		JCCompilationUnit cu = compiler.parse(new ContentBasedJavaFileObject(source.getName(), source.getRawInput()));
		return JcTreeConverter.convert(cu);
	}
}
