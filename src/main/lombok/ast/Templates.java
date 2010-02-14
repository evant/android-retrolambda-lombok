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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.NonNull;
import lombok.ast.template.AdditionalCheck;
import lombok.ast.template.CopyMethod;
import lombok.ast.template.GenerateAstNode;
import lombok.ast.template.InitialValue;
import lombok.ast.template.NotChildOfNode;

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
	
	/* check: exDecl must have exactly 1 VDEntry */
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
	List<Statement> inits;
	Expression condition;
	List<Statement> updates;
	@NonNull Statement statement;
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
	
	@AdditionalCheck
	static void checkNotLoneTry(List<SyntaxProblem> problems, Try node) {
		if (node.catches().size() == 0 && node.getRawFinally() == null) {
			problems.add(new SyntaxProblem(node, "try statement with no catches and no finally"));
		}
	}
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class})
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
		for (AnnotationElement elem : self.elements().getContents()) {
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
			for (Node n : ((ArrayInitializer)self.getRawValue()).expressions().getRawContents()) if (n != null) result.add(n);
			return result;
		}
		return Collections.singletonList(self.getRawValue());
	}
}

@GenerateAstNode
class ModifiersTemplate {
	List<KeywordModifier> keywords;
	List<Annotation> annotations;
	
	@CopyMethod
	static boolean isPublic(Modifiers m) {
		return contains(m, "public");
	}
	
	@CopyMethod
	static boolean isProtected(Modifiers m) {
		return contains(m, "protected");
	}
	
	@CopyMethod
	static boolean isPrivate(Modifiers m) {
		return contains(m, "private");
	}
	
	@CopyMethod
	static boolean isPackagePrivate(Modifiers m) {
		return !contains(m, "public") && !contains(m, "protected") && !contains(m, "private");
	}
	
	@CopyMethod
	static boolean isStatic(Modifiers m) {
		return contains(m, "static");
	}
	
	@CopyMethod
	static boolean isFinal(Modifiers m) {
		return contains(m, "final");
	}
	
	@CopyMethod
	static boolean isAbstract(Modifiers m) {
		return contains(m, "abstract");
	}
	
	private static boolean contains(Modifiers m, String keyword) {
		for (Node k : m.keywords().getRawContents()) if (k instanceof KeywordModifier && "public".equals(((KeywordModifier)k).getName())) return true;
		return false;
	}
}

@GenerateAstNode(implementing={Statement.class, TypeMember.class})
class VariableDeclarationTemplate {
	@NonNull VariableDefinition definition;
}

@GenerateAstNode
class VariableDefinitionTemplate {
	@InitialValue("new lombok.ast.Modifiers()")
	@NonNull Modifiers modifiers;
	@NonNull TypeReference typeReference;
	List<VariableDefinitionEntry> variables;
	
	@NotChildOfNode
	boolean varargs;
}

@GenerateAstNode
class VariableDefinitionEntryTemplate {
	@NonNull Identifier name;
	@NotChildOfNode int dimensions;
	Expression initializer;
	
	@CopyMethod
	static TypeReference getTypeReference(VariableDefinitionEntry self) {
		if (!(self.getParent() instanceof VariableDefinition)) throw new AstException(
				self, "Cannot calculate type reference of a VariableDefinitionEntry without a VariableDefinition as parent");
		
		
		VariableDefinition parent = (VariableDefinition) self.getParent();
		
		TypeReference typeRef = parent.getTypeReference().copy();
		return typeRef.setArrayDimensions(typeRef.getArrayDimensions() + self.getDimensions() + (parent.isVarargs() ? 1 : 0));
	}
}

@GenerateAstNode(implementing=Expression.class)
class InlineIfExpressionTemplate {
	@NonNull Expression condition;
	@NonNull Expression ifTrue;
	@NonNull Expression ifFalse;
	
	@CopyMethod
	static boolean needsParentheses(InlineIfExpression self) {
		try {
			return BinaryExpressionTemplate.needsParentheses(self, BinaryOperator.ASSIGN.pLevel()-1);
		} catch (Throwable ignore) {
			return true;
		}
	}
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class})
class IdentifierTemplate {
	@NotChildOfNode
	@NonNull String name;
	
