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
import static lombok.ast.grammar.RunForEachFileInDirRunner.fixLineEndings;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import lombok.ast.grammar.RunForEachFileInDirRunner.DirDescriptor;

abstract class TreeBuilderRunner<N> extends RunForEachFileInDirRunner.SourceFileBasedTester {
	@Override
	protected Collection<DirDescriptor> getDirDescriptors() {
		return Arrays.asList(
				DirDescriptor.of(new File("test/resources/idempotency"), true),
				DirDescriptor.of(new File("test/resources/alias"), true),
				DirDescriptor.of(new File("test/resources/special"), true));
	}
	
	protected boolean testCompiler(Source source) throws Exception {
		N parsedWithTargetCompiler = parseWithTargetCompiler(source);
		if (parsedWithTargetCompiler == null) {
			// Skip test if target compiler can't compile it.
			// A separate test that checks if test samples compile
			// at all will do error reporting.
			return false;
		}
		String targetString = convertToString(source, parsedWithTargetCompiler);
		
		source.parseCompilationUnit();
		if (!source.getProblems().isEmpty()) {
			StringBuilder message = new StringBuilder();
			for (ParseProblem p : source.getProblems()) {
				message.append(p.toString());
				message.append("\n");
			}
			printDebugInformation(source, targetString, null);
			fail(message.toString());
		}
		
		String lombokString;
		try {
			lombokString = fixLineEndings(convertToString(source, parseWithLombok(source)));
		} catch (Exception e) {
			printDebugInformation(source, targetString, null);
			throw e;
		} catch (Error e) {
			printDebugInformation(source, targetString, null);
			throw e;
		}
		
		try {
			assertEquals(targetString, lombokString);
		} catch (AssertionError e) {
			printDebugInformation(source, targetString, lombokString);
			throw e;
		}
		
		return true;
	}
	
	protected void printDebugInformation(Source source, String targetString, String lombokString) {
		String name = source.getName();
		System.out.printf("==== Processing %s ====\n", name);
		System.out.println(fixLineEndings(source.getRawInput()));
		System.out.println("=========== Expected ============");
		System.out.println(targetString);
		if (lombokString != null) {
			System.out.println("============ Actual =============");
			System.out.println(lombokString);
		}
		System.out.printf("======= End of %s =======\n", name);
	}
	
	protected abstract String convertToString(Source source, N tree) throws Exception;
	
	protected abstract N parseWithLombok(Source source) throws Exception;
	
	protected abstract N parseWithTargetCompiler(Source source) throws Exception;
}
