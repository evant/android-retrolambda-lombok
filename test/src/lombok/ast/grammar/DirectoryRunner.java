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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Test.None;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import com.google.common.collect.Lists;

public class DirectoryRunner extends Runner {
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
	
	public DirectoryRunner(Class<?> testClass) {
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
		Method directoryMethod = testClass.getDeclaredMethod("getDirectory");
		directory = (File) directoryMethod.invoke(null);
		
		try {
			Method mirrorMethod = testClass.getDeclaredMethod("getMirrorDirectory");
			mirrorDirectory = (File)mirrorMethod.invoke(null);
		} catch (NoSuchMethodException e) {
			//then we'll go on without one!
		}
		
		File root = mirrorDirectory == null ? directory : mirrorDirectory;
		List<File> files = listFilesRecursively(root);
		
		Map<Method, Description> noFileNeededMap = new TreeMap<Method, Description>(methodComparator);
		
		for (File file : files) {
			if (!file.getName().endsWith(".java")) continue;
			Map<Method, Description> methodToDescMap = new TreeMap<Method, Description>(methodComparator);
			
			String fileName = root.toURI().relativize(file.toURI()).toString();
			tests.put(fileName, methodToDescMap);
			
			for (Method m : testClass.getMethods()) {
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
	
	private List<File> listFilesRecursively(File directory) {
		List<File> all = Lists.newArrayList();
		listFilesRecursively(all, directory);
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
	
	private void listFilesRecursively(List<File> collector, File directory) {
		File[] listFiles = directory.listFiles();
		Arrays.sort(listFiles, FILE_SORTER);
		for (File f : listFiles) {
			if (f.isDirectory()) {
				listFilesRecursively(collector, f);
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
		
		for (Entry<String, Map<Method, Description>> entry : tests.entrySet()) {
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
			
			for (Entry<Method, Description> test : methodList.entrySet()) {
				Description testDescription = test.getValue();
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
			else if (paramTypes[0] == Source.class) params = new Object[] {new Source(rawSource, fileName)};
			else return false;
			break;
		case 2:
			if (mirrorDirectory == null) return false;
			params = new Object[2];
			String mainFileName = fileName.replaceAll("\\.\\d+\\.java$", ".java");
			String expectedContent = FileUtils.readFileToString(new File(directory, mainFileName), "UTF-8");
			
			if (paramTypes[0] == String.class) params[0] = expectedContent;
			else if (paramTypes[0] == Source.class) params[0] = new Source(expectedContent, mainFileName);
			else return false;
			
			if (paramTypes[1] == String.class) params[1] = rawSource;
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
