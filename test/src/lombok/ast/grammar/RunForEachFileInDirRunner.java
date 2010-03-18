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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import lombok.Data;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Test.None;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import com.google.common.collect.Lists;

public class RunForEachFileInDirRunner extends Runner {
	@Data
	public static final class DirDescriptor {
		private final File directory;
		private final Pattern inclusionPattern;
		private final Pattern exclusionPattern;
		private final File mirrorDirectory;
		private final boolean recurse;
		
		public static DirDescriptor of(File directory, boolean recurse) {
			return new DirDescriptor(directory, Pattern.compile("^.*\\.java$"), null, null, recurse);
		}
		
		public DirDescriptor withExclusion(Pattern pattern) {
			return new DirDescriptor(directory, inclusionPattern, pattern, mirrorDirectory, recurse);
		}
		
		public DirDescriptor withInclusion(Pattern pattern) {
			return new DirDescriptor(directory, pattern, exclusionPattern, mirrorDirectory, recurse);
		}
		
		public DirDescriptor withMirror(File mirror) {
			return new DirDescriptor(directory, inclusionPattern, exclusionPattern, mirror, recurse);
		}
	}
	
	public static abstract class SourceFileBasedTester {
		protected abstract Collection<DirDescriptor> getDirDesciptors();
	}
	
	private static final Comparator<Method> methodComparator = new Comparator<Method>() {
		@Override public int compare(Method o1, Method o2) {
			String sig1 = o1.getName() + Arrays.toString(o1.getParameterTypes());
			String sig2 = o2.getName() + Arrays.toString(o2.getParameterTypes());
			return sig1.compareTo(sig2);
		}
	};
	
	private static final Comparator<String> stringComparator = new Comparator<String>() {
		@Override public int compare(String o1, String o2) {
			if (o1 == null ? o2 == null : o1.equals(o2)) return 0;
			if (o1 == null) return -1;
			if (o2 == null) return +1;
			return o1.compareTo(o2);
		}
	};
	
	private final Description description;
	private final Map<String, Map<Method, Description>> tests = new TreeMap<String, Map<Method, Description>>(stringComparator);
	private final Throwable failure;
	private final Class<?> testClass;
	private File directory, mirrorDirectory;
	
	public RunForEachFileInDirRunner(Class<?> testClass) {
		this.testClass = testClass;
		description = Description.createSuiteDescription(testClass);
		Throwable error = null;
		try {
			addTests(testClass);
		}
		catch (Throwable t) {
			error = t;
		}
		this.failure = error;
	}
	
	private void addTests(Class<?> testClass) throws Exception {
		if (!SourceFileBasedTester.class.isAssignableFrom(testClass)) {
			throw new AssertionError("Can't add tests if the class doesn't extend SourceFileBasedTester.");
		}
		
		SourceFileBasedTester tester = (SourceFileBasedTester) testClass.newInstance();
		
		for (DirDescriptor descriptor : tester.getDirDesciptors()) {
			directory = descriptor.getDirectory();
			mirrorDirectory = descriptor.getMirrorDirectory();
			
			File root = mirrorDirectory == null ? directory : mirrorDirectory;
			List<File> files = listFiles(root, descriptor.isRecurse());
			
			Map<Method, Description> noFileNeededMap = new TreeMap<Method, Description>(methodComparator);
			
			for (File file : files) {
				if (descriptor.getInclusionPattern() != null && !descriptor.getInclusionPattern().matcher(file.getCanonicalPath()).matches()) continue;
				if (descriptor.getExclusionPattern() != null && descriptor.getExclusionPattern().matcher(file.getCanonicalPath()).matches()) continue;
				
				Map<Method, Description> methodToDescMap = new TreeMap<Method, Description>(methodComparator);
				
				String fileName = root.toURI().relativize(file.toURI()).toString();
				tests.put(fileName, methodToDescMap);
				
				for (Method m : testClass.getDeclaredMethods()) {
					if (m.getAnnotation(Test.class) != null) {
						if (m.getParameterTypes().length == 0) {
							if (!noFileNeededMap.containsKey(m)) {
								Description testDescription = Description.createTestDescription(testClass, m.getName());
								description.addChild(testDescription);
								noFileNeededMap.put(m, testDescription);
							}
						} else {
							Description testDescription = Description.createTestDescription(testClass, m.getName() + ": " + fileName);
							description.addChild(testDescription);
							methodToDescMap.put(m, testDescription);
						}
					}
				}
			}
		}
	}
	
