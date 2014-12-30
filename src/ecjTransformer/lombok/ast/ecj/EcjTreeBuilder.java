/*
 * Copyright (C) 2010-2011 The Project Lombok Authors.
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.ast.AnnotationDeclaration;
import lombok.ast.AnnotationValueArray;
import lombok.ast.AstVisitor;
import lombok.ast.BinaryOperator;
import lombok.ast.Comment;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Identifier;
import lombok.ast.JavadocContainer;
import lombok.ast.KeywordModifier;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.Position;
import lombok.ast.RawListAccessor;
import lombok.ast.UnaryOperator;
import lombok.ast.VariableReference;
import lombok.ast.grammar.SourceStructure;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
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
import org.eclipse.jdt.internal.compiler.ast.Javadoc;
import org.eclipse.jdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LongLiteral;
import org.eclipse.jdt.internal.compiler.ast.LongLiteralMinValue;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
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
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.parser.JavadocParser;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static lombok.ast.ConversionPositionInfo.getConversionPositionInfo;

/**
 * Turns {@code lombok.ast} based ASTs into eclipse/ecj's {@code org.eclipse.jdt.internal.compiler.ast.ASTNode} model.
 */
public class EcjTreeBuilder {
	private static final int VISIBILITY_MASK = 7;
	static final char[] PACKAGE_INFO = "package-info".toCharArray();
	
	private final Map<lombok.ast.Node, Collection<SourceStructure>> sourceStructures;
	private List<? extends ASTNode> result = null;
	private final String rawInput;
	private final ProblemReporter reporter;
	private final ProblemReporter silentProblemReporter;
	private final CompilationResult compilationResult;
	private final CompilerOptions options;
	private final EnumSet<BubblingFlags> bubblingFlags = EnumSet.noneOf(BubblingFlags.class);
	private final EnumSet<BubblingFlags> AUTO_REMOVABLE_BUBBLING_FLAGS = EnumSet.of(BubblingFlags.LOCALTYPE);
	
	private enum BubblingFlags {
		ASSERT, LOCALTYPE, ABSTRACT_METHOD
	}
	
	private enum VariableKind {
		UNSUPPORTED {
			AbstractVariableDeclaration create() {
				throw new UnsupportedOperationException(); 
			}
		},
		FIELD {
			AbstractVariableDeclaration create() {
				return new FieldDeclaration(); 
			}
		}, 
		LOCAL {
			AbstractVariableDeclaration create() {
				return new LocalDeclaration(null, 0, 0); 
			}
		}, 
		ARGUMENT {
			AbstractVariableDeclaration create() {
				return new Argument(null, 0, null, 0);
			}
		};
		
		abstract AbstractVariableDeclaration create();
		
		static VariableKind kind(lombok.ast.VariableDefinition node) {
			//TODO rewrite this whole thing.
			lombok.ast.Node parent = node.getParent();
			if (parent instanceof lombok.ast.VariableDeclaration) {
				if (parent.getParent() instanceof lombok.ast.TypeBody ||
					parent.getParent() instanceof lombok.ast.EnumTypeBody) {
					return FIELD;
				} else {
					return LOCAL;
				}
			}
			if (parent instanceof lombok.ast.For || 
				parent instanceof lombok.ast.ForEach){
				return LOCAL;
			}
			if (parent instanceof lombok.ast.Catch ||
				parent instanceof lombok.ast.MethodDeclaration ||
				parent instanceof lombok.ast.ConstructorDeclaration){
				return ARGUMENT;
			}
			return UNSUPPORTED;
		}
	}
	
	private static final IProblemFactory SILENT_PROBLEM_FACTORY = new IProblemFactory() {
		
		@Override public String getLocalizedMessage(int problemId, int elaborationId, String[] messageArguments) {
			return null;
		}
		
		@Override public String getLocalizedMessage(int problemId, String[] messageArguments) {
			return null;
		}
		
		@Override public Locale getLocale() {
			return Locale.getDefault();
		}
		
		@Override public CategorizedProblem createProblem(char[] originatingFileName, int problemId, String[] problemArguments, int elaborationId, String[] messageArguments, int severity, int startPosition, int endPosition, int lineNumber, int columnNumber) {
			return null;
		}
		
		@Override public CategorizedProblem createProblem(char[] originatingFileName, int problemId, String[] problemArguments, String[] messageArguments, int severity, int startPosition, int endPosition, int lineNumber, int columnNumber) {
			return null;
		}
	};
	
	public EcjTreeBuilder(lombok.ast.grammar.Source source, CompilerOptions options) {
		this(source, createDefaultProblemReporter(options), createSilentProblemReporter(options), new CompilationResult(source.getName().toCharArray(), 0, 0, 0));
	}
	
	public EcjTreeBuilder(String rawInput, String name, CompilerOptions options) {
		this(rawInput, createDefaultProblemReporter(options), createSilentProblemReporter(options), new CompilationResult(name.toCharArray(), 0, 0, 0));
	}
	
	private static ProblemReporter createDefaultProblemReporter(CompilerOptions options) {
		return new ProblemReporter(new IErrorHandlingPolicy() {
			public boolean proceedOnErrors() {
				return true;
			}
			
			public boolean stopOnFirstError() {
				return false;
			}
		}, options, new DefaultProblemFactory(Locale.ENGLISH));
	}
	
	private static ProblemReporter createSilentProblemReporter(CompilerOptions options) {
		return new ProblemReporter(new IErrorHandlingPolicy() {
			public boolean proceedOnErrors() {
				return true;
			}
			
			public boolean stopOnFirstError() {
				return false;
			}
		}, options, SILENT_PROBLEM_FACTORY);
	}
	
	public EcjTreeBuilder(lombok.ast.grammar.Source source, ProblemReporter reporter, ProblemReporter silentProblemReporter, CompilationResult compilationResult) {
		this.options = reporter.options;
		this.sourceStructures = source.getSourceStructures();
		this.rawInput = source.getRawInput();
		this.reporter = reporter;
		this.silentProblemReporter = silentProblemReporter;
		this.compilationResult = compilationResult;
	}
	
	public EcjTreeBuilder(String rawInput, ProblemReporter reporter, ProblemReporter silentProblemReporter, CompilationResult compilationResult) {
		this.options = reporter.options;
		this.sourceStructures = null;
		this.rawInput = rawInput;
		this.reporter = reporter;
		this.silentProblemReporter = silentProblemReporter;
		this.compilationResult = compilationResult;
	}
	
	private EcjTreeBuilder(EcjTreeBuilder parent) {
		this.reporter = parent.reporter;
		this.silentProblemReporter = parent.silentProblemReporter;
		this.options = parent.options;
		this.rawInput = parent.rawInput;
		this.compilationResult = parent.compilationResult;
		this.sourceStructures = parent.sourceStructures;
	}
	
	private EcjTreeBuilder create() {
		return new EcjTreeBuilder(this);
	}
	
	private Expression toExpression(lombok.ast.Node node) {
		return (Expression) toTree(node);
	}
	
	private Statement toStatement(lombok.ast.Node node) {
		return (Statement) toTree(node);
	}
	
	private ASTNode toTree(lombok.ast.Node node) {
		if (node == null) return null;
		EcjTreeBuilder newBuilder = create();
		node.accept(newBuilder.visitor);
		bubblingFlags.addAll(newBuilder.bubblingFlags);
		try {
			return newBuilder.get();
		} catch (RuntimeException e) {
			System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
			throw e;
		}
	}
	
	private char[] toName(lombok.ast.Identifier node) {
		if (node == null) {
			return null;
		}
		return node.astValue().toCharArray();
	}
	
	private <T extends ASTNode> T[] toArray(Class<T> type, List<T> list) {
		if (list.isEmpty()) return null;
		@SuppressWarnings("unchecked")
		T[] emptyArray = (T[]) Array.newInstance(type, 0);
		return list.toArray(emptyArray);
	}
	
	private <T extends ASTNode> T[] toArray(Class<T> type, lombok.ast.Node node) {
		return toArray(type, toList(type, node));
	}
	
	private <T extends ASTNode> T[] toArray(Class<T> type, lombok.ast.StrictListAccessor<?, ?> accessor) {
		List<T> list = Lists.newArrayList();
		for (lombok.ast.Node node : accessor) {
			EcjTreeBuilder newBuilder = create();
			node.accept(newBuilder.visitor);
			bubblingFlags.addAll(newBuilder.bubblingFlags);
			
			List<? extends ASTNode> values;
			
			values = newBuilder.getAll();
			
			for (ASTNode value : values) {
				if (value != null && !type.isInstance(value)) {
					throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + type.getName());
				}
				list.add(type.cast(value));
			}
		}
		
