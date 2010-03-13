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
package lombok.ast.ecj;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

import lombok.ast.BinaryOperator;
import lombok.ast.UnaryOperator;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.AssertStatement;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.BreakStatement;
import org.eclipse.jdt.internal.compiler.ast.CaseStatement;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.CharLiteral;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Clinit;
import org.eclipse.jdt.internal.compiler.ast.CombinedBinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ExtendedStringLiteral;
import org.eclipse.jdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.FloatLiteral;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.IntLiteralMinValue;
import org.eclipse.jdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LongLiteral;
import org.eclipse.jdt.internal.compiler.ast.LongLiteralMinValue;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.jdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedSuperReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.StringLiteralConcatenation;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.SynchronizedStatement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;

import com.google.common.collect.Maps;

/**
 * Turns {@code lombok.ast} based ASTs into eclipse/ecj's {@code org.eclipse.jdt.internal.compiler.ast.ASTNode} model.
 */
public class EcjTreeBuilder extends lombok.ast.ForwardingAstVisitor {
	private List<? extends ASTNode> result = null;
	private final lombok.ast.grammar.Source source;
	private final ProblemReporter reporter;
	private final CompilationResult compilationResult;
	private final CompilerOptions options;
	
	public EcjTreeBuilder(lombok.ast.grammar.Source source, CompilerOptions options) {
		this.options = options;
		IErrorHandlingPolicy policy = new IErrorHandlingPolicy() {
			public boolean proceedOnErrors() {
				return true;
			}
			
			public boolean stopOnFirstError() {
				return false;
			}
		};
		
		this.source = source;
		this.reporter = new ProblemReporter(policy, options, new DefaultProblemFactory(Locale.ENGLISH));
		this.compilationResult = new CompilationResult(source.getName().toCharArray(), 0, 0, 0);
	}
	
	private EcjTreeBuilder(EcjTreeBuilder parent) {
		this.source = parent.source;
		this.reporter = parent.reporter;
		this.options = parent.options;
		this.compilationResult = parent.compilationResult;
	}
	
	private EcjTreeBuilder create() {
		return new EcjTreeBuilder(this);
	}
	
	private Expression toExpression(lombok.ast.Node node) {
		return (Expression) toTree(node);
	}
	
	private ASTNode toTree(lombok.ast.Node node) {
		if (node == null) return null;
		EcjTreeBuilder visitor = create();
		node.accept(visitor);
		try {
			return visitor.get();
		} catch (RuntimeException e) {
			System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
			throw e;
		}
	}
	
	private char[] toName(lombok.ast.Identifier node) {
		return node.getName().toCharArray();
	}
	
	private <T extends ASTNode> T[] toList(Class<T> type, List<T> list) {
		if (list.isEmpty()) return null;
		@SuppressWarnings("unchecked")
		T[] emptyArray = (T[]) Array.newInstance(type, 0);
		return list.toArray(emptyArray);
	}
	
	private <T extends ASTNode> T[] toList(Class<T> type, lombok.ast.StrictListAccessor<?, ?> accessor) {
		List<T> list = new ArrayList<T>();
		for (lombok.ast.Node node : accessor) {
			EcjTreeBuilder visitor = create();
			node.accept(visitor);
			
			List<? extends ASTNode> values;
			
			values = visitor.getAll();
			
			for (ASTNode value : values) {
				if (value != null && !type.isInstance(value)) {
					throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + type.getName());
				}
				list.add(type.cast(value));
			}
		}
		
