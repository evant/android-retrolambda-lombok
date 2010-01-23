package lombok.ast.printer;

import org.parboiled.support.InputLocation;

import lombok.ast.ASTVisitor;
import lombok.ast.AlternateConstructorInvocation;
import lombok.ast.Annotation;
import lombok.ast.AnnotationDeclaration;
import lombok.ast.AnnotationElement;
import lombok.ast.AnnotationMethodDeclaration;
import lombok.ast.ArrayAccess;
import lombok.ast.ArrayCreation;
import lombok.ast.ArrayDimension;
import lombok.ast.ArrayInitializer;
import lombok.ast.Assert;
import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.Block;
import lombok.ast.BooleanLiteral;
import lombok.ast.Break;
import lombok.ast.Case;
import lombok.ast.Cast;
import lombok.ast.Catch;
import lombok.ast.CharLiteral;
import lombok.ast.ClassDeclaration;
import lombok.ast.ClassLiteral;
import lombok.ast.Comment;
import lombok.ast.CompilationUnit;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.ConstructorInvocation;
import lombok.ast.Continue;
import lombok.ast.Default;
import lombok.ast.DoWhile;
import lombok.ast.EmptyStatement;
import lombok.ast.EnumConstant;
import lombok.ast.EnumDeclaration;
import lombok.ast.FloatingPointLiteral;
import lombok.ast.For;
import lombok.ast.ForEach;
import lombok.ast.Identifier;
import lombok.ast.If;
import lombok.ast.ImportDeclaration;
import lombok.ast.InlineIfExpression;
import lombok.ast.InstanceInitializer;
import lombok.ast.InstanceOf;
import lombok.ast.IntegralLiteral;
import lombok.ast.InterfaceDeclaration;
import lombok.ast.KeywordModifier;
import lombok.ast.LabelledStatement;
import lombok.ast.ListAccessor;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.NullLiteral;
import lombok.ast.PackageDeclaration;
import lombok.ast.Return;
import lombok.ast.Select;
import lombok.ast.StaticInitializer;
import lombok.ast.StringLiteral;
import lombok.ast.Super;
import lombok.ast.SuperConstructorInvocation;
import lombok.ast.Switch;
import lombok.ast.Synchronized;
import lombok.ast.This;
import lombok.ast.Throw;
import lombok.ast.Try;
import lombok.ast.TypeArguments;
import lombok.ast.TypeBody;
import lombok.ast.TypeReference;
import lombok.ast.TypeReferencePart;
import lombok.ast.TypeVariable;
import lombok.ast.UnaryExpression;
import lombok.ast.UnaryOperator;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.While;
import lombok.ast.WildcardKind;

public class HtmlPrinter extends HtmlBuilder {
	//Private utility methods
	private void visit(Node node) {
		if (node != null) node.accept(this);
	}
	
	private void visitAll(ListAccessor<?, ?> nodes, String separator, String prefix, String suffix) {
		if (nodes.isEmpty()) return;
		append(prefix);
		boolean first = true;
		for (Node n : nodes.getRawContents()) {
			if (!first) {
				append(separator);
			}
			first = false;
			visit(n);
		}
		append(suffix);
	}
	
	private boolean isValidJavaIdentifier(String in) {
		if (in == null || in.length() == 0) return false;
		
		char c = in.charAt(0);
		if (!Character.isJavaIdentifierStart(c)) return false;
		char[] cs = in.toCharArray();
		for (int i = 1; i < cs.length; i++) {
			if (!Character.isJavaIdentifierPart(cs[i])) return false;
		}
		return true;
	}
	
	//Basics
	public boolean visitTypeReference(TypeReference node) {
		WildcardKind kind = node.getWildcard();
		buildInline(node);
		if (kind == WildcardKind.UNBOUND) {
			append("?");
			closeInline();
			return true;
		} else if (kind == WildcardKind.EXTENDS) {
			append("?");
			space();
			keyword("extends");
			space();
		} else if (kind == WildcardKind.SUPER) {
			append("?");
			space();
			keyword("super");
			space();
		}
		
		visitAll(node.parts(), ".", "", "");
		
		for (int i = 0 ; i < node.getArrayDimensions(); i++) append("[]");
		
		closeInline();
		return true;
	}
	
