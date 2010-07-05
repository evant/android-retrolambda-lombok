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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import lombok.ast.Annotation;
import lombok.ast.AnnotationElement;
import lombok.ast.BooleanLiteral;
import lombok.ast.CharLiteral;
import lombok.ast.ClassLiteral;
import lombok.ast.FloatingPointLiteral;
import lombok.ast.IntegralLiteral;
import lombok.ast.Node;
import lombok.ast.ResolutionException;
import lombok.ast.StringLiteral;
import lombok.ast.UnaryExpression;
import lombok.ast.UnaryOperator;

import com.google.common.collect.Lists;

class AnnotationProxy implements InvocationHandler {
	private final Resolver resolver;
	private final Annotation node;
	
	AnnotationProxy(Resolver resolver, Annotation node) {
		this.resolver = resolver;
		this.node = node;
	}
	
	private boolean tryAsString(Node val, Class<?> expectedType, List<Object> returnValues) {
		if (expectedType != String.class) return false;
		
		if (val instanceof StringLiteral) {
			returnValues.add(((StringLiteral)val).astValue());
			return true;
		}
		
		throw new ResolutionException(val, "Expected string literal");
	}
	
	@SuppressWarnings("unchecked")
	private boolean tryAsEnum(Node val, Class<?> expectedType, List<Object> returnValues) {
		if (!expectedType.isEnum()) return false;
		
		returnValues.add(this.resolver.resolveEnum(expectedType.asSubclass(Enum.class), val));
		return true;
	}
	
	private boolean tryAsBoolean(Node val, Class<?> expectedType, List<Object> returnValues) {
		if (expectedType != boolean.class) return false;
		
		if (val instanceof BooleanLiteral) {
			boolean v = ((BooleanLiteral)val).astValue();
			returnValues.add(v);
			return true;
		}
		
		throw new ResolutionException(val, "Expected boolean literal");
	}
	
	private boolean tryAsNumeric(Node val, Class<?> expectedType, List<Object> returnValues) {
		if (!Resolver.NUMERIC_PRIMITIVE_CLASSES.contains(expectedType)) return false;
		
		boolean negative = false;
		if (val instanceof UnaryExpression && ((UnaryExpression)val).astOperator() == UnaryOperator.UNARY_MINUS) {
			val = ((UnaryExpression)val).rawOperand();
			negative = true;
		}
		
		if (!(val instanceof IntegralLiteral) && !(val instanceof FloatingPointLiteral) && !(val instanceof CharLiteral)) {
			throw new ResolutionException(val, "Expected number or character literal");
		}
		
		boolean isIntegral = true;
		long iVal; {
			if (val instanceof IntegralLiteral) {
				long v = ((IntegralLiteral)val).astLongValue();
				iVal = negative ? -v : v;
			} else if (val instanceof CharLiteral) {
				long v = ((CharLiteral)val).astValue();
				iVal = negative ? -v : v;
			} else {
				iVal = 0;
				isIntegral = false;
			}
		}
		
		double dVal; {
			if (val instanceof FloatingPointLiteral) {
				dVal = ((FloatingPointLiteral)val).astDoubleValue();
			} else {
				dVal = 0.0;
			}
		}
		
		if (expectedType == double.class) returnValues.add(isIntegral ? (double) iVal : dVal);
		else if (expectedType == float.class) returnValues.add(isIntegral ? (float) iVal : dVal);
		else if (expectedType == long.class) returnValues.add(isIntegral ? iVal : (long) dVal);
		else if (expectedType == int.class) returnValues.add(isIntegral ? (int) iVal : (int) dVal);
		else if (expectedType == char.class) returnValues.add(isIntegral ? (char) iVal : (char) dVal);
		else if (expectedType == short.class) returnValues.add(isIntegral ? (short) iVal : (short) dVal);
		else if (expectedType == byte.class) returnValues.add(isIntegral ? (byte) iVal : (byte) dVal);
		else throw new AssertionError("Forgotten primitive numeric type");
		
		return true;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String name = method.getName();
		Class<?> expectedType = method.getReturnType();
		boolean array = false;
		if (expectedType.isArray()) {
			array = true;
			expectedType = expectedType.getComponentType();
		}
		
		List<Object> returnValues = Lists.newArrayList();
		
		List<String> classNames = Lists.newArrayList();
		AnnotationClassNotAvailableException classNotAvailable = null;
		for (AnnotationElement elem : node.astElements()) {
			if (!(elem.astName() == null && name.equals("value")) && !name.equals(elem.astName())) continue;
			for (Node val : elem.getValues()) {
				if (tryAsNumeric(val, expectedType, returnValues)) continue;
				if (tryAsBoolean(val, expectedType, returnValues)) continue;
				if (tryAsString(val, expectedType, returnValues)) continue;
				if (tryAsEnum(val, expectedType, returnValues)) continue;
				if (expectedType == Class.class) {
					if (val instanceof ClassLiteral) {
						String className = ((ClassLiteral)val).astTypeReference().getTypeName();
						String cName = className;
						int dims = 0;
						while (cName.endsWith("[]")) {
							dims++;
							cName = cName.substring(0, className.length() - 2);
						}
						try {
							Class<?> c = Resolver.PRIMITIVE_CLASS_MAP.get(cName);
							if (c == null) {
								ClassLoader cl = Resolver.class.getClassLoader();
								if (cl == null) cl = ClassLoader.getSystemClassLoader();
								c = Class.forName(cName, false, cl);
							}
							if (dims > 0) {
								int[] dimsA = new int[dims];
								c = Array.newInstance(c, dimsA).getClass();
							}
							returnValues.add(c);
							continue;
						} catch (ClassNotFoundException e) {
							classNotAvailable = new AnnotationClassNotAvailableException(val, className);
						} finally {
							classNames.add(className);
						}
					} else throw new ResolutionException(val, "Expected class literal");
				}
				if (expectedType.isAnnotation()) {
					if (val instanceof Annotation) {
						returnValues.add(this.resolver.toAnnotationInstance(expectedType.asSubclass(java.lang.annotation.Annotation.class), (Annotation)val));
						continue;
					} else {
						throw new ResolutionException(val, "Expected an annotation of type " + expectedType);
					}
				}
				throw new ResolutionException(val, "Not a valid annotation type: " + expectedType);
			}
		}
		
		if (classNotAvailable != null) {
			classNotAvailable.setClassNames(classNames);
			throw classNotAvailable;
		}
		
		if (array) {
			Object arr = Array.newInstance(expectedType, returnValues.size());
			for (int i = 0; i < returnValues.size(); i++) Array.set(arr, i, returnValues.get(i));
			return arr;
		}
		
		switch (returnValues.size()) {
		case 0:
			Object def = method.getDefaultValue();
			if (def != null) return def;
			throw new ResolutionException(node, "Missing annotation method: " + method.getName());
		case 1:
			return returnValues.get(0);
		default:
			throw new ResolutionException(node, "Multiple values for a single-value annotation method: " + method.getName());
		}
	}
}