		return toArray(type, list);
	}
	
	private <T extends ASTNode> List<T> toList(Class<T> type, lombok.ast.Node node) {
		if (node == null) return Lists.newArrayList();
		EcjTreeBuilder newBuilder = create();
		node.accept(newBuilder.visitor);
		bubblingFlags.addAll(newBuilder.bubblingFlags);
		@SuppressWarnings("unchecked")
		List<T> all = (List<T>)newBuilder.getAll();
		return Lists.newArrayList(all);
	}
	
	public void visit(lombok.ast.Node node) {
		node.accept(visitor);
	}
	
	public ASTNode get() {
		if (result.isEmpty()) {
			return null;
		}
		if (result.size() == 1) {
			return result.get(0);
		}
		throw new RuntimeException("Expected only one result but got " + result.size());
	}
	
	public List<? extends ASTNode> getAll() {
		return result;
	}
	
	private static <T extends ASTNode> T posParen(T in, lombok.ast.Node node) {
		if (in == null) return null;
		if (node instanceof lombok.ast.Expression) {
			List<Position> parensPositions = ((lombok.ast.Expression)node).astParensPositions();
			if (!parensPositions.isEmpty()) {
				in.sourceStart = parensPositions.get(parensPositions.size() - 1).getStart();
				in.sourceEnd = parensPositions.get(parensPositions.size() - 1).getEnd() - 1;
			}
		}
		
		return in;
	}
	
	private static boolean isExplicitlyAbstract(Modifiers m) {
		for (KeywordModifier keyword : m.astKeywords()) {
			if ("abstract".equals(keyword.astName())) return true;
		}
		return false;
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
	
	private final AstVisitor visitor = new ForwardingAstVisitor() {
		@Override
		public boolean visitCompilationUnit(lombok.ast.CompilationUnit node) {
			int sourceLength = rawInput == null ? 0 : rawInput.length();
			CompilationUnitDeclaration cud = new CompilationUnitDeclaration(reporter, compilationResult, sourceLength);
			cud.bits |= ASTNode.HasAllMethodBodies;
			
			cud.currentPackage = (ImportReference) toTree(node.astPackageDeclaration());
			cud.imports = toArray(ImportReference.class, node.astImportDeclarations());
			cud.types = toArray(TypeDeclaration.class, node.astTypeDeclarations());
			
			if (CharOperation.equals(PACKAGE_INFO, cud.getMainTypeName())) {
				TypeDeclaration[] newTypes;
				if (cud.types == null) {
					newTypes = new TypeDeclaration[1];
				} else {
					newTypes = new TypeDeclaration[cud.types.length + 1];
					System.arraycopy(cud.types, 0, newTypes, 1, cud.types.length);
				}
				TypeDeclaration decl = new TypeDeclaration(compilationResult);
				decl.name = PACKAGE_INFO.clone();
				decl.modifiers = ClassFileConstants.AccDefault | ClassFileConstants.AccInterface;
				
				newTypes[0] = decl;
				cud.types = newTypes;
				
				lombok.ast.PackageDeclaration pkgDeclaration = node.astPackageDeclaration();
				Comment javadoc = pkgDeclaration == null ? null : pkgDeclaration.astJavadoc();
				if (javadoc != null) {
					boolean markDep = javadoc.isMarkedDeprecated();
					cud.javadoc = (Javadoc) toTree(javadoc);
					if (markDep) decl.modifiers |= ClassFileConstants.AccDeprecated;
					decl.javadoc = cud.javadoc;
				}
			}
			
			bubblingFlags.remove(BubblingFlags.ASSERT);
			bubblingFlags.removeAll(AUTO_REMOVABLE_BUBBLING_FLAGS);
			if (!bubblingFlags.isEmpty()) {
				throw new RuntimeException("Unhandled bubbling flags left: " + bubblingFlags);
			}
			return set(node, cud);
		}
		
		private boolean set(lombok.ast.Node node, ASTNode value) {
			if (result != null) throw new IllegalStateException("result is already set");
			
			if (node instanceof lombok.ast.Expression) {
				int parens = ((lombok.ast.Expression)node).getIntendedParens();
				value.bits |= (parens << ASTNode.ParenthesizedSHIFT) & ASTNode.ParenthesizedMASK;
				posParen(value, node);
			}
			if (value instanceof NameReference) {
				updateRestrictionFlags(node, (NameReference)value);
			}
			List<ASTNode> result = Lists.newArrayList();
			if (value != null) result.add(value);
			EcjTreeBuilder.this.result = result;
			
			return true;
		}
		
		private boolean set(lombok.ast.Node node, List<? extends ASTNode> values) {
			if (values.isEmpty()) System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
			
			if (result != null) throw new IllegalStateException("result is already set");
			result = values;
			return true;
		}
		
		private int calculateExplicitDeclarations(Iterable<lombok.ast.Statement> statements) {
			int explicitDeclarations = 0;
			if (statements != null) {
				for (lombok.ast.Statement s : statements) {
					if (s instanceof lombok.ast.VariableDeclaration) explicitDeclarations++;
					if (s instanceof lombok.ast.ClassDeclaration) explicitDeclarations++;
				}
			}
			
			return explicitDeclarations;
		}
		
		@Override
		public boolean visitPackageDeclaration(lombok.ast.PackageDeclaration node) {
			long[] pos = partsToPosArray(node.rawParts());
			ImportReference pkg = new ImportReference(chain(node.astParts()), pos, true, ClassFileConstants.AccDefault);
			pkg.annotations = toArray(Annotation.class, node.astAnnotations());
			pkg.declarationSourceStart = jstart(node);
			pkg.declarationSourceEnd = pkg.declarationEnd = end(node);
			
			return set(node, pkg);
		}
		
		//TODO Create a test file with a whole bunch of comments. Possibly in a separate non-idempotency subdir, as printing comments idempotently is a rough nut to crack.
		
		@Override
		public boolean visitImportDeclaration(lombok.ast.ImportDeclaration node) {
			int staticFlag = node.astStaticImport() ? ClassFileConstants.AccStatic : ClassFileConstants.AccDefault;
			long[] pos = partsToPosArray(node.rawParts());
			ImportReference imp = new ImportReference(chain(node.astParts()), pos, node.astStarImport(), staticFlag);
			imp.declarationSourceStart = start(node);
			imp.declarationSourceEnd = imp.declarationEnd = end(node);
			return set(node, imp);
		}
		
		@Override
		public boolean visitClassDeclaration(lombok.ast.ClassDeclaration node) {
			TypeDeclaration decl = createTypeBody(node.astBody().astMembers(), node, true, 0);
			
			decl.annotations = toArray(Annotation.class, node.astModifiers().astAnnotations());
			decl.superclass = (TypeReference) toTree(node.astExtending());
			decl.superInterfaces = toArray(TypeReference.class, node.astImplementing());
			
			markTypeReferenceIsSuperType(decl);
			
			decl.typeParameters = toArray(TypeParameter.class, node.astTypeVariables());
			
			decl.name = toName(node.astName());
			
			updateTypeBits(node.getParent(), decl, false);
			
			setupJavadoc(decl, node);
			
			//TODO test inner types. Give em everything - (abstract) methods, initializers, static initializers, MULTIPLE initializers.
			return set(node, decl);
		}
		
		private void updateTypeBits(lombok.ast.Node parent, TypeDeclaration decl, boolean isEnum) {
			if (parent == null) {
				return;
			}
			if (parent instanceof lombok.ast.CompilationUnit) {
				char[] mainTypeName = new CompilationUnitDeclaration(reporter, compilationResult, 0).getMainTypeName();
				if (!CharOperation.equals(decl.name, mainTypeName)) {
					decl.bits |= ASTNode.IsSecondaryType;
				}
				return;
			} 
			
			if (parent instanceof lombok.ast.TypeBody ||
				parent instanceof lombok.ast.EnumTypeBody) {
				decl.bits |= ASTNode.IsMemberType;
				return;
			}
			
			//TODO test if a type declared in an enum constant is possible
			decl.bits |= ASTNode.IsLocalType;
			bubblingFlags.add(BubblingFlags.LOCALTYPE);
		}
		
		private void markTypeReferenceIsSuperType(TypeDeclaration decl) {
			if (decl.superclass != null) {
				decl.superclass.bits |= ASTNode.IsSuperType;
			}
			if (decl.superInterfaces != null) {
				for (TypeReference t : decl.superInterfaces) {
					t.bits |= ASTNode.IsSuperType;
				}
			}
		}
		
		
		@Override
		public boolean visitInterfaceDeclaration(lombok.ast.InterfaceDeclaration node) {
			TypeDeclaration decl = createTypeBody(node.astBody().astMembers(), node, false, ClassFileConstants.AccInterface);
			
			decl.annotations = toArray(Annotation.class, node.astModifiers().astAnnotations());
			decl.superInterfaces = toArray(TypeReference.class, node.astExtending());
			
			markTypeReferenceIsSuperType(decl);
			
			decl.typeParameters = toArray(TypeParameter.class, node.astTypeVariables());
			decl.name = toName(node.astName());
			
			updateTypeBits(node.getParent(), decl, false);
			
			setupJavadoc(decl, node);
			
			return set(node, decl);
		}	
		
		@Override
		public boolean visitEnumDeclaration(lombok.ast.EnumDeclaration node) {
			FieldDeclaration[] fields = null;
			
			if (node.astBody() != null) {
				fields = toArray(FieldDeclaration.class, node.astBody().astConstants());
			}
			TypeDeclaration decl = createTypeBody(node.astBody().astMembers(), node, true, ClassFileConstants.AccEnum, fields);
			
			decl.annotations = toArray(Annotation.class, node.astModifiers().astAnnotations());
			decl.superInterfaces = toArray(TypeReference.class, node.astImplementing());
			
			markTypeReferenceIsSuperType(decl);
			
			decl.name = toName(node.astName());
			
			updateTypeBits(node.getParent(), decl, true);
			
			setupJavadoc(decl, node);
			
			return set(node, decl);
		}
		
		@Override
		public boolean visitEnumConstant(lombok.ast.EnumConstant node) {
			//TODO check where the javadoc and annotations go: the field or the type
			
			FieldDeclaration decl = new FieldDeclaration();
			decl.annotations = toArray(Annotation.class, node.astAnnotations());
			decl.name = toName(node.astName());
			
			decl.sourceStart = start(node.astName());
			decl.sourceEnd = end(node.astName());
			decl.declarationSourceStart = decl.modifiersSourceStart = jstart(node);
			decl.declarationSourceEnd = decl.declarationEnd = end(node);
			Position ecjDeclarationSourcePos = getConversionPositionInfo(node, "declarationSource");
			if (ecjDeclarationSourcePos != null) decl.declarationSourceEnd = ecjDeclarationSourcePos.getEnd() - 1;
			
			AllocationExpression init;
			if (node.astBody() == null) {
				init = new AllocationExpression();
				init.enumConstant = decl;
			} else {
				TypeDeclaration type = createTypeBody(node.astBody().astMembers(), null, false, 0);
				type.sourceStart = type.sourceEnd = start(node.astBody());
				type.bodyEnd--;
				type.declarationSourceStart = type.sourceStart;
				type.declarationSourceEnd = end(node);
				type.name = CharOperation.NO_CHAR;
				type.bits &= ~ASTNode.IsMemberType;
				decl.bits |= ASTNode.HasLocalType;
				type.bits |= ASTNode.IsLocalType | ASTNode.IsAnonymousType;
				init = new QualifiedAllocationExpression(type);
				init.enumConstant = decl;
			}
			init.arguments = toArray(Expression.class, node.astArguments());
			decl.initialization = init;
			
			if (bubblingFlags.remove(BubblingFlags.LOCALTYPE)) {
				decl.bits |= ASTNode.HasLocalType;
			}
			
			setupJavadoc(decl, node);
			
			return set(node, decl);
		}
		
		@Override
		public boolean visitAnnotationDeclaration(lombok.ast.AnnotationDeclaration node) {
			TypeDeclaration decl = createTypeBody(node.astBody().astMembers(), node, false,
					ClassFileConstants.AccAnnotation | ClassFileConstants.AccInterface);
			decl.annotations = toArray(Annotation.class, node.astModifiers().astAnnotations());
			decl.name = toName(node.astName());
			updateTypeBits(node.getParent(), decl, false);
			
			setupJavadoc(decl, node);
			
			return set(node, decl);
		}
		
		private void setupJavadoc(ASTNode node, lombok.ast.JavadocContainer container) {
			if (container != null && container.rawJavadoc() instanceof lombok.ast.Comment) {
				lombok.ast.Comment javadoc = (Comment) container.rawJavadoc();
				boolean markDep = javadoc.isMarkedDeprecated();
				
				if (node instanceof AbstractMethodDeclaration) {
					AbstractMethodDeclaration decl = (AbstractMethodDeclaration) node;
					decl.javadoc = (Javadoc) toTree(javadoc);
					if (markDep) decl.modifiers |= ClassFileConstants.AccDeprecated;
				}
				if (node instanceof FieldDeclaration) {
					FieldDeclaration decl = (FieldDeclaration) node;
					decl.javadoc = (Javadoc) toTree(javadoc);
					if (markDep) decl.modifiers |= ClassFileConstants.AccDeprecated;
				}
				if (node instanceof TypeDeclaration) {
					TypeDeclaration decl = (TypeDeclaration) node;
					decl.javadoc = (Javadoc) toTree(javadoc);
					if (markDep) decl.modifiers |= ClassFileConstants.AccDeprecated;
				}
			}
		}
		
		@Override
		public boolean visitConstructorDeclaration(lombok.ast.ConstructorDeclaration node) {
			ConstructorDeclaration decl = new ConstructorDeclaration(compilationResult);
			decl.bodyStart = start(node.rawBody()) + 1;
			decl.bodyEnd = end(node.rawBody()) - 1;
			decl.declarationSourceStart = jstart(node);
			decl.declarationSourceEnd = end(node);
			decl.sourceStart = start(node.astTypeName());
			/* set sourceEnd */ {
				Position ecjPos = getConversionPositionInfo(node, "signature");
				decl.sourceEnd = ecjPos == null ? posOfStructure(node, ")", false) - 1 : ecjPos.getEnd() - 1;
				
				if (!node.rawThrownTypeReferences().isEmpty()) {
					decl.sourceEnd = end(node.rawThrownTypeReferences().last());
				}
			}
			decl.annotations = toArray(Annotation.class, node.astModifiers().astAnnotations());
			decl.modifiers = toModifiers(node.astModifiers());
			decl.typeParameters = toArray(TypeParameter.class, node.astTypeVariables());
			decl.arguments = toArray(Argument.class, node.astParameters());
			decl.thrownExceptions = toArray(TypeReference.class, node.astThrownTypeReferences());
			decl.statements = toArray(Statement.class, node.astBody().astContents());
			decl.selector = toName(node.astTypeName());
			
			setupJavadoc(decl, node);
			
			if (decl.statements == null || decl.statements.length == 0 || !(decl.statements[0] instanceof ExplicitConstructorCall)) {
				decl.constructorCall = new ExplicitConstructorCall(ExplicitConstructorCall.ImplicitSuper);
				decl.constructorCall.sourceStart = decl.sourceStart;
				decl.constructorCall.sourceEnd = decl.sourceEnd;
			} else {
				//TODO check how super() and this() work
				decl.constructorCall = (ExplicitConstructorCall)decl.statements[0];
				if (decl.statements.length > 1) {
					Statement[] newStatements = new Statement[decl.statements.length - 1];
					System.arraycopy(decl.statements, 1, newStatements, 0, newStatements.length);
					decl.statements = newStatements;
				} else {
					decl.statements = null;
				}
			}
			
			if (bubblingFlags.remove(BubblingFlags.LOCALTYPE)) {
				decl.bits |= ASTNode.HasLocalType;
			}
			
			// Unlike other method(-like) constructs, while ConstructorDeclaration has a
			// explicitDeclarations field, its kept at 0, so we don't need to calculate that value here.
			
			if (isUndocumented(node.astBody())) decl.bits |= ASTNode.UndocumentedEmptyBlock;
			
			setupJavadoc(decl, node);
			
			return set(node, decl);
		}
		
		@Override
		public boolean visitMethodDeclaration(lombok.ast.MethodDeclaration node) {
			MethodDeclaration decl = new MethodDeclaration(compilationResult);
			decl.declarationSourceStart = jstart(node);
			decl.declarationSourceEnd = end(node);
			decl.sourceStart = start(node.astMethodName());
			boolean setOriginalPosOnType = false;
			/* set sourceEnd */ {
				Position ecjPos = getConversionPositionInfo(node, "signature");
				decl.sourceEnd = ecjPos == null ? posOfStructure(node, ")", false) - 1: ecjPos.getEnd() - 1;
				if (countStructure(node, "]") > 0) {
					decl.sourceEnd = posOfStructure(node, "]", false) - 1;
					setOriginalPosOnType = true;
				}
				
				if (!node.rawThrownTypeReferences().isEmpty()) {
					decl.sourceEnd = end(node.rawThrownTypeReferences().last());
				}
			}
			if (node.rawBody() == null) {
				decl.bodyStart = decl.sourceEnd + 1;
				decl.bodyEnd = end(node) - 1;
			} else {
				decl.bodyStart = start(node.rawBody()) + 1;
				decl.bodyEnd = end(node.rawBody()) - 1;
			}
			decl.annotations = toArray(Annotation.class, node.astModifiers().astAnnotations());
			decl.modifiers = toModifiers(node.astModifiers());
			decl.returnType = (TypeReference) toTree(node.astReturnTypeReference());
			if (setOriginalPosOnType) {
				if (decl.returnType instanceof ArrayTypeReference) {
					((ArrayTypeReference)decl.returnType).originalSourceEnd = end(node.rawReturnTypeReference());
				}
			}
			decl.typeParameters = toArray(TypeParameter.class, node.astTypeVariables());
			decl.arguments = toArray(Argument.class, node.astParameters());
			decl.selector = toName(node.astMethodName());
			decl.thrownExceptions = toArray(TypeReference.class, node.astThrownTypeReferences());
			if (node.astBody() == null) {
				decl.modifiers |= ExtraCompilerModifiers.AccSemicolonBody;
			} else {
				decl.statements = toArray(Statement.class, node.astBody().astContents());
				decl.explicitDeclarations = calculateExplicitDeclarations(node.astBody().astContents());
			}
			
			if (bubblingFlags.remove(BubblingFlags.LOCALTYPE)) {
				decl.bits |= ASTNode.HasLocalType;
			}
			
			if (isExplicitlyAbstract(node.astModifiers())) {
				bubblingFlags.add(BubblingFlags.ABSTRACT_METHOD);
			}
			if (isUndocumented(node.astBody())) decl.bits |= ASTNode.UndocumentedEmptyBlock;
			
			setupJavadoc(decl, node);
			
			return set(node, decl);
		}
		
		@Override
		public boolean visitAnnotationMethodDeclaration(lombok.ast.AnnotationMethodDeclaration node) {
			AnnotationMethodDeclaration decl = new AnnotationMethodDeclaration(compilationResult);
			decl.modifiers = toModifiers(node.astModifiers()) + ExtraCompilerModifiers.AccSemicolonBody;
			decl.declarationSourceStart = jstart(node);
			decl.declarationSourceEnd = end(node);
			decl.sourceStart = start(node.astMethodName());
			boolean setOriginalPosOnType = false;
			/* set sourceEnd */ {
				Position ecjSigPos = getConversionPositionInfo(node, "signature");
				Position ecjExtDimPos = getConversionPositionInfo(node, "extendedDimensions");
				if (ecjSigPos != null && ecjExtDimPos != null) {
					decl.sourceEnd = ecjSigPos.getEnd() - 1;
					decl.extendedDimensions = ecjExtDimPos.getStart();
				} else {
					decl.sourceEnd = posOfStructure(node, ")", false) - 1;
					decl.extendedDimensions = countStructure(node, "]");
					if (decl.extendedDimensions > 0) {
						decl.sourceEnd = posOfStructure(node, "]", false) - 1;
						setOriginalPosOnType = true;
					}
				}
			}
			decl.bodyStart = end(node);
			decl.bodyEnd = end(node);
			if (node.astDefaultValue() != null) {
				decl.modifiers |= ClassFileConstants.AccAnnotationDefault;
			}
			decl.annotations = toArray(Annotation.class, node.astModifiers().astAnnotations());
			decl.defaultValue = toExpression(node.astDefaultValue());
			decl.selector = toName(node.astMethodName());
			decl.returnType = (TypeReference) toTree(node.astReturnTypeReference());
			if (setOriginalPosOnType) {
				if (decl.returnType instanceof ArrayTypeReference) {
					((ArrayTypeReference)decl.returnType).originalSourceEnd = end(node.rawReturnTypeReference());
				}
			}
			
			if (isExplicitlyAbstract(node.astModifiers())) {
				bubblingFlags.add(BubblingFlags.ABSTRACT_METHOD);
			}
			
			setupJavadoc(decl, node);
			
			return set(node, decl);
		}
		
		private TypeDeclaration createTypeBody(lombok.ast.StrictListAccessor<lombok.ast.TypeMember, ?> members, lombok.ast.TypeDeclaration type, boolean canHaveConstructor, int extraModifiers, FieldDeclaration... initialFields) {
			TypeDeclaration decl = new TypeDeclaration(compilationResult);
			decl.modifiers = (type == null ? 0 : toModifiers(type.astModifiers())) | extraModifiers;
			if (members.isEmpty() && isUndocumented(members.owner())) decl.bits |= ASTNode.UndocumentedEmptyBlock;
			
			if (type != null) {
				decl.sourceStart = start(type.astName());
				decl.sourceEnd = end(type.astName());
				decl.declarationSourceStart = jstart(type);
				decl.declarationSourceEnd = end(type);
				if (!(type instanceof AnnotationDeclaration) || !type.astModifiers().isEmpty() || type.rawJavadoc() != null) {
					decl.modifiersSourceStart = jstart(type.astModifiers());
				} else {
					decl.modifiersSourceStart = -1;
				}
			}
			decl.bodyStart = start(members.owner()) + 1;
			decl.bodyEnd = end(members.owner());
			
			boolean hasExplicitConstructor = false;
			List<AbstractMethodDeclaration> methods = Lists.newArrayList();
			List<FieldDeclaration> fields = Lists.newArrayList();
			List<TypeDeclaration> types = Lists.newArrayList();
			
			if (initialFields != null) fields.addAll(Arrays.asList(initialFields));
			
			for (lombok.ast.TypeMember member : members) {
				if (member instanceof lombok.ast.ConstructorDeclaration) {
					hasExplicitConstructor = true;
					AbstractMethodDeclaration method =(AbstractMethodDeclaration)toTree(member);
					methods.add(method);
				} else if (member instanceof lombok.ast.MethodDeclaration ||
						member instanceof lombok.ast.AnnotationMethodDeclaration) {
					AbstractMethodDeclaration method =(AbstractMethodDeclaration)toTree(member);
					methods.add(method);
				} else if (member instanceof lombok.ast.VariableDeclaration) {
					for (FieldDeclaration field : toList(FieldDeclaration.class, member)) {
						fields.add(field);
					}
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
				} else {
					throw new RuntimeException("Unhandled member type " + member.getClass().getSimpleName());
				}
				
			}
			
			if (!hasExplicitConstructor && canHaveConstructor) {
				ConstructorDeclaration defaultConstructor = new ConstructorDeclaration(compilationResult);
				defaultConstructor.bits |= ASTNode.IsDefaultConstructor;
				defaultConstructor.constructorCall = new ExplicitConstructorCall(ExplicitConstructorCall.ImplicitSuper);
				defaultConstructor.modifiers = decl.modifiers & VISIBILITY_MASK;
				defaultConstructor.selector = toName(type.astName());
				defaultConstructor.sourceStart = defaultConstructor.declarationSourceStart =
						defaultConstructor.constructorCall.sourceStart =
						start(type.astName());
				defaultConstructor.sourceEnd = defaultConstructor.bodyEnd =
						defaultConstructor.constructorCall.sourceEnd =
						defaultConstructor.declarationSourceEnd =
						end(type.astName());
				methods.add(0, defaultConstructor);
			}
			
			decl.memberTypes = toArray(TypeDeclaration.class, types);
			decl.methods = toArray(AbstractMethodDeclaration.class, methods);
			decl.fields = toArray(FieldDeclaration.class, fields);
			if (bubblingFlags.contains(BubblingFlags.ASSERT)) {
				decl.bits |= ASTNode.ContainsAssertion;
			}
			if (bubblingFlags.remove(BubblingFlags.ABSTRACT_METHOD)) {
				decl.bits |= ASTNode.HasAbstractMethods;
			}
			
			decl.addClinit();
			return decl;
		}
		
		@Override
		public boolean visitExpressionStatement(lombok.ast.ExpressionStatement node) {
			Statement statement = toStatement(node.astExpression());
			try {
				Field f = statement.getClass().getField("statementEnd");
				f.set(statement, end(node));
			} catch (Exception ignore) {
				// Not all these classes may have a statementEnd.
			}
			return set(node, statement);
		}
		
		@Override
		public boolean visitConstructorInvocation(lombok.ast.ConstructorInvocation node) {
			AllocationExpression inv;
			if (node.astQualifier() != null || node.astAnonymousClassBody() != null) {
				if (node.astAnonymousClassBody() != null) {
					TypeDeclaration decl = createTypeBody(node.astAnonymousClassBody().astMembers(), null, false, 0);
					Position ecjSigPos = getConversionPositionInfo(node, "signature");
					decl.sourceStart = ecjSigPos == null ? start(node.rawTypeReference()) : ecjSigPos.getStart();
					decl.sourceEnd = ecjSigPos == null ? posOfStructure(node, ")", false) - 1 : ecjSigPos.getEnd() - 1;
					decl.declarationSourceStart = decl.sourceStart;
					decl.declarationSourceEnd = end(node);
					decl.name = CharOperation.NO_CHAR;
					decl.bits |= ASTNode.IsAnonymousType | ASTNode.IsLocalType;
					bubblingFlags.add(BubblingFlags.LOCALTYPE);
					inv = new QualifiedAllocationExpression(decl);
				} else {
					inv = new QualifiedAllocationExpression();
				}
				if (node.astQualifier() != null) {
					((QualifiedAllocationExpression)inv).enclosingInstance = toExpression(node.astQualifier());
				}
				
			} else {
				inv = new AllocationExpression();
			}
			
			if (!node.astConstructorTypeArguments().isEmpty()) {
				inv.typeArguments = toArray(TypeReference.class, node.astConstructorTypeArguments());
			}
			inv.type = (TypeReference) toTree(node.astTypeReference());
			inv.arguments = toArray(Expression.class, node.astArguments());
			inv.sourceStart = start(node);
			inv.sourceEnd = end(node);
			return set(node, inv);
		}
		
		@Override
		public boolean visitAlternateConstructorInvocation(lombok.ast.AlternateConstructorInvocation node) {
			ExplicitConstructorCall inv = new ExplicitConstructorCall(ExplicitConstructorCall.This);
			inv.sourceStart = posOfStructure(node, "this", true);
			inv.sourceEnd = end(node);
	//		inv.modifiers = decl.modifiers & VISIBILITY_MASK;
			if (!node.astConstructorTypeArguments().isEmpty()) {
				inv.typeArguments = toArray(TypeReference.class, node.astConstructorTypeArguments());
				Position ecjTypeArgsPos = getConversionPositionInfo(node, "typeArguments");
				inv.typeArgumentsSourceStart = ecjTypeArgsPos == null ? posOfStructure(node, "<", true) : ecjTypeArgsPos.getStart();
			}
			inv.arguments = toArray(Expression.class, node.astArguments());
			return set(node, inv);
		}
		
		@Override
		public boolean visitSuperConstructorInvocation(lombok.ast.SuperConstructorInvocation node) {
			ExplicitConstructorCall inv = new ExplicitConstructorCall(ExplicitConstructorCall.Super);
			inv.sourceStart = start(node);
			inv.sourceEnd = end(node);
	//		inv.modifiers = decl.modifiers & VISIBILITY_MASK;
			if (!node.astConstructorTypeArguments().isEmpty()) {
				inv.typeArguments = toArray(TypeReference.class, node.astConstructorTypeArguments());
				Position ecjTypeArgsPos = getConversionPositionInfo(node, "typeArguments");
				inv.typeArgumentsSourceStart = ecjTypeArgsPos == null ? posOfStructure(node, "<", true) : ecjTypeArgsPos.getStart();
			}
			inv.arguments = toArray(Expression.class, node.astArguments());
			inv.qualification = toExpression(node.astQualifier());
			return set(node, inv);
		}
	
		@Override
		public boolean visitMethodInvocation(lombok.ast.MethodInvocation node) {
			MessageSend inv = new MessageSend();
			inv.sourceStart = start(node);
			inv.sourceEnd = end(node);
			inv.nameSourcePosition = pos(node.astName());
			
			inv.arguments = toArray(Expression.class, node.astArguments());
			inv.receiver = toExpression(node.astOperand());
			if (inv.receiver instanceof NameReference) {
				inv.receiver.bits |= Binding.TYPE;
			}
			
			//TODO do we have an implicit this style call somewhere in our test sources?
			if (inv.receiver == null) {
				inv.receiver = new ThisReference(0, 0);
				inv.receiver.bits |= ASTNode.IsImplicitThis;
			}
			if (!node.astMethodTypeArguments().isEmpty()) inv.typeArguments = toArray(TypeReference.class, node.astMethodTypeArguments());
			inv.selector = toName(node.astName());
			return set(node, inv);
		}
		
		@Override
		public boolean visitSuper(lombok.ast.Super node) {
			if (node.astQualifier() == null) {
				return set(node, new SuperReference(start(node), end(node)));
			}
			return set(node, new QualifiedSuperReference((TypeReference) toTree(node.astQualifier()), start(node), end(node)));
		}
		
		@Override
		public boolean visitUnaryExpression(lombok.ast.UnaryExpression node) {
			if (node.astOperator() == UnaryOperator.UNARY_MINUS) {
				if (node.astOperand() instanceof lombok.ast.IntegralLiteral && node.astOperand().getParens() == 0) {
					lombok.ast.IntegralLiteral lit = (lombok.ast.IntegralLiteral)node.astOperand();
					if (!lit.astMarkedAsLong() && lit.astIntValue() == Integer.MIN_VALUE) {
						IntLiteralMinValue minLiteral = new IntLiteralMinValue(
							lit.rawValue().toCharArray(), null, start(node), end(node));
						return set(node, minLiteral);
					}
					if (lit.astMarkedAsLong() && lit.astLongValue() == Long.MIN_VALUE) {
						LongLiteralMinValue minLiteral = new LongLiteralMinValue(
							lit.rawValue().toCharArray(), null, start(node), end(node));
						return set(node, minLiteral);
					}
				}
			}
			
			Expression operand = toExpression(node.astOperand());
			int ecjOperator = UNARY_OPERATORS.get(node.astOperator());
			
			switch (node.astOperator()) {
			case PREFIX_INCREMENT:
			case PREFIX_DECREMENT:
				return set(node, new PrefixExpression(operand, IntLiteral.One, ecjOperator, start(node)));
			case POSTFIX_INCREMENT:
			case POSTFIX_DECREMENT:
				return set(node, new PostfixExpression(operand, IntLiteral.One, ecjOperator, end(node)));
			default:
				UnaryExpression expr = new UnaryExpression(toExpression(node.astOperand()), ecjOperator);
				expr.sourceStart = start(node);
				expr.sourceEnd = end(node);
				return set(node, expr);
			}
		}
		
		@Override
		public boolean visitBinaryExpression(lombok.ast.BinaryExpression node) {
			Expression lhs = toExpression(node.astLeft());
			Expression rhs = toExpression(node.astRight());
			
			if (node.astOperator() == BinaryOperator.ASSIGN) {
				return set(node, posParen(new Assignment(lhs, rhs, end(node)), node));
			}
			
			//TODO add a test with 1 + 2 + 3 + "" + 4 + 5 + 6 + "foo"; as well as 5 + 2 + 3 - 5 - 8 -7 - 8 * 10 + 20;
			
			int ecjOperator = BINARY_OPERATORS.get(node.astOperator());
			if (node.astOperator().isAssignment()) {
				return set(node, posParen(new CompoundAssignment(lhs, rhs, ecjOperator, end(node)), node));
			} else if (node.astOperator() == BinaryOperator.EQUALS || node.astOperator() == BinaryOperator.NOT_EQUALS) {
				return set(node, posParen(new EqualExpression(lhs, rhs, ecjOperator), node));
			} else if (node.astOperator() == BinaryOperator.LOGICAL_AND) {
				return set(node, posParen(new AND_AND_Expression(lhs, rhs, ecjOperator), node));
			} else if (node.astOperator() == BinaryOperator.LOGICAL_OR) {
				return set(node, posParen(new OR_OR_Expression(lhs, rhs, ecjOperator), node));
			} else if (node.astOperator() == BinaryOperator.PLUS && node.astLeft().getParens() == 0) {
				Expression stringConcatExpr = posParen(tryStringConcat(lhs, rhs), node);
				if (stringConcatExpr != null) return set(node, stringConcatExpr);
			}
			
			return set(node, posParen(new BinaryExpression(lhs, rhs, ecjOperator), node));
		}
		
		private Expression tryStringConcat(Expression lhs, Expression rhs) {
			if (options.parseLiteralExpressionsAsConstants) {
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
			} else {
				if (lhs instanceof StringLiteralConcatenation) {
					if (rhs instanceof StringLiteral) {
						return ((StringLiteralConcatenation)lhs).extendsWith((StringLiteral)rhs);
					}
				} else if (lhs instanceof StringLiteral) {
					if (rhs instanceof StringLiteral) {
						return new StringLiteralConcatenation((StringLiteral) lhs, (StringLiteral) rhs);
					}
				}
			}
			
			return null;
		}
		
		@Override
		public boolean visitCast(lombok.ast.Cast node) {
			Expression typeRef = toExpression(node.astTypeReference());
			Expression operand = toExpression(node.astOperand());
			
			CastExpression expr = createCastExpression(typeRef, operand);
			
			Position ecjTypePos = getConversionPositionInfo(node, "type");
			typeRef.sourceStart = ecjTypePos == null ? posOfStructure(node, "(", true) + 1 : ecjTypePos.getStart();
			typeRef.sourceEnd = ecjTypePos == null ? posOfStructure(node, ")", 0, false) - 2 : ecjTypePos.getEnd() - 1;
			expr.sourceStart = start(node);
			expr.sourceEnd = end(node);
			return set(node, expr);
		}
		
		@SneakyThrows
		private CastExpression createCastExpression(Expression typeRef, Expression operand) {
			try {
				return (CastExpression) CastExpression.class.getConstructors()[0].newInstance(operand, typeRef);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}
		
		@Override
		public boolean visitInstanceOf(lombok.ast.InstanceOf node) {
			return set(node, new InstanceOfExpression(toExpression(node.astObjectReference()), (TypeReference) toTree(node.astTypeReference())));
		}
		
		@Override
		public boolean visitInlineIfExpression(lombok.ast.InlineIfExpression node) {
			return set(node, new ConditionalExpression(toExpression(node.astCondition()), toExpression(node.astIfTrue()), toExpression(node.astIfFalse())));
		}
		
		@Override
		public boolean visitSelect(lombok.ast.Select node) {
			//TODO for something like ("" + "").foo.bar;
			/* try chain-of-identifiers */ {
				List<lombok.ast.Identifier> selects = Lists.newArrayList();
				List<Long> pos = Lists.newArrayList();
				lombok.ast.Select current = node;
				while (true) {
					selects.add(current.astIdentifier());
					pos.add(pos(current.astIdentifier()));
					if (current.astOperand() instanceof lombok.ast.Select) current = (lombok.ast.Select) current.astOperand();
					else if (current.astOperand() instanceof lombok.ast.VariableReference) {
						selects.add(((lombok.ast.VariableReference) current.astOperand()).astIdentifier());
						pos.add(pos(current.rawOperand()));
						Collections.reverse(selects);
						long[] posArray = new long[pos.size()];
						for (int i = 0; i < posArray.length; i++) posArray[i] = pos.get(posArray.length - i - 1);
						char[][] tokens = chain(selects, selects.size());
						QualifiedNameReference ref = new QualifiedNameReference(tokens, posArray, start(node), end(node));
						return set(node, ref);
					} else {
						break;
					}
				}
			}
			
			FieldReference ref = new FieldReference(toName(node.astIdentifier()), pos(node));
			ref.nameSourcePosition = pos(node.astIdentifier());
			ref.receiver = toExpression(node.astOperand());
			
			//TODO ("" + 10).a.b = ... DONT forget to doublecheck var/type restriction flags
			return set(node, ref);
		}
		
		@Override
		public boolean visitTypeReference(lombok.ast.TypeReference node) {
			// TODO make sure there's a test case that covers every eclipsian type ref: hierarchy on "org.eclipse.jdt.internal.compiler.ast.TypeReference".
			
			Wildcard wildcard = null;
			TypeReference ref = null;
			
			switch (node.astWildcard()) {
			case UNBOUND:
				wildcard = new Wildcard(Wildcard.UNBOUND);
				wildcard.sourceStart = start(node);
				wildcard.sourceEnd = end(node);
				return set(node, wildcard);
			case EXTENDS:
				wildcard = new Wildcard(Wildcard.EXTENDS);
				break;
			case SUPER:
				wildcard = new Wildcard(Wildcard.SUPER);
				break;
			}
			
			char[][] qualifiedName = null;
			char[] singleName = null;
			boolean qualified = node.astParts().size() != 1;
			int dims = node.astArrayDimensions();
			TypeReference[][] params = new TypeReference[node.astParts().size()][];
			boolean hasGenerics = false;
			
			if (!qualified) {
				singleName = toName(node.astParts().first().astIdentifier());
			} else {
				List<lombok.ast.Identifier> identifiers = Lists.newArrayList();
				for (lombok.ast.TypeReferencePart part : node.astParts()) identifiers.add(part.astIdentifier());
				qualifiedName = chain(identifiers, identifiers.size());
			}
			
			{
				int ctr = 0;
				for (lombok.ast.TypeReferencePart part : node.astParts()) {
					params[ctr] = new TypeReference[part.astTypeArguments().size()];
					int ctr2 = 0;
					boolean partHasGenerics = false;
					for (lombok.ast.TypeReference x : part.astTypeArguments()) {
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
					if (dims == 0) {
						ref = new SingleTypeReference(singleName, partsToPosArray(node.rawParts())[0]);
					} else {
						ref = new ArrayTypeReference(singleName, dims, 0L);
						ref.sourceStart = start(node);
						ref.sourceEnd = end(node);
						((ArrayTypeReference)ref).originalSourceEnd = end(node.rawParts().last());
					}
				} else {
					ref = new ParameterizedSingleTypeReference(singleName, params[0], dims, partsToPosArray(node.rawParts())[0]);
					if (dims > 0) ref.sourceEnd = end(node);
				}
			} else {
				if (!hasGenerics) {
					if (dims == 0) {
						long[] pos = partsToPosArray(node.rawParts());
						ref = new QualifiedTypeReference(qualifiedName, pos);
					} else {
						long[] pos = partsToPosArray(node.rawParts());
						ref = new ArrayQualifiedTypeReference(qualifiedName, dims, pos);
						ref.sourceEnd = end(node);
					}
				} else {
					//TODO test what happens with generic types that also have an array dimension or two.
					long[] pos = partsToPosArray(node.rawParts());
					ref = new ParameterizedQualifiedTypeReference(qualifiedName, params, dims, pos);
					if (dims > 0) ref.sourceEnd = end(node);
				}
			}
			
			if (wildcard != null) {
				wildcard.bound = ref;
				ref = wildcard;
				ref.sourceStart = start(node);
				ref.sourceEnd = wildcard.bound.sourceEnd;
			}
			
			return set(node, ref);
		}
		
		@Override
		public boolean visitTypeVariable(lombok.ast.TypeVariable node) {
			// TODO test multiple bounds on a variable, e.g. <T extends A & B & C>
			TypeParameter param = new TypeParameter();
			param.declarationSourceStart = start(node);
			param.declarationSourceEnd = end(node);
			param.sourceStart = start(node.astName());
			param.sourceEnd = end(node.astName());
			param.name = toName(node.astName());
			if (!node.astExtending().isEmpty()) {
				TypeReference[] p = toArray(TypeReference.class, node.astExtending());
				for (TypeReference t : p) t.bits |= ASTNode.IsSuperType;
				param.type = p[0];
				if (p.length > 1) {
					param.bounds = new TypeReference[p.length - 1];
					System.arraycopy(p, 1, param.bounds, 0, p.length - 1);
				}
				param.declarationSourceEnd = p[p.length - 1].sourceEnd;
			}
			
			return set(node, param);
		}
		
		@Override
		public boolean visitStaticInitializer(lombok.ast.StaticInitializer node) {
			Initializer init = new Initializer((Block) toTree(node.astBody()), ClassFileConstants.AccStatic);
			init.declarationSourceStart = start(node);
			init.sourceStart = start(node.astBody());
			init.sourceEnd = init.declarationSourceEnd = end(node);
			init.bodyStart = init.sourceStart + 1;
			init.bodyEnd = init.sourceEnd - 1;
			return set(node, init);
		}
	
		@Override
		public boolean visitInstanceInitializer(lombok.ast.InstanceInitializer node) {
			Initializer init = new Initializer((Block) toTree(node.astBody()), 0);
			if (bubblingFlags.remove(BubblingFlags.LOCALTYPE)) {
				init.bits |= ASTNode.HasLocalType;
			}
			init.sourceStart = init.declarationSourceStart = start(node);
			init.sourceEnd = init.declarationSourceEnd = end(node);
			init.bodyStart = init.sourceStart + 1;
			init.bodyEnd = init.sourceEnd - 1;
			return set(node, init);
		}
		
		@Override
		public boolean visitIntegralLiteral(lombok.ast.IntegralLiteral node) {
			if (node.astMarkedAsLong()) {
				return set(node, LongLiteral.buildLongLiteral(node.rawValue().toCharArray(), start(node), end(node)));
			}
			return set(node, IntLiteral.buildIntLiteral(node.rawValue().toCharArray(), start(node), end(node)));
		}
		
		@Override
		public boolean visitFloatingPointLiteral(lombok.ast.FloatingPointLiteral node) {
			if (node.astMarkedAsFloat()) {
				return set(node, new FloatLiteral(node.rawValue().toCharArray(), start(node), end(node)));
			}
			return set(node, new DoubleLiteral(node.rawValue().toCharArray(), start(node), end(node)));
		}
		
		@Override
		public boolean visitBooleanLiteral(lombok.ast.BooleanLiteral node) {
			return set(node, node.astValue() ? new TrueLiteral(start(node), end(node)) : new FalseLiteral(start(node), end(node)));
		}
		
		@Override
		public boolean visitNullLiteral(lombok.ast.NullLiteral node) {
			return set(node, new NullLiteral(start(node), end(node)));
		}
		
		@Override
		public boolean visitVariableReference(VariableReference node) {
			SingleNameReference ref = new SingleNameReference(toName(node.astIdentifier()), pos(node));
			return set(node, ref);
		}
		
		@Override
		public boolean visitIdentifier(lombok.ast.Identifier node) {
			SingleNameReference ref = new SingleNameReference(toName(node), pos(node));
			return set(node, ref);
		}
		
		@Override
		public boolean visitCharLiteral(lombok.ast.CharLiteral node) {
			return set(node, new CharLiteral(node.rawValue().toCharArray(), start(node), end(node)));
		}
		
		@Override
		public boolean visitStringLiteral(lombok.ast.StringLiteral node) {
			return set(node, new StringLiteral(node.astValue().toCharArray(), start(node), end(node), 0));
		}
		
		@Override
		public boolean visitBlock(lombok.ast.Block node) {
			Block block = new Block(0);
			block.statements = toArray(Statement.class, node.astContents());
			if (block.statements == null) {
				if (isUndocumented(node)) block.bits |= ASTNode.UndocumentedEmptyBlock;
			} else {
				block.explicitDeclarations = calculateExplicitDeclarations(node.astContents());
			}
			block.sourceStart = start(node);
			block.sourceEnd = end(node);
			return set(node, block);
		}
		
		@Override
		public boolean visitAnnotationValueArray(AnnotationValueArray node) {
			ArrayInitializer init = new ArrayInitializer();
			init.sourceStart = start(node);
			init.sourceEnd = end(node);
			init.expressions = toArray(Expression.class, node.astValues());
			
			return set(node, init);
		}
		
		@Override
		public boolean visitArrayInitializer(lombok.ast.ArrayInitializer node) {
			ArrayInitializer init = new ArrayInitializer();
			init.sourceStart = start(node);
			init.sourceEnd = end(node);
			init.expressions = toArray(Expression.class, node.astExpressions());
			
			return set(node, init);
		}
		
		@Override
		public boolean visitArrayCreation(lombok.ast.ArrayCreation node) {
			ArrayAllocationExpression aae = new ArrayAllocationExpression();
			aae.sourceStart = start(node);
			aae.sourceEnd = end(node);
			aae.type = (TypeReference) toTree(node.astComponentTypeReference());
			// TODO uncompilable parser test: new Type<Generics>[]...
			// TODO uncompilable parser test: new Type[][expr][][expr]...
			aae.type.bits |= ASTNode.IgnoreRawTypeCheck;
			
			int i = 0;
			Expression[] dimensions = new Expression[node.astDimensions().size()];
			for (lombok.ast.ArrayDimension dim : node.astDimensions()) {
				dimensions[i++] = (Expression) toTree(dim.astDimension());
			}
			aae.dimensions = dimensions;
			aae.initializer = (ArrayInitializer) toTree(node.astInitializer());
			return set(node, aae);
		}
		
		@Override
		public boolean visitArrayDimension(lombok.ast.ArrayDimension node) {
			return set(node, toExpression(node.astDimension()));
		}
		
		@Override
		public boolean visitThis(lombok.ast.This node) {
			if (node.astQualifier() == null) {
				return set(node, new ThisReference(start(node), end(node)));
			}
			return set(node, new QualifiedThisReference((TypeReference) toTree(node.astQualifier()), start(node), end(node)));
		}
		
		@Override
		public boolean visitClassLiteral(lombok.ast.ClassLiteral node) {
			return set(node, new ClassLiteralAccess(end(node), (TypeReference) toTree(node.astTypeReference())));
		}
		
		@Override
		public boolean visitArrayAccess(lombok.ast.ArrayAccess node) {
			ArrayReference ref = new ArrayReference(toExpression(node.astOperand()), toExpression(node.astIndexExpression()));
			ref.sourceEnd = end(node);
			return set(node, ref);
		}
		
		@Override
		public boolean visitAssert(lombok.ast.Assert node) {
			//TODO check the flags after more test have been added: asserts in constructors, methods etc.
			bubblingFlags.add(BubblingFlags.ASSERT);
			if (node.astMessage() == null) {
				return set(node, new AssertStatement(toExpression(node.astAssertion()), start(node)));
			}
			return set(node, new AssertStatement(toExpression(node.astMessage()), toExpression(node.astAssertion()), start(node)));
		}
		
		@Override
		public boolean visitDoWhile(lombok.ast.DoWhile node) {
			return set(node, new DoStatement(toExpression(node.astCondition()), toStatement(node.astStatement()), start(node), end(node)));
		}
		
		@Override
		public boolean visitContinue(lombok.ast.Continue node) {
			return set(node, new ContinueStatement(toName(node.astLabel()), start(node), end(node)));
		}
		
		@Override
		public boolean visitBreak(lombok.ast.Break node) {
			return set(node, new BreakStatement(toName(node.astLabel()), start(node), end(node)));
		}
		
		@Override
		public boolean visitForEach(lombok.ast.ForEach node) {
			ForeachStatement forEach = new ForeachStatement((LocalDeclaration) toTree(node.astVariable()), start(node));
			forEach.sourceEnd = end(node);
			forEach.collection = toExpression(node.astIterable());
			forEach.action = toStatement(node.astStatement());
			return set(node, forEach);
		}
		
		@Override
		public boolean visitVariableDeclaration(lombok.ast.VariableDeclaration node) {
			List<AbstractVariableDeclaration> list = toList(AbstractVariableDeclaration.class, node.astDefinition());
			if (list.size() > 0) setupJavadoc(list.get(0), node);
			return set(node, list);
		}
		
		@Override
		public boolean visitVariableDefinition(lombok.ast.VariableDefinition node) {
			List<AbstractVariableDeclaration> values = Lists.newArrayList();
			Annotation[] annotations = toArray(Annotation.class, node.astModifiers().astAnnotations());
			int modifiers = toModifiers(node.astModifiers());
			TypeReference base = (TypeReference) toTree(node.astTypeReference());
			AbstractVariableDeclaration prevDecl = null, firstDecl = null;
			for (lombok.ast.VariableDefinitionEntry entry : node.astVariables()) {
				VariableKind kind = VariableKind.kind(node);
				AbstractVariableDeclaration decl = kind.create();
				decl.annotations = annotations;
				decl.initialization = toExpression(entry.astInitializer());
				decl.modifiers = modifiers;
				decl.name = toName(entry.astName());
				if (entry.astArrayDimensions() == 0 && !node.astVarargs()) {
					decl.type = base;
				} else if (entry.astArrayDimensions() > 0 || node.astVarargs()) {
					decl.type = (TypeReference) toTree(entry.getEffectiveTypeReference());
					decl.type.sourceStart = base.sourceStart;
					Position ecjTypeSourcePos = getConversionPositionInfo(entry, "typeSourcePos");
					if (ecjTypeSourcePos != null) {
						decl.type.sourceEnd = ecjTypeSourcePos.getEnd() - 1;
					} else {
						// This makes no sense whatsoever but eclipse wants it this way.
						if (firstDecl == null && (base.dimensions() > 0 || node.getParent() instanceof lombok.ast.ForEach)) {
							decl.type.sourceEnd = posOfStructure(entry, "]", false) - 1;
						} else if (firstDecl != null) {
							// This replicates an eclipse bug; the end pos of the type of b in: int[] a[][], b[]; is in fact the second closing ] of a.
							decl.type.sourceEnd = firstDecl.type.sourceEnd;
						} else decl.type.sourceEnd = base.sourceEnd;
						// Yet another eclipse inconsistency.
						if (kind == VariableKind.FIELD && base instanceof ArrayQualifiedTypeReference) {
							long[] poss = ((ArrayQualifiedTypeReference)base).sourcePositions;
							decl.type.sourceEnd = (int) poss[poss.length - 1];
						}
					}
					if (node.astVarargs()) {
						if (decl.type instanceof ArrayTypeReference) {
							((ArrayTypeReference)decl.type).originalSourceEnd = decl.type.sourceEnd;
						}
						Position ecjTyperefPos = getConversionPositionInfo(node, "typeref");
						decl.type.sourceEnd = ecjTyperefPos == null ? posOfStructure(node, "...", false) - 1 : ecjTyperefPos.getEnd() - 1;
					} else {
						if (decl.type instanceof ArrayTypeReference) {
							((ArrayTypeReference)decl.type).originalSourceEnd = decl.type.sourceEnd;
						}
						if (decl.type instanceof ArrayQualifiedTypeReference) {
							((ArrayQualifiedTypeReference)decl.type).sourcePositions = ((QualifiedTypeReference)base).sourcePositions.clone();
						}
					}
				}
				if (node.astVarargs()) {
					decl.type.bits |= ASTNode.IsVarArgs;
				}
				if (decl instanceof FieldDeclaration) {
					if (bubblingFlags.remove(BubblingFlags.LOCALTYPE)) {
						decl.bits |= ASTNode.HasLocalType;
					}
				}
				
				decl.sourceStart = start(entry.astName());
				decl.sourceEnd = end(entry.astName());
				decl.declarationSourceStart = jstart(node);
				switch (kind) {
				case LOCAL:
					int end;
					if (node.getParent() instanceof lombok.ast.VariableDeclaration) end = end(node.getParent());
					else {
						if (entry.rawInitializer() != null) end = end(entry.rawInitializer());
						else end = end(entry.astName());
					}
					decl.declarationSourceEnd = decl.declarationEnd = end;
					Position ecjDeclarationSourcePos = getConversionPositionInfo(entry, "declarationSource");
					if (ecjDeclarationSourcePos != null) decl.declarationSourceEnd = ecjDeclarationSourcePos.getEnd() - 1;
					break;
				case ARGUMENT:
					decl.declarationSourceEnd = decl.declarationEnd = end(entry.astName());
					ecjDeclarationSourcePos = getConversionPositionInfo(entry, "declarationSource");
					if (ecjDeclarationSourcePos != null) decl.declarationSourceEnd = ecjDeclarationSourcePos.getEnd() - 1;
					break;
				case FIELD:
					decl.declarationSourceEnd = decl.declarationEnd = end(node.getParent());
					ecjDeclarationSourcePos = getConversionPositionInfo(entry, "declarationSource");
					Position ecjPart1Pos = getConversionPositionInfo(entry, "varDeclPart1");
					Position ecjPart2Pos = getConversionPositionInfo(entry, "varDeclPart2");
					if (ecjDeclarationSourcePos != null) decl.declarationSourceEnd = ecjDeclarationSourcePos.getEnd() - 1;
					((FieldDeclaration)decl).endPart1Position = ecjPart1Pos == null ? end(node.rawTypeReference()) + 1 : ecjPart1Pos.getEnd() - 1;
					((FieldDeclaration)decl).endPart2Position = ecjPart2Pos == null ? end(node.getParent()) : ecjPart2Pos.getEnd() - 1;
					if (ecjPart2Pos == null && prevDecl instanceof FieldDeclaration) {
						((FieldDeclaration)prevDecl).endPart2Position = start(entry) - 1;
					}
					break;
				}
				values.add(decl);
				prevDecl = decl;
				if (firstDecl == null) firstDecl = decl;
			}
			
			return set(node, values);
		}
		
		@Override
		public boolean visitIf(lombok.ast.If node) {
			if (node.astElseStatement() == null) {
				return set(node, new IfStatement(toExpression(node.astCondition()), toStatement(node.astStatement()), start(node), end(node)));
			}
			return set(node, new IfStatement(toExpression(node.astCondition()), toStatement(node.astStatement()), toStatement(node.astElseStatement()), start(node), end(node)));
		}
		
		@Override
		public boolean visitLabelledStatement(lombok.ast.LabelledStatement node) {
			return set(node, new LabeledStatement(toName(node.astLabel()), toStatement(node.astStatement()), pos(node.astLabel()), end(node)));
		}
		
		@Override
		public boolean visitFor(lombok.ast.For node) {
			//TODO make test for modifiers on variable declarations
			//TODO make test for empty for/foreach etc.
			if(node.isVariableDeclarationBased()) {
				return set(node, new ForStatement(toArray(Statement.class, node.astVariableDeclaration()),
						toExpression(node.astCondition()),
						toArray(Statement.class, node.astUpdates()),
						toStatement(node.astStatement()),
						true, start(node), end(node)));
			}
			return set(node, new ForStatement(toArray(Statement.class, node.astExpressionInits()),
					toExpression(node.astCondition()),
					toArray(Statement.class, node.astUpdates()),
					toStatement(node.astStatement()),
					false, start(node), end(node)));
		}
		
		@Override
		public boolean visitSwitch(lombok.ast.Switch node) {
			SwitchStatement value = new SwitchStatement();
			value.sourceStart = start(node);
			value.sourceEnd = end(node);
			value.blockStart = start(node.rawBody());
			value.expression = toExpression(node.astCondition());
			value.statements = toArray(Statement.class, node.astBody().astContents());
			if (value.statements == null) {
				if (isUndocumented(node.astBody())) value.bits |= ASTNode.UndocumentedEmptyBlock;
			} else {
				value.explicitDeclarations = calculateExplicitDeclarations(node.astBody().astContents());
			}
			return set(node, value);
		}
		
		@Override
		public boolean visitSynchronized(lombok.ast.Synchronized node) {
			return set(node, new SynchronizedStatement(toExpression(node.astLock()), (Block) toTree(node.astBody()), start(node), end(node)));
		}
		
		@Override
		public boolean visitTry(lombok.ast.Try node) {
			TryStatement tryStatement = new TryStatement();
			tryStatement.sourceStart = start(node);
			tryStatement.sourceEnd = end(node);
			
			tryStatement.tryBlock = (Block) toTree(node.astBody());
			int catchSize = node.astCatches().size();
			if (catchSize > 0) {
				tryStatement.catchArguments = new Argument[catchSize];
				tryStatement.catchBlocks = new Block[catchSize];
				int i = 0;
				for (lombok.ast.Catch c : node.astCatches()) {
					tryStatement.catchArguments[i] = (Argument)toTree(c.astExceptionDeclaration());
					tryStatement.catchBlocks[i] = (Block) toTree(c.astBody());
					i++;
				}
			}
			tryStatement.finallyBlock = (Block) toTree(node.astFinally());
			return set(node, tryStatement);
		}
		
		@Override
		public boolean visitThrow(lombok.ast.Throw node) {
			return set(node, new ThrowStatement(toExpression(node.astThrowable()), start(node), end(node)));
		}
		
		@Override
		public boolean visitWhile(lombok.ast.While node) {
			return set(node, new WhileStatement(toExpression(node.astCondition()), toStatement(node.astStatement()), start(node), end(node)));
		}
		
		@Override
		public boolean visitReturn(lombok.ast.Return node) {
			return set(node, new ReturnStatement(toExpression(node.astValue()), start(node), end(node)));
		}
		
		@Override
		public boolean visitAnnotation(lombok.ast.Annotation node) {
			//TODO add test where the value is the result of string concatenation
			TypeReference type = (TypeReference) toTree(node.astAnnotationTypeReference());
			boolean isEcjNormal = Position.UNPLACED == getConversionPositionInfo(node, "isNormalAnnotation");
			
			if (node.astElements().isEmpty() && countStructure(node, "(") == 0 && !isEcjNormal) {
				MarkerAnnotation ann = new MarkerAnnotation(type, start(node));
				ann.declarationSourceEnd = end(node);
				return set(node, ann);
			}
			MemberValuePair[] values = toArray(MemberValuePair.class, node.astElements());
			if (values != null && (values.length == 1 && values[0].name == null)) {
				SingleMemberAnnotation ann = new SingleMemberAnnotation(type, start(node));
				ann.declarationSourceEnd = end(node);
				ann.memberValue = values[0].value;
				return set(node, ann);
			}
			NormalAnnotation ann = new NormalAnnotation(type, start(node));
			ann.declarationSourceEnd = end(node);
			ann.memberValuePairs = values;
			return set(node, ann);
		}
		
		@Override
		public boolean visitAnnotationElement(lombok.ast.AnnotationElement node) {
			//TODO make a test where the array initializer is the default value
			MemberValuePair pair = new MemberValuePair(toName(node.astName()), start(node), end(node.astName()), null);
			// giving the value to the constructor will set the ASTNode.IsAnnotationDefaultValue flag
			pair.value = toExpression(node.astValue());
			if (pair.name != null && pair.value instanceof ArrayInitializer) {
				pair.value.bits |= ASTNode.IsAnnotationDefaultValue;
			}
			return set(node, pair);
		}
		
		
		@Override
		public boolean visitCase(lombok.ast.Case node) {
			// end and start args are switched around on CaseStatement, presumably because the API designer was drunk at the time.
			return set(node, new CaseStatement(toExpression(node.astCondition()), end(node.rawCondition()), start(node)));
		}
		
		@Override
		public boolean visitDefault(lombok.ast.Default node) {
			// end and start args are switched around on CaseStatement, presumably because the API designer was drunk at the time.
			return set(node, new CaseStatement(null, posOfStructure(node, "default", false) - 1, start(node)));
		}
		
		@Override
		public boolean visitComment(lombok.ast.Comment node) {
			if (!node.isJavadoc()) {
				throw new RuntimeException("Only javadoc expected here");
			}
			
			Node parent = node.getParent();
			parent = parent == null ? null : parent.getParent();
			while (parent != null && !(parent instanceof lombok.ast.TypeDeclaration)) parent = parent.getParent();
			String typeName = null;
			if (parent instanceof lombok.ast.TypeDeclaration) {
				Identifier identifier = ((lombok.ast.TypeDeclaration)parent).astName();
				if (identifier != null) typeName = identifier.astValue();
			}
			
			if (typeName == null) {
				typeName = getTypeNameFromFileName(compilationResult.getFileName());
			}
			
			return set(node, new JustJavadocParser(silentProblemReporter, typeName).parse(rawInput, node.getPosition().getStart(), node.getPosition().getEnd()));
		}
		
		@Override
		public boolean visitEmptyStatement(lombok.ast.EmptyStatement node) {
			return set(node, new EmptyStatement(start(node), end(node)));
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
				c[i++] = part.astValue().toCharArray();
			}
			return c;
		}
		
		private void updateRestrictionFlags(lombok.ast.Node node, NameReference ref) {
			ref.bits &= ~ASTNode.RestrictiveFlagMASK;
			ref.bits |= Binding.VARIABLE;
			
			if (node.getParent() instanceof lombok.ast.MethodInvocation) {
				if (((lombok.ast.MethodInvocation)node.getParent()).astOperand() == node) {
					ref.bits |= Binding.TYPE;
				}
			}
			
			if (node.getParent() instanceof lombok.ast.Select) {
				if (((lombok.ast.Select)node.getParent()).astOperand() == node) {
					ref.bits |= Binding.TYPE;
				}
			}
		}
		
		// Only works for TypeBody, EnumTypeBody and Block
		private boolean isUndocumented(lombok.ast.Node block) {
			if (block == null) return false;
			if (rawInput == null) return false;
			
			lombok.ast.Position pos = block.getPosition();
			if (pos.isUnplaced() || pos.size() < 3) return true;
			
			String content = rawInput.substring(pos.getStart() + 1, pos.getEnd() - 1);
			return content.trim().isEmpty();
		}
	};
	
	private static class JustJavadocParser extends JavadocParser {
		private static final char[] GENERIC_JAVA_CLASS_SUFFIX = "class Y{}".toCharArray();
		
		JustJavadocParser(ProblemReporter reporter, String mainTypeName) {
			super(makeDummyParser(reporter, mainTypeName));
		}
		
		private static Parser makeDummyParser(ProblemReporter reporter, String mainTypeName) {
			Parser parser = new Parser(reporter, false);
			CompilationResult cr = new CompilationResult((mainTypeName + ".java").toCharArray(), 0, 1, 0);
			parser.compilationUnit = new CompilationUnitDeclaration(reporter, cr, 0);
			return parser;
		}
		
		Javadoc parse(String rawInput, int from, int to) {
			char[] rawContent;
			
			rawContent = new char[to + GENERIC_JAVA_CLASS_SUFFIX.length];
			Arrays.fill(rawContent, 0, from, ' ');
			System.arraycopy(rawInput.substring(from, to).toCharArray(), 0, rawContent, from, to - from);
			// Eclipse crashes if there's no character following the javadoc.
			System.arraycopy(GENERIC_JAVA_CLASS_SUFFIX, 0, rawContent, to, GENERIC_JAVA_CLASS_SUFFIX.length);
			
			this.sourceLevel = ClassFileConstants.JDK1_6;
			this.scanner.setSource(rawContent);
			this.source = rawContent;
			this.javadocStart = from;
			this.javadocEnd = to;
			this.reportProblems = true;
			this.docComment = new Javadoc(this.javadocStart, this.javadocEnd);
			commentParse();
			this.docComment.valuePositions = -1;
			this.docComment.sourceEnd--;
			return docComment;
		}
	}
	
	private static int jstart(lombok.ast.Node node) {
		if (node == null) return 0;
		int start = start(node);
		if (node instanceof lombok.ast.JavadocContainer) {
			lombok.ast.Node javadoc = ((lombok.ast.JavadocContainer)node).rawJavadoc();
			if (javadoc != null) return Math.min(start, start(javadoc));
		}
		if (node instanceof lombok.ast.VariableDefinition && node.getParent() instanceof lombok.ast.VariableDeclaration) {
			lombok.ast.Node javadoc = ((lombok.ast.JavadocContainer)node.getParent()).rawJavadoc();
			if (javadoc != null) return Math.min(start, start(javadoc));
		}
		if (node instanceof lombok.ast.Modifiers && node.getParent() instanceof JavadocContainer) {
			lombok.ast.Node javadoc = ((lombok.ast.JavadocContainer)node.getParent()).rawJavadoc();
			if (javadoc != null) return Math.min(start, start(javadoc));
		}
		return start;
	}
	
	private static int start(lombok.ast.Node node) {
		if (node == null || node.getPosition().isUnplaced()) return 0;
		return node.getPosition().getStart();
	}
	
	private static int end(lombok.ast.Node node) {
		if (node == null || node.getPosition().isUnplaced()) return 0;
		return node.getPosition().getEnd() - 1;
	}
	
	private static long pos(lombok.ast.Node n) {
		return (((long)start(n)) << 32) | end(n);
	}
	
	private static long[] partsToPosArray(RawListAccessor<?, ?> parts) {
		long[] pos = new long[parts.size()];
		int idx = 0;
		for (lombok.ast.Node n : parts) {
			if (n instanceof lombok.ast.TypeReferencePart) {
				pos[idx++] = pos(((lombok.ast.TypeReferencePart) n).astIdentifier());
			} else {
				pos[idx++] = pos(n);
			}
		}
		return pos;
	}
	
	private int countStructure(lombok.ast.Node node, String structure) {
		int result = 0;
		if (sourceStructures != null && sourceStructures.containsKey(node)) {
			for (SourceStructure struct : sourceStructures.get(node)) {
				if (structure.equals(struct.getContent())) result++;
			}
		}
		
		return result;
	}
	
	private int posOfStructure(lombok.ast.Node node, String structure, boolean atStart) {
		return posOfStructure(node, structure, atStart ? 0 : Integer.MAX_VALUE, atStart);
	}
	
	private int posOfStructure(lombok.ast.Node node, String structure, int idx, boolean atStart) {
		int start = node.getPosition().getStart();
		int end = node.getPosition().getEnd();
		Integer result = null;
		
		if (sourceStructures != null && sourceStructures.containsKey(node)) {
			for (SourceStructure struct : sourceStructures.get(node)) {
				if (structure.equals(struct.getContent())) {
					result = atStart ? struct.getPosition().getStart() : struct.getPosition().getEnd();
					if (idx-- <= 0) break;
				}
			}
		}
		
		if (result != null) return result;
		return atStart ? start : end;
	}
	
	private static String getTypeNameFromFileName(char[] fileName) {
		String f = new String(fileName);
		int start = Math.max(f.lastIndexOf('/'), f.lastIndexOf('\\'));
		int end = f.lastIndexOf('.');
		if (end == -1) end = f.length();
		return f.substring(start + 1, end);
	}
}
