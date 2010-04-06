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
package lombok.ast;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.NonNull;
import lombok.ast.template.CopyMethod;
import lombok.ast.template.GenerateAstNode;
import lombok.ast.template.InitialValue;
import lombok.ast.template.NotChildOfNode;

class ExpressionMixin {
	@NotChildOfNode(suppressSetter=true, codeToCopy="new java.util.ArrayList<lombok.ast.Position>(this.parensPositions)")
	@NonNull
	@InitialValue("new java.util.ArrayList<lombok.ast.Position>()")
	List<Position> parensPositions;
	
	@CopyMethod
	static int getParens(Expression self) {
		return self.getParensPositions() == null ? 0 : self.getParensPositions().size();
	}
	
	@CopyMethod
	static int getIntendedParens(Expression self) {
		return Math.max(needsParentheses(self) ? 1 : 0, self.getParens());
	}
	
	@CopyMethod
	static boolean needsParentheses(Expression self) {
		return false;
	}
	
	@CopyMethod
	static boolean isStatementExpression(Expression self) {
		if (self instanceof MethodInvocation) return true;
		if (self instanceof ConstructorInvocation) return true;
		if (self instanceof BinaryExpression) {
			try {
				return ((BinaryExpression)self).getOperator().isAssignment();
			} catch (Exception e) {
				return false;
			}
		}
		if (self instanceof UnaryExpression) {
			try {
				switch (((UnaryExpression)self).getOperator()) {
				case POSTFIX_DECREMENT:
				case POSTFIX_INCREMENT:
				case PREFIX_DECREMENT:
				case PREFIX_INCREMENT:
					return true;
				default:
					return false;
				}
			} catch (Exception e) {
				return false;
			}
		}
		
		return false;
	}
	
}

@GenerateAstNode(implementing=Statement.class)
class AssertTemplate {
	@NonNull Expression assertion;
	Expression message;
}

@GenerateAstNode(implementing={Statement.class, DescribedNode.class})
class CatchTemplate {
	@NonNull VariableDefinition exceptionDeclaration;
	@NonNull Block body;
	
