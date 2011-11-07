/*
 * Copyright (C) 2010 The Project Lombok Authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;

import lombok.ast.grammar.RunForEachFileInDirRunner.DirDescriptor;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RunForEachFileInDirRunner.class)
public class JavaCompilerTest extends RunForEachFileInDirRunner.SourceFileBasedTester {
	@Override
	protected Collection<DirDescriptor> getDirDescriptors() {
		return Arrays.asList(
				DirDescriptor.of(new File("test/resources/idempotency"), true),
				DirDescriptor.of(new File("test/resources/alias"), true));
	}
	
	@Test
	public void testJavaCompiler(Source source) throws IOException {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		File tempDir = getTempDir();
		tempDir.mkdirs();
		List<String> options = Arrays.asList("-proc:none", "-d", tempDir.getAbsolutePath());
		CompilationTask task = compiler.getTask(null, null, null, options, null, Collections.singleton(new ContentBasedJavaFileObject(source.getName(), source.getRawInput())));
		assertTrue(task.call());
	}
	
	private File getTempDir() {
		String[] rawDirs = {
				System.getProperty("java.io.tmpdir"),
				"/tmp",
				"C:\\Windows\\Temp"
		};
		
		for (String dir : rawDirs) {
			if (dir == null) continue;
			File f = new File(dir);
			if (!f.isDirectory()) continue;
			return new File(f, "lombok.ast-test");
		}
		
		return new File(getDirDescriptors().iterator().next().getDirectory(), "tmp");
	}
}
