package lombok.ast.ecj;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

import lombok.ast.StringLiteral;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
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
import org.eclipse.jdt.internal.compiler.ast.Javadoc;
import org.eclipse.jdt.internal.compiler.ast.JavadocAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.JavadocArgumentExpression;
import org.eclipse.jdt.internal.compiler.ast.JavadocArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocArraySingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocFieldReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocImplicitTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocMessageSend;
import org.eclipse.jdt.internal.compiler.ast.JavadocQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LongLiteral;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.OR_OR_Expression;
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
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;

public class ASTNodePrinter extends ASTVisitor {
	private final StringBuilder output = new StringBuilder();
	private int indent;
	private String rel;
	
	enum Scopes{
		COMPILATION_UNIT {
			void traverse0(ASTVisitor visitor, ASTNode node) throws Exception {
				node.getClass().getMethod("traverse", ASTVisitor.class, CompilationUnitScope.class).invoke(node, visitor, null);
			}
		},
		CLASS {
			void traverse0(ASTVisitor visitor, ASTNode node) throws Exception {
				node.getClass().getMethod("traverse", ASTVisitor.class, ClassScope.class).invoke(node, visitor, null);
			}
		},
		METHOD {
			void traverse0(ASTVisitor visitor, ASTNode node) throws Exception {
				try {
					node.getClass().getMethod("traverse", ASTVisitor.class, MethodScope.class).invoke(node, visitor, null);
				}
				catch (NoSuchMethodException e) {
					BLOCK.traverse0(visitor, node);
				}
			}
		},
		BLOCK {
			void traverse0(ASTVisitor visitor, ASTNode node) throws Exception {
				node.getClass().getMethod("traverse", ASTVisitor.class, BlockScope.class).invoke(node, visitor, null);
			}
		};
		
