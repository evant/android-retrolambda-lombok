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
package lombok.ast.grammar;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.CharLiteral;
import lombok.ast.FloatingPointLiteral;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.IntegralLiteral;
import lombok.ast.LiteralType;
import lombok.ast.Node;
import lombok.ast.Position;
import lombok.ast.StringLiteral;
import lombok.ast.UnaryExpression;
import lombok.ast.UnaryOperator;
import lombok.ast.grammar.RunForEachFileInDirRunner.DirDescriptor;
import lombok.ast.javac.JcTreeConverter;
import lombok.ast.printer.SourcePrinter;
import lombok.ast.printer.StructureFormatter;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.OptionName;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

/**
 * Compares a lombok.ast AST parsed by lombok with a JCTree AST parsed by javac, converted to lombok.ast.
 */
@RunWith(RunForEachFileInDirRunner.class)
public class JcTreeConverterType1Test extends TreeBuilderRunner<Node> {
	public JcTreeConverterType1Test() {
		super(false);
	}
	
//	@Override protected Collection<DirDescriptor> getDirDescriptors() {
//		return Arrays.asList(
//				DirDescriptor.of(new File("test/resources/idempotency"), true)
//						.withInclusion(Pattern.compile("^.*(?:[c]002_Ex).*\\.java$", Pattern.CASE_INSENSITIVE)));
////		return Arrays.asList(
////				DirDescriptor.of(new File("test/resources/idempotency"), true),
////				DirDescriptor.of(new File("test/resources/alias"), true),
////				DirDescriptor.of(new File("test/resources/special"), true));
//	}
	
	@Test
	public boolean testJcTreeConverter(Source source) throws Exception {
		return testCompiler(source);
	}
	
	private void normalizeNumberLiterals(Node tree) {
		tree.accept(new ForwardingAstVisitor() {
			@Override public boolean visitIntegralLiteral(IntegralLiteral node) {
				long v = node.astMarkedAsLong() ? node.astLongValue() : node.astIntValue();
				if (node.astLiteralType() != LiteralType.DECIMAL && v < 0) {
					if (node.astMarkedAsLong()) node.astLongValue(Math.abs(node.astLongValue()));
					else node.astIntValue(Math.abs(node.astIntValue()));
					UnaryExpression e = new UnaryExpression().astOperator(UnaryOperator.UNARY_MINUS);
					node.replace(e);
					e.astOperand(node);
					e.setPosition(node.getPosition());
				}
				node.astLiteralType(LiteralType.DECIMAL);
				return false;
			}
			
			@Override public boolean visitFloatingPointLiteral(FloatingPointLiteral node) {
				if (node.astMarkedAsFloat()) node.astFloatValue(node.astFloatValue());
				else node.astDoubleValue(node.astDoubleValue());
				node.astLiteralType(LiteralType.DECIMAL);
				return false;
			}
		});
	}
	
	private void foldStringConcats(Node tree) {
		tree.accept(new ForwardingAstVisitor() {
			@Override public boolean visitBinaryExpression(BinaryExpression node) {
				if (node.rawLeft() != null) node.rawLeft().accept(this);
				if (node.rawRight() != null) node.rawRight().accept(this);
				if (
						node.rawLeft() instanceof StringLiteral && node.rawRight() instanceof StringLiteral &&
						node.astOperator() == BinaryOperator.PLUS) {
					String left = ((StringLiteral) node.rawLeft()).astValue();
					String right = ((StringLiteral) node.rawRight()).astValue();
					int start = node.rawLeft().getPosition().getStart();
					int end = node.rawRight().getPosition().getEnd();
					if (left != null && right != null && node.getParent() != null) {
						node.getParent().replaceChild(node, new StringLiteral().astValue(left + right).setPosition(new Position(start, end)));
					}
				}
				return false;
			}
		});
	}
	
	protected String convertToString(Source source, Node tree) {
		foldStringConcats(tree);
		normalizeNumberLiterals(tree);
		StructureFormatter formatter = StructureFormatter.formatterWithoutPositions();
		formatter.skipProperty(IntegralLiteral.class, "value");
		formatter.skipProperty(FloatingPointLiteral.class, "value");
		formatter.skipProperty(CharLiteral.class, "value");
		formatter.skipProperty(StringLiteral.class, "value");
		tree.accept(new SourcePrinter(formatter));
		return formatter.finish();
	}
	
	protected Node parseWithLombok(Source source) {
		List<Node> nodes = source.getNodes();
		assertEquals(1, nodes.size());
		
		return nodes.get(0);
	}
	
	protected Node parseWithTargetCompiler(Source source) {
		Context context = new Context();
		
		Options.instance(context).put(OptionName.ENCODING, "UTF-8");
		
		JavaCompiler compiler = new JavaCompiler(context);
		compiler.genEndPos = true;
		compiler.keepComments = true;
		
		JCCompilationUnit cu = compiler.parse(new ContentBasedJavaFileObject(source.getName(), source.getRawInput()));
		return JcTreeConverter.convert(cu);
	}
}