	@CopyMethod
	static String getDescription(Identifier self) {
		return self.getName();
	}
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class})
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
	static boolean needsParentheses(BinaryExpression self) {
		try {
			return needsParentheses(self, self.getOperator().pLevel());
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

@GenerateAstNode(implementing={Expression.class, DescribedNode.class})
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
	static boolean needsParentheses(UnaryExpression self) {
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
	
	@CopyMethod
	static String getTypeName(TypeReference t) {
		StringBuilder out = new StringBuilder();
		for (TypeReferencePart p : t.parts().getContents()) {
			if (out.length() > 0) out.append(".");
			out.append(p.getTypeName());
		}
		
		for (int i = 0; i < t.getArrayDimensions(); i++) out.append("[]");
		
		return out.toString();
	}
	
	@CopyMethod
	static boolean hasGenerics(TypeReference t) {
		return getGenerics(t).isEmpty();
	}
	
	@CopyMethod
	static ListAccessor<TypeReference, TypeReference> getGenerics(TypeReference t) {
		return t.parts().last().generics().wrap(t);
	}
}

@GenerateAstNode
class TypeReferencePartTemplate {
	@NonNull Identifier identifier;
	@InitialValue("new TypeArguments()")
	@NonNull TypeArguments typeArguments;
	
	@CopyMethod
	static ListAccessor<TypeReference, TypeReferencePart> generics(TypeReferencePart self) {
		return self.getTypeArguments().generics().wrap(self);
	}
	
	@CopyMethod
	static String getTypeName(TypeReferencePart p) {
		if (p.generics().isEmpty()) return p.getIdentifier().getName();
		
		StringBuilder out = new StringBuilder();
		out.append(p.getIdentifier().getName()).append("<");
		boolean first = true;
		for (TypeReference t : p.generics().getContents()) {
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

@GenerateAstNode(implementing=Expression.class)
class CastTemplate {
	@NonNull TypeReference typeReference;
	@NonNull Expression operand;
	
	@CopyMethod
	static boolean needsParentheses(Cast self) {
		try {
			return BinaryExpressionTemplate.needsParentheses(self, 1);
		} catch (Throwable ignore) {
			return true;
		}
	}
}

@GenerateAstNode(implementing=Expression.class)
class InstanceOfTemplate {
	@NonNull Expression objectReference;
	@NonNull TypeReference typeReference;
	
	@CopyMethod
	static boolean needsParentheses(InstanceOf self) {
		try {
			return BinaryExpressionTemplate.needsParentheses(self, BinaryOperator.LESS.pLevel());
		} catch (Throwable ignore) {
			return true;
		}
	}
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class})
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

@GenerateAstNode(implementing={Expression.class, DescribedNode.class})
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

@GenerateAstNode(implementing=Expression.class)
class SelectTemplate {
	@NonNull Expression operand;
	@NonNull Identifier identifier;
}

@GenerateAstNode(implementing=Expression.class)
class ArrayAccessTemplate {
	@NonNull Expression operand;
	@NonNull Expression indexExpression;
}

@GenerateAstNode(implementing=Expression.class)
class ArrayCreationTemplate {
	@NonNull TypeReference componentTypeReference;
	List<ArrayDimension> dimensions;
	ArrayInitializer initializer;
}

@GenerateAstNode
class ArrayDimensionTemplate {
	Expression dimension;
}

@GenerateAstNode(implementing=Expression.class)
class ArrayInitializerTemplate {
	List<Expression> expressions;
}

@GenerateAstNode(implementing=Expression.class)
class ThisTemplate {
	TypeReference qualifier;
}

@GenerateAstNode(implementing=Expression.class)
class SuperTemplate {
	TypeReference qualifier;
}

@GenerateAstNode(implementing={Expression.class, DescribedNode.class})
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
	@NotChildOfNode
	@NonNull String name;
	
	@CopyMethod
	static String getDescription(KeywordModifier self) {
		return self.getName();
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

@GenerateAstNode(implementing={Literal.class, Expression.class})
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

@GenerateAstNode(implementing={Expression.class, Literal.class, DescribedNode.class})
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

@GenerateAstNode(implementing={Literal.class, Expression.class, DescribedNode.class})
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
					boolean thirdFits = second >= '0' && second <= '7';
					
					if (first > '3') {
						if (secondFits) {
							i++;
							value.append((first - '0') * 010 + (second - '0'));
							continue;
						}
						value.append(first - '0');
						continue;
					}
					
					if (secondFits && thirdFits) {
						i += 2;
						value.append((first - '0') * 0100 + (second - '0') * 010 + (third - '0'));
						continue;
					}
					
					if (secondFits) {
						i++;
						value.append((first - '0') * 010 + (second - '0'));
						continue;
					}
					
					value.append(first - '0');
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
	@NotChildOfNode
	boolean blockComment;
	
	@NotChildOfNode
	String content;
}

@GenerateAstNode(implementing={TypeMember.class, DescribedNode.class})
class AnnotationMethodDeclarationTemplate {
	@InitialValue("new lombok.ast.Modifiers()")
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

@GenerateAstNode(implementing={TypeMember.class, DescribedNode.class})
class MethodDeclarationTemplate {
	@InitialValue("new lombok.ast.Modifiers()")
	@NonNull Modifiers modifiers;
	
	List<TypeVariable> typeVariables;
	@NonNull TypeReference returnTypeReference;
	@NonNull Identifier methodName;
	List<VariableDefinition> parameters;
	List<TypeReference> thrownTypeReferences;
	@NonNull Block body;
	
	@CopyMethod
	static String getDescription(MethodDeclaration self) {
		try {
			return self.getMethodName().getName();
		} catch (Exception e) {
			return null;
		}
	}
}

@GenerateAstNode(implementing=TypeMember.class)
class ConstructorDeclarationTemplate {
	@InitialValue("new lombok.ast.Modifiers()")
	@NonNull Modifiers modifiers;
	
	List<TypeVariable> typeVariables;
	@NonNull Identifier typeName;
	List<VariableDefinition> parameters;
	List<TypeReference> thrownTypeReferences;
	@NonNull Block body;
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

@GenerateAstNode(implementing={TypeMember.class, TypeDeclaration.class})
class AnnotationDeclarationTemplate {
	@InitialValue("new lombok.ast.Modifiers()")
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

@GenerateAstNode(implementing={TypeMember.class, Statement.class, TypeDeclaration.class})
class ClassDeclarationTemplate {
	@InitialValue("new lombok.ast.Modifiers()")
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

@GenerateAstNode(implementing={TypeMember.class, TypeDeclaration.class})
class InterfaceDeclarationTemplate {
	@InitialValue("new lombok.ast.Modifiers()")
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

@GenerateAstNode(implementing={TypeMember.class, DescribedNode.class})
class EnumConstantTemplate {
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

@GenerateAstNode(implementing={TypeMember.class, TypeDeclaration.class})
class EnumDeclarationTemplate {
	@InitialValue("new lombok.ast.Modifiers()")
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