	@CopyMethod
	static String getDescription(Catch self) {
		try {
			return self.getExceptionDeclaration().getTypeReference().getDescription();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing=Statement.class)
class BlockTemplate {
	List<Statement> contents;
}

@GenerateAstNode(implementing=Statement.class)
class DoWhileTemplate {
	@NonNull Expression condition;
	@NonNull Statement statement;
}

@GenerateAstNode(implementing=Statement.class)
class WhileTemplate {
	@NonNull Expression condition;
	@NonNull Statement statement;
}

@GenerateAstNode(implementing=Statement.class)
class ForTemplate {
	List<Expression> expressionInits;
	VariableDefinition variableDeclaration;
	Expression condition;
	List<Expression> updates;
	@NonNull Statement statement;
	
	@CopyMethod
	static boolean isVariableDeclarationBased(For self) {
		return self.getRawVariableDeclaration() != null && self.rawExpressionInits().isEmpty();
	}
	
	@CopyMethod
	static boolean isStatementExpressionsBased(For self) {
		return self.getRawVariableDeclaration() == null;
	}
}

@GenerateAstNode(implementing=Statement.class)
class ForEachTemplate {
	@NonNull VariableDefinition variable;
	@NonNull Expression iterable;
	@NonNull Statement statement;
}

@GenerateAstNode(implementing=Statement.class)
class IfTemplate {
	@NonNull Expression condition;
	@NonNull Statement statement;
	Statement elseStatement;
}

@GenerateAstNode(implementing=Statement.class)
class SynchronizedTemplate {
	@NonNull Expression lock;
	@NonNull Block body;
}

@GenerateAstNode(implementing=Statement.class)
class TryTemplate {
	@NonNull Block body;
	List<Catch> catches;
	Block finally_;
}

@GenerateAstNode(implementing=DescribedNode.class)
class AnnotationTemplate {
	@NonNull TypeReference annotationTypeReference;
	List<AnnotationElement> elements;
	
	@CopyMethod
	static String getDescription(Annotation self) {
		try {
			return self.getAnnotationTypeReference().getDescription();
		} catch (Exception e) {
			return null;
		}
	}
	
	@CopyMethod
	static List<Node> getValueValues(Annotation self) {
		List<Node> result = getValues(self, null);
		return result.isEmpty() ? getValues(self, "value") : result;
	}
	
	@CopyMethod
	static List<Node> getValues(Annotation self, String key) {
		for (AnnotationElement elem : self.elements()) {
			if (key == null && elem.getRawName() == null) return elem.getValues();
			if (elem.getRawName() instanceof Identifier) {
				if (key != null && key.equals(elem.getName().getName())) return elem.getValues();
			}
		}
		
		return Collections.emptyList();
	}
}

@GenerateAstNode(implementing={DescribedNode.class})
class AnnotationElementTemplate {
	Identifier name;
	@NonNull Expression value;
	
	@CopyMethod
	static String getDescription(AnnotationElement self) {
		try {
			return self.getName().getName();
		} catch (Exception e) {
			return null;
		}
	}
	
	@CopyMethod
	static List<Node> getValues(AnnotationElement self) {
		if (self.getRawValue() == null) return Collections.emptyList();
		if (self.getRawValue() instanceof ArrayInitializer) {
			List<Node> result = new ArrayList<Node>();
			for (Node n : ((ArrayInitializer)self.getRawValue()).rawExpressions()) if (n != null) result.add(n);
			return result;
		}
		return Collections.singletonList(self.getRawValue());
	}
}

@GenerateAstNode
class ModifiersTemplate {
	List<KeywordModifier> keywords;
	List<Annotation> annotations;
	
	/**
	 * Returns the keyword-based modifiers the way {@link java.lang.reflect.Modifiers} works.
	 * Only those keywords that are explicitly in the AST are reported; to also include implicit flags, such
	 * as for example the idea that methods in interfaces are always public and abstract even if not marked as such,
	 * use {@link #getEffectiveModifierFlags(Modifiers)}.
	 */
	@CopyMethod
	static int getExplicitModifierFlags(Modifiers m) {
		int out = 0;
		for (Node n : m.rawKeywords()) {
			if (n instanceof KeywordModifier) {
				out |= ((KeywordModifier)n).asReflectModifiers();
			}
		}
		
		return out;
	}
	
	/**
	 * Returns the keyword-based modifiers the way {@link java.lang.reflect.Modifiers} works. Also sets flags that are implicitly true due to the nature
	 * of the node that the modifiers are attached to (for example, inner interfaces are implicitly static and thus if the Modifiers object is a child of
	 * such a declaration, its static bit will be set. Similarly, method declarations in interfaces are abstract and public whether or not those keywords
	 * have been applied to the node).
	 */
	@CopyMethod
	static int getEffectiveModifierFlags(Modifiers m) {
		int explicit = getExplicitModifierFlags(m);
		int out = explicit;
		Node container = m.getParent();
		Node parent = container == null ? null : container.getParent();
		
		// Interfaces and Enums can only be static by their very nature.
		if (container instanceof TypeDeclaration && !(container instanceof ClassDeclaration)) {
			out |= Modifier.STATIC;
		}
		
		// We consider top-level types as static, because semantically that makes sense.
		if (container instanceof ClassDeclaration && parent instanceof CompilationUnit) {
			out |= Modifier.STATIC;
		}
		
		boolean containerIsInterface = container instanceof InterfaceDeclaration ||
				container instanceof AnnotationDeclaration;
		boolean parentIsInterface = parent instanceof InterfaceDeclaration ||
				parent instanceof AnnotationDeclaration;
		
		// We consider interfaces as abstract, because semantically that makes sense.
		if (containerIsInterface) {
			out |= Modifier.ABSTRACT;
		}
		
		// Types in interfaces are always static.
		if (container instanceof ClassDeclaration && parentIsInterface) {
			out |= Modifier.STATIC;
		}
		
		if (container instanceof MethodDeclaration &&
				parentIsInterface && (explicit & Modifier.STATIC) == 0) {
			
			out |= Modifier.PUBLIC | Modifier.ABSTRACT;
		}
		
		if (container instanceof VariableDeclaration && parentIsInterface) {
			out |= Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC;
		}
		
		return out;
	}
	
	@CopyMethod
	static boolean isPublic(Modifiers m) {
		return 0 != (getEffectiveModifierFlags(m) & java.lang.reflect.Modifier.PUBLIC);
	}
	
	@CopyMethod
	static boolean isProtected(Modifiers m) {
		return 0 != (getEffectiveModifierFlags(m) & java.lang.reflect.Modifier.PROTECTED);
	}
	
	@CopyMethod
	static boolean isPrivate(Modifiers m) {
		return 0 != (getEffectiveModifierFlags(m) & java.lang.reflect.Modifier.PRIVATE);
	}
	
	@CopyMethod
	static boolean isPackagePrivate(Modifiers m) {
		return 0 == (getEffectiveModifierFlags(m) & (
				Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED
				));
	}
	
	@CopyMethod
	static boolean isStatic(Modifiers m) {
		return 0 != (getEffectiveModifierFlags(m) & java.lang.reflect.Modifier.STATIC);
	}
	
	@CopyMethod
	static boolean isFinal(Modifiers m) {
		return 0 != (getEffectiveModifierFlags(m) & java.lang.reflect.Modifier.FINAL);
	}
	
	@CopyMethod
	static boolean isAbstract(Modifiers m) {
		return 0 != (getEffectiveModifierFlags(m) & java.lang.reflect.Modifier.ABSTRACT);
	}
	
	@CopyMethod
	static boolean isEmpty(Modifiers m) {
		return m.rawKeywords().isEmpty() && m.rawAnnotations().isEmpty();
	}
}

@GenerateAstNode(implementing={Statement.class, TypeMember.class, JavadocContainer.class})
class VariableDeclarationTemplate {
	Comment javadoc;
	@NonNull VariableDefinition definition;
}

@GenerateAstNode
class VariableDefinitionTemplate {
	@InitialValue("adopt(new lombok.ast.Modifiers())")
	@NonNull Modifiers modifiers;
	@NonNull TypeReference typeReference;
	List<VariableDefinitionEntry> variables;
	
	@NotChildOfNode
	boolean varargs;
}

@GenerateAstNode
class VariableDefinitionEntryTemplate {
	@NonNull Identifier name;
	@NotChildOfNode int arrayDimensions;
	Expression initializer;
	
	@CopyMethod
	static TypeReference getEffectiveTypeReference(VariableDefinitionEntry self) {
		if (!(self.getParent() instanceof VariableDefinition)) throw new AstException(
				self, "Cannot calculate type reference of a VariableDefinitionEntry without a VariableDefinition as parent");
		
		
		VariableDefinition parent = (VariableDefinition) self.getParent();
		
		TypeReference typeRef = parent.getTypeReference().copy();
		return typeRef.setArrayDimensions(typeRef.getArrayDimensions() + self.getArrayDimensions() + (parent.isVarargs() ? 1 : 0));
	}
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class InlineIfExpressionTemplate {
	@NonNull Expression condition;
	@NonNull Expression ifTrue;
	@NonNull Expression ifFalse;
	
	@CopyMethod
	static boolean needsParentheses(Expression self) {
		try {
			return BinaryExpressionTemplate.needsParentheses(self, BinaryOperator.ASSIGN.pLevel()-1);
		} catch (Throwable ignore) {
			return true;
		}
	}
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class}, mixin=ExpressionMixin.class)
class IdentifierTemplate {
	@NotChildOfNode
	@NonNull String name;
	
	@CopyMethod
	static String getDescription(Identifier self) {
		return self.getName();
	}
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class}, mixin=ExpressionMixin.class)
class BinaryExpressionTemplate {
	@NonNull Expression left;
	@NonNull Expression right;
	@NotChildOfNode(rawFormParser="parseOperator", rawFormGenerator="generateOperator")
	@NonNull BinaryOperator operator;
	