	private List<File> listFiles(File directory, boolean recurse) {
		List<File> all = Lists.newArrayList();
		_listFiles(all, directory, recurse);
		return all;
	}
	
	private static final Comparator<File> FILE_SORTER = new Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			boolean dir1 = o1.isDirectory();
			boolean dir2 = o2.isDirectory();
			
			if (dir1 == dir2) {
				return o1.getName().compareTo(o2.getName());
			}
			return dir1 ? 1 : -1;
		}
	};
	
	private void _listFiles(List<File> collector, File directory, boolean recurse) {
		File[] listFiles = directory.listFiles();
		Arrays.sort(listFiles, FILE_SORTER);
		for (File f : listFiles) {
			if (f.isDirectory() && recurse) {
				_listFiles(collector, f, recurse);
			} else {
				collector.add(f);
			}
		}
	}
	
	@Override
	public Description getDescription() {
		return description;
	}
	
	@Override
	public void run(RunNotifier notifier) {
		if (failure != null) {
			notifier.fireTestStarted(description);
			notifier.fireTestFailure(new Failure(description, failure));
			notifier.fireTestFinished(description);
			return;
		}
		
		for (Map.Entry<String, Map<Method, Description>> entry : tests.entrySet()) {
			Map<Method, Description> methodList = entry.getValue();
			String content;
			Throwable error;
			
			try {
				content = entry.getKey() == null ? null : FileUtils.readFileToString(new File(
						mirrorDirectory == null ? directory : mirrorDirectory, entry.getKey()), "UTF-8");
				error = null;
			} catch (IOException e) {
				content = null;
				error = e;
			}
			
			boolean skipTest = content != null && content.startsWith("//SKIP");
			
			for (Map.Entry<Method, Description> test : methodList.entrySet()) {
				Description testDescription = test.getValue();
				if (skipTest) {
					notifier.fireTestIgnored(testDescription);
					continue;
				}
				notifier.fireTestStarted(testDescription);
				if (error != null) {
					notifier.fireTestFailure(new Failure(testDescription, error));
				} else {
					try {
						if (!runTest(content, entry.getKey(), test.getKey())) {
							notifier.fireTestIgnored(testDescription);
						}
					} catch (Throwable t) {
						notifier.fireTestFailure(new Failure(testDescription, t));
					}
				}
				notifier.fireTestFinished(testDescription);
			}
		}
	}
	
	private boolean runTest(String rawSource, String fileName, Method method) throws Throwable {
		Class<?>[] paramTypes = method.getParameterTypes();
		Object[] params;
		Test t = method.getAnnotation(Test.class);
		
		switch (paramTypes.length) {
		case 0:
			assert rawSource == null;
			params = new Object[0];
			break;
		case 1:
			if (mirrorDirectory != null) return false;
			if (paramTypes[0] == String.class) params = new Object[] {rawSource};
			else if (paramTypes[0] == File.class) params = new Object[] {new File(directory, fileName)};
			else if (paramTypes[0] == Source.class) params = new Object[] {new Source(rawSource, fileName)};
			else return false;
			break;
		case 2:
			if (mirrorDirectory == null) return false;
			params = new Object[2];
			String mainFileName = fileName.replaceAll("\\.\\d+\\.java$", ".java");
			String expectedContent = FileUtils.readFileToString(new File(directory, mainFileName), "UTF-8");
			
			if (paramTypes[0] == String.class) params[0] = expectedContent;
			else if (paramTypes[0] == File.class) params[0] = new File(directory, fileName);
			else if (paramTypes[0] == Source.class) params[0] = new Source(expectedContent, mainFileName);
			else return false;
			
			if (paramTypes[1] == String.class) params[1] = rawSource;
			else if (paramTypes[1] == File.class) params[1] = new File(mirrorDirectory, fileName);
			else if (paramTypes[1] == Source.class) params[1] = new Source(rawSource, fileName);
			else return false;
			
			break;
		default:
			return false;
		}
		
		Object instance = testClass.newInstance();
		
		if (t != null && t.expected() != None.class) {
			try {
				Object executed = method.invoke(instance, params);
				if (executed instanceof Boolean && !(Boolean)executed) return false; 
				Assert.fail("Expected exception: " + t.expected().getName());
			} catch (InvocationTargetException e) {
				if (t.expected().isInstance(e.getCause())) return true;
				throw e.getCause();
			}
		} else {
			try {
				Object executed = method.invoke(instance, params);
				if (executed instanceof Boolean && !(Boolean)executed) return false; 
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}
		
		return true;
	}
}
