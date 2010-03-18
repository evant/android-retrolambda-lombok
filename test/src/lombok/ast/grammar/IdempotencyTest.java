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
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import lombok.ast.Node;
import lombok.ast.grammar.RunForEachFileInDirRunner.DirDescriptor;
import lombok.ast.printer.SourcePrinter;
import lombok.ast.printer.TextFormatter;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RunForEachFileInDirRunner.class)
public class IdempotencyTest extends RunForEachFileInDirRunner.SourceFileBasedTester {
	@Override
	protected Collection<DirDescriptor> getDirDesciptors() {
		return Collections.singleton(DirDescriptor.of(new File("test/resources/idempotency"), true));
	}
	
	@Test
	public void testIdempotency(Source source) throws IOException {
		source.parseCompilationUnit();
		
		if (!source.getProblems().isEmpty()) {
			fail(source.getProblems().get(0).toString());
		}
		
		Node node = source.getNodes().get(0);
		TextFormatter formatter = new TextFormatter(source);
		node.accept(new SourcePrinter(formatter));
		String actual = formatter.finish();
		try {
			assertEquals(source.getRawInput(), actual);
		} catch (AssertionError e) {
			System.out.println("------------------RAW:");
			System.out.println(source.getRawInput());
			System.out.println("-------PARSED-PRINTED:");
			System.out.println(actual);
			System.out.println("----------------------");
			throw e;
		}
	}
}