	@CopyMethod
	static String getDescription(BinaryExpression self) {
		try {
			return self.getOperator().getSymbol();
		} catch (Exception e) {
			return self.getRawOperator();
		}
	}
	
	static String generateOperator(BinaryOperator op) {
		return op.getSymbol();
	}
	
	static BinaryOperator parseOperator(String op) {
		if (op == null) throw new IllegalArgumentException("missing operator");
		BinaryOperator result = BinaryOperator.fromSymbol(op.trim());
		if (result == null) throw new IllegalArgumentException("unknown binary operator: " + op.trim());
		return result;
	}
	
	@CopyMethod
	static boolean needsParentheses(Expression self) {
		try {
			return needsParentheses(self, ((BinaryExpression)self).getOperator().pLevel());
		} catch (Throwable ignore) {
			return true;
		}
	}
	
	static boolean needsParentheses(Node self, int pLevel) {
		Node parent = self.getParent();
		
		if (parent instanceof InlineIfExpression) {
			if (!(self instanceof InlineIfExpression)) {
				return pLevel >= BinaryOperator.ASSIGN.pLevel();
			}
			return ((InlineIfExpression)parent).getRawIfFalse() != self;
		}
		
		if (parent instanceof UnaryExpression || parent instanceof Cast || parent instanceof ConstructorInvocation) {
			if (parent instanceof ConstructorInvocation && ((ConstructorInvocation)parent).getRawQualifier() != self) return false;
			final int otherPLevel = 1;
			if (otherPLevel > pLevel) return false;
			if (otherPLevel < pLevel) return true;
			
			boolean otherIsPostfix = false;
			boolean selfIsPostfix = false;
			
			try {
				if (parent instanceof ConstructorInvocation) otherIsPostfix = true;
				else otherIsPostfix = ((UnaryExpression)parent).getOperator().isPostfix();
			} catch (Throwable ignore) {}
			try {
				if (self instanceof ConstructorInvocation) selfIsPostfix = true;
				else selfIsPostfix = ((UnaryExpression)self).getOperator().isPostfix();
			} catch (Throwable ignore) {}
			return (!selfIsPostfix && otherIsPostfix);
		}
		
		if (parent instanceof ConstructorInvocation) return self == ((ConstructorInvocation)parent).getRawQualifier();
		if (parent instanceof MethodInvocation) return self == ((MethodInvocation)parent).getRawOperand();
		if (parent instanceof ArrayAccess) return self == ((ArrayAccess)parent).getRawOperand();
		if (parent instanceof Select) return self == ((Select)parent).getRawOperand();
		if (parent instanceof InstanceOf) return pLevel > BinaryOperator.LESS.pLevel();
		if (parent instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression)parent;
			int otherPLevel;
			try {
				otherPLevel = be.getOperator().pLevel();
			} catch (Throwable ignore) {
				return true;
			}
			if (otherPLevel > pLevel) return false;
			if (otherPLevel < pLevel) return true;
			if (be.getRawLeft() == self) {
				return pLevel == BinaryOperator.ASSIGN.pLevel();
			}
			if (be.getRawRight() == self) {
				return pLevel != BinaryOperator.ASSIGN.pLevel();
			}
			return true;
		}
		
