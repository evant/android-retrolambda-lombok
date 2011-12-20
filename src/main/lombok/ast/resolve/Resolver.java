/*
 * Copyright (C) 2009-2011 The Project Lombok Authors.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.ast.Annotation;
import lombok.ast.Block;
import lombok.ast.CompilationUnit;
import lombok.ast.Expression;
import lombok.ast.Identifier;
import lombok.ast.ImportDeclaration;
import lombok.ast.Node;
import lombok.ast.NullLiteral;
import lombok.ast.PackageDeclaration;
import lombok.ast.RawListAccessor;
import lombok.ast.ResolutionException;
import lombok.ast.Select;
import lombok.ast.TypeBody;
import lombok.ast.TypeDeclaration;
import lombok.ast.TypeReference;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Contains simplistic (guesstimations) resolution that doesn't require full resolution and symbol lookup but it isn't perfect.
 */
public class Resolver {
	/**
	 * If {@code value} is an enum constant of type {@code enumClass}, then the enum value will be returned.
	 * {@code null} will be returned if {@code value} is an actual null literal.
	 * 
	 * @throws ResolutionException If {@code value} cannot be converted.
	 */
	public <E extends Enum<E>> E resolveEnum(Class<E> enumClass, Node value) {
		// case1: null
		if (value instanceof NullLiteral) return null;
		
		// case2: Identifier
		String enumName = null;
		
		if (value instanceof Identifier) {
			enumName = ((Identifier)value).astValue();
		}
		
		// case3: EnumSimpleName.Identifier or EnumFQN.Identifier
		String typeName = null;
		String packageName = null;
		if (value instanceof Select) {
			List<String> chain = unwrapSelectChain((Select) value);
			switch (chain.size()) {
			case 0:
				throw new ResolutionException(value, "empty");
			default:
				packageName = Joiner.on('.').join(chain.subList(0, chain.size() - 2));
			case 2:
				typeName = chain.get(chain.size() - 2);
			case 1:
				enumName = chain.get(chain.size() - 1);
			}
			
			boolean unexpectedType = false;
			if (packageName != null) {
				Package p = enumClass.getPackage();
				unexpectedType = p != null && !p.getName().equals(packageName);
			}
			unexpectedType |= (typeName != null && !enumClass.getSimpleName().equals(typeName));
			
			if (unexpectedType) throw new ResolutionException(value, "Expected " + enumClass.getName() + " and not " + packageName + "." + typeName);
		}
		
		for (E enumConstant : enumClass.getEnumConstants()) {
			String target = enumConstant.name();
			if (target.equals(enumName)) return enumConstant;
		}
		
		throw new ResolutionException(value, "Not a valid value for enum " + enumClass.getSimpleName() + ": " + enumName);
	}
	
	static final List<Class<?>> NUMERIC_PRIMITIVE_CLASSES = ImmutableList.<Class<?>>of(
			long.class, int.class, short.class, byte.class, double.class, float.class, char.class);
	static final Map<String, Class<?>> PRIMITIVE_CLASS_MAP = ImmutableMap.<String, Class<?>>builder()
			.put("boolean", boolean.class)
			.put("byte", byte.class)
			.put("short", short.class)
			.put("int", int.class)
			.put("long", long.class)
			.put("char", char.class)
			.put("float", float.class)
			.put("double", double.class)
			.build();
	
	private static class ImportList {
		final List<String> explicits = new ArrayList<String>();
		final List<String> stars = new ArrayList<String>();
		
		ImportList() {
			stars.add("java.lang");
		}
	}
	
	private ImportList getImportList(Node n) {
		ImportList il = new ImportList();
		
		while (n != null) {
			if (n instanceof CompilationUnit) {
				CompilationUnit cu = (CompilationUnit) n;
				PackageDeclaration pkg = cu.astPackageDeclaration();
				if (pkg != null) il.stars.add(pkg.getPackageName());
				for (ImportDeclaration imp : cu.astImportDeclarations()) {
					if (imp.astStaticImport()) continue;
					if (imp.astStarImport()) {
						String i = imp.asFullyQualifiedName();
						i = i.substring(0, i.length() - 2);
						il.stars.add(i);
					} else {
						il.explicits.add(imp.asFullyQualifiedName());
					}
				}
			}
			
			n = n.getParent();
		}
		
		return il;
	}
	
