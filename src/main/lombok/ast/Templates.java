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

import java.util.List;

import lombok.NonNull;
import lombok.ast.template.AdditionalCheck;
import lombok.ast.template.CopyMethod;
import lombok.ast.template.GenerateAstNode;
import lombok.ast.template.InitialValue;
import lombok.ast.template.NotChildOfNode;

@GenerateAstNode(extending=Statement.class)
class AssertTemplate {
	@NonNull Expression assertion;
	Expression message;
}

@GenerateAstNode(extending=Statement.class)
class CatchTemplate {
	@NonNull VariableDeclaration exceptionDeclaration;
	@NonNull Block body;
	
	/* check: exDecl must have exactly 1 VDEntry */
}

@GenerateAstNode(extending=Statement.class)
class BlockTemplate {
	List<Statement> contents;
}

@GenerateAstNode(extending=Statement.class)
class DoWhileTemplate {
	@NonNull Expression condition;
	@NonNull Statement statement;
}

@GenerateAstNode(extending=Statement.class)
class WhileTemplate {
	@NonNull Expression condition;
	@NonNull Statement statement;
}

@GenerateAstNode(extending=Statement.class)
class ForTemplate {
	List<Statement> inits;
	Expression condition;
	List<Statement> updates;
	@NonNull Statement statement;
}

@GenerateAstNode(extending=Statement.class)
class ForEachTemplate {
	@NonNull VariableDeclaration variable;
	@NonNull Expression iterable;
	@NonNull Statement statement;
}

@GenerateAstNode(extending=Statement.class)
class IfTemplate {
	@NonNull Expression condition;
	@NonNull Statement statement;
	Statement elseStatement;
}

@GenerateAstNode(extending=Statement.class)
class SynchronizedTemplate {
	@NonNull Expression lock;
	@NonNull Block body;
}

@GenerateAstNode(extending=Statement.class)
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

@GenerateAstNode
class AnnotationTemplate {
	
}

@GenerateAstNode
class ModifiersTemplate {
	//moet AnnotationMod en KeywMod een common interface hebben of zo??
	//what shoud this even look like?
	//2 lists: 1 for key...
	
	//en dan een stack of utility methods for isPublic en zo?
	//how about 1 iterator to iterate through ALL keywords? And as what? "Node"? Or common interface?
	
	//what would it have? 9the common interface)
	
	//'isKeyword' - silly, instanceof check works as well
	
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

@GenerateAstNode(extending=Statement.class)
class VariableDeclarationTemplate {
	@NonNull TypeReference typeReference;
	List<VariableDeclarationEntry> variables;
}

@GenerateAstNode
class VariableDeclarationEntryTemplate {
	@NonNull Identifier name;
	@NotChildOfNode int dimensions;
	Expression initializer;
	
	@CopyMethod
	static TypeReference getTypeReference(VariableDeclarationEntry self) {
		if (!(self.getParent() instanceof VariableDeclaration)) throw new AstException(
				self, "Cannot calculate type reference of a VariableDeclarationEntry without a VariableDeclaration as parent");
		
		
		TypeReference typeRef = ((VariableDeclaration)self.getParent()).getTypeReference().copy();
		return typeRef.setArrayDimensions(typeRef.getArrayDimensions() + self.getDimensions());
	}
}

@GenerateAstNode(extending=Expression.class)
class InlineIfExpressionTemplate {
	@NonNull Expression condition;
	@NonNull Expression ifTrue;
	@NonNull Expression ifFalse;
}

@GenerateAstNode(extending=Expression.class)
class IncrementExpressionTemplate {
	@NonNull Expression operand;
	@NotChildOfNode boolean decrement = false;
	@NotChildOfNode boolean prefix = false;
}

@GenerateAstNode
class IdentifierTemplate {
	@NotChildOfNode
	@NonNull String name;
}

@GenerateAstNode(extending=Expression.class)
class BinaryExpressionTemplate {
	@NonNull Expression left;
	@NonNull Expression right;
	@NotChildOfNode(rawFormParser="parseOperator", rawFormGenerator="generateOperator")
	@NonNull BinaryOperator operator;
	
	static String generateOperator(BinaryOperator op) {
		return op.getSymbol();
	}
	
	static BinaryOperator parseOperator(String op) {
		if (op == null) throw new IllegalArgumentException("missing operator");
		BinaryOperator result = BinaryOperator.fromSymbol(op.trim());
		if (result == null) throw new IllegalArgumentException("unknown binary operator: " + op.trim());
		return result;
	}
}

@GenerateAstNode(extending=Expression.class)
class UnaryExpressionTemplate {
	@NonNull Expression operand;
	@NotChildOfNode(rawFormParser="parseOperator", rawFormGenerator="generateOperator")
	@NonNull UnaryOperator operator;
	
	static String generateOperator(UnaryOperator op) {
		return op.getSymbol();
	}
	
