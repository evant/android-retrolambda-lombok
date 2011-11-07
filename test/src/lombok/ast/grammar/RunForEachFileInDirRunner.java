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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.Data;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Test.None;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

public class RunForEachFileInDirRunner extends Runner {
	@Data
	public static final class DirDescriptor implements Comparable<DirDescriptor> {
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
		
		@Override
		public int compareTo(DirDescriptor other) {
			int result = directory.getAbsolutePath().compareTo(other.directory.getAbsolutePath());
			if (result != 0) return result;
			if (mirrorDirectory != null && other.mirrorDirectory == null) return +1;
			if (mirrorDirectory == null && other.mirrorDirectory != null) return -1;
			if (mirrorDirectory != null) result = mirrorDirectory.getAbsolutePath().compareTo(other.mirrorDirectory.getAbsolutePath());
			if (result != 0) return result;
			if (recurse && !other.recurse) return +1;
			if (!recurse && other.recurse) return -1;
			if (inclusionPattern != null && other.inclusionPattern == null) return +1;
			if (inclusionPattern == null && other.inclusionPattern != null) return -1;
			if (inclusionPattern != null) result = inclusionPattern.pattern().compareTo(other.inclusionPattern.pattern());
			if (result != 0) return result;
			if (exclusionPattern != null && other.exclusionPattern == null) return +1;
			if (exclusionPattern == null && other.exclusionPattern != null) return -1;
			if (exclusionPattern != null) result = exclusionPattern.pattern().compareTo(other.exclusionPattern.pattern());
			return result;
		}
	}
	
	public static String fixLineEndings(String in) {
		String plaf = System.getProperty("line.separator", "\n");
		if (plaf.equals("\n")) return in;
		return in.replace(plaf, "\n");
	}
	
	@Data
	private static final class RunData {
		private final File main, alias;
		private final Map<Method, Description> methods;
	}
	
	public static abstract class SourceFileBasedTester {
		protected abstract Collection<DirDescriptor> getDirDescriptors();
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
	private final Map<String, RunData> tests = Maps.newTreeMap(stringComparator);
	private final Throwable failure;
	private final Class<?> testClass;
	private List<Method> beforeClassMethods = Lists.newArrayList();
	private List<Method> afterClassMethods = Lists.newArrayList();
	
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
		
		List<DirDescriptor> descriptors = Lists.newArrayList(tester.getDirDescriptors());
		Collections.sort(descriptors);
		
		File commonRoot = findCommonRoots(descriptors);
		
		for (Method m : testClass.getDeclaredMethods()) {
			if (m.getAnnotation(BeforeClass.class) != null) {
				if (m.getParameterTypes().length != 0) {
					System.err.println("Skipping @BeforeClass method: " + m.getName() + " - it should not have any parameters.");
				}
				beforeClassMethods.add(m);
			}
			if (m.getAnnotation(AfterClass.class) != null) {
				if (m.getParameterTypes().length != 0) {
					System.err.println("Skipping @AfterClass method: " + m.getName() + " - it should not have any parameters.");
				}
				afterClassMethods.add(m);
			}
		}
		
