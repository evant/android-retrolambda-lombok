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
package lombok.ast.ecj;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.val;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class EcjTreePrinter {
	private static final Multimap<Class<?>, ComponentField> visitedClasses = ArrayListMultimap.create();
	private static final List<String> POSITION_FIELDNAMES = ImmutableList.of(
			"sourceStart",
			"sourceEnd",
			"originalSourceEnd",
			"bodyStart",
			"bodyEnd",
			"blockStart",
			"declarationSourceStart",
			"declarationSourceEnd",
			"declarationEnd",
			"endPart1Position",
			"endPart2Position",
			"valuePositions",
			"sourcePositions",
			"modifiersSourceStart",
			"typeArgumentsSourceStart",
			"statementEnd",
			"labelEnd",
			"nameSourcePosition",
			"tagSourceStart",
			"tagSourceEnd"
			);
	
	private final Printer printer;
	private Set<String> propertySkipList = Sets.newHashSet();
	private Multimap<String, Object> propertyIfValueSkipList = ArrayListMultimap.create();
	private Map<String, String> stringReplacements = Maps.newHashMap();
	private List<ReferenceTrackingSkip> referenceTrackingSkipList = Lists.newArrayList();
	
	private EcjTreePrinter(boolean printPositions) {
		printer = new Printer(printPositions);
	}
	
	public static EcjTreePrinter printerWithPositions() {
		return new EcjTreePrinter(true);
	}
	
	public static EcjTreePrinter printerWithoutPositions() {
		return new EcjTreePrinter(false);
	}
	
	@Override
	public String toString() {
		return getContent();
	}
	
	public String getContent() {
		String result = printer.content.toString();
		for (val entry : stringReplacements.entrySet()) {
			result = result.replace(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	public void visit(ASTNode node) {
		visitor.visitEcjNode(node);
	}
	
	public EcjTreePrinter skipProperty(Class<? extends ASTNode> type, String propertyName) {
		propertySkipList.add(type.getSimpleName() + "/" + propertyName);
		return this;
	}
	
	public EcjTreePrinter skipPropertyIfHasValue(Class<? extends ASTNode> type, String propertyName, Object value) {
		propertyIfValueSkipList.put(type.getSimpleName() + "/" + propertyName, value);
		return this;
	}
	
	public void stringReplace(String original, String replacement) {
		stringReplacements.put(original, replacement);
	}
	
	@Data
	private static class ReferenceTrackingSkip {
		private final Class<? extends ASTNode> parent;
		private final Class<?> type;
	}
	
	public EcjTreePrinter skipReferenceTracking(Class<? extends ASTNode> parent, Class<?> type) {
		referenceTrackingSkipList.add(new ReferenceTrackingSkip(parent, type));
		return this;
	}
	
	private final EcjTreeVisitor visitor = new EcjTreeVisitor() {
		@Override public void visitAny(ASTNode node) {
			Collection<ComponentField> fields = findFields(node);
			for (ComponentField f : fields) {
				String skipListKey = node.getClass().getSimpleName() + "/" + f.field.getName();
				if (propertySkipList.contains(skipListKey)) continue;
				
				Object value;
				
				if (node instanceof ConditionalExpression) ((ConditionalExpression)node).valueIfTrue.sourceEnd = -2;
				if ("originalSourceEnd".equals(f.field.getName()) && node instanceof ArrayTypeReference) {
					//workaround for eclipse arbitrarily skipping this field and setting it.
					value = -2;
				} else {
					value = readField(f.field, node);
				}
				if (value == null) {
					continue;
				}
				
				if (propertyIfValueSkipList.get(skipListKey).contains(value)) continue;
				
				boolean trackRef = true;
				for (ReferenceTrackingSkip skip : referenceTrackingSkipList) {
					if (skip.getParent() != null && !skip.getParent().isInstance(node)) continue;
					if (skip.getType() != null && !skip.getType().isInstance(value)) continue;
					trackRef = false;
					break;
				}
				f.print(printer, this, value, trackRef);
			}
		}
		
		//TODO all the javadocy nodes need to be as methods in EcjTreeVisitor.
		
		@Override
		public void visitOther(ASTNode node) {
			visitAny(node);
		}
	};
	
	@SneakyThrows(IllegalAccessException.class)
	private Object readField(Field field, ASTNode node) {
		return field.get(node);
	}
	
	private static Collection<ComponentField> findFields(ASTNode node) {
		Class<? extends ASTNode> clazz = node.getClass();
		if (visitedClasses.containsKey(clazz)) {
			return visitedClasses.get(clazz);
		}
		List<ComponentField> fields = Lists.newArrayList();
		for (Field f : findAllFields(clazz)) {
			if ((f.getModifiers() & Modifier.STATIC) != 0) continue;
			fields.add(new ComponentField(f));
		}
		Collections.sort(fields);
		visitedClasses.putAll(clazz, fields);
		return fields;
	}
	
	
	private static List<Field> findAllFields(Class<?> clazz) {
		List<Field> allFields = Lists.newArrayList();
		findAllFieldsRecursively(allFields, clazz);
		return allFields;
	}

	private static void findAllFieldsRecursively(List<Field> allFields, Class<?> clazz) {
		if (clazz == Object.class) {
			return;
		}
		allFields.addAll(Arrays.asList(clazz.getDeclaredFields()));
		findAllFieldsRecursively(allFields, clazz.getSuperclass());
	}
	
	static class Printer {
		final Map<Object, Integer> visited = new MapMaker().weakKeys().makeMap();
		int objectCounter = 0;
		
		private final StringBuilder content = new StringBuilder();
		private int indent;
		
		private final boolean printPositions;
		
		public Printer(boolean printPositions) {
			this.printPositions = printPositions;
		}
		
		void begin(String typeName, String description, Class<?> clazz, int id) {
			printIndent();
			content.append(typeName).append(" ").append(description)
					.append(" = ").append(clazz.getSimpleName());
			if (id != -1) content.append(" (id:").append(id).append(")");
			content.append("\n");
			indent++;
		}
		
		void end() {
			indent--;
		}
		
		public void printProperty(String typeName, String name, Object value, boolean posField) {
			printIndent();
			String stringValue;
			if (posField) {
				if (value instanceof Long) {
					long longValue = (Long)value;
					stringValue = String.format("(%d, %d)", (int)(longValue >> 32), (int)(longValue & 0xFFFFFFFFL));
				} else {
					stringValue = String.valueOf(value);
				}
			} else if ("bits".equals(name) && value instanceof Integer) {
				stringValue = formatBits((Integer)value);
			} else if (value instanceof Long) {
				long longValue = ((Long)value).longValue();
				stringValue = String.format("0x%1$016x (%1$d)  %2$d<<32 | %3$d", value, (int)(longValue>>32), (int)(longValue & 0xFFFFFFFFL));
			} else if (value instanceof Integer) {
				stringValue = String.format("0x%1$08x (%1$d)", value);
			} else if (value instanceof char[]) {
				stringValue = new lombok.ast.StringLiteral().astValue(new String((char[])value)).rawValue();
			} else if (value instanceof char[][]) {
				StringBuilder sb = new StringBuilder();
				for (char[] single : ((char[][])value)) {
					if (sb.length() != 0) {
						sb.append(", ");
					}
					sb.append(new lombok.ast.StringLiteral().astValue(new String(single)).rawValue());
				}
				stringValue = "{" + sb.toString() + "}";
			} else if ("compilationResult".equals(name)) {
				stringValue = value == null ? "[NULL]" : "[SET]";
			} else if ("problemReporter".equals(name)) {
				return;
			} else {
				stringValue = String.valueOf(value);
			}
			content.append(typeName).append(" ").append(name).append(" = ").append(stringValue).append("\n");
		}
		
		private static String formatBits(int value) {
			List<Integer> elems = Lists.newArrayList();
			int pos = 0;
			while (value != 0) {
				pos++;
				if ((value & 1) != 0) elems.add(pos);
				value >>>= 1;
			}
			return elems.isEmpty() ? "NONE" : "[" + Joiner.on(",").join(elems) + "]";
		}
		
		private void printIndent() {
			for (int i = 0; i < indent; i++) {
				content.append("  ");
			}
		}
	}
	
	static class ComponentField implements Comparable<ComponentField>{
		private static final List<Package> KNOWN_PACKAGES = ImmutableList.of(
				String.class.getPackage(),
				ASTNode.class.getPackage(),
				TypeBinding.class.getPackage(),
				Constant.class.getPackage()
		);
		
		private ImmutableMap<Class<?>, Object> DEFAULTS = 
				ImmutableMap.<Class<?>, Object>builder()
						.put(boolean.class, false)
						.put(byte.class, (byte)0)
						.put(short.class, (short)0)
						.put(int.class, 0)
						.put(char.class, '\0')
						.put(long.class, 0L)
						.put(float.class, 0f)
						.put(double.class, .0)
						.build();
		
		private final Field field;
		private final int dimensions;
		private final Class<?> type;
		
		public ComponentField(Field field) {
			this.field = field;
			field.setAccessible(true);
			Class<?> type = field.getType();
			int dimensions = 0;
			while (type.isArray()) {
				dimensions++;
				type = type.getComponentType();
			}
			this.dimensions = dimensions;
			this.type = type;
		}
		
		@Override
		public String toString() {
			return createDescription();
		}
		
		private String createDescription() {
			StringBuilder result = new StringBuilder();
			result.append(typeName());
			for (int dim = 0; dim < dimensions; dim++) {
				result.append("[]");
			}
			result.append(" ").append(field.getName());
			return result.toString();
		}
		
		private String typeName() {
			if (type.isPrimitive() || KNOWN_PACKAGES.contains(type.getPackage())) {
				return type.getSimpleName(); 
			}
			return type.getName();
		}
		
		boolean isPositionField() {
			return POSITION_FIELDNAMES.contains(field.getName());
		}
		
		@Override
		public int compareTo(ComponentField o) {
			Class<?> otherType = o.type;
			
			if (isPositionField() && o.isPositionField()) {
				return POSITION_FIELDNAMES.indexOf(field.getName()) - POSITION_FIELDNAMES.indexOf(o.field.getName());
			}
			
			if (isPositionField() || o.isPositionField()) return isPositionField() ? -1 : 1;
			
			if (type.isPrimitive() == otherType.isPrimitive()) {
				return String.CASE_INSENSITIVE_ORDER.compare(field.getName(), o.field.getName());
			}
			return type.isPrimitive() ? -1 : 1;
		}
		
		public void printVisited(Printer printer, Integer id) {
			printer.printProperty(typeName(), field.getName(), "reference to " + id, false);
		}
		
		public void print(Printer printer, EcjTreeVisitor visitor, Object value, boolean trackRef) {
			boolean posField = isPositionField();
			
			if (!printer.printPositions && posField) return;
			if (!posField && isDefault(value)) return;
			
			unroll(printer, visitor, value, 0, field.getName(), posField, trackRef);
		}
		
		private void unroll(Printer printer, EcjTreeVisitor visitor, Object value, int dim, String description, boolean posField, boolean trackRef) {
			if (dim == dimensions) {
				if (value instanceof ASTNode) {
					if (!trackRef) {
						printer.begin(typeName(), description, value.getClass(), printer.objectCounter++);
						visitor.visitEcjNode((ASTNode)value);
						printer.end();
					} else if (printer.visited.containsKey(value)) {
						printVisited(printer, printer.visited.get(value));
					} else {
						printer.visited.put(value, printer.objectCounter);
						printer.begin(typeName(), description, value.getClass(), printer.objectCounter++);
						visitor.visitEcjNode((ASTNode)value);
						printer.end();
					}
				} else {
					printer.printProperty(typeName(), description, value, posField);
				}
			} else {
				if (value == null) {
					printer.printProperty(typeName(), description,  "NULL", posField);
				} else {
					if (type == char.class && dimensions - dim <= 2) {
						if (dimensions - dim == 1) {
							printer.printProperty(typeName(), description + "[]", value, posField);
						} else {
							printer.printProperty(typeName(), description + "[][]", value, posField);
						}
						return;
					}
					int length = Array.getLength(value);
					for (int i = 0; i < length; i++) {
						unroll(printer, visitor, Array.get(value, i), dim + 1, description + "[" + i + "]", posField, trackRef);
					}
				}
			}
		}
		
		private boolean isDefault(Object value) {
			if (value == null) return true;
			
			if (type.isPrimitive() && dimensions == 0) {
				return DEFAULTS.get(type).equals(value);
			}
			return false;
		}
	}
}