	static UnaryOperator parseOperator(String op) {
		if (op == null) throw new IllegalArgumentException("missing operator");
		UnaryOperator result = UnaryOperator.fromSymbol(op.trim());
		if (result == null) throw new IllegalArgumentException("unknown unary operator: " + op.trim());
		return result;
	}
}

@GenerateAstNode
class TypeVariableTemplate {
	@NonNull Identifier name;
	List<TypeReference> extending;
}

@GenerateAstNode
class TypeReferenceTemplate {
	@NotChildOfNode
	@InitialValue("lombok.ast.WildcardKind.NONE")
	@NonNull WildcardKind wildcard;
	
	@NotChildOfNode
	int arrayDimensions;
	
	List<TypeReferencePart> parts;
	
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

@GenerateAstNode(extending=Expression.class)
class CastTemplate {
	@NonNull TypeReference typeReference;
	@NonNull Expression operand;
}

@GenerateAstNode(extending=Expression.class)
class IdentifierExpressionTemplate {
	@NonNull Identifier identifier;
}

@GenerateAstNode(extending=Expression.class)
class InstanceOfTemplate {
	@NonNull Expression objectReference;
	@NonNull TypeReference typeReference;
}

@GenerateAstNode(extending=Expression.class)
class ConstructorInvocationTemplate {
	Expression qualifier;
	TypeArguments constructorTypeArguments;
	@NonNull TypeReference typeReference;
	List<Expression> arguments;
	ClassBody anonymousClassBody;
}

@GenerateAstNode(extending=Expression.class)
class MethodInvocationTemplate {
	Expression operand;
	TypeArguments methodTypeArguments;
	@NonNull Identifier name;
	List<Expression> arguments;
}

@GenerateAstNode
class ClassBodyTemplate {
	
}

@GenerateAstNode(extending=Expression.class)
class SelectTemplate {
	@NonNull Expression operand;
	@NonNull Identifier identifier;
}

@GenerateAstNode(extending=Expression.class)
class ArrayAccessTemplate {
	@NonNull Expression operand;
	@NonNull Expression indexExpression;
}

@GenerateAstNode(extending=Expression.class)
class ArrayCreationTemplate {
	@NonNull TypeReference componentTypeReference;
	List<ArrayDimension> dimensions;
	ArrayInitializer initializer;
}

@GenerateAstNode
class ArrayDimensionTemplate {
	Expression dimension;
}

@GenerateAstNode(extending=Expression.class)
class ArrayInitializerTemplate {
	List<Expression> expressions;
}

@GenerateAstNode(extending=Expression.class)
class ThisTemplate {
	TypeReference qualifier;
}

@GenerateAstNode(extending=Expression.class)
class SuperTemplate {
	TypeReference qualifier;
}

@GenerateAstNode(extending=Expression.class)
class ClassLiteralTemplate {
	@NonNull TypeReference typeReference;
}

@GenerateAstNode
class KeywordModifierTemplate {
	@NotChildOfNode
	@NonNull String name;
}

@GenerateAstNode(extending=Statement.class)
class EmptyStatementTemplate {}

@GenerateAstNode(extending=Statement.class)
class LabelledStatementTemplate {
	@NonNull Identifier label;
	@NonNull Statement statement;
}

@GenerateAstNode(extending=Statement.class)
class SwitchTemplate {
	@NonNull Expression condition;
	@NonNull Block body;
}

@GenerateAstNode(extending=Statement.class)
class CaseTemplate {
	@NonNull Expression condition;
}

@GenerateAstNode(extending=Statement.class)
class DefaultTemplate {
}

@GenerateAstNode(extending=Expression.class, implementing=Literal.class)
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

@GenerateAstNode(extending=Expression.class, implementing=Literal.class)
class CharLiteralTemplate {
	@NotChildOfNode(rawFormParser="parseChar", rawFormGenerator="generateChar")
	@NonNull Character value;
	
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

@GenerateAstNode(extending=Expression.class, implementing=Literal.class)
class StringLiteralTemplate {
	@NotChildOfNode(rawFormParser="parseString", rawFormGenerator="generateString")
	@NonNull String value;
	
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

@GenerateAstNode(extending=Statement.class)
class BreakTemplate {
	Identifier label;
	
	@CopyMethod
	static boolean hasLabel(Break self) {
		return self.getRawLabel() != null;
	}
}

@GenerateAstNode(extending=Statement.class)
class ContinueTemplate {
	Identifier label;
	
	@CopyMethod
	static boolean hasLabel(Continue self) {
		return self.getRawLabel() != null;
	}
}

@GenerateAstNode(extending=Statement.class)
class ReturnTemplate {
	Expression value;
}

@GenerateAstNode(extending=Statement.class)
class ThrowTemplate {
	@NonNull Expression throwable;
}
