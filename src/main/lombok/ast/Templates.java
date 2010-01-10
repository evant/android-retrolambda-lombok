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