		abstract void traverse0(ASTVisitor visitor, ASTNode node) throws Exception;
		void traverse(ASTVisitor visitor, ASTNode node) {
			try {
				traverse0(visitor, node);
			}
			catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException) {
					throw ((RuntimeException)cause);
				}
				if (cause instanceof Error) {
					throw ((Error)cause);
				}
				throw new RuntimeException(cause);
			}
			catch (Exception cause) {
				if (cause instanceof RuntimeException) {
					throw ((RuntimeException)cause);
				}
				throw new RuntimeException(cause);
			}
		}
	}
	
	@Override
	public String toString() {
		return output.toString();
	}
	
	
	private void printNode(ASTNode nodeKind) {
		printNode(nodeKind == null ? "NULL" : nodeKind.getClass().getSimpleName());
		if (nodeKind != null) {
			property("bits", nodeKind.bits);
		}
	}
	
	private void printNode(String nodeKind) {
		printIndent();
		if (rel != null)
			output.append(rel).append(": ");
		rel = null;
		output.append("[").append(nodeKind).append("]\n");
		indent++;
	}
	
	private void printIndent() {
		for (int i = 0; i < indent; i++) {
			output.append("\t");
		}
	}
	
	private void property(String rel, Object val) {
		printIndent();
		if (rel != null)
			output.append(rel).append(": ");
		if (val instanceof ASTNode)
			output.append("!!ASTN-AS-PROP!!");
		if (val == null) {
			output.append("[NULL]\n");
		} else {
			String content;
			if (val instanceof char[]) {
				content = "= " + new StringLiteral().setValue(new String((char[])val)).getRawValue();
			} else if (val instanceof char[][]) {
				StringBuilder sb = new StringBuilder();
				for (char[] single : ((char[][])val)) {
					if (sb.length() != 0) {
						sb.append(", ");
					}
					sb.append(new StringLiteral().setValue(new String((char[])single)).getRawValue());
				}
				content = "= {" + sb.toString() + "}";
			} else {
				content = String.valueOf(val);
			}
			output.append("[").append(val.getClass().getSimpleName()).append(" ").append(content).append("]\n");
		}
	}
	
	private void child(String rel, ASTNode node, Scopes scopes) {
		this.rel = rel;
		if (node != null) {
			scopes.traverse(this, node);
		} else {
			printNode("NULL");
			indent--;
		}
	}
	
	private void children(String rel, Object nodes, Scopes scopes) {
		this.rel = rel;
		
		if (nodes == null) {
			printNode("ARRAYNULL");
			indent--;
			return;
		}
		
		if (!nodes.getClass().isArray()) {
			throw new IllegalArgumentException("nodes is not an array but a " + nodes.getClass().getName()); 
		}
		if (!ASTNode.class.isAssignableFrom(nodes.getClass().getComponentType())) {
			throw new IllegalArgumentException("nodes does not contain ASTNode-s but " + nodes.getClass().getComponentType().getName());
		}
		int size = Array.getLength(nodes);
		if (size == 0) {
			printNode("ARRAYEMPTY");
			indent--;
		} else {
			for (int i = 0; i < size; i++) {
				child(String.format("%s[%d]", rel, i), (ASTNode)Array.get(nodes, i), scopes);
			}
		}
	}
	
	@Override
	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// // return false;
	}
	
	@Override
	public boolean visit(AND_AND_Expression andAndExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(AnnotationMethodDeclaration annotationTypeDeclaration, ClassScope classScope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(Argument argument, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(Argument argument, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(AssertStatement assertStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(Assignment assignment, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(Block block, BlockScope scope) {
		printNode(block);
		children("statements", block.statements, Scopes.BLOCK);
		indent--;
		return false;
	}
	
	@Override
	public boolean visit(BreakStatement breakStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(CaseStatement caseStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(CastExpression castExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(CharLiteral charLiteral, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(Clinit clinit, ClassScope scope) {
		printNode(clinit);
		indent--;
		return false;
	}
	
	@Override
	public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		printNode(compilationUnitDeclaration);
		child("currentPackage", compilationUnitDeclaration.currentPackage, Scopes.COMPILATION_UNIT);
		children("imports", compilationUnitDeclaration.imports, Scopes.COMPILATION_UNIT);
		children("types", compilationUnitDeclaration.types, Scopes.COMPILATION_UNIT);
		indent--;
		return false;
	}
	
	@Override
	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ConditionalExpression conditionalExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
		printNode(constructorDeclaration);
		child("javadoc", constructorDeclaration.javadoc, Scopes.METHOD);
		children("annotations", constructorDeclaration.annotations, Scopes.METHOD);
		children("typeParameters", constructorDeclaration.typeParameters, Scopes.METHOD);
		children("arguments", constructorDeclaration.arguments, Scopes.METHOD);
		children("thrownExceptions", constructorDeclaration.thrownExceptions, Scopes.METHOD);
		child("constructorCall", constructorDeclaration.constructorCall, Scopes.METHOD);
		children("statements", constructorDeclaration.statements, Scopes.METHOD);
		indent--;
		return false;
	}
	
	@Override
	public boolean visit(ContinueStatement continueStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(DoStatement doStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(EmptyStatement emptyStatement, BlockScope scope) {
		printNode(emptyStatement);
		indent--;
		return false;
	}
	
	@Override
	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
		printNode(explicitConstructor);
		property("accessMode", explicitConstructor.accessMode);
		child("qualification", explicitConstructor.qualification, Scopes.BLOCK);
		children("typeArguments", explicitConstructor.typeArguments, Scopes.BLOCK);
		children("arguments", explicitConstructor.arguments, Scopes.BLOCK);
		indent--;
		return false;
	}
	
	@Override
	public boolean visit(ExtendedStringLiteral extendedStringLiteral, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(FieldReference fieldReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(FloatLiteral floatLiteral, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ForeachStatement forStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ForStatement forStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
		printNode(importRef);
		property("tokens", importRef.tokens);
		indent--;
		return false;
	}
	
	@Override
	public boolean visit(Initializer initializer, MethodScope scope) {
		printNode(initializer);
		property("modifiers", initializer.modifiers);
		child("block", initializer.block, Scopes.METHOD);
		indent--;
		return false;
	}
	
	@Override
	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(Javadoc javadoc, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(Javadoc javadoc, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocAllocationExpression expression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocAllocationExpression expression, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocArgumentExpression expression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocArgumentExpression expression, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocArrayQualifiedTypeReference typeRef, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocArrayQualifiedTypeReference typeRef, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocArraySingleTypeReference typeRef, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocArraySingleTypeReference typeRef, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocFieldReference fieldRef, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocFieldReference fieldRef, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocImplicitTypeReference implicitTypeReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocImplicitTypeReference implicitTypeReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocMessageSend messageSend, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocMessageSend messageSend, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocQualifiedTypeReference typeRef, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocQualifiedTypeReference typeRef, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocReturnStatement statement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocReturnStatement statement, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocSingleNameReference argument, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocSingleNameReference argument, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocSingleTypeReference typeRef, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(JavadocSingleTypeReference typeRef, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(LabeledStatement labeledStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(LongLiteral longLiteral, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(MarkerAnnotation annotation, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(MemberValuePair pair, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(NormalAnnotation annotation, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(OR_OR_Expression orOrExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ParameterizedSingleTypeReference parameterizedSingleTypeReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ParameterizedSingleTypeReference parameterizedSingleTypeReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(QualifiedNameReference qualifiedNameReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(QualifiedSuperReference qualifiedSuperReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(QualifiedSuperReference qualifiedSuperReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(QualifiedThisReference qualifiedThisReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(QualifiedThisReference qualifiedThisReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(SingleMemberAnnotation annotation, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(SingleNameReference singleNameReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(org.eclipse.jdt.internal.compiler.ast.StringLiteral stringLiteral, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(StringLiteralConcatenation literal, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(SuperReference superReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(SynchronizedStatement synchronizedStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ThisReference thisReference, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ThisReference thisReference, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(TryStatement tryStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		printNode(typeDeclaration);
		child("javadoc", typeDeclaration.javadoc, Scopes.CLASS);
		children("annotations", typeDeclaration.annotations, Scopes.METHOD);
		child("superclass", typeDeclaration.superclass, Scopes.CLASS);
		children("superInterfaces", typeDeclaration.superInterfaces, Scopes.CLASS);
		children("typeParameters", typeDeclaration.typeParameters, Scopes.CLASS);
		children("memberTypes", typeDeclaration.memberTypes, Scopes.CLASS);
		children("fields", typeDeclaration.fields, Scopes.METHOD);
		children("methods", typeDeclaration.methods, Scopes.CLASS);
		indent--;
		return false;
	}
	
	@Override
	public boolean visit(TypeParameter typeParameter, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(TypeParameter typeParameter, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(Wildcard wildcard, BlockScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
	
	@Override
	public boolean visit(Wildcard wildcard, ClassScope scope) {
		throw new UnsupportedOperationException();
		// return false;
	}
}
