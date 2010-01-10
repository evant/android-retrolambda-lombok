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
import lombok.ast.template.GenerateAstNode;

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
	Statement initializion;
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
	
	//TODO actually call this thing!
//	static void additionalSyntacticChecks(List<SyntaxProblem> problems, Try node) {
//		if (node.catches().size() == 0 && node.getRawFinally() == null) {
//			problems.add(new SyntaxProblem(node, "try statement with no catches and no finally"));
//		}
//	}
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
	boolean decrement = false;
	boolean prefix = false;
}
