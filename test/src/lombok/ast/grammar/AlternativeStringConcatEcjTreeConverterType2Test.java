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
package lombok.ast.grammar;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import lombok.ast.grammar.RunForEachFileInDirRunner.DirDescriptor;

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.junit.Test;

public class AlternativeStringConcatEcjTreeConverterType2Test extends EcjTreeConverterType2Test {
	@Test
	public boolean testEcjTreeConverter(Source source) throws Exception {
		return testCompiler(source);
	}
	
	@Override protected Collection<DirDescriptor> getDirDescriptors() {
		return Arrays.asList(
			DirDescriptor.of(new File("test/resources/idempotency"), true).withInclusion(Pattern.compile("^.*StringConcatExpressions.java$", Pattern.CASE_INSENSITIVE))
		);
	}
	
	@Override protected CompilerOptions ecjCompilerOptions() {
		CompilerOptions options = super.ecjCompilerOptions();
		options.parseLiteralExpressionsAsConstants = false;
		return options;
	}
}
