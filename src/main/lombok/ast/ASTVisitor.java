package lombok.ast;

public interface ASTVisitor {
	public boolean visitIf(If node);
	public boolean visitFor(For node);
	public boolean visitForEach(ForEach node);
	public boolean visitTry(Try node);
	public boolean visitCatch(Catch node);
	public boolean visitWhile(While node);
	public boolean visitDoWhile(DoWhile node);
	public boolean visitSynchronized(Synchronized node);
	public boolean visitBlock(Block node);
	public boolean visitAssert(Assert node);
	public boolean visitVariableDeclaration(VariableDeclaration node);
	public boolean visitVariableDeclarationEntry(VariableDeclarationEntry node);
	public boolean visitType(Type node);
	public boolean visitIdentifier(Identifier node);
	public boolean visitIntegralLiteral(IntegralLiteral node);
	public boolean visitFloatingPointLiteral(FloatingPointLiteral node);
	public boolean visitBooleanLiteral(BooleanLiteral node);
	public boolean visitCharLiteral(CharLiteral node);
	public boolean visitStringLiteral(StringLiteral node);
	public boolean visitNullLiteral(NullLiteral node);
}