	/**
	 * Checks if the given {@code typeReference} could legally be referring to the listed fully qualified {@code typeName}.
	 * Will check import statements, and checks for any shadowing by types in this file with the same name.
	 * <p>
	 * <em>WARNING:</em> This method will return {@code true} if there's a star import of i.e. {@code java.util.*},
	 * you ask if {@code ArrayList} could be referring to {@code java.util.ArrayList}, and there is another class in the same package also named
	 * {@code ArrayList}. The right answer is of course {@code false}, but without a classpath/sourcepath this cannot be determined. Therefore,
	 * this method is 99% but not 100% accurate. Also, god kills a puppy everytime you write a star import.
	 * <p>
	 * <em>NB:</em> Any type arguments (generics) on either the {@code typeReference} or {@code wanted} are stripped off before comparing the two.
	 * <p>
	 * <em>NB:</em> If you want array types, just put array brackets at the end of your {@code wanted} string, for example: "{@code java.lang.String[]}".
	 * 
	 * @param wanted A fully qualified type name, such as {@code java.lang.String}.
	 * @param typeReference A type reference node.
	 * @return {@code true} if the typeReference is most likely a reference to {@code wanted}.
	 */
	public boolean typesMatch(String wanted, TypeReference typeReference) {
		String name = stripGenerics(typeReference.getTypeName());
		wanted = stripGenerics(wanted);
		
		if (name.equals(wanted)) return true;
		
		/* check array dimensions */ {
			int dims1 = typeReference.astArrayDimensions();
			int dims2 = 0;
			while (wanted.endsWith("[]")) {
				dims2++;
				wanted = wanted.substring(0, wanted.length() - 2);
			}
			if (dims1 != dims2) return false;
			if (dims1 > 0) name = name.substring(0, name.length() - dims1 * 2);
		}
		
		int dot = wanted.lastIndexOf('.');
		String wantedPkg = dot == -1 ? "" : wanted.substring(0, dot);
		String wantedName = dot == -1 ? wanted : wanted.substring(dot + 1);
		
		// If type ref appears fully qualified and it hasn't matched by now, we're done.
		if (name.indexOf('.') > -1) return false;
		
		// 'Baz' will never be a match for foo.bar.NotBaz.
		if (!wantedName.equals(name)) return false;
		
		ImportList imports = getImportList(typeReference);
		
		/* If matching List to java.util.List and java.awt.List is explicitly imported, no match. */ {
			String ending = "." + wantedName;
			for (String explicit : imports.explicits) {
				if (explicit.endsWith(ending) && !explicit.equals(wanted)) return false;
			}
		}
		
		potentialMatchFound:
		/* To match List to java.util.List either there has to be a java.util.* or java.util.List import. */ {
			if (wantedPkg.length() > 0 && imports.stars.contains(wantedPkg)) break potentialMatchFound;
			if (imports.explicits.contains(wanted)) break potentialMatchFound;
			return false;
		}
		
		//name is definitely a simple name, and it might match. Walk up type tree and if it doesn't match any of those, delve into import statements.
		Node n = typeReference.getParent();
		Node prevN = null;
		CompilationUnit cu = null;
		while (n != null) {
			RawListAccessor<?, ?> list;
			boolean stopAtSelf;
			
			if (n instanceof Block) {
				list = ((Block) n).rawContents();
				stopAtSelf = true;
			} else if (n instanceof TypeBody) {
				list = ((TypeBody) n).rawMembers();
				stopAtSelf = false;
			} else if (n instanceof CompilationUnit) {
				list = ((CompilationUnit) n).rawTypeDeclarations();
				cu = (CompilationUnit) n;
				stopAtSelf = false;
			} else {
				list = null;
				stopAtSelf = false;
			}
			
			if (list != null) {
				for (Node c : list) {
					if (c instanceof TypeDeclaration && namesMatch(name, ((TypeDeclaration) c).astName())) return false;
					if (stopAtSelf && c == prevN) break;
				}
			}
			
			prevN = n;
			n = n.getParent();
		}
		
		//A locally defined type is definitely not what's targeted so it could still be our wanted type reference. Let's check imports.
		if (wantedPkg.isEmpty()) return cu == null || cu.rawPackageDeclaration() == null;
		
		return true;
	}
	
