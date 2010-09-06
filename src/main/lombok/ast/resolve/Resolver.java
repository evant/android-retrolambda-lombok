/*
 * Copyright © 2009, 2010 Reinier Zwitserloot and Roel Spilker.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.ast.Annotation;
import lombok.ast.Block;
import lombok.ast.CompilationUnit;
import lombok.ast.EnumTypeBody;
import lombok.ast.Expression;
import lombok.ast.Identifier;
import lombok.ast.ImportDeclaration;
import lombok.ast.Node;
import lombok.ast.NullLiteral;
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
	
	/**
	 * Checks if the given {@code typeReference} could legally be referring to the listed fully qualified {@code typeName}.
	 */
	public boolean typesMatch(String wanted, TypeReference typeReference) {
		String name = typeReference.getTypeName();
		if (name.equals(wanted)) return true;
		
		/* checks array dimensions */ {
			int dims1 = typeReference.astArrayDimensions();
			int dims2 = 0;
			while (wanted.endsWith("[]")) {
				dims2++;
				wanted = wanted.substring(0, wanted.length() - 2);
			}
			if (dims1 != dims2) return false;
		}
		
		int dot = wanted.lastIndexOf('.');
		String wantedPkg = dot == -1 ? "" : wanted.substring(0, dot);
		String wantedName = dot == -1 ? wanted : wanted.substring(dot + 1);
		
		if (name.indexOf('.') == -1 && wantedName.equals(name)) {
			//name is definitely a simple name, and it might match. Walk up type tree and if it doesn't match any of those, delve into import statements.
			Node n = typeReference.getParent();
			CompilationUnit cu = null;
			while (n != null) {
				RawListAccessor<?, ?> list;
				if (n instanceof Block) list = ((Block) n).rawContents();
				else if (n instanceof TypeBody) list = ((TypeBody) n).rawMembers();
				else if (n instanceof EnumTypeBody) list = ((EnumTypeBody) n).rawMembers();
				else if (n instanceof CompilationUnit) {
					list = ((CompilationUnit) n).rawTypeDeclarations();
					cu = (CompilationUnit) n;
				}
				else list = null;
				
				if (list != null) {
					for (Node c : ((Block) n).rawContents()) {
						if (c instanceof TypeDeclaration && namesMatch(name, ((TypeDeclaration) c).astName())) return false;
					}
				}
				
				n = n.getParent();
			}
			
			//A locally defined type is definitely not what's targetted so it could still be our wanted type reference. Let's check imports.
			if (wantedPkg.isEmpty()) return cu == null || cu.rawPackageDeclaration() == null;
			
			if (cu != null) {
				for (Node imp : cu.rawImportDeclarations()) {
					if (!(imp instanceof ImportDeclaration)) continue;
					ImportDeclaration i = (ImportDeclaration) imp;
					String impName = i.asFullyQualifiedName();
					if (!i.astStaticImport() && i.astStarImport() && wantedPkg.equals(impName)) return true;
					if (impName.equals(wanted)) return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean namesMatch(String name, Identifier astName) {
		return name == null ? astName.astValue() == null : name.equals(astName.astValue());
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
	
	/**
	 * Turns an annotation AST node into an actual instance of an annotation class provided you already know its type.
	 * <strong>NB: non-literal compile-time constants cannot be converted, and you should avoid querying classes;
	 * instead call {@code resolver.getAnnotationClassAsString(objectReturnedByThisMethod, "annotation method name")}.
	 * 
	 * @see #getAnnotationClassesAsStrings(java.lang.annotation.Annotation, String)
	 * @see #getAnnotationClassAsString(java.lang.annotation.Annotation, String)
	 */
	public <A extends java.lang.annotation.Annotation> A toAnnotationInstance(final Class<A> type, final Annotation node) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, new AnnotationProxy(this, node)));
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