	public boolean visitTypeReferencePart(TypeReferencePart node) {
		buildInline(node);
		visit(node.getRawIdentifier());
		visit(node.getRawTypeArguments());
		closeInline();
		return true;
	}
	
	public HtmlPrinter(String rawSource) {
		super(rawSource);
	}
	
	public boolean visitIdentifier(Identifier node) {
		String name = node.getName();
		if (!node.isSyntacticallyValid()) {
			if (name == null || name.isEmpty()) {
				reportAssertionFailureNext(node, "null or empty identifier that is nevertheless syntactically valid", null);
			} else if (!isValidJavaIdentifier(name)) {
				reportAssertionFailureNext(node, "identifier name contains characters that aren't legal in an identifier", null);
			}
		}
		
		if (name == null) name = FAIL + "NULL_IDENTIFIER" + FAIL;
		else if (name.isEmpty()) name = FAIL + "EMPTY_IDENTIFIER" + FAIL;
		else if (!isValidJavaIdentifier(name)) name = FAIL + "INVALID_IDENTIFIER: " + name + FAIL;
		
		buildInline(node);
		append(name);
		closeInline();
		return true;
	}
	
	public boolean visitIntegralLiteral(IntegralLiteral node) {
		String raw = node.getRawValue();
		try {
			IntegralLiteral il = new IntegralLiteral();
			if (node.isMarkedAsLong()) il.setLongValue(node.longValue());
			else il.setIntValue(node.intValue());
			raw = il.getRawValue();
		} catch (Exception e) {
			if (node.isSyntacticallyValid()) {
				reportAssertionFailureNext(node, "correct integral literal nevertheless does not pass getValue->setValue->getRawValue process.", e);
			}
		}
		
		buildInline(node);
		append(raw);
		closeInline();
		return true;
	}
	
	public boolean visitFloatingPointLiteral(FloatingPointLiteral node) {
		String raw = node.getRawValue();
		try {
			FloatingPointLiteral fpl = new FloatingPointLiteral();
			if (node.isMarkedAsFloat()) fpl.setFloatValue(node.floatValue());
			else fpl.setDoubleValue(node.doubleValue());
			raw = fpl.getRawValue();
		} catch (Exception e) {
			if (node.isSyntacticallyValid()) {
				reportAssertionFailureNext(node, "correct floating point literal nevertheless does not pass getValue->setValue->getRawValue process.", e);
			}
		}
		
		buildInline(node);
		append(raw);
		closeInline();
		return true;
	}
	
	public boolean visitBooleanLiteral(BooleanLiteral node) {
		String raw = node.getRawValue();
		try {
			raw = new BooleanLiteral().setValue(node.getValue()).getRawValue();
		} catch (Exception e) {
			if (node.isSyntacticallyValid()) {
				reportAssertionFailureNext(node, "correct boolean literal nevertheless does not pass getValue->setValue->getRawValue process.", e);
			}
		}
		
		buildInline(node);
		append(raw);
		closeInline();
		return true;
	}
	
	public boolean visitCharLiteral(CharLiteral node) {
		String raw = node.getRawValue();
		try {
			raw = new CharLiteral().setValue(node.getValue()).getRawValue();
		} catch (Exception e) {
			if (node.isSyntacticallyValid()) {
				reportAssertionFailureNext(node, "correct char literal nevertheless does not pass getValue->setValue->getRawValue process.", e);
			}
		}
		
		buildInline(node);
		append(raw);
		closeInline();
		return true;
	}
	