	private String stripGenerics(String name) {
		int genericsStart = name.indexOf('<');
		int genericsEnd = name.lastIndexOf('>');
		if (genericsStart != -1 && genericsEnd == name.length() -1) name = name.substring(0, genericsStart);
		return name;
	}
	
	private boolean namesMatch(String name, Identifier astName) {
		return name == null ? astName.astValue() == null : name.equals(astName.astValue());
	}
	
	/**
	 * Turns an annotation AST node into an actual instance of an annotation class provided you already know its type.
	 * <strong>NB: non-literal compile-time constants cannot be converted, and you should avoid querying classes;
	 * instead call {@code resolver.getAnnotationClassAsString(objectReturnedByThisMethod, "annotation method name")}.
	 * 
	 * @see #getAnnotationClassesAsStrings(java.lang.annotation.Annotation, String)
	 * @see #getAnnotationClassAsString(java.lang.annotation.Annotation, String)
	 */
	public <A extends java.lang.annotation.Annotation> A toAnnotationInstance(Class<A> type, Annotation node) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, new AnnotationProxy(this, node)));
	}
	
	/**
	 * Use together with {@link #toAnnotationInstance(Class, Annotation)}.
	 * 
	 * @see #getAnnotationClassesAsStrings(java.lang.annotation.Annotation, String)
	 */
	public String getAnnotationClassAsString(java.lang.annotation.Annotation annotation, String methodName) {
		try {
			Method m = annotation.getClass().getMethod(methodName);
			if (m.getReturnType() != Class.class) throw new IllegalArgumentException("Method " + methodName + " does not have 'Class' as return type");
			try {
				return Class.class.cast(m.invoke(annotation)).toString();
			} catch (AnnotationClassNotAvailableException e) {
				return e.getClassName();
			}
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Method " + methodName + " isn't accessible", e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException("Method " + methodName + " cannot be invoked", e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Method " + methodName + " does not exist");
		}
	}
	
	/**
	 * Use together with {@link #toAnnotationInstance(Class, Annotation)}.
	 * 
	 * @see #getAnnotationClassAsString(java.lang.annotation.Annotation, String)
	 */
	public List<String> getAnnotationClassesAsStrings(java.lang.annotation.Annotation annotation, String methodName) {
		try {
			Method m = annotation.getClass().getMethod(methodName);
			boolean array;
			if (m.getReturnType() == Class.class) array = false;
			else if (m.getReturnType() == Class[].class) array = true;
			else throw new IllegalArgumentException("Method " + methodName + " does not have 'Class' or 'Class[]' as return type");
			
			try {
				Class<?>[] cs;
				if (array) {
					cs = Class[].class.cast(m.invoke(annotation));
				} else {
					cs = new Class[1];
					cs[0] = Class.class.cast(m.invoke(annotation));
				}
				
				List<String> result = Lists.newArrayList();
				for (Class<?> c : cs) result.add(c.getName());
				return result;
			} catch (AnnotationClassNotAvailableException e) {
				return e.getClassNames();
			}
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Method " + methodName + " isn't accessible", e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException("Method " + methodName + " cannot be invoked", e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Method " + methodName + " does not exist");
		}
	}
	
	private List<String> unwrapSelectChain(Select s) {
		List<String> list = Lists.newArrayList();
		while (s != null) {
			list.add(s.astIdentifier().astValue());
			Expression parent = s.astOperand();
			if (parent instanceof Select) {
				s = (Select) parent;
			} else if (parent instanceof Identifier) {
				s = null;
				list.add(((Identifier)parent).astValue());
			} else if (parent == null) {
				break;
			} else {
				throw new ResolutionException(parent, "Identifies expected here, not a " + parent.getClass().getSimpleName());
			}
		}
		
		Collections.reverse(list);
		return list;
	}
}
