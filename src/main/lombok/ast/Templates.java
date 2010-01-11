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
import lombok.ast.template.GenerateAstNode;
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
	Statement initialization;
	Expression condition;
	List<Statement> increments;
	@NonNull Statement statement;
}

@GenerateAstNode(extending=Statement.class)
class ForEachTemplate {
	@NonNull VariableDeclaration element;
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

@GenerateAstNode(extending=Statement.class)
class VariableDeclarationTemplate {
	@NonNull Type type;
	List<VariableDeclarationEntry> variables;
}

@GenerateAstNode
class VariableDeclarationEntryTemplate {
	@NonNull Identifier name;
	Expression initializer;
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
		if (result != null) throw new IllegalArgumentException("unknown binary operator: " + op.trim());
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
		if (result != null) throw new IllegalArgumentException("unknown unary operator: " + op.trim());
		return result;
	}
}