		return false;
	}
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class}, mixin=ExpressionMixin.class)
class UnaryExpressionTemplate {
	@NonNull Expression operand;
	@NotChildOfNode
	@NonNull UnaryOperator operator;
	
	@CopyMethod
	static String getDescription(UnaryExpression self) {
		try {
			return String.format("%s%s%s", self.getOperator().isPostfix() ? "X" : "", self.getOperator().getSymbol(), self.getOperator().isPostfix() ? "" : "X");
		} catch (Exception e) {
			return null;
		}
	}
	
	@CopyMethod
	static boolean needsParentheses(Expression self) {
		try {
			return BinaryExpressionTemplate.needsParentheses(self, 1);
		} catch (Throwable ignore) {
			return true;
		}
	}
}

@GenerateAstNode(implementing=DescribedNode.class)
class TypeVariableTemplate {
	@NonNull Identifier name;
	List<TypeReference> extending;
	
	@CopyMethod
	static String getDescription(TypeVariable self) {
		try {
			return self.getName().getName();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing=DescribedNode.class)
class TypeReferenceTemplate {
	@NotChildOfNode
	@InitialValue("lombok.ast.WildcardKind.NONE")
	@NonNull WildcardKind wildcard;
	
	@NotChildOfNode
	int arrayDimensions;
	
	List<TypeReferencePart> parts;
	
	@CopyMethod
	static String getDescription(TypeReference self) {
		try {
			return self.getTypeName();
		} catch (Exception e) {
			return null;
		}
	}
	
	private static final String PRIMITIVE_NAMES = " int long float double char short byte boolean ";
	
	@CopyMethod
	static boolean isPrimitive(TypeReference t) {
		if (t.getArrayDimensions() > 0 || t.rawParts().size() != 1) return false;
		Node part = t.rawParts().first();
		if (part instanceof TypeReferencePart) {
			String name = ((TypeReferencePart)part).getIdentifier().getName();
			return name.indexOf(' ') == -1 && PRIMITIVE_NAMES.contains(" " + name + " ");
		}
		return false;
	}
	
	@CopyMethod
	static boolean isVoid(TypeReference t) {
		if (t.rawParts().size() != 1) return false;
		Node part = t.rawParts().first();
		if (part instanceof TypeReferencePart) {
			String name = ((TypeReferencePart)part).getIdentifier().getName();
			return "void".equals(name);
		}
		return false;
	}
	
	@CopyMethod
	static String getTypeName(TypeReference t) {
		StringBuilder out = new StringBuilder();
		for (TypeReferencePart p : t.parts()) {
			if (out.length() > 0) out.append(".");
			out.append(p.getTypeName());
		}
		
		for (int i = 0; i < t.getArrayDimensions(); i++) out.append("[]");
		
		return out.toString();
	}
	
	@CopyMethod
	static boolean hasGenerics(TypeReference t) {
		return generics(t).isEmpty();
	}
	
	@CopyMethod
	static StrictListAccessor<TypeReference, TypeReference> generics(TypeReference t) {
		return t.parts().last().getTypeArguments().genericsAccessor.wrap(t).asStrict();
	}
}

@GenerateAstNode
class TypeReferencePartTemplate {
	@NonNull Identifier identifier;
	@InitialValue("adopt(new lombok.ast.TypeArguments())")
	@NonNull TypeArguments typeArguments;
	
	@CopyMethod
	static StrictListAccessor<TypeReference, TypeReferencePart> generics(TypeReferencePart self) {
		return self.getTypeArguments().genericsAccessor.wrap(self).asStrict();
	}
	
	@CopyMethod
	static String getTypeName(TypeReferencePart p) {
		if (p.generics().isEmpty()) return p.getIdentifier().getName();
		
		StringBuilder out = new StringBuilder();
		out.append(p.getIdentifier().getName()).append("<");
		boolean first = true;
		for (TypeReference t : p.generics()) {
			if (!first) out.append(", ");
			first = false;
			switch (t.getWildcard()) {
			case EXTENDS:
				out.append("? extends ");
				out.append(t.getTypeName());
				break;
			case SUPER:
				out.append("? super ");
				out.append(t.getTypeName());
				break;
			default:
			case NONE:
				out.append(t.getTypeName());
				break;
			case UNBOUND:
				out.append("?");
				break;
			}
		}
		return out.append(">").toString();
	}
}

@GenerateAstNode
class TypeArgumentsTemplate {
	List<TypeReference> generics;
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class CastTemplate {
	@NonNull TypeReference typeReference;
	@NonNull Expression operand;
	
	@CopyMethod
	static boolean needsParentheses(Expression self) {
		try {
			return BinaryExpressionTemplate.needsParentheses(self, 1);
		} catch (Throwable ignore) {
			return true;
		}
	}
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class InstanceOfTemplate {
	@NonNull Expression objectReference;
	@NonNull TypeReference typeReference;
	
	@CopyMethod
	static boolean needsParentheses(Expression self) {
		try {
			return BinaryExpressionTemplate.needsParentheses(self, BinaryOperator.LESS.pLevel());
		} catch (Throwable ignore) {
			return true;
		}
	}
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class}, mixin=ExpressionMixin.class)
class ConstructorInvocationTemplate {
	Expression qualifier;
	TypeArguments constructorTypeArguments;
	@NonNull TypeReference typeReference;
	List<Expression> arguments;
	TypeBody anonymousClassBody;
	
	@CopyMethod
	static String getDescription(ConstructorInvocation self) {
		try {
			return self.getTypeReference().getDescription();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing=Statement.class)
class AlternateConstructorInvocationTemplate {
	TypeArguments constructorTypeArguments;
	List<Expression> arguments;
}

@GenerateAstNode(implementing=Statement.class)
class SuperConstructorInvocationTemplate {
	Expression qualifier;
	TypeArguments constructorTypeArguments;
	List<Expression> arguments;
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class}, mixin=ExpressionMixin.class)
class MethodInvocationTemplate {
	Expression operand;
	TypeArguments methodTypeArguments;
	@NonNull Identifier name;
	List<Expression> arguments;
	
	@CopyMethod
	static String getDescription(MethodInvocation self) {
		try {
			return self.getName().getName();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class SelectTemplate {
	@NonNull Expression operand;
	@NonNull Identifier identifier;
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class ArrayAccessTemplate {
	@NonNull Expression operand;
	@NonNull Expression indexExpression;
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class ArrayCreationTemplate {
	@NonNull TypeReference componentTypeReference;
	List<ArrayDimension> dimensions;
	ArrayInitializer initializer;
}

@GenerateAstNode
class ArrayDimensionTemplate {
	Expression dimension;
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class ArrayInitializerTemplate {
	List<Expression> expressions;
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class ThisTemplate {
	TypeReference qualifier;
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class SuperTemplate {
	TypeReference qualifier;
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class}, mixin=ExpressionMixin.class)
class ClassLiteralTemplate {
	@NonNull TypeReference typeReference;
	
	@CopyMethod
	static String getDescription(ClassLiteral self) {
		try {
			return self.getTypeReference().getDescription();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing=DescribedNode.class)
class KeywordModifierTemplate {
	private static final Map<String, Integer> REFLECT_MODIFIERS;
	static {
		Map<String, Integer> reflectModifiers = new HashMap<String, Integer>();
		reflectModifiers.put("public", Modifier.PUBLIC);
		reflectModifiers.put("private", Modifier.PRIVATE);
		reflectModifiers.put("protected", Modifier.PROTECTED);
		reflectModifiers.put("static", Modifier.STATIC);
		reflectModifiers.put("final", Modifier.FINAL);
		reflectModifiers.put("synchronized", Modifier.SYNCHRONIZED);
		reflectModifiers.put("volatile", Modifier.VOLATILE);
		reflectModifiers.put("transient", Modifier.TRANSIENT);
		reflectModifiers.put("native", Modifier.NATIVE);
		reflectModifiers.put("interface", Modifier.INTERFACE);
		reflectModifiers.put("abstract", Modifier.ABSTRACT);
		reflectModifiers.put("strictfp", Modifier.STRICT);
		REFLECT_MODIFIERS = reflectModifiers;
	}
	
	@NotChildOfNode
	@NonNull String name;
	
	@CopyMethod
	static String getDescription(KeywordModifier self) {
		return self.getName();
	}
	
	@CopyMethod
	static int asReflectModifiers(KeywordModifier self) {
		Integer value = REFLECT_MODIFIERS.get(self.getName());
		return value == null ? 0 : value;
	}
}

@GenerateAstNode(implementing=Statement.class)
class EmptyStatementTemplate {}

@GenerateAstNode(implementing={Statement.class, DescribedNode.class})
class LabelledStatementTemplate {
	@NonNull Identifier label;
	@NonNull Statement statement;
	
	@CopyMethod
	static String getDescription(LabelledStatement self) {
		try {
			return self.getLabel().getName();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing=Statement.class)
class SwitchTemplate {
	@NonNull Expression condition;
	@NonNull Block body;
}

@GenerateAstNode(implementing=Statement.class)
class CaseTemplate {
	@NonNull Expression condition;
}

@GenerateAstNode(implementing=Statement.class)
class DefaultTemplate {
}

@GenerateAstNode(implementing={Literal.class, Expression.class}, mixin=ExpressionMixin.class)
class BooleanLiteralTemplate {
	@NotChildOfNode(rawFormParser="parseBoolean", rawFormGenerator="generateBoolean")
	@NonNull Boolean value;
	
	static String generateBoolean(Boolean bool) {
		return String.valueOf(bool);
	}
	
	static Boolean parseBoolean(String bool) {
		if (bool == null) throw new IllegalArgumentException("missing boolean");
		bool = bool.trim();
		if (bool.equals("true")) return true;
		if (bool.equals("false")) return false;
		throw new IllegalArgumentException("invalid boolean literal:" + bool); 
	}
}

@GenerateAstNode(implementing={Expression.class, Literal.class, DescribedNode.class}, mixin=ExpressionMixin.class)
class CharLiteralTemplate {
	@NotChildOfNode(rawFormParser="parseChar", rawFormGenerator="generateChar")
	@NonNull Character value;
	
	@CopyMethod
	static String getDescription(CharLiteral self) {
		return self.getValue() != null ? String.valueOf(self.getValue()) : null;
	}
	
	static String toEscape(char c, boolean forCharLiteral, char next) {
		if (c == '\'') return forCharLiteral ? "\\'" : "'";
		if (c == '"') return forCharLiteral ? "\"" : "\\\"";
		if (c == '\b') return "\\b";
		if (c == '\t') return "\\t";
		if (c == '\n') return "\\n";
		if (c == '\f') return "\\f";
		if (c == '\r') return "\\r";
		if (c == '\\') return "\\\\";
		if (c < 0x20 || c == 127) {
			String octalEscape = Integer.toString(c, 010);
			boolean fill = (next >= '0' && next <= '7') && octalEscape.length() < 3;
			while (fill && octalEscape.length() < 3) octalEscape = "0" + octalEscape;
			return "\\" + octalEscape;
		}
		return "" + c;
	}
	
	static char fromEscape(char x) {
		if (x == 'b') return '\b';
		if (x == 't') return '\t';
		if (x == 'n') return '\n';
		if (x == 'f') return '\f';
		if (x == 'r') return '\r';
		if (x == '\'') return '\'';
		if (x == '"') return '"';
		if (x == '\\') return '\\';
		return 0;
	}
	
	static String generateChar(Character c) {
		return "'" + toEscape(c, true, 'a') + "'";
	}
	
	static Character parseChar(String raw) {
		if (raw == null) throw new IllegalArgumentException("missing character literal");
		String v = raw.trim();
		
		if (!v.startsWith("'") || !v.endsWith("'")) throw new IllegalArgumentException(
				"Character literals should be enclosed in single quotes: " + v);
		
		String content = v.substring(1, v.length()-1);
		if (content.length() == 0) throw new IllegalArgumentException(
				"Empty character literal not allowed");
		
		if (content.charAt(0) == '\\') {
			if (content.length() == 1) throw new IllegalArgumentException("Incomplete backslash escape: '\\'");
			char x = content.charAt(1);
			char fromEscape = fromEscape(x);
			if (fromEscape != 0 && content.length() == 2) return fromEscape;
			if (x >= '0' && x <= '7') {
				try {
					int possible = Integer.parseInt(content.substring(1), 010);
					if (possible <= 0377) return (char)possible;
				} catch (NumberFormatException e) {
					//fallthrough
				}
			}
			
			throw new IllegalArgumentException("Not a valid character literal: " + v);
		}
		
		if (content.length() == 1) {
			char x = content.charAt(0);
			if (x == '\'' || x == '\n' || x == '\r') {
				throw new IllegalArgumentException("Not a valid character literal: " + v);
			} else {
				return x;
			}
		}
		
		throw new IllegalArgumentException("Not a valid character literal: " + v);
	}
}

@GenerateAstNode(implementing={Literal.class, Expression.class, DescribedNode.class}, mixin=ExpressionMixin.class)
class StringLiteralTemplate {
	@NotChildOfNode(rawFormParser="parseString", rawFormGenerator="generateString")
	@NonNull String value;
	
	@CopyMethod
	static String getDescription(StringLiteral self) {
		if (self.getValue() == null) return null;
		String v = self.getValue();
		if (v.length() > 17) return v.substring(0, 8) + "\u2026" + v.substring(v.length() - 8);
		return v;
	}
	
	static String generateString(String literal) {
		StringBuilder raw = new StringBuilder().append('"');
		char[] cs = literal.toCharArray();
		for (int i = 0; i < cs.length; i++) {
			char c = cs[i];
			char next = (i < cs.length-1) ? cs[i+1] : 'a';
			raw.append(CharLiteralTemplate.toEscape(c, false, next));
		}
		return raw.append('"').toString();
	}
	
	static String parseString(String raw) {
		if (raw == null) throw new IllegalArgumentException("missing string literal");
		String v = raw.trim();
		
		if (!v.startsWith("\"") || !v.endsWith("\"")) throw new IllegalArgumentException(
				"String literals should be enclosed in double quotes: " + v);
		
		String content = v.substring(1, v.length()-1);
		char[] cs = content.toCharArray();
		StringBuilder value = new StringBuilder();
		
		for (int i = 0; i < cs.length; i++) {
			if (cs[i] == '\n' || cs[i] == '\r') {
				throw new IllegalArgumentException("newlines not allowed in string literal: " + v);
			}
			
			if (cs[i] == '"') {
				throw new IllegalArgumentException("unescaped double quotes not allowed in string literal: " + v);
			}
			
			if (cs[i] == '\\') {
				if (i == v.length() -1) {
					throw new IllegalArgumentException("Incomplete backslash escape: " + v);
				}
				char x = cs[++i];
				char fromEscape = CharLiteralTemplate.fromEscape(x);
				if (fromEscape != 0) {
					value.append(fromEscape);
					continue;
				}
				
				if (x >= '0' && x <= '7') {
					char first = x;
					char second = (i < cs.length -1) ? cs[i+1] : 'a';
					char third = (i < cs.length -2) ? cs[i+2] : 'a';
					
					boolean secondFits = second >= '0' && second <= '7';
					boolean thirdFits = third >= '0' && third <= '7';
					
					if (first > '3') {
						if (secondFits) {
							i++;
							value.append((char)((first - '0') * 010 + (second - '0')));
							continue;
						}
						value.append((char)(first - '0'));
						continue;
					}
					
					if (secondFits && thirdFits) {
						i += 2;
						value.append((char)((first - '0') * 0100 + (second - '0') * 010 + (third - '0')));
						continue;
					}
					
					if (secondFits) {
						i++;
						value.append((char)((first - '0') * 010 + (second - '0')));
						continue;
					}
					
					value.append((char)(first - '0'));
					continue;
				}
				
				throw new IllegalArgumentException("Invalid string literal (invalid backslash escape): " + v);
			}
			
			value.append(cs[i]);
		}
		
		return value.toString();
	}
}

@GenerateAstNode(implementing=Statement.class)
class BreakTemplate {
	Identifier label;
	
	@CopyMethod
	static boolean hasLabel(Break self) {
		return self.getRawLabel() != null;
	}
}

@GenerateAstNode(implementing=Statement.class)
class ContinueTemplate {
	Identifier label;
	
	@CopyMethod
	static boolean hasLabel(Continue self) {
		return self.getRawLabel() != null;
	}
}

@GenerateAstNode(implementing=Statement.class)
class ReturnTemplate {
	Expression value;
}

@GenerateAstNode(implementing=Statement.class)
class ThrowTemplate {
	@NonNull Expression throwable;
}

@GenerateAstNode
class CommentTemplate {
	private static final Pattern DEPRECATED_DETECTOR = Pattern.compile("^(?:.*(?:[*{}]|\\s))?@deprecated(?:(?:[*{}]|\\s).*)?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	@NotChildOfNode
	boolean blockComment;
	
	@NotChildOfNode
	String content;
	
	@CopyMethod
	static boolean isJavadoc(Comment self) {
		return self.isBlockComment() && self.getContent().startsWith("*");
	}
	
	@CopyMethod
	static boolean isMarkedDeprecated(Comment self) {
		return isJavadoc(self) && DEPRECATED_DETECTOR.matcher(self.getContent()).matches();
	}
}

@GenerateAstNode(implementing={TypeMember.class, DescribedNode.class, JavadocContainer.class})
class AnnotationMethodDeclarationTemplate {
	Comment javadoc;
	
	@InitialValue("adopt(new lombok.ast.Modifiers())")
	@NonNull Modifiers modifiers;
	
	@NonNull TypeReference returnTypeReference;
	@NonNull Identifier methodName;
	Expression defaultValue;
	
	@CopyMethod
	static String getDescription(AnnotationMethodDeclaration self) {
		try {
			return self.getMethodName().getName();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing={TypeMember.class, DescribedNode.class, JavadocContainer.class})
class MethodDeclarationTemplate {
	Comment javadoc;
	
	@InitialValue("adopt(new lombok.ast.Modifiers())")
	@NonNull Modifiers modifiers;
	
	List<TypeVariable> typeVariables;
	@NonNull TypeReference returnTypeReference;
	@NonNull Identifier methodName;
	List<VariableDefinition> parameters;
	List<TypeReference> thrownTypeReferences;
	Block body;
	
	@CopyMethod
	static String getDescription(MethodDeclaration self) {
		try {
			return self.getMethodName().getName();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing={TypeMember.class, JavadocContainer.class})
class ConstructorDeclarationTemplate {
	Comment javadoc;
	
	@InitialValue("adopt(new lombok.ast.Modifiers())")
	@NonNull Modifiers modifiers;
	
	List<TypeVariable> typeVariables;
	@NonNull Identifier typeName;
	List<VariableDefinition> parameters;
	List<TypeReference> thrownTypeReferences;
	@NonNull Block body;
	
	//TODO test if our syntax checkers flag misnamed constructors.
}

@GenerateAstNode(implementing=TypeMember.class)
class InstanceInitializerTemplate {
	@NonNull Block body;
}

@GenerateAstNode(implementing=TypeMember.class)
class StaticInitializerTemplate {
	@NonNull Block body;
}

@GenerateAstNode
class TypeBodyTemplate {
	List<TypeMember> members;
}

@GenerateAstNode
class EnumTypeBodyTemplate {
	List<TypeMember> members;
	List<EnumConstant> constants;
}

@GenerateAstNode(implementing={TypeMember.class, TypeDeclaration.class, JavadocContainer.class})
class AnnotationDeclarationTemplate {
	Comment javadoc;
	
	@InitialValue("adopt(new lombok.ast.Modifiers())")
	@NonNull Modifiers modifiers;
	
	@NonNull Identifier name;
	@NonNull TypeBody body;
	
	@CopyMethod
	static String getDescription(AnnotationDeclaration self) {
		try {
			return self.getName().getName();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing={TypeMember.class, TypeDeclaration.class})
class EmptyDeclarationTemplate {
	@InitialValue("adopt(new lombok.ast.Modifiers())")
	@NonNull Modifiers modifiers;

	@CopyMethod
	static String getDescription(EmptyDeclaration self) {
		try {
			return ";";
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing={TypeMember.class, Statement.class, TypeDeclaration.class, JavadocContainer.class})
class ClassDeclarationTemplate {
	Comment javadoc;
	
	@InitialValue("adopt(new lombok.ast.Modifiers())")
	@NonNull Modifiers modifiers;
	
	@NonNull Identifier name;
	@NonNull TypeBody body;
	List<TypeVariable> typeVariables;
	TypeReference extending;
	List<TypeReference> implementing;
	
	@CopyMethod
	static String getDescription(ClassDeclaration self) {
		try {
			return self.getName().getName();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing={TypeMember.class, TypeDeclaration.class, JavadocContainer.class})
class InterfaceDeclarationTemplate {
	Comment javadoc;
	
	@InitialValue("adopt(new lombok.ast.Modifiers())")
	@NonNull Modifiers modifiers;
	
	@NonNull Identifier name;
	@NonNull TypeBody body;
	List<TypeVariable> typeVariables;
	List<TypeReference> extending;
	
	@CopyMethod
	static String getDescription(InterfaceDeclaration self) {
		try {
			return self.getName().getName();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing={TypeMember.class, DescribedNode.class, JavadocContainer.class})
class EnumConstantTemplate {
	Comment javadoc;
	TypeBody body;
	@NonNull Identifier name;
	List<Annotation> annotations;
	List<Expression> arguments;
	
	@CopyMethod
	static String getDescription(EnumConstant self) {
		try {
			return self.getName().getName();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing={TypeMember.class, TypeDeclaration.class, JavadocContainer.class})
class EnumDeclarationTemplate {
	Comment javadoc;
	
	@InitialValue("adopt(new lombok.ast.Modifiers())")
	@NonNull Modifiers modifiers;
	
	@NonNull Identifier name;
	@NonNull EnumTypeBody body;
	List<TypeReference> implementing;
	
	@CopyMethod
	static String getDescription(EnumDeclaration self) {
		try {
			return self.getName().getName();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode
class PackageDeclarationTemplate {
	List<Annotation> annotations;
	List<Identifier> parts;
	
	@CopyMethod
	static String getPackageName(PackageDeclaration node) {
		StringBuilder result = new StringBuilder();
		for (Identifier part : node.parts()) {
			if (result.length() != 0) {
				result.append(".");
			}
			result.append(part.getName());
		}
		return result.toString();
	}
}

@GenerateAstNode
class ImportDeclarationTemplate {
	@NotChildOfNode
	boolean staticImport;
	
	List<Identifier> parts;
	
	@NotChildOfNode
	boolean starImport;
}

@GenerateAstNode
class CompilationUnitTemplate {
	PackageDeclaration packageDeclaration;
	List<ImportDeclaration> importDeclarations;
	List<TypeDeclaration> typeDeclarations;
}

@GenerateAstNode(implementing=Statement.class)
class ExpressionStatementTemplate {
	@NonNull Expression expression;
}