		return toList(type, list);
	}
	
	private <T extends ASTNode> List<T> toList(Class<T> type, lombok.ast.Node node) {
		if (node == null) return new ArrayList<T>();
		EcjTreeBuilder visitor = create();
		node.accept(visitor);
		@SuppressWarnings("unchecked")
		List<T> all = (List<T>)visitor.getAll();
		return new ArrayList<T>(all);
	}
	
	public ASTNode get() {
		return result.isEmpty() ? null : result.get(0);
	}
	
	public List<? extends ASTNode> getAll() {
		return result;
	}
	
	private boolean set(lombok.ast.Node node, ASTNode value) {
		if (result != null) throw new IllegalStateException("result is already set");
		
		if (node instanceof lombok.ast.Expression) {
			int parens = ((lombok.ast.Expression)node).getIntendedParens();
			value.bits |= (parens << ASTNode.ParenthesizedSHIFT) & ASTNode.ParenthesizedMASK;
		}
		List<ASTNode> result = new ArrayList<ASTNode>();
		if (value != null) result.add(value);
		this.result = result;
		return true;
	}
	
	private boolean set(lombok.ast.Node node, List<? extends ASTNode> values) {
		if (values.isEmpty()) System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());

		if (result != null) throw new IllegalStateException("result is already set");
		result = values;
		return true;
	}
	
	@Override
	public boolean visitCompilationUnit(lombok.ast.CompilationUnit node) {
		int sourceLength = source.getRawInput() == null ? 0 : source.getRawInput().length();
		CompilationUnitDeclaration cud = new CompilationUnitDeclaration(this.reporter, compilationResult, sourceLength);
		cud.bits |= ASTNode.HasAllMethodBodies;
		
		cud.currentPackage = (ImportReference) toTree(node.getPackageDeclaration());
		cud.imports = toList(ImportReference.class, node.importDeclarations());
		cud.types = toList(TypeDeclaration.class, node.typeDeclarations());
		
		return set(node, cud);
	}
	
	@Override
	public boolean visitPackageDeclaration(lombok.ast.PackageDeclaration node) {
		//TODO handle annotations.
		return set(node, new ImportReference(chain(node.parts()), new long[node.parts().size()], true, ClassFileConstants.AccDefault));
	}
	
	@Override
	public boolean visitImportDeclaration(lombok.ast.ImportDeclaration node) {
		int staticFlag = node.isStaticImport() ? ClassFileConstants.AccStatic : ClassFileConstants.AccDefault;
		return set(node, new ImportReference(chain(node.parts()), new long[node.parts().size()], node.isStarImport(), staticFlag));
	}
	
	@Override
	public boolean visitClassDeclaration(lombok.ast.ClassDeclaration node) {
		TypeDeclaration decl = new TypeDeclaration(compilationResult);
		if (node.getBody().members().isEmpty()) decl.bits |= ASTNode.UndocumentedEmptyBlock;
		
		boolean hasExplicitConstructor = false;
		List<AbstractMethodDeclaration> methods = new ArrayList<AbstractMethodDeclaration>();
		List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
		List<TypeDeclaration> types = new ArrayList<TypeDeclaration>();
		for (lombok.ast.TypeMember member : node.getBody().members()) {
			if (member instanceof lombok.ast.ConstructorDeclaration) {
				hasExplicitConstructor = true;
				methods.add((AbstractMethodDeclaration) toTree(member));
			} else if (member instanceof lombok.ast.MethodDeclaration) {
				methods.add((AbstractMethodDeclaration) toTree(member));
			} else if (member instanceof lombok.ast.VariableDeclaration) {
				fields.add((FieldDeclaration) toTree(member));
			} else if (member instanceof lombok.ast.StaticInitializer) {
				fields.add((FieldDeclaration) toTree(member));
			} else if (member instanceof lombok.ast.InstanceInitializer) {
				fields.add((FieldDeclaration) toTree(member));
			} else if (member instanceof lombok.ast.TypeDeclaration) {
				TypeDeclaration innerType = (TypeDeclaration) toTree(member);
				//TODO check if you need to do this too for static inners.
				if (innerType != null) {
					innerType.enclosingType = decl;
					types.add(innerType);
				}
			}
		}
		
		decl.name = toName(node.getName());
		if (node.hasParent()) {
			if (node.getParent() instanceof lombok.ast.CompilationUnit) {
				char[] mainTypeName = new CompilationUnitDeclaration(reporter, compilationResult, 0).getMainTypeName();
				if (!CharOperation.equals(decl.name, mainTypeName)) {
					decl.bits |= ASTNode.IsSecondaryType;
				}
			} else if (node.getParent() instanceof lombok.ast.TypeBody) {
				decl.bits |= ASTNode.IsMemberType;
			} else {
				decl.bits |= ASTNode.IsLocalType;
			}
		}
		
		decl.modifiers = toModifiers(node.getModifiers());
		
		if (!hasExplicitConstructor) {
			ConstructorDeclaration defaultConstructor = new ConstructorDeclaration(compilationResult);
			defaultConstructor.bits |= ASTNode.IsDefaultConstructor;
			defaultConstructor.constructorCall = new ExplicitConstructorCall(ExplicitConstructorCall.ImplicitSuper);
			defaultConstructor.modifiers = decl.modifiers & 7;
			methods.add(0, defaultConstructor);
		}
		
		decl.memberTypes = toList(TypeDeclaration.class, types);
		decl.methods = toList(AbstractMethodDeclaration.class, methods);
		decl.fields = toList(FieldDeclaration.class, fields);
		decl.annotations = null; //TODO
		decl.superclass = (TypeReference) toTree(node.getExtending());
		decl.superInterfaces = toList(TypeReference.class, node.implementing());
		decl.typeParameters = toList(TypeParameter.class, node.typeVariables());
		
		decl.addClinit();
		
		//TODO test inner types. Give em everything - (abstract) methods, initializers, static initializers, MULTIPLE initializers.
		
		
		return set(node, decl);
	}
	
	@Override
	public boolean visitVariableDeclaration(lombok.ast.VariableDeclaration node) {
		List<AbstractVariableDeclaration> values = new ArrayList<AbstractVariableDeclaration>();
		
		boolean isFieldDecls = node.getParent() instanceof lombok.ast.TypeDeclaration;
		
		Annotation[] annotations = null; //TODO
		int modifiers = toModifiers(node.getDefinition().getModifiers());
		
		for (lombok.ast.VariableDefinitionEntry entry : node.getDefinition().variables()) {
			AbstractVariableDeclaration decl = isFieldDecls ? new FieldDeclaration() : new LocalDeclaration(null, 0, 0);
			decl.annotations = annotations;
			decl.initialization = toExpression(entry.getInitializer());
			decl.modifiers = modifiers;
			decl.name = toName(entry.getName());
			decl.type = (TypeReference) toTree(entry.getEffectiveTypeReference());
			values.add(decl);
		}
		
		return set(node, values);
	}
	
	@Override
	public boolean visitUnaryExpression(lombok.ast.UnaryExpression node) {
		if (node.getOperator() == UnaryOperator.UNARY_MINUS) {
			if (node.getOperand() instanceof lombok.ast.IntegralLiteral && node.getOperand().getParens() == 0) {
				lombok.ast.IntegralLiteral lit = (lombok.ast.IntegralLiteral)node.getOperand();
				if (!lit.isMarkedAsLong() && lit.intValue() == Integer.MIN_VALUE) {
					return set(node, new IntLiteralMinValue());
				}
				if (lit.isMarkedAsLong() && lit.longValue() == Long.MIN_VALUE) {
					return set(node, new LongLiteralMinValue());
				}
			}
		}
		
		Expression operand = toExpression(node.getOperand());
		int ecjOperator = UNARY_OPERATORS.get(node.getOperator());
		
		switch (node.getOperator()) {
		case PREFIX_INCREMENT:
		case PREFIX_DECREMENT:
			return set(node, new PrefixExpression(operand, IntLiteral.One, ecjOperator, 0));
		case POSTFIX_INCREMENT:
		case POSTFIX_DECREMENT:
			return set(node, new PostfixExpression(operand, IntLiteral.One, ecjOperator, 0));
		default:
			return set(node, new UnaryExpression(toExpression(node.getOperand()), ecjOperator));
		}
	}
	
	@Override
	public boolean visitExpressionStatement(lombok.ast.ExpressionStatement node) {
		return set(node, (Statement)toTree(node.getExpression()));
	}
	
	@Override
	public boolean visitMethodInvocation(lombok.ast.MethodInvocation node) {
		MessageSend inv = new MessageSend();
		
		inv.arguments = toList(Expression.class, node.arguments());
		inv.receiver = toExpression(node.getOperand());
		//TODO check if getMethodTypeArguments() should perhaps be never null.
		if (node.getMethodTypeArguments() != null) inv.typeArguments = toList(TypeReference.class, node.getMethodTypeArguments().generics());
		inv.selector = toName(node.getName());
		return set(node, inv);
	}
	
	@Override
	public boolean visitBinaryExpression(lombok.ast.BinaryExpression node) {
		Expression base = visitBinaryExpression0(node);
		if (!(base instanceof BinaryExpression)) {
			return set(node, base);
		}
		
		BinaryExpression binExpr = (BinaryExpression) base;
		int op = opForBinaryExpression(binExpr);
		
		if (binExpr.left instanceof BinaryExpression && opForBinaryExpression((BinaryExpression)binExpr.left) == op) {
			CombinedBinaryExpression parent = null;
			int arity = 0;
			Expression newLeft = binExpr.left;
			if (binExpr.left instanceof CombinedBinaryExpression) {
				parent = (CombinedBinaryExpression) binExpr.left;
				arity = parent.arity;
				newLeft = new BinaryExpression(parent.left, parent.right, op);
			}
			CombinedBinaryExpression newBase = new CombinedBinaryExpression(newLeft, binExpr.right, op, 0);
			newBase.arity = arity+1;
			return set(node, newBase);
		}
		
		return set(node, base);
	}
	
	private static int opForBinaryExpression(BinaryExpression binExpr) {
		return (binExpr.bits & ASTNode.OperatorMASK) >>> ASTNode.OperatorSHIFT;
	}
	
	private Expression visitBinaryExpression0(lombok.ast.BinaryExpression node) {
		Expression lhs = toExpression(node.getLeft());
		Expression rhs = toExpression(node.getRight());
		
		if (node.getOperator() == BinaryOperator.ASSIGN) {
			return new Assignment(lhs, rhs, 0);
		}
		
		//TODO add a test with 1 + 2 + 3 + "" + 4 + 5 + 6 + "foo"; as well as 5 + 2 + 3 - 5 - 8 -7 - 8 * 10 + 20;
		
		int ecjOperator = BINARY_OPERATORS.get(node.getOperator());
		if (node.getOperator().isAssignment()) {
			return new CompoundAssignment(lhs, rhs, ecjOperator, 0);
		} else if (node.getOperator() == BinaryOperator.EQUALS || node.getOperator() == BinaryOperator.NOT_EQUALS) {
			return new EqualExpression(lhs, rhs, ecjOperator);
		} else if (node.getOperator() == BinaryOperator.LOGICAL_AND) {
			return new AND_AND_Expression(lhs, rhs, ecjOperator);
		} else if (node.getOperator() == BinaryOperator.LOGICAL_OR) {
			return new OR_OR_Expression(lhs, rhs, ecjOperator);
		} else if (node.getOperator() == BinaryOperator.PLUS && node.getLeft().getParens() == 0) {
			if (lhs instanceof ExtendedStringLiteral) {
				if (rhs instanceof CharLiteral) {
					return ((ExtendedStringLiteral)lhs).extendWith((CharLiteral)rhs);
				} else if (rhs instanceof StringLiteral) {
					return ((ExtendedStringLiteral)lhs).extendWith((StringLiteral)rhs);
				}
			} else if (lhs instanceof StringLiteral) {
				if (rhs instanceof CharLiteral) {
					return new ExtendedStringLiteral((StringLiteral)lhs, (CharLiteral)rhs);
				} else if (rhs instanceof StringLiteral) {
					return new ExtendedStringLiteral((StringLiteral)lhs, (StringLiteral)rhs);
				}
			}
		}
		
		return new BinaryExpression(lhs, rhs, ecjOperator);
	}
	
	@Override
	public boolean visitCast(lombok.ast.Cast node) {
		//TODO try stuffing every possible eclipsian type style in a cast, because the stupid thing treats it as a name reference. D'oh.
		Expression typeRef = toExpression(node.getTypeReference());
		if (typeRef.getClass() == SingleTypeReference.class && !node.getTypeReference().isPrimitive()) {
			SingleTypeReference str = (SingleTypeReference) typeRef;
			//Why you ask? I don't know. It seems dumb. Ask the ecj guys.
			typeRef = new SingleNameReference(str.token, 0);
			typeRef.sourceStart = str.sourceStart;
			typeRef.sourceEnd = str.sourceEnd;
			typeRef.bits = (typeRef.bits & ~Binding.VARIABLE) | Binding.TYPE;
		} else if (typeRef.getClass() == QualifiedTypeReference.class) {
			QualifiedTypeReference qtr = (QualifiedTypeReference) typeRef;
			//Same here, but for the more complex types, they stay types.
			typeRef = new QualifiedNameReference(qtr.tokens, qtr.sourcePositions, qtr.sourceStart, qtr.sourceEnd);
			typeRef.bits = (typeRef.bits & ~Binding.VARIABLE) | Binding.TYPE;
		}
		return set(node, new CastExpression(toExpression(node.getOperand()), typeRef));
	}
	
	@Override
	public boolean visitInstanceOf(lombok.ast.InstanceOf node) {
		return set(node, new InstanceOfExpression(toExpression(node.getObjectReference()), (TypeReference) toTree(node.getTypeReference())));
	}
	
	@Override
	public boolean visitInlineIfExpression(lombok.ast.InlineIfExpression node) {
		return set(node, new ConditionalExpression(toExpression(node.getCondition()), toExpression(node.getIfTrue()), toExpression(node.getIfFalse())));
	}
	
	@Override
	public boolean visitConstructorInvocation(lombok.ast.ConstructorInvocation node) {
		//TODO handle AICL.
		AllocationExpression inv;
		if (node.getQualifier() != null) {
			inv = new QualifiedAllocationExpression();
			((QualifiedAllocationExpression)inv).enclosingInstance = toExpression(node.getQualifier());
		} else {
			inv = new AllocationExpression();
		}
		
		inv.arguments = toList(Expression.class, node.arguments());
		inv.type = (TypeReference) toTree(node.getTypeReference());
		//TODO investigate if this thing should perhaps never be null.
		if (node.getConstructorTypeArguments() != null) {
			inv.typeArguments = toList(TypeReference.class, node.getConstructorTypeArguments().generics());
		}
		return set(node, inv);
	}
	
	@Override
	public boolean visitSelect(lombok.ast.Select node) {
		//TODO for something like ("" + "").foo.bar;
		/* try chain-of-identifiers */ {
			List<lombok.ast.Identifier> selects = new ArrayList<lombok.ast.Identifier>();
			lombok.ast.Select current = node;
			while (true) {
				selects.add(current.getIdentifier());
				if (current.getOperand() instanceof lombok.ast.Select) current = (lombok.ast.Select) current.getOperand();
				else if (current.getOperand() instanceof lombok.ast.Identifier) {
					selects.add((lombok.ast.Identifier) current.getOperand());
					Collections.reverse(selects);
					char[][] tokens = chain(selects, selects.size());
					QualifiedNameReference ref = new QualifiedNameReference(tokens, new long[tokens.length], 0, 0);
					ref.bits &= ~Binding.TYPE;
					return set(node, ref);
				} else {
					break;
				}
			}
		}
		
		throw new RuntimeException("Select fail");
	}
	
	private static final EnumMap<UnaryOperator, Integer> UNARY_OPERATORS = Maps.newEnumMap(UnaryOperator.class);
	static {
		UNARY_OPERATORS.put(UnaryOperator.BINARY_NOT, OperatorIds.TWIDDLE);
		UNARY_OPERATORS.put(UnaryOperator.LOGICAL_NOT, OperatorIds.NOT); 
		UNARY_OPERATORS.put(UnaryOperator.UNARY_PLUS, OperatorIds.PLUS);
		UNARY_OPERATORS.put(UnaryOperator.PREFIX_INCREMENT, OperatorIds.PLUS); 
		UNARY_OPERATORS.put(UnaryOperator.UNARY_MINUS, OperatorIds.MINUS);
		UNARY_OPERATORS.put(UnaryOperator.PREFIX_DECREMENT, OperatorIds.MINUS); 
		UNARY_OPERATORS.put(UnaryOperator.POSTFIX_INCREMENT, OperatorIds.PLUS); 
		UNARY_OPERATORS.put(UnaryOperator.POSTFIX_DECREMENT, OperatorIds.MINUS);
	}
	
	private static final EnumMap<BinaryOperator, Integer> BINARY_OPERATORS = Maps.newEnumMap(BinaryOperator.class);
	static {
		BINARY_OPERATORS.put(BinaryOperator.PLUS_ASSIGN, OperatorIds.PLUS);
		BINARY_OPERATORS.put(BinaryOperator.MINUS_ASSIGN, OperatorIds.MINUS);
		BINARY_OPERATORS.put(BinaryOperator.MULTIPLY_ASSIGN, OperatorIds.MULTIPLY);
		BINARY_OPERATORS.put(BinaryOperator.DIVIDE_ASSIGN, OperatorIds.DIVIDE);
		BINARY_OPERATORS.put(BinaryOperator.REMAINDER_ASSIGN, OperatorIds.REMAINDER);
		BINARY_OPERATORS.put(BinaryOperator.AND_ASSIGN, OperatorIds.AND);
		BINARY_OPERATORS.put(BinaryOperator.XOR_ASSIGN, OperatorIds.XOR);
		BINARY_OPERATORS.put(BinaryOperator.OR_ASSIGN, OperatorIds.OR);
		BINARY_OPERATORS.put(BinaryOperator.SHIFT_LEFT_ASSIGN, OperatorIds.LEFT_SHIFT);
		BINARY_OPERATORS.put(BinaryOperator.SHIFT_RIGHT_ASSIGN, OperatorIds.RIGHT_SHIFT);
		BINARY_OPERATORS.put(BinaryOperator.BITWISE_SHIFT_RIGHT_ASSIGN, OperatorIds.UNSIGNED_RIGHT_SHIFT);
		BINARY_OPERATORS.put(BinaryOperator.LOGICAL_OR, OperatorIds.OR_OR);
		BINARY_OPERATORS.put(BinaryOperator.LOGICAL_AND, OperatorIds.AND_AND);
		BINARY_OPERATORS.put(BinaryOperator.BITWISE_OR, OperatorIds.OR);
		BINARY_OPERATORS.put(BinaryOperator.BITWISE_XOR, OperatorIds.XOR);
		BINARY_OPERATORS.put(BinaryOperator.BITWISE_AND, OperatorIds.AND);
		BINARY_OPERATORS.put(BinaryOperator.EQUALS, OperatorIds.EQUAL_EQUAL);
		BINARY_OPERATORS.put(BinaryOperator.NOT_EQUALS, OperatorIds.NOT_EQUAL);
		BINARY_OPERATORS.put(BinaryOperator.GREATER, OperatorIds.GREATER);
		BINARY_OPERATORS.put(BinaryOperator.GREATER_OR_EQUAL, OperatorIds.GREATER_EQUAL);
		BINARY_OPERATORS.put(BinaryOperator.LESS, OperatorIds.LESS);
		BINARY_OPERATORS.put(BinaryOperator.LESS_OR_EQUAL, OperatorIds.LESS_EQUAL);
		BINARY_OPERATORS.put(BinaryOperator.SHIFT_LEFT, OperatorIds.LEFT_SHIFT);
		BINARY_OPERATORS.put(BinaryOperator.SHIFT_RIGHT, OperatorIds.RIGHT_SHIFT);
		BINARY_OPERATORS.put(BinaryOperator.BITWISE_SHIFT_RIGHT, OperatorIds.UNSIGNED_RIGHT_SHIFT);
		BINARY_OPERATORS.put(BinaryOperator.PLUS, OperatorIds.PLUS);
		BINARY_OPERATORS.put(BinaryOperator.MINUS, OperatorIds.MINUS);
		BINARY_OPERATORS.put(BinaryOperator.MULTIPLY, OperatorIds.MULTIPLY);
		BINARY_OPERATORS.put(BinaryOperator.DIVIDE, OperatorIds.DIVIDE);
		BINARY_OPERATORS.put(BinaryOperator.REMAINDER, OperatorIds.REMAINDER);
	}
	
	@Override
	public boolean visitTypeReference(lombok.ast.TypeReference node) {
		// TODO make sure there's a test case that covers every eclipsian type ref: hierarchy on "org.eclipse.jdt.internal.compiler.ast.TypeReference".
		
		Wildcard wildcard = null;
		TypeReference ref = null;
		
		switch (node.getWildcard()) {
		case UNBOUND:
			return set(node, new Wildcard(Wildcard.UNBOUND));
		case EXTENDS:
			wildcard = new Wildcard(Wildcard.EXTENDS);
			break;
		case SUPER:
			wildcard = new Wildcard(Wildcard.SUPER);
			break;
		}
		
		char[][] qualifiedName = null;
		char[] singleName = null;
		boolean qualified = node.parts().size() != 1;
		int dims = node.getArrayDimensions();
		TypeReference[][] params = new TypeReference[node.parts().size()][];
		boolean hasGenerics = false;
		
		if (!qualified) {
			singleName = toName(node.parts().first().getIdentifier());
		} else {
			List<lombok.ast.Identifier> identifiers = new ArrayList<lombok.ast.Identifier>();
			for (lombok.ast.TypeReferencePart part : node.parts()) identifiers.add(part.getIdentifier());
			qualifiedName = chain(identifiers, identifiers.size());
		}
		
		{
			int ctr = 0;
			for (lombok.ast.TypeReferencePart part : node.parts()) {
				params[ctr] = new TypeReference[part.generics().size()];
				int ctr2 = 0;
				boolean partHasGenerics = false;
				for (lombok.ast.TypeReference x : part.generics()) {
					hasGenerics = true;
					partHasGenerics = true;
					params[ctr][ctr2++] = (TypeReference) toTree(x);
				}
				if (!partHasGenerics) params[ctr] = null;
				ctr++;
			}
		}
		
		if (!qualified) {
			if (!hasGenerics) {
				if (dims == 0) ref = new SingleTypeReference(singleName, 0L);
				else ref = new ArrayTypeReference(singleName, dims, 0L);
			} else {
				ref = new ParameterizedSingleTypeReference(singleName, params[0], dims, 0L);
			}
		} else {
			if (!hasGenerics) {
				if (dims == 0) ref = new QualifiedTypeReference(qualifiedName, new long[node.parts().size()]);
				else ref = new ArrayQualifiedTypeReference(qualifiedName, dims, new long[node.parts().size()]);
			} else {
				ref = new ParameterizedQualifiedTypeReference(qualifiedName, params, dims, new long[node.parts().size()]);
			}
		}
		
		if (ref == null) throw new RuntimeException("typeref fail.");
		
		if (wildcard != null) {
			wildcard.bound = ref;
			ref = wildcard;
		}
		
		return set(node, ref);
	}
	
	@Override
	public boolean visitTypeVariable(lombok.ast.TypeVariable node) {
		// TODO test multiple bounds on a variable, e.g. <T extends A & B & C>
		TypeParameter param = new TypeParameter();
		param.name = toName(node.getName());
		if (!node.extending().isEmpty()) {
			TypeReference[] p = toList(TypeReference.class, node.extending());
			for (TypeReference t : p) t.bits |= ASTNode.IsSuperType;
			param.type = p[0];
			if (p.length > 1) {
				param.bounds = new TypeReference[p.length - 1];
				System.arraycopy(p, 1, param.bounds, 0, p.length - 1);
			}
		}
		
		return set(node, param);
	}
	
	@Override
	public boolean visitInstanceInitializer(lombok.ast.InstanceInitializer node) {
		return set(node, new Initializer((Block) toTree(node.getBody()), 0));
	}
	
	@Override
	public boolean visitStaticInitializer(lombok.ast.StaticInitializer node) {
		return set(node, new Initializer((Block) toTree(node.getBody()), ClassFileConstants.AccStatic));
	}
	
	@Override
	public boolean visitIntegralLiteral(lombok.ast.IntegralLiteral node) {
		if (node.isMarkedAsLong()) {
			return set(node, new LongLiteral(node.getRawValue().toCharArray(), 0, 0));
		}
		return set(node, new IntLiteral(node.getRawValue().toCharArray(), 0, 0));
	}
	
	@Override
	public boolean visitFloatingPointLiteral(lombok.ast.FloatingPointLiteral node) {
		if (node.isMarkedAsFloat()) {
			return set(node, new FloatLiteral(node.getRawValue().toCharArray(), 0, 0));
		}
		return set(node, new DoubleLiteral(node.getRawValue().toCharArray(), 0, 0));
	}
	
	@Override
	public boolean visitBooleanLiteral(lombok.ast.BooleanLiteral node) {
		return set(node, node.getValue() ? new TrueLiteral(0, 0) : new FalseLiteral(0, 0));
	}
	
	@Override
	public boolean visitNullLiteral(lombok.ast.NullLiteral node) {
		return set(node, new NullLiteral(0, 0));
	}
	
	@Override
	public boolean visitIdentifier(lombok.ast.Identifier node) {
		SingleNameReference ref = new SingleNameReference(toName(node), 0L);
		ref.bits &= ~Binding.TYPE;
		return set(node, ref);
	}
	
	@Override public boolean visitCharLiteral(lombok.ast.CharLiteral node) {
		return set(node, new CharLiteral(node.getRawValue().toCharArray(), 0, 0));
	}
	
	@Override public boolean visitStringLiteral(lombok.ast.StringLiteral node) {
		return set(node, new StringLiteral(node.getValue().toCharArray(), 0, 0, 0));
	}
	
	@Override
	public boolean visitBlock(lombok.ast.Block node) {
		Block block = new Block(0);
		block.statements = toList(Statement.class, node.contents());
		if (block.statements == null) {
			block.bits |= ASTNode.UndocumentedEmptyBlock;
		} else {
			//TODO test what happens with vardecls in catch blocks and for each loops, as well as for inner blocks.
			block.explicitDeclarations = 0;
			for (lombok.ast.Statement s : node.contents()) if (s instanceof lombok.ast.VariableDeclaration) block.explicitDeclarations++;
		}
		return set(node, block);
	}
	
	@Override
	public boolean visitArrayInitializer(lombok.ast.ArrayInitializer node) {
		ArrayInitializer arrayInitializer = new ArrayInitializer();
		arrayInitializer.expressions = toList(Expression.class, node.expressions());
		return set(node, arrayInitializer);
	}
	
	@Override
	public boolean visitArrayCreation(lombok.ast.ArrayCreation node) {
		ArrayAllocationExpression aae = new ArrayAllocationExpression();
		aae.type = (TypeReference) toTree(node.getComponentTypeReference());
		aae.type.bits |= ASTNode.IgnoreRawTypeCheck;
		
		Expression[] dimensions = toList(Expression.class, node.dimensions());
		int dims = node.dimensions().size();
		if (dimensions == null) {
			dimensions = new Expression[dims];
		} else if (dimensions.length < dims) {
			dimensions = Arrays.copyOf(dimensions, dims);
		}
		
		aae.dimensions = dimensions;
		aae.initializer = (ArrayInitializer) toTree(node.getInitializer());
		return set(node, aae);
	}
	
	@Override
	public boolean visitArrayDimension(lombok.ast.ArrayDimension node) {
		return set(node, toExpression(node.getDimension()));
	}
	
	@Override
	public boolean visitThis(lombok.ast.This node) {
		if (node.getQualifier() == null) {
			return set(node, new ThisReference(0, 0));
		}
		return set(node, new QualifiedThisReference((TypeReference) toTree(node.getQualifier()), 0, 0));
	}
	
	@Override
	public boolean visitClassLiteral(lombok.ast.ClassLiteral node) {
		return set(node, new ClassLiteralAccess(0, (TypeReference) toTree(node.getTypeReference())));
	}
	
	@Override
	public boolean visitArrayAccess(lombok.ast.ArrayAccess node) {
		return set(node, new ArrayReference(toExpression(node.getOperand()), toExpression(node.getIndexExpression())));
	}
	
	@Override
	public boolean visitAssert(lombok.ast.Assert node) {
		//TODO check the flags after more test have been added: asserts in constructors, methods etc.
		if (node.getMessage() == null) {
			return set(node, new AssertStatement(toExpression(node.getAssertion()), 0));
		}
		return set(node, new AssertStatement(toExpression(node.getMessage()), toExpression(node.getAssertion()), 0));
	}
	
	@Override
	public boolean visitEmptyStatement(lombok.ast.EmptyStatement node) {
		return set(node, new EmptyStatement(0, 0));
	}
	
	@Override
	public boolean visitEmptyDeclaration(lombok.ast.EmptyDeclaration node) {
		return set(node, (ASTNode)null);
	}
	
	private int toModifiers(lombok.ast.Modifiers modifiers) {
		return modifiers.getExplicitModifierFlags();
	}
	
	@Override
	public boolean visitNode(lombok.ast.Node node) {
		throw new UnsupportedOperationException(String.format("Unhandled node '%s' (%s)", node, node.getClass().getSimpleName()));
	}
	
	private char[][] chain(lombok.ast.StrictListAccessor<lombok.ast.Identifier, ?> parts) {
		return chain(parts, parts.size());
	}
	
	private char[][] chain(Iterable<lombok.ast.Identifier> parts, int size) {
		char[][] c = new char[size][];
		int i = 0;
		for (lombok.ast.Identifier part : parts) {
			c[i++] = part.getName().toCharArray();
		}
		return c;
	}
	
	private Expression dummy() {
		return new StringLiteral("dummy".toCharArray(), 0, 0, 0);
	}
}