	public boolean visitStringLiteral(StringLiteral node) {
		String raw = node.getRawValue();
		try {
			raw = new StringLiteral().setValue(node.getValue()).getRawValue();
		} catch (Exception e) {
			if (node.isSyntacticallyValid()) {
				reportAssertionFailureNext(node, "correct string literal nevertheless does not pass getValue->setValue->getRawValue process.", e);
			}
		}
		
		buildInline(node);
		append(raw);
		closeInline();
		return true;
	}
	
	public boolean visitNullLiteral(NullLiteral node) {
		buildInline(node);
		keyword("null");
		closeInline();
		return true;
	}

	//Expressions
	public boolean visitBinaryExpression(BinaryExpression node) {
		buildInline(node);
		boolean parens = node.needsParentheses();
		if (parens) append("(");
		visit(node.getRawLeft());
		space();
		try {
			operator(node.getOperator().getSymbol());
		} catch (Exception e) {
			operator(node.getRawOperator());
		}
		space();
		visit(node.getRawRight());
		if (parens) append(")");
		closeInline();
		return true;
	}
	
	public boolean visitUnaryExpression(UnaryExpression node) {
		buildInline(node);
		UnaryOperator op;
		try {
			op = node.getOperator();
		} catch (Exception e) {
			visitNode(node.getOperand());
			closeInline();
			return true;
		}
		boolean parens = node.needsParentheses();
		if (parens) append("(");
		if (!op.isPostfix()) operator(op.getSymbol());
		visitNode(node.getOperand());
		if (op.isPostfix()) operator(op.getSymbol());
		if (parens) append(")");
		closeInline();
		return true;
	}
	
	public boolean visitCast(Cast node) {
		buildInline(node);
		boolean parens = node.needsParentheses();
		if (parens) append("(");
		append("(");
		visitNode(node.getRawTypeReference());
		append(")");
		space();
		visitNode(node.getRawOperand());
		if (parens) append(")");
		closeInline();
		return true;
	}
	
	public boolean visitInlineIfExpression(InlineIfExpression node) {
		buildInline(node);
		boolean parens = node.needsParentheses();
		if (parens) append("(");
		visit(node.getRawCondition());
		space();
		operator("?");
		space();
		visit(node.getRawIfTrue());
		space();
		operator(":");
		space();
		visit(node.getRawIfFalse());
		if (parens) append(")");
		closeInline();
		return true;
	}
	
	public boolean visitInstanceOf(InstanceOf node) {
		buildInline(node);
		boolean parens = node.needsParentheses();
		if (parens) append("(");
		visit(node.getRawObjectReference());
		space();
		keyword("instanceof");
		space();
		visit(node.getRawTypeReference());
		if (parens) append(")");
		closeInline();
		return true;
	}
	
	public boolean visitConstructorInvocation(ConstructorInvocation node) {
		buildInline(node);
		if (node.getRawQualifier() != null) {
			visit(node.getRawQualifier());
			append(".");
		}
		keyword("new");
		space();
		visit(node.getRawConstructorTypeArguments());
		visit(node.getRawTypeReference());
		append("(");
		visitAll(node.arguments(), ", ", "", "");
		append(")");
		closeInline();
		return true;
	}
	
	public boolean visitMethodInvocation(MethodInvocation node) {
		buildInline(node);
		if (node.getRawOperand() != null) {
			visit(node.getRawOperand());
			append(".");
		}
		visit(node.getRawMethodTypeArguments());
		visit(node.getRawName());
		append("(");
		visitAll(node.arguments(), ", ", "", "");
		append(")");
		closeInline();
		return true;
	}
	
	public boolean visitSelect(Select node) {
		buildInline(node);
		if (node.getRawOperand() != null) {
			visit(node.getRawOperand());
			append(".");
		}
		visit(node.getRawIdentifier());
		closeInline();
		return true;
	}
	
	public boolean visitArrayAccess(ArrayAccess node) {
		buildInline(node);
		visit(node.getRawOperand());
		append("[");
		visit(node.getRawIndexExpression());
		append("]");
		closeInline();
		return true;
	}
	
