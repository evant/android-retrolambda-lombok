package lombok.ast.printer;

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
import lombok.ast.ExpressionStatement;
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
	
	public boolean visitClassLiteral(ClassLiteral node) {
		buildInline(node);
		visit(node.getRawTypeReference());
		append(".");
		keyword("class");
		closeInline();
		return true;
	}
	
	public boolean visitSuper(Super node) {
		buildInline(node);
		if (node.getRawQualifier() != null) {
			visit(node.getRawQualifier());
			append(".");
		}
		keyword("super");
		closeInline();
		return true;
	}
	
	public boolean visitThis(This node) {
		buildInline(node);
		if (node.getRawQualifier() != null) {
			visit(node.getRawQualifier());
			append(".");
		}
		keyword("this");
		closeInline();
		return true;
	}
	
	//Statements
	public boolean visitExpressionStatement(ExpressionStatement node) {
		buildBlock(node);
		visit(node.getRawExpression());
		append(";");
		closeBlock();
		return true;
	}
	
	public boolean visitLabelledStatement(LabelledStatement node) {
		buildBlock(node);
		if (node.getRawLabel() != null) {
			visit(node.getRawLabel());
			append(":");
		}
		visit(node.getRawStatement());
		closeBlock();
		return true;
	}
	
	public boolean visitIf(If node) {
		buildBlock(node);
		keyword("if");
		space();
		append("(");
		visit(node.getRawCondition());
		append(")");
		space();
		startSuppressBlock();
		visit(node.getRawStatement());
		startSuppressBlock();
		if (node.getRawElseStatement() != null) {
			space();
			keyword("else");
			space();
			startSuppressBlock();
			visit(node.getRawElseStatement());
			startSuppressBlock();
		}
		closeBlock();
		return true;
	}
	
	public boolean visitFor(For node) {
		buildBlock(node);
		keyword("for");
		space();
		append("(");
		visitAll(node.inits(), ", ", "", "");
		append(";");
		space();
		visit(node.getRawCondition());
		append(";");
		space();
		visitAll(node.updates(), ", ", "", "");
		append(")");
		space();
		startSuppressBlock();
		visit(node.getRawStatement());
		endSuppressBlock();
		closeBlock();
		return true;
	}
	
	public boolean visitForEach(ForEach node) {
		buildBlock(node);
		keyword("for");
		space();
		append("(");
		visit(node.getRawVariable());
		space();
		append(":");
		space();
		visit(node.getRawIterable());
		append(")");
		space();
		startSuppressBlock();
		visit(node.getRawStatement());
		endSuppressBlock();
		closeBlock();
		return true;
	}
	
	public boolean visitTry(Try node) {
		buildBlock(node);
		keyword("try");
		space();
		startSuppressBlock();
		visit(node.getRawBody());
		endSuppressBlock();
		visitAll(node.catches(), " ", " ", "");
		if (node.getRawFinally() != null) {
			space();
			keyword("finally");
			space();
			startSuppressBlock();
			visit(node.getRawFinally());
			endSuppressBlock();
		}
		closeBlock();
		return true;
	}
	
	public boolean visitCatch(Catch node) {
		buildInline(node);
		keyword("catch");
		space();
		append("(");
		visit(node.getRawExceptionDeclaration());
		append(")");
		space();
		startSuppressBlock();
		visit(node.getRawBody());
		endSuppressBlock();
		closeInline();
		return true;
	}
	
	public boolean visitWhile(While node) {
		buildBlock(node);
		keyword("while");
		space();
		append("(");
		visit(node.getRawCondition());
		append(")");
		space();
		startSuppressBlock();
		visit(node.getRawStatement());
		endSuppressBlock();
		closeBlock();
		return true;
	}
	
	public boolean visitDoWhile(DoWhile node) {
		buildBlock(node);
		keyword("do");
		space();
		startSuppressBlock();
		visit(node.getRawStatement());
		endSuppressBlock();
		space();
		keyword("while");
		space();
		append("(");
		visit(node.getRawCondition());
		append(")");
		append(";");
		closeBlock();
		return true;
	}
	
	public boolean visitSynchronized(Synchronized node) {
		buildBlock(node);
		keyword("synchronized");
		space();
		append("(");
		visit(node.getRawLock());
		append(")");
		space();
		startSuppressBlock();
		visit(node.getBody());
		endSuppressBlock();
		closeBlock();
		return true;
	}
	
	public boolean visitBlock(Block node) {
		buildBlock(node);
		append("{");
		buildBlock(null);
		visitAll(node.contents(), "", "", "");
		closeBlock();
		append("}");
		closeBlock();
		return true;
	}
	
	public boolean visitAssert(Assert node) {
		buildBlock(node);
		keyword("assert");
		space();
		visit(node.getRawAssertion());
		if (node.getRawMessage() != null) {
			space();
			append(":");
			space();
			visit(node.getRawMessage());
		}
		append(";");
		closeBlock();
		return true;
	}
	
	public boolean visitEmptyStatement(EmptyStatement node) {
		buildBlock(node);
		append(";");
		closeBlock();
		return true;
	}
	
	public boolean visitSwitch(Switch node) {
		buildBlock(node);
		keyword("switch");
		space();
		append("(");
		visit(node.getRawCondition());
		append(")");
		space();
		startSuppressBlock();
		visit(node.getRawBody());
		endSuppressBlock();
		return true;
	}
	
	public boolean visitCase(Case node) {
		buildBlock(node);
		keyword("case");
		space();
		visit(node.getRawCondition());
		append(":");
		closeBlock();
		return true;
	}
	
	public boolean visitDefault(Default node) {
		buildBlock(node);
		keyword("default");
		append(":");
		closeBlock();
		return true;
	}
	
	public boolean visitBreak(Break node) {
		buildBlock(node);
		keyword("break");
		if (node.getRawLabel() != null) {
			space();
			visit(node.getRawLabel());
		}
		append(";");
		closeBlock();
		return true;
	}
	
	public boolean visitContinue(Continue node) {
		buildBlock(node);
		keyword("continue");
		if (node.getRawLabel() != null) {
			space();
			visit(node.getRawLabel());
		}
		append(";");
		closeBlock();
		return true;
	}
	
	public boolean visitReturn(Return node) {
		buildBlock(node);
		keyword("return");
		if (node.getRawValue() != null) {
			space();
			visit(node.getRawValue());
		}
		append(";");
		closeBlock();
		return true;
	}
	
	public boolean visitThrow(Throw node) {
		buildBlock(node);
		keyword("throw");
		space();
		node.getRawThrowable();
		append(";");
		closeBlock();
		return true;
	}
	
	//Structural
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
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).keywords().isEmpty()) {
				space();
			}
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
	
	public boolean visitAnnotation(Annotation node) {
		buildInline(node);
		append("@");
		visit(node.getRawAnnotationTypeReference());
		visitAll(node.elements(), ", ", "(", ")");
		closeInline();
		return true;
	}
	
	public boolean visitAnnotationElement(AnnotationElement node) {
		buildInline(node);
		if (node.getRawName() != null) {
			visit(node.getRawName());
			space();
			append("=");
			space();
		}
		visit(node.getValue());
		closeInline();
		return true;
	}
	
	public boolean visitTypeBody(TypeBody node) {
		buildBlock(node);
		append("{");
		buildBlock(null);
		if (node.getParent() instanceof EnumDeclaration) {
			buildBlock(null);
			visitAll(((EnumDeclaration)node.getParent()).constants(), ", ", "", "");
			if (!node.members().isEmpty()) append(";");
			closeBlock();
		}
		visitAll(node.members(), "\n", "", "");
		closeBlock();
		append("}");
		closeBlock();
		return true;
	}
	
	//Class Bodies
	public boolean visitMethodDeclaration(MethodDeclaration node) {
		buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).keywords().isEmpty()) {
				space();
			}
		}
		visitAll(node.typeVariables(), ", ", "<", ">");
		if (!node.typeVariables().isEmpty()) space();
		visit(node.getRawReturnTypeReference());
		space();
		visit(node.getRawMethodName());
		append("(");
		visitAll(node.parameters(), ", ", "", "");
		append(")");
		space();
		if (!node.thrownTypeReferences().isEmpty()) {
			keyword("throws");
			visitAll(node.thrownTypeReferences(), ", ", " ", " ");
		}
		startSuppressBlock();
		visit(node.getRawBody());
		if (node.getRawBody() == null) {
			append(";");
		}
		endSuppressBlock();
		closeBlock();
		
		return true;
	}
	
	public boolean visitConstructorDeclaration(ConstructorDeclaration node) {
		buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).keywords().isEmpty()) {
				space();
			}
		}
		visitAll(node.typeVariables(), ", ", "<", ">");
		if (!node.typeVariables().isEmpty()) space();
		visit(node.getRawTypeName());
		append("(");
		visitAll(node.parameters(), ", ", "", "");
		append(")");
		space();
		if (!node.thrownTypeReferences().isEmpty()) {
			keyword("throws");
			visitAll(node.thrownTypeReferences(), ", ", " ", " ");
		}
		startSuppressBlock();
		visit(node.getRawBody());
		if (node.getRawBody() == null) {
			append(";");
		}
		endSuppressBlock();
		closeBlock();
		
		return true;
	}
	
	public boolean visitSuperConstructorInvocation(SuperConstructorInvocation node) {
		buildBlock(node);
		if (node.getRawQualifier() != null) {
			visit(node.getRawQualifier());
			append(".");
		}
		visit(node.getRawConstructorTypeArguments());
		keyword("super");
		append("(");
		visitAll(node.arguments(), ", ", "", "");
		append(")");
		append(";");
		closeBlock();
		return true;
	}
	
	public boolean visitAlternateConstructorInvocation(AlternateConstructorInvocation node) {
		buildBlock(node);
		visit(node.getRawConstructorTypeArguments());
		keyword("this");
		append("(");
		visitAll(node.arguments(), ", ", "", "");
		append(")");
		append(";");
		closeBlock();
		return true;
	}
	
	public boolean visitInstanceInitializer(InstanceInitializer node) {
		buildBlock(node);
		startSuppressBlock();
		visit(node.getRawBody());
		startSuppressBlock();
		closeBlock();
		return true;
	}
	
	public boolean visitStaticInitializer(StaticInitializer node) {
		buildBlock(node);
		keyword("static");
		space();
		startSuppressBlock();
		visit(node.getRawBody());
		startSuppressBlock();
		closeBlock();
		return true;
	}
	
	public boolean visitClassDeclaration(ClassDeclaration node) {
		buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).keywords().isEmpty()) {
				space();
			}
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
		startSuppressBlock();
		visit(node.getRawBody());
		endSuppressBlock();
		closeBlock();
		return true;
	}
	
	public boolean visitInterfaceDeclaration(InterfaceDeclaration node) {
		buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).keywords().isEmpty()) {
				space();
			}
		}
		keyword("interface");
		space();
		visit(node.getRawName());
		visitAll(node.typeVariables(), ", ", "<", ">");
		space();
		if (!node.extending().isEmpty()) {
			keyword("extends");
			visitAll(node.extending(), ", ", " ", " ");
		}
		startSuppressBlock();
		visit(node.getRawBody());
		endSuppressBlock();
		closeBlock();
		return true;
	}
	
	public boolean visitEnumDeclaration(EnumDeclaration node) {
		buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).keywords().isEmpty()) {
				space();
			}
		}
		keyword("enum");
		space();
		visit(node.getRawName());
		space();
		if (!node.implementing().isEmpty()) {
			keyword("implements");
			visitAll(node.implementing(), ", ", " ", " ");
		}
		
		//logic of printing enum constants is in visitTypeBody
		startSuppressBlock();
		visit(node.getRawBody());
		endSuppressBlock();
		closeBlock();
		return true;
	}
	
	public boolean visitEnumConstant(EnumConstant node) {
		buildInline(node);
		visitAll(node.annotations(), "", "", "");
		visit(node.getRawName());
		visitAll(node.arguments(), ", ", "(", ")");
		if (node.getRawBody() != null) {
			space();
			visit(node.getRawBody());
		}
		closeInline();
		return true;
	}
	
	public boolean visitAnnotationDeclaration(AnnotationDeclaration node) {
		buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).keywords().isEmpty()) {
				space();
			}
		}
		append("@");
		keyword("interface");
		space();
		visit(node.getRawName());
		space();
		startSuppressBlock();
		visit(node.getRawBody());
		endSuppressBlock();
		closeBlock();
		return true;
	}
	
	public boolean visitAnnotationMethodDeclaration(AnnotationMethodDeclaration node) {
		buildBlock(node);
		if (node.getRawModifiers() != null) {
			visit(node.getRawModifiers());
			if (node.getRawModifiers() instanceof Modifiers && !((Modifiers)node.getRawModifiers()).keywords().isEmpty()) {
				space();
			}
		}
		visit(node.getRawReturnTypeReference());
		space();
		visit(node.getRawMethodName());
		append("(");
		append(")");
		if (node.getRawDefaultValue() != null) {
			space();
			keyword("default");
			space();
			visit(node.getRawDefaultValue());
		}
		append(";");
		closeBlock();
		return true;
	}
	
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
	public boolean visitParseArtefact(Node node) {
		buildInline(node);
		fail("ARTEFACT: " + node.getClass().getSimpleName());
		closeInline();
		return true;
	}
	
	public boolean visitComment(Comment node) {
		buildBlock(node);
		append(node.isBlockComment() ? "/*" : "//");
		if (node.getContent() == null) fail("MISSING_COMMENT");
		else append(node.getContent());
		if (node.isBlockComment()) append("*/");
		closeBlock();
		return true;
	}
	
	public void setTimeTaken(long taken) {
		super.setTimeTaken(taken);
	}
}
