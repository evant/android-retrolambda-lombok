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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.ast.template.CopyMethod;
import lombok.ast.template.ForcedType;
import lombok.ast.template.GenerateAstNode;
import lombok.ast.template.Mandatory;
import lombok.ast.template.NotChildOfNode;
import lombok.ast.template.ParentAccessor;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

class TypeMemberMixin {
	@CopyMethod
	static TypeDeclaration upUpToTypeDeclaration(TypeMember self) {
		TypeBody body = self.upToTypeBody();
		return body == null ? null : body.upToTypeDeclaration();
	}
}

class ExpressionMixin {
	@NotChildOfNode(suppressSetter=true, codeToCopy="new java.util.ArrayList<lombok.ast.Position>(this.parensPositions)")
	@Mandatory("new java.util.ArrayList<lombok.ast.Position>()")
	List<Position> parensPositions;
	
	@CopyMethod
	static int getParens(Expression self) {
		return self.astParensPositions() == null ? 0 : self.astParensPositions().size();
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
				return ((BinaryExpression)self).astOperator().isAssignment();
			} catch (Exception e) {
				return false;
			}
		}
		if (self instanceof UnaryExpression) {
			try {
				switch (((UnaryExpression)self).astOperator()) {
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
	@Mandatory Expression assertion;
	Expression message;
}

@GenerateAstNode(implementing=DescribedNode.class)
class CatchTemplate {
	@ParentAccessor @Mandatory VariableDefinition exceptionDeclaration;
	@ParentAccessor @Mandatory Block body;
	
	@CopyMethod
	static String getDescription(Catch self) {
		try {
			return self.astExceptionDeclaration().astTypeReference().getDescription();
		} catch (NullPointerException e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing=Statement.class)
class BlockTemplate {
	@ParentAccessor List<Statement> contents;
}

@GenerateAstNode(implementing=Statement.class)
class DoWhileTemplate {
	@Mandatory Expression condition;
	@Mandatory Statement statement;
}

@GenerateAstNode(implementing=Statement.class)
class WhileTemplate {
	@Mandatory Expression condition;
	@Mandatory Statement statement;
}

@GenerateAstNode(implementing=Statement.class)
class ForTemplate {
	List<Expression> expressionInits;
	@ParentAccessor VariableDefinition variableDeclaration;
	Expression condition;
	List<Expression> updates;
	@Mandatory Statement statement;
	
	@CopyMethod
	static boolean isVariableDeclarationBased(For self) {
		return self.rawVariableDeclaration() != null && self.rawExpressionInits().isEmpty();
	}
	
	@CopyMethod
	static boolean isStatementExpressionsBased(For self) {
		return self.rawVariableDeclaration() == null;
	}
}

@GenerateAstNode(implementing=Statement.class)
class ForEachTemplate {
	@ParentAccessor @Mandatory VariableDefinition variable;
	@Mandatory Expression iterable;
	@Mandatory Statement statement;
}

@GenerateAstNode(implementing=Statement.class)
class IfTemplate {
	@Mandatory Expression condition;
	@Mandatory Statement statement;
	Statement elseStatement;
}

@GenerateAstNode(implementing=Statement.class)
class SynchronizedTemplate {
	@Mandatory Expression lock;
	@ParentAccessor @Mandatory Block body;
}

@GenerateAstNode(implementing=Statement.class)
class TryTemplate {
	@ParentAccessor("TryBody") @Mandatory Block body;
	@ParentAccessor List<Catch> catches;
	@ParentAccessor("Finally") Block finally_;
}

@GenerateAstNode(implementing={DescribedNode.class, AnnotationValue.class})
class AnnotationTemplate {
	@Mandatory TypeReference annotationTypeReference;
	@ParentAccessor List<AnnotationElement> elements;
	
	@CopyMethod
	static String getDescription(Annotation self) {
		try {
			return self.astAnnotationTypeReference().getDescription();
		} catch (NullPointerException e) {
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
		for (AnnotationElement elem : self.astElements()) {
			if (key == null && elem.astName() == null) return elem.getValues();
			if (key != null && elem.astName() != null && elem.astName().astValue().equals(key)) return elem.getValues();
		}
		
		return ImmutableList.of();
	}
}

// TODO add unit test to see if annotations-in-annotations actually works.

@GenerateAstNode(implementing={DescribedNode.class})
class AnnotationElementTemplate {
	@ForcedType Identifier name;
	@Mandatory AnnotationValue value;
	
	@CopyMethod
	static String getDescription(AnnotationElement self) {
		try {
			return self.astName().astValue();
		} catch (NullPointerException e) {
			return null;
		}
	}
	
	@CopyMethod
	static List<Node> getValues(AnnotationElement self) {
		if (self.rawValue() == null) return ImmutableList.of();
		if (self.rawValue() instanceof AnnotationValueArray) {
			ImmutableList.Builder<Node> result = ImmutableList.builder();
			for (Node n : ((AnnotationValueArray)self.rawValue()).rawValues()) result.add(n);
			return result.build();
		}
		return ImmutableList.of(self.rawValue());
	}
}

@GenerateAstNode(implementing=AnnotationValue.class)
class AnnotationValueArrayTemplate {
	List<AnnotationValue> values;
}

@GenerateAstNode
class ModifiersTemplate {
	@ParentAccessor List<KeywordModifier> keywords;
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
		for (KeywordModifier n : m.astKeywords()) out |= n.asReflectModifiers();
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
		Node declaration = m.getParent();
		
		// Interfaces and Enums can only be static by their very nature.
		if (declaration instanceof TypeDeclaration && !(declaration instanceof ClassDeclaration)) {
			out |= Modifier.STATIC;
		}
		
		// We consider top-level types as static, because semantically that makes sense.
		if (declaration instanceof ClassDeclaration && ((ClassDeclaration)declaration).upIfTopLevelToCompilationUnit() != null) {
			out |= Modifier.STATIC;
		}
		
		// We consider interfaces as abstract, because semantically that makes sense.
		if (declaration instanceof TypeDeclaration && ((TypeDeclaration)declaration).isInterface()) {
			out |= Modifier.ABSTRACT;
		}
		
		// Types in interfaces are always static.
		if (declaration instanceof ClassDeclaration) {
			TypeDeclaration container = ((ClassDeclaration)declaration).upUpToTypeDeclaration();
			if (container != null && container.isInterface()) out |= Modifier.STATIC;
		}
		
		if (declaration instanceof MethodDeclaration) {
			TypeDeclaration container = ((MethodDeclaration)declaration).upUpToTypeDeclaration();
			if (container != null && container.isInterface() && (explicit & Modifier.STATIC) == 0) {
				out |= Modifier.PUBLIC | Modifier.ABSTRACT;
			}
		}
		
		if (declaration instanceof VariableDeclaration) {
			TypeDeclaration container = ((VariableDeclaration)declaration).upUpToTypeDeclaration();
			if (container != null && container.isInterface()) {
				out |= Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC;
			}
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

@GenerateAstNode(implementing={Statement.class, TypeMember.class, JavadocContainer.class}, mixin=TypeMemberMixin.class)
class VariableDeclarationTemplate {
	Comment javadoc;
	@ParentAccessor @Mandatory VariableDefinition definition;
}

// TODO add adopt() to non-NCONs.

@GenerateAstNode
class VariableDefinitionTemplate {
	@Mandatory("new lombok.ast.Modifiers()") @ForcedType Modifiers modifiers;
	@Mandatory TypeReference typeReference;
	@ParentAccessor List<VariableDefinitionEntry> variables;
	
	@NotChildOfNode
	boolean varargs;
	
	@CopyMethod
	static TypeDeclaration upUpIfFieldToTypeDeclaration(VariableDefinition self) {
		VariableDeclaration decl = self.upToVariableDeclaration();
		return decl == null ? null : decl.upUpToTypeDeclaration();
	}
	
	@CopyMethod
	static Block upUpIfLocalVariableToBlock(VariableDefinition self) {
		VariableDeclaration decl = self.upToVariableDeclaration();
		return decl == null ? null : decl.upToBlock();
	}
}

@GenerateAstNode
class VariableDefinitionEntryTemplate {
	@ForcedType @Mandatory("new lombok.ast.Identifier()") Identifier name;
	@NotChildOfNode int arrayDimensions;
	Expression initializer;
	
	@CopyMethod
	static TypeReference getEffectiveTypeReference(VariableDefinitionEntry self) {
		VariableDefinition parent = self.upToVariableDefinition();
		if (parent == null) throw new AstException(
				self, "Cannot calculate type reference of a VariableDefinitionEntry without a VariableDefinition as parent");
		
		TypeReference typeRef = parent.astTypeReference().copy();
		return typeRef.astArrayDimensions(typeRef.astArrayDimensions() + self.astArrayDimensions() + (parent.astVarargs() ? 1 : 0));
	}
	
	@CopyMethod
	static Modifiers getModifiersOfParent(VariableDefinitionEntry self) {
		VariableDefinition parent = self.upToVariableDefinition();
		return parent == null ? new Modifiers() : parent.astModifiers();
	}
	
	@CopyMethod
	static TypeDeclaration upUpIfFieldToTypeDeclaration(VariableDefinitionEntry self) {
		VariableDefinition def = self.upToVariableDefinition();
		if (def == null) return null;
		VariableDeclaration decl = def.upToVariableDeclaration();
		return decl == null ? null : decl.upUpToTypeDeclaration();
	}
	
	@CopyMethod
	static Block upUpIfLocalVariableToBlock(VariableDefinitionEntry self) {
		VariableDefinition def = self.upToVariableDefinition();
		if (def == null) return null;
		VariableDeclaration decl = def.upToVariableDeclaration();
		return decl == null ? null : decl.upToBlock();
	}
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class InlineIfExpressionTemplate {
	@Mandatory Expression condition;
	@Mandatory Expression ifTrue;
	@Mandatory Expression ifFalse;
	
	@CopyMethod
	static boolean needsParentheses(Expression self) {
		try {
			return BinaryExpressionTemplate.needsParentheses(self, BinaryOperator.ASSIGN.pLevel()-1);
		} catch (Throwable ignore) {
			return true;
		}
	}
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class VariableReferenceTemplate {
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier identifier;
}

@GenerateAstNode(implementing=DescribedNode.class)
class IdentifierTemplate {
	@NotChildOfNode
	@Mandatory("\"\"") String value;
	
	@CopyMethod
	static String getDescription(Identifier self) {
		return self.astValue();
	}
	
	@CopyMethod(isStatic=true)
	static Identifier of(String value) {
		return new Identifier().astValue(value);
	}
}

// TODO If @InitialValue and not @Mandatory, generate a warning.
// TODO @InitialValue also means setter needs: this.fieldName = fieldName == null ? adopt("initialValueExpr") : adopt(fieldName); - and update ID
// TODO update ExpressionsParser to return VR instead of Identifier for idents.

@GenerateAstNode(implementing={Expression.class, DescribedNode.class}, mixin=ExpressionMixin.class)
class BinaryExpressionTemplate {
	@Mandatory Expression left;
	@Mandatory Expression right;
	@NotChildOfNode(rawFormParser="parseOperator", rawFormGenerator="generateOperator")
	@Mandatory BinaryOperator operator;
	
	@CopyMethod
	static String getDescription(BinaryExpression self) {
		try {
			return self.astOperator().getSymbol();
		} catch (Exception e) {
			return self.rawOperator();
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
			return needsParentheses(self, ((BinaryExpression)self).astOperator().pLevel());
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
			return ((InlineIfExpression)parent).rawIfFalse() != self;
		}
		
		if (parent instanceof UnaryExpression || parent instanceof Cast || parent instanceof ConstructorInvocation) {
			//If we're the child of a ConstructorInvocation, are we its parameter (definitely no parens needed) or its qualifier?
			if (parent instanceof ConstructorInvocation && ((ConstructorInvocation)parent).rawQualifier() != self) return false;
			final int otherPLevel = 1;
			if (otherPLevel > pLevel) return false;
			if (otherPLevel < pLevel) return true;
			
			boolean otherIsPostfix = false;
			boolean selfIsPostfix = false;
			
			try {
				if (parent instanceof ConstructorInvocation) otherIsPostfix = true;
				else otherIsPostfix = ((UnaryExpression)parent).astOperator().isPostfix();
			} catch (Throwable ignore) {}
			try {
				if (self instanceof ConstructorInvocation) selfIsPostfix = true;
				else selfIsPostfix = ((UnaryExpression)self).astOperator().isPostfix();
			} catch (Throwable ignore) {}
			return (!selfIsPostfix && otherIsPostfix);
		}
		
		if (parent instanceof ConstructorInvocation) return self == ((ConstructorInvocation)parent).rawQualifier();
		if (parent instanceof MethodInvocation) return self == ((MethodInvocation)parent).rawOperand();
		if (parent instanceof ArrayAccess) return self == ((ArrayAccess)parent).rawOperand();
		if (parent instanceof Select) return self == ((Select)parent).rawOperand();
		if (parent instanceof InstanceOf) return pLevel > BinaryOperator.LESS.pLevel();
		if (parent instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression)parent;
			int otherPLevel;
			try {
				otherPLevel = be.astOperator().pLevel();
			} catch (Throwable ignore) {
				return true;
			}
			if (otherPLevel > pLevel) return false;
			if (otherPLevel < pLevel) return true;
			if (be.rawLeft() == self) {
				return pLevel == BinaryOperator.ASSIGN.pLevel();
			}
			if (be.rawRight() == self) {
				return pLevel != BinaryOperator.ASSIGN.pLevel();
			}
			return true;
		}
		
		return false;
	}
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class}, mixin=ExpressionMixin.class)
class UnaryExpressionTemplate {
	@Mandatory Expression operand;
	@NotChildOfNode
	@Mandatory UnaryOperator operator;
	
	@CopyMethod
	static String getDescription(UnaryExpression self) {
		try {
			return String.format("%s%s%s", self.astOperator().isPostfix() ? "X" : "", self.astOperator().getSymbol(), self.astOperator().isPostfix() ? "" : "X");
		} catch (NullPointerException e) {
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
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier name;
	@ParentAccessor("TypeVariableBound") List<TypeReference> extending;
	
	@CopyMethod
	static String getDescription(TypeVariable self) {
		return self.astName().astValue();
	}
}

@GenerateAstNode(implementing=DescribedNode.class)
class TypeReferenceTemplate {
	@NotChildOfNode
	@Mandatory("lombok.ast.WildcardKind.NONE") WildcardKind wildcard;
	
	@NotChildOfNode
	int arrayDimensions;
	
	@ParentAccessor List<TypeReferencePart> parts;
	
	@CopyMethod
	static String getDescription(TypeReference self) {
		try {
			return self.getTypeName();
		} catch (Exception e) {
			return null;
		}
	}
	
	private static final String PRIMITIVE_NAMES = " int long float double char short byte boolean ";
	
	@CopyMethod(isStatic = true)
	static TypeReference VOID() {
		return new TypeReference().astParts().addToEnd(new TypeReferencePart().astIdentifier(Identifier.of("void")));
	}
	
	@CopyMethod
	static boolean isPrimitive(TypeReference self) {
		if (self.astArrayDimensions() > 0 || self.rawParts().size() != 1) return false;
		try {
			String name = self.astParts().first().astIdentifier().astValue();
			return name.indexOf(' ') == -1 && PRIMITIVE_NAMES.contains(" " + name + " ");
		} catch (NullPointerException e) {
			return false;
		}
	}
	
	@CopyMethod
	static boolean isVoid(TypeReference self) {
		if (self.rawParts().size() != 1) return false;
		try {
			String name = self.astParts().first().astIdentifier().astValue();
			return name.equals("void");
		} catch (NullPointerException e) {
			return false;
		}
	}
	
	@CopyMethod
	static String getTypeName(TypeReference self) {
		StringBuilder out = new StringBuilder();
		for (TypeReferencePart p : self.astParts()) {
			if (out.length() > 0) out.append(".");
			out.append(p.getTypeName());
		}
		
		for (int i = 0; i < self.astArrayDimensions(); i++) out.append("[]");
		
		return out.toString();
	}
	
	@CopyMethod
	static boolean hasTypeArguments(TypeReference self) {
		return getTypeArguments(self).isEmpty();
	}
	
	@CopyMethod
	static StrictListAccessor<TypeReference, TypeReference> getTypeArguments(TypeReference self) {
		try {
			return self.astParts().last().typeArgumentsAccessor.wrap(self).asStrict();
		} catch (Exception e) {
			return ListAccessor.emptyStrict("typeArguments", self);
		}
	}
}

@GenerateAstNode
class TypeReferencePartTemplate {
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier identifier;
	@ParentAccessor("TypeArgument") List<TypeReference> typeArguments;
	
	@CopyMethod
	static String getTypeName(TypeReferencePart self) {
		String name = self.astIdentifier().astValue();
		
		if (self.astTypeArguments().isEmpty()) return name;
		
		StringBuilder out = new StringBuilder();
		out.append(name).append("<");
		boolean first = true;
		for (TypeReference t : self.astTypeArguments()) {
			if (!first) out.append(", ");
			first = false;
			switch (t.astWildcard()) {
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

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class CastTemplate {
	@Mandatory TypeReference typeReference;
	@Mandatory Expression operand;
	
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
	@Mandatory Expression objectReference;
	@Mandatory TypeReference typeReference;
	
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
	List<TypeReference> constructorTypeArguments;
	@Mandatory TypeReference typeReference;
	List<Expression> arguments;
	@ParentAccessor("AnonymousClass") NormalTypeBody anonymousClassBody;
	
	@CopyMethod
	static String getDescription(ConstructorInvocation self) {
		try {
			return self.astTypeReference().getDescription();
		} catch (NullPointerException e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing=Statement.class)
class AlternateConstructorInvocationTemplate {
	List<TypeReference> constructorTypeArguments;
	List<Expression> arguments;
}

@GenerateAstNode(implementing=Statement.class)
class SuperConstructorInvocationTemplate {
	Expression qualifier;
	List<TypeReference> constructorTypeArguments;
	List<Expression> arguments;
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class}, mixin=ExpressionMixin.class)
class MethodInvocationTemplate {
	Expression operand;
	List<TypeReference> methodTypeArguments;
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier name;
	List<Expression> arguments;
	
	@CopyMethod
	static String getDescription(MethodInvocation self) {
		return self.astName().astValue();
	}
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class SelectTemplate {
	@Mandatory Expression operand;
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier identifier;
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class ArrayAccessTemplate {
	@Mandatory Expression operand;
	@Mandatory Expression indexExpression;
}

@GenerateAstNode(implementing=Expression.class, mixin=ExpressionMixin.class)
class ArrayCreationTemplate {
	@Mandatory TypeReference componentTypeReference;
	@ParentAccessor List<ArrayDimension> dimensions;
	@ParentAccessor ArrayInitializer initializer;
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
	@ParentAccessor @Mandatory TypeReference typeReference;
	
	@CopyMethod
	static String getDescription(ClassLiteral self) {
		try {
			return self.astTypeReference().getDescription();
		} catch (NullPointerException e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing=DescribedNode.class)
class KeywordModifierTemplate {
	private static final BiMap<String, Integer> REFLECT_MODIFIERS = ImmutableBiMap.<String, Integer>builder()
		.put("public", Modifier.PUBLIC)
		.put("private", Modifier.PRIVATE)
		.put("protected", Modifier.PROTECTED)
		.put("static", Modifier.STATIC)
		.put("final", Modifier.FINAL)
		.put("synchronized", Modifier.SYNCHRONIZED)
		.put("volatile", Modifier.VOLATILE)
		.put("transient", Modifier.TRANSIENT)
		.put("native", Modifier.NATIVE)
		.put("abstract", Modifier.ABSTRACT)
		.put("strictfp", Modifier.STRICT)
		.build();
	
	@NotChildOfNode
	@Mandatory("\"\"") String name;
	
	@CopyMethod
	static String getDescription(KeywordModifier self) {
		return self.astName();
	}
	
	@CopyMethod
	static int asReflectModifiers(KeywordModifier self) {
		Integer value = REFLECT_MODIFIERS.get(self.astName());
		return value == null ? 0 : value;
	}
	
	@CopyMethod(isStatic=true)
	static KeywordModifier fromReflectModifier(int modifierFlag) {
		String keyword = REFLECT_MODIFIERS.inverse().get(modifierFlag);
		return keyword == null ? null : new KeywordModifier().astName(keyword);
	}
	
	@CopyMethod(isStatic=true)
	static List<KeywordModifier> fromReflectModifiers(int modifierFlags) {
		List<KeywordModifier> list = Lists.newArrayList();
		for (Map.Entry<Integer, String> entry : REFLECT_MODIFIERS.inverse().entrySet()) {
			if ((modifierFlags & entry.getKey()) != 0) list.add(KeywordModifier.fromReflectModifier(entry.getKey()));
		}
		
		return list;
	}
	
	@CopyMethod(isStatic=true)
	static KeywordModifier STATIC() {
		return new KeywordModifier().astName("static");
	}
	
	@CopyMethod(isStatic=true)
	static KeywordModifier PUBLIC() {
		return new KeywordModifier().astName("public");
	}
	
	@CopyMethod(isStatic=true)
	static KeywordModifier PROTECTED() {
		return new KeywordModifier().astName("protected");
	}
	
	@CopyMethod(isStatic=true)
	static KeywordModifier PRIVATE() {
		return new KeywordModifier().astName("private");
	}
}

@GenerateAstNode(implementing=Statement.class)
class EmptyStatementTemplate {}

@GenerateAstNode(implementing={Statement.class, DescribedNode.class})
class LabelledStatementTemplate {
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier label;
	@Mandatory Statement statement;
	
	@CopyMethod
	static String getDescription(LabelledStatement self) {
		return self.astLabel().astValue();
	}
}

@GenerateAstNode(implementing=Statement.class)
class SwitchTemplate {
	@Mandatory Expression condition;
	@ParentAccessor @Mandatory Block body;
}

@GenerateAstNode(implementing=Statement.class)
class CaseTemplate {
	@Mandatory Expression condition;
}

@GenerateAstNode(implementing=Statement.class)
class DefaultTemplate {
}

@GenerateAstNode(implementing={Literal.class, Expression.class}, mixin=ExpressionMixin.class)
class BooleanLiteralTemplate {
	@NotChildOfNode(rawFormParser="parseBoolean", rawFormGenerator="generateBoolean")
	@Mandatory Boolean value;
	
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
	@Mandatory Character value;
	
	@CopyMethod
	static String getDescription(CharLiteral self) {
		return self.astValue() != null ? String.valueOf(self.astValue()) : null;
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
	@Mandatory String value;
	
	@CopyMethod
	static String getDescription(StringLiteral self) {
		String v = self.astValue();
		if (v == null) return null;
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
	@ForcedType Identifier label;
	
	@CopyMethod
	static boolean hasLabel(Break self) {
		return self.astLabel() != null;
	}
}

@GenerateAstNode(implementing=Statement.class)
class ContinueTemplate {
	@ForcedType Identifier label;
	
	@CopyMethod
	static boolean hasLabel(Continue self) {
		return self.astLabel() != null;
	}
}

@GenerateAstNode(implementing=Statement.class)
class ReturnTemplate {
	Expression value;
}

@GenerateAstNode(implementing=Statement.class)
class ThrowTemplate {
	@Mandatory Expression throwable;
}

@GenerateAstNode
class CommentTemplate {
	private static final Pattern DEPRECATED_DETECTOR = Pattern.compile("^(?:.*(?:[*{}]|\\s))?@deprecated(?:(?:[*{}]|\\s).*)?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	@NotChildOfNode
	boolean blockComment;
	
	@NotChildOfNode
	@Mandatory("\"\"")
	String content;
	
	@CopyMethod
	static boolean isJavadoc(Comment self) {
		return self.astBlockComment() && self.astContent().startsWith("*");
	}
	
	@CopyMethod
	static boolean isMarkedDeprecated(Comment self) {
		return isJavadoc(self) && DEPRECATED_DETECTOR.matcher(self.astContent()).matches();
	}
}

@GenerateAstNode(implementing={TypeMember.class, DescribedNode.class, JavadocContainer.class}, mixin=TypeMemberMixin.class)
class AnnotationMethodDeclarationTemplate {
	Comment javadoc;
	
	@Mandatory("new lombok.ast.Modifiers()") @ForcedType Modifiers modifiers;
	
	@Mandatory TypeReference returnTypeReference;
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier methodName;
	Expression defaultValue;
	
	@CopyMethod
	static String getDescription(AnnotationMethodDeclaration self) {
		return self.astMethodName().astValue();
	}
}

@GenerateAstNode(implementing={TypeMember.class, DescribedNode.class, JavadocContainer.class}, mixin=TypeMemberMixin.class)
class MethodDeclarationTemplate {
	Comment javadoc;
	
	@Mandatory("new lombok.ast.Modifiers()") @ForcedType Modifiers modifiers;
	
	List<TypeVariable> typeVariables;
	@ParentAccessor("ReturnType") @Mandatory TypeReference returnTypeReference;
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier methodName;
	@ParentAccessor("Parameter") List<VariableDefinition> parameters;
	List<TypeReference> thrownTypeReferences;
	@ParentAccessor Block body;
	
	@CopyMethod
	static String getDescription(MethodDeclaration self) {
		return self.astMethodName().astValue();
	}
}

@GenerateAstNode(implementing={TypeMember.class, JavadocContainer.class}, mixin=TypeMemberMixin.class)
class ConstructorDeclarationTemplate {
	Comment javadoc;
	
	@Mandatory("new lombok.ast.Modifiers()") @ForcedType Modifiers modifiers;
	
	List<TypeVariable> typeVariables;
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier typeName;
	@ParentAccessor("Parameter") List<VariableDefinition> parameters;
	List<TypeReference> thrownTypeReferences;
	@ParentAccessor @Mandatory Block body;
	
	//TODO test if our syntax checkers flag misnamed constructors.
}

@GenerateAstNode(implementing=TypeMember.class, mixin=TypeMemberMixin.class)
class InstanceInitializerTemplate {
	@ParentAccessor @Mandatory Block body;
}

@GenerateAstNode(implementing=TypeMember.class, mixin=TypeMemberMixin.class)
class StaticInitializerTemplate {
	@ParentAccessor @Mandatory Block body;
}

@GenerateAstNode(implementing=TypeBody.class)
class NormalTypeBodyTemplate {
	List<TypeMember> members;
}

@GenerateAstNode(implementing=TypeBody.class)
class EnumTypeBodyTemplate {
	@ParentAccessor List<EnumConstant> constants;
	List<TypeMember> members;
	
	@CopyMethod
	static ConstructorInvocation upIfAnonymousClassToConstructorInvocation(EnumTypeBody self) {
		return null;
	}
	
	@CopyMethod
	static EnumConstant upToEnumConstant(EnumTypeBody self) {
		return null;
	}
}

@GenerateAstNode(implementing={TypeMember.class, Statement.class, TypeDeclaration.class, JavadocContainer.class}, mixin=TypeMemberMixin.class)
class AnnotationDeclarationTemplate {
	Comment javadoc;
	
	@Mandatory("new lombok.ast.Modifiers()") @ForcedType Modifiers modifiers;
	
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier name;
	@Mandatory NormalTypeBody body;
	
	@CopyMethod
	static String getDescription(AnnotationDeclaration self) {
		return self.astName().astValue();
	}
	
	@CopyMethod
	static boolean isInterface(AnnotationDeclaration self) {
		return true;
	}
}

@GenerateAstNode(implementing={TypeMember.class, TypeDeclaration.class}, mixin=TypeMemberMixin.class)
class EmptyDeclarationTemplate {
	@CopyMethod
	static String getDescription(EmptyDeclaration self) {
		try {
			return ";";
		} catch (Exception e) {
			return null;
		}
	}
	
	@CopyMethod
	static boolean isInterface(EmptyDeclaration self) {
		return false;
	}
	
	@CopyMethod
	static Modifiers astModifiers(EmptyDeclaration self) {
		return new Modifiers();
	}
	
	@CopyMethod
	static Identifier astName(EmptyDeclaration self) {
		return new Identifier();
	}
	
	@CopyMethod
	static EmptyDeclaration astName(EmptyDeclaration self, Identifier name) {
		return self;
	}
	
	@CopyMethod
	static EmptyDeclaration astModifiers(EmptyDeclaration self, Modifiers modifiers) {
		return self;
	}
	
	@CopyMethod
	static TypeBody astBody(EmptyDeclaration self) {
		return null;
	}
	
	@CopyMethod
	static Node rawBody(EmptyDeclaration self) {
		return null;
	}
	
	@CopyMethod
	static Block upToBlock(EmptyDeclaration self) {
		return null;
	}
}

@GenerateAstNode(implementing={TypeMember.class, Statement.class, TypeDeclaration.class, JavadocContainer.class}, mixin=TypeMemberMixin.class)
class ClassDeclarationTemplate {
	Comment javadoc;
	
	@Mandatory("new lombok.ast.Modifiers()") @ForcedType Modifiers modifiers;
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier name;
	@Mandatory NormalTypeBody body;
	List<TypeVariable> typeVariables;
	TypeReference extending;
	List<TypeReference> implementing;
	
	@CopyMethod
	static String getDescription(ClassDeclaration self) {
		return self.astName().astValue();
	}
	
	@CopyMethod
	static boolean isInterface(ClassDeclaration self) {
		return false;
	}
}

@GenerateAstNode(implementing={TypeMember.class, Statement.class, TypeDeclaration.class, JavadocContainer.class}, mixin=TypeMemberMixin.class)
class InterfaceDeclarationTemplate {
	Comment javadoc;
	
	@Mandatory("new lombok.ast.Modifiers()") @ForcedType Modifiers modifiers;
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier name;
	@Mandatory NormalTypeBody body;
	List<TypeVariable> typeVariables;
	List<TypeReference> extending;
	
	@CopyMethod
	static String getDescription(InterfaceDeclaration self) {
		return self.astName().astValue();
	}
	
	@CopyMethod
	static boolean isInterface(InterfaceDeclaration self) {
		return true;
	}
}

@GenerateAstNode(implementing={TypeMember.class, DescribedNode.class, JavadocContainer.class}, mixin=TypeMemberMixin.class)
class EnumConstantTemplate {
	Comment javadoc;
	@ParentAccessor NormalTypeBody body;
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier name;
	List<Annotation> annotations;
	List<Expression> arguments;
	
	@CopyMethod
	static String getDescription(EnumConstant self) {
		return self.astName().astValue();
	}
}

@GenerateAstNode(implementing={TypeMember.class, Statement.class, TypeDeclaration.class, JavadocContainer.class}, mixin=TypeMemberMixin.class)
class EnumDeclarationTemplate {
	Comment javadoc;
	
	@Mandatory("new lombok.ast.Modifiers()") @ForcedType Modifiers modifiers;
	@Mandatory("new lombok.ast.Identifier()") @ForcedType Identifier name;
	@ParentAccessor @Mandatory EnumTypeBody body;
	List<TypeReference> implementing;
	
	@CopyMethod
	static String getDescription(EnumDeclaration self) {
		return self.astName().astValue();
	}
	
	@CopyMethod
	static boolean isInterface(EnumDeclaration self) {
		return false;
	}
}

@GenerateAstNode
class PackageDeclarationTemplate {
	List<Annotation> annotations;
	List<Identifier> parts;
	
	@CopyMethod
	static String getPackageName(PackageDeclaration node) {
		StringBuilder result = new StringBuilder();
		for (Identifier part : node.astParts()) {
			if (result.length() != 0) result.append(".");
			result.append(part.astValue());
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
	
	@CopyMethod
	static String asFullyQualifiedName(ImportDeclaration self) {
		StringBuilder result = new StringBuilder();
		for (Identifier part : self.astParts()) {
			if (result.length() != 0) result.append(".");
			result.append(part.astValue());
		}
		if (self.astStarImport()) result.append(".*");
		return result.toString();
	}
}

@GenerateAstNode
class CompilationUnitTemplate {
	@ParentAccessor PackageDeclaration packageDeclaration;
	@ParentAccessor List<ImportDeclaration> importDeclarations;
	@ParentAccessor("TopLevel") List<TypeDeclaration> typeDeclarations;
}

@GenerateAstNode(implementing=Statement.class)
class ExpressionStatementTemplate {
	@Mandatory Expression expression;
}