	public boolean visitArrayCreation(ArrayCreation node) {
		buildInline(node);
		keyword("new");
		space();
		visit(node.getRawComponentTypeReference());
		visitAll(node.dimensions(), "", "", "");
		if (node.getRawInitializer() != null) {
			space();
			visit(node.getRawInitializer());
		}
		closeInline();
		return true;
	}
	
	public boolean visitArrayInitializer(ArrayInitializer node) {
		buildInline(node);
		append("{");
		visitAll(node.expressions(), ", ", "", "");
		append("}");
		closeInline();
		return true;
	}
	
	public boolean visitArrayDimension(ArrayDimension node) {
		buildInline(node);
		append("[");
		visit(node.getRawDimension());
		append("]");
		closeInline();
		return true;
	}
	
//	public abstract boolean visitClassLiteral(ClassLiteral node);
//	public abstract boolean visitSuper(Super node);
//	public abstract boolean visitThis(This node);
//	
//	//Statements
//	public abstract boolean visitLabelledStatement(LabelledStatement node);
//	public abstract boolean visitIf(If node);
//	public abstract boolean visitFor(For node);
//	public abstract boolean visitForEach(ForEach node);
//	public abstract boolean visitTry(Try node);
//	public abstract boolean visitCatch(Catch node);
//	public abstract boolean visitWhile(While node);
//	public abstract boolean visitDoWhile(DoWhile node);
//	public abstract boolean visitSynchronized(Synchronized node);
	public boolean visitBlock(Block node) {
		buildInline(node);
		append("{");
		buildBlock(null);
		visitAll(node.contents(), "", "", "");
		closeBlock();
		append("}");
		closeInline();
		return true;
	}
//	public abstract boolean visitAssert(Assert node);
//	public abstract boolean visitEmptyStatement(EmptyStatement node);
//	public abstract boolean visitSwitch(Switch node);
//	public abstract boolean visitCase(Case node);
//	public abstract boolean visitDefault(Default node);
//	public abstract boolean visitBreak(Break node);
//	public abstract boolean visitContinue(Continue node);
//	public abstract boolean visitReturn(Return node);
//	public abstract boolean visitThrow(Throw node);
//	
//	//Structural
	public boolean visitVariableDeclaration(VariableDeclaration node) {
		buildBlock(node);
		visit(node.getRawDefinition());
		append(";");
		closeBlock();
		return true;
	}
	public boolean visitVariableDefinition(VariableDefinition node) {
		buildInline(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			space();
		}
		visit(node.getRawTypeReference());
		space();
		visitAll(node.variables(), ", ", "", "");
		closeInline();
		
		return true;
	}
	
	public boolean visitVariableDefinitionEntry(VariableDefinitionEntry node) {
		buildInline(node);
		visit(node.getRawName());
		for (int i = 0; i < node.getDimensions(); i++) append("[]");
		if (node.getRawInitializer() != null) {
			append(" = ");
			visit(node.getRawInitializer());
		}
		closeInline();
		
		return true;
	}
	
	public boolean visitTypeArguments(TypeArguments node) {
		buildInline(node);
		visitAll(node.generics(), ", ", "<", ">");
		closeInline();
		return true;
	}
	
	public boolean visitTypeVariable(TypeVariable node) {
		buildInline(node);
		visit(node.getRawName());
		if (!node.extending().isEmpty()) {
			keyword("extends");
			visitAll(node.extending(), " & ", " ", "");
		}
		closeInline();
		return true;
	}
	
	public boolean visitKeywordModifier(KeywordModifier node) {
		buildInline(node);
		if (node.getName() == null || node.getName().isEmpty()) fail("MISSING_MODIFIER");
		else keyword(node.getName());
		closeInline();
		return true;
	}
	
	public boolean visitModifiers(Modifiers node) {
		buildInline(node);
		visitAll(node.annotations(), "", "", "");
		visitAll(node.keywords(), " ", "", "");
		closeInline();
		return true;
	}
	
//	public abstract boolean visitAnnotation(Annotation node);
//	public abstract boolean visitAnnotationElement(AnnotationElement node);
	