		for (DirDescriptor descriptor : descriptors) {
			File directory = descriptor.getDirectory();
			File mirrorDirectory = descriptor.getMirrorDirectory();
			
			File root = mirrorDirectory == null ? directory : mirrorDirectory;
			List<File> files = listFiles(root, descriptor.isRecurse());
			
			Map<Method, Description> noFileNeededMap = Maps.newTreeMap(methodComparator);
			
			for (File file : files) {
				if (descriptor.getInclusionPattern() != null && !descriptor.getInclusionPattern().matcher(file.getCanonicalPath()).matches()) continue;
				if (descriptor.getExclusionPattern() != null && descriptor.getExclusionPattern().matcher(file.getCanonicalPath()).matches()) continue;
				
				Map<Method, Description> methodToDescMap = Maps.newTreeMap(methodComparator);
				
				String fileName = commonRoot == null ? file.getCanonicalPath() : commonRoot.toURI().relativize(file.toURI()).toString();
				String relativePath = root.toURI().relativize(file.toURI()).toString();
				tests.put(fileName, new RunData(
						new File(directory, relativePath),
						mirrorDirectory == null ? null : new File(mirrorDirectory, relativePath),
						methodToDescMap));
				
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
	
	private static File findCommonRoots(Collection<DirDescriptor> dirDescriptors) throws IOException {
		List<File> dirs = Lists.newArrayList();
		for (DirDescriptor d : dirDescriptors) dirs.add(d.getMirrorDirectory() == null ? d.getDirectory() : d.getMirrorDirectory());
		
		File common = dirs == null ? null : dirs.get(0).getCanonicalFile();
		for (int i = 1; i < dirs.size(); i++) common = commonality(common, dirs.get(i).getCanonicalFile());
		return common == null ? new File(".") : common;
	}
	
	private static File commonality(File a, File b) throws IOException {
		if (a == null || b == null) return null;
		
		List<File> pa = Lists.newArrayList();
		List<File> pb = Lists.newArrayList();
		
		a = a.getCanonicalFile();
		b = b.getCanonicalFile();
		
		while (a != null) {
			pa.add(a);
			a = a.getParentFile();
		}
		
		while (b != null) {
			pb.add(b);
			b = b.getParentFile();
		}
		
		Collections.reverse(pa);
		Collections.reverse(pb);
		
		File common = null;
		
		for (int i = 0; ; i++) {
			if (pa.size() <= i || pb.size() <= i) return common;
			if (!pa.get(i).equals(pb.get(i))) return common;
			common = pa.get(i);
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
		if (listFiles == null) listFiles = new File[0];
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
		
		for (Method m : beforeClassMethods) {
			runClassMethod(m);
		}
		
		for (Map.Entry<String, RunData> entry : tests.entrySet()) {
			RunData data = entry.getValue();
			Map<Method, Description> methodList = data.getMethods();
			String content;
			Throwable error;
			
			try {
				content = entry.getKey() == null ? null : Files.toString(
						data.getAlias() == null ? data.getMain() : data.getAlias(), Charsets.UTF_8);
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
						if (!runTest(content, data.getMain(), data.getAlias(), test.getKey())) {
							notifier.fireTestIgnored(testDescription);
						}
					} catch (Throwable t) {
						notifier.fireTestFailure(new Failure(testDescription, t));
					}
				}
				notifier.fireTestFinished(testDescription);
			}
		}
		
		for (Method m : afterClassMethods) {
			runClassMethod(m);
		}
	}
	
	private void runClassMethod(Method m) {
		try {
			if (Modifier.isStatic(m.getModifiers())) {
				m.invoke(null);
			} else {
				m.invoke(testClass.newInstance());
			}
		} catch (InvocationTargetException e) {
			e.getCause().printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean runTest(String rawSource, File main, File alias, Method method) throws Throwable {
		Class<?>[] paramTypes = method.getParameterTypes();
		Object[] params;
		Test t = method.getAnnotation(Test.class);
		
		switch (paramTypes.length) {
		case 0:
			assert rawSource == null;
			params = new Object[0];
			break;
		case 1:
			if (alias != null) return false;
			if (paramTypes[0] == String.class) params = new Object[] {rawSource};
			else if (paramTypes[0] == File.class) params = new Object[] {main};
			else if (paramTypes[0] == Source.class) params = new Object[] {new Source(rawSource, main.getAbsolutePath())};
			else return false;
			break;
		case 2:
			if (alias == null) return false;
			params = new Object[2];
			main = new File(main.getParent(), alias.getName().replaceAll("\\.\\d+\\.java$", ".java"));
			
			String expectedContent = Files.toString(main, Charsets.UTF_8);
			
			if (paramTypes[0] == String.class) params[0] = expectedContent;
			else if (paramTypes[0] == File.class) params[0] = main;
			else if (paramTypes[0] == Source.class) params[0] = new Source(expectedContent, main.getAbsolutePath());
			else return false;
			
			if (paramTypes[1] == String.class) params[1] = rawSource;
			else if (paramTypes[1] == File.class) params[1] = alias;
			else if (paramTypes[1] == Source.class) params[1] = new Source(rawSource, alias.getAbsolutePath());
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
