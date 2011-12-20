/*
 * Copyright (C) 2011 The Project Lombok Authors.
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
package lombok.ast.resolve;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodDeclaration;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.grammar.Source;

import org.junit.Test;

public class TypesMatchTest {
	private static final String SIMPLE_SOURCE =
			"import java.util.*;\n" +
			"public class SimpleTypesMatchTest {\n" +
			"	public List<?> test() {}\n" +
			"}\n";
	
	@Test
	public void testBasicTypesMatch() {
		final AtomicInteger hit = new AtomicInteger();
		Source s = new Source(SIMPLE_SOURCE, "SimpleTypesMatchTest.java");
		s.parseCompilationUnit();
		s.getNodes().get(0).accept(new ForwardingAstVisitor() {
			@Override public boolean visitMethodDeclaration(MethodDeclaration node) {
				assertTrue("typesMatch with star import should match but fails:" + node.astReturnTypeReference(),
						new Resolver().typesMatch("java.util.List", node.astReturnTypeReference()));
				assertFalse("typesMatch with no relevant imports should fail but matches",
						new Resolver().typesMatch("java.awt.List", node.astReturnTypeReference()));
				hit.incrementAndGet();
				return true;
			}
		});
		assertEquals("expected 1 hit on MethodDeclaration", 1, hit.get());
	}
	
	private static final String METHOD_LOCAL_MASKING_SOURCE =
			"import java.util.*;\n" +
			"public class MaskingTypesMatchTest {\n" +
			"	void test() {\n" +
			"		List<?> thisIsAUtilList = null;\n" +
			"		class List<T> {}\n" +
			"		List<?> butThisIsnt = null;\n" +
			"	}\n" +
			"}\n";
	
	@Test
	public void testMethodLocalMaskingTypesMatch() {
		Source s = new Source(METHOD_LOCAL_MASKING_SOURCE, "MaskingTypesMatchTest.java");
		s.parseCompilationUnit();
		final AtomicInteger hit1 = new AtomicInteger();
		final AtomicInteger hit2 = new AtomicInteger();
		s.getNodes().get(0).accept(new ForwardingAstVisitor() {
			@Override public boolean visitVariableDefinitionEntry(VariableDefinitionEntry node) {
				if ("thisIsAUtilList".equals(node.astName().astValue())) {
					assertTrue("thisIsAUtilList isn't matched to java.util.List, but should.",
							new Resolver().typesMatch("java.util.List<?>", node.upToVariableDefinition().astTypeReference()));
					hit1.incrementAndGet();
				}
				if ("butThisIsnt".equals(node.astName().astValue())) {
					assertFalse("butThisIsnt is matched to java.util.List, but shouldn't be.",
							new Resolver().typesMatch("java.util.List<?>", node.upToVariableDefinition().astTypeReference()));
					hit2.incrementAndGet();
				}
				
				return true;
			}
		});
		assertEquals("expected 1 hit on VarDefEntry 'thisIsAUtilList'", 1, hit1.get());
		assertEquals("expected 1 hit on VarDefEntry 'butThisIsnt'", 1, hit2.get());
	}
	
	private static final String INNER_MASKING_SOURCE =
			"import java.util.*;\n" +
			"public class MaskingTypesMatchTest {\n" +
			"	List<?> myReturnTypeIsNotJavaUtilList() {}\n" +
			"	\n" +
			"	class List {}\n" +
			"}\n";
	
	@Test
	public void testInnerMaskingTypesMatch() {
		Source s = new Source(INNER_MASKING_SOURCE, "MaskingTypesMatchTest.java");
		s.parseCompilationUnit();
		final AtomicInteger hit = new AtomicInteger();
		s.getNodes().get(0).accept(new ForwardingAstVisitor() {
			@Override public boolean visitMethodDeclaration(MethodDeclaration node) {
				assertFalse("typesMatch with star import but should fail due to inner masking, yet succeeds.",
						new Resolver().typesMatch("java.util.List", node.astReturnTypeReference()));
				hit.incrementAndGet();
				return true;
			}
		});
		assertEquals("expected 1 hit on MethodDeclaration", 1, hit.get());
	}
	
	private static final String TOP_LEVEL_MASKING_SOURCE =
			"import java.util.*;\n" +
			"public class MaskingTypesMatchTest {\n" +
			"	List<?> myReturnTypeIsNotJavaUtilList() {}\n" +
			"}\n" +
			"\n" +
			"class List {}\n";
	
	@Test
	public void testTopLevelMaskingTypesMatch() {
		Source s = new Source(TOP_LEVEL_MASKING_SOURCE, "MaskingTypesMatchTest.java");
		s.parseCompilationUnit();
		final AtomicInteger hit = new AtomicInteger();
		s.getNodes().get(0).accept(new ForwardingAstVisitor() {
			@Override public boolean visitMethodDeclaration(MethodDeclaration node) {
				assertFalse("typesMatch with star import but should fail due to top level masking, yet succeeds.",
						new Resolver().typesMatch("java.util.List", node.astReturnTypeReference()));
				hit.incrementAndGet();
				return true;
			}
		});
		assertEquals("expected 1 hit on MethodDeclaration", 1, hit.get());
	}
	
	private static final String ARRAYS_SOURCE =
			"public class ArraysMatchTest {\n" +
			"	String[] test(String x) {}\n" +
			"}";
	
	@Test
	public void testArrayTypesMatch() {
		Source s = new Source(ARRAYS_SOURCE, "ArrayTypesMatchTest.java");
		s.parseCompilationUnit();
		final AtomicInteger hit = new AtomicInteger();
		s.getNodes().get(0).accept(new ForwardingAstVisitor() {
			@Override public boolean visitMethodDeclaration(MethodDeclaration node) {
				assertTrue("typesMatch with String[] should match java.lang.String[] but doesn't.",
						new Resolver().typesMatch("java.lang.String[]", node.astReturnTypeReference()));
				assertFalse("typesMatch with String[] should NOT match java.lang.String[][] but does.",
						new Resolver().typesMatch("java.lang.String[][]", node.astReturnTypeReference()));
				assertFalse("typesMatch with String[] should NOT match java.lang.String but does.",
						new Resolver().typesMatch("java.lang.String", node.astReturnTypeReference()));
				assertTrue("typesMatch with String should match java.lang.String but doesn't.",
						new Resolver().typesMatch("java.lang.String", node.astParameters().first().astTypeReference()));
				assertFalse("typesMatch with String should NOT match java.lang.String[] but does.",
						new Resolver().typesMatch("java.lang.String[]", node.astParameters().first().astTypeReference()));
				hit.incrementAndGet();
				return true;
			}
		});
		assertEquals("expected 1 hit on MethodDeclaration", 1, hit.get());
	}
	
	private static final String IMPORTS_SOURCE =
			"import java.awt.List;\n" +
			"import java.util.*;\n" +
			"" +
			"public class ImportsTest {\n" +
			"	List test() {}\n" +
			"}";
	
	@Test
	public void testImportedTypesMatch() {
		Source s = new Source(IMPORTS_SOURCE, "ImportsTest.java");
		s.parseCompilationUnit();
		final AtomicInteger hit = new AtomicInteger();
		s.getNodes().get(0).accept(new ForwardingAstVisitor() {
			@Override public boolean visitMethodDeclaration(MethodDeclaration node) {
				assertFalse("typesMatch with java.awt.List+java.util.* imported should NOT match java.util.List",
						new Resolver().typesMatch("java.util.List", node.astReturnTypeReference()));
				assertTrue("typesMatch with java.awt.List+java.util.* imported SHOULD match java.awt.List",
						new Resolver().typesMatch("java.awt.List", node.astReturnTypeReference()));
				hit.incrementAndGet();
				return true;
			}
		});
		assertEquals("expected 1 hit on MethodDeclaration", 1, hit.get());
	}
}