	public boolean visitTypeBody(TypeBody node) {
		buildInline(node);
		append("{");
		buildBlock(null);
		visitAll(node.members(), "\n", "", "");
		closeBlock();
		append("}");
		closeInline();
		return true;
	}
	
//	//Class Bodies
	public boolean visitMethodDeclaration(MethodDeclaration node) {
		buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			space();
		}
		visitAll(node.typeVariables(), ", ", "<", ">");
		if (!node.typeVariables().isEmpty()) space();
		visit(node.getRawReturnTypeReference());
		space();
		visit(node.getRawMethodName());
		if (node.parameters().isEmpty()) append("()");
		else visitAll(node.parameters(), ", ", "(", ")");
		space();
		if (!node.thrownTypeReferences().isEmpty()) {
			keyword("throws");
			visitAll(node.thrownTypeReferences(), ", ", " ", " ");
		}
		visit(node.getBody());
		closeBlock();
		
		return true;
	}
	
//	public abstract boolean visitConstructorDeclaration(ConstructorDeclaration node);
//	public abstract boolean visitSuperConstructorInvocation(SuperConstructorInvocation node);
//	public abstract boolean visitAlternateConstructorInvocation(AlternateConstructorInvocation node);
	
	public boolean visitInstanceInitializer(InstanceInitializer node) {
		buildBlock(node);
		visit(node.getRawBody());
		closeBlock();
		return true;
	}
	
	public boolean visitStaticInitializer(StaticInitializer node) {
		buildBlock(node);
		keyword("static");
		space();
		visit(node.getRawBody());
		closeBlock();
		return true;
	}
	
	public boolean visitClassDeclaration(ClassDeclaration node) {
		buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			space();
		}
		keyword("class");
		space();
		visit(node.getRawName());
		visitAll(node.typeVariables(), ", ", "<", ">");
		space();
		if (node.getRawExtending() != null) {
			keyword("extends");
			space();
			visit(node.getRawExtending());
			space();
		}
		if (!node.implementing().isEmpty()) {
			keyword("implements");
			visitAll(node.implementing(), ", ", " ", " ");
		}
		visit(node.getRawBody());
		closeBlock();
		return true;
	}
	
//	public abstract boolean visitInterfaceDeclaration(InterfaceDeclaration node);
//	public abstract boolean visitEnumDeclaration(EnumDeclaration node);
//	public abstract boolean visitEnumConstant(EnumConstant node);
//	public abstract boolean visitAnnotationDeclaration(AnnotationDeclaration node);
//	public abstract boolean visitAnnotationMethodDeclaration(AnnotationMethodDeclaration node);
	public boolean visitCompilationUnit(CompilationUnit node) {
		buildBlock(node);
		if (node.getRawPackageDeclaration() != null) {
			visit(node.getRawPackageDeclaration());
			newline();
		}
		visitAll(node.importDeclarations(), "", "", "\n");
		visitAll(node.typeDeclarations(), "\n", "", "");
		closeBlock();
		return true;
	}
	
	public boolean visitPackageDeclaration(PackageDeclaration node) {
		buildBlock(node);
		visitAll(node.annotations(), "", "", "");
		keyword("package");
		space();
		visitAll(node.parts(), ".", "", "");
		append(";");
		closeBlock();
		return true;
	}
	
	public boolean visitImportDeclaration(ImportDeclaration node) {
		buildBlock(node);
		keyword("import");
		space();
		if (node.isStaticImport()) {
			keyword("static");
			space();
		}
		visitAll(node.parts(), ".", "", "");
		if (node.isStarImport()) {
			append(".*");
		}
		append(";");
		closeBlock();
		return true;
	}
//	
//	//Various
//	public abstract boolean visitParseArtefact(Node node);
//	public abstract boolean visitComment(Comment node);
	
	public void setTimeTaken(long taken) {
		super.setTimeTaken(taken);
	}
}
