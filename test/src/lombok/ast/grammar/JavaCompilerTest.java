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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DirectoryRunner.class)
public class JavaCompilerTest {
	public static File getDirectory() {
		return new File("test/idempotency");
	}
	
	@Test
	public void testJavaCompiler(Source source) throws IOException {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		File tempDir = getTempDir();
		tempDir.mkdirs();
		List<String> options = Arrays.asList("-proc:none", "-d", tempDir.getAbsolutePath());
		CompilationTask task = compiler.getTask(null, null, null, options, null, Collections.singleton(new TestJavaFileObject(source.getName(), source.getRawInput())));
		assertTrue(task.call());
	}
	
	private static File getTempDir() {
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
		
		return new File(getDirectory(), "tmp");
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
