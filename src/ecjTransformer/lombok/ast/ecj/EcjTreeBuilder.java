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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Node;
import lombok.ast.StrictListAccessor;
import lombok.ast.javac.JcTreeBuilder;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;

/**
 * Turns {@code lombok.ast} based ASTs into eclipse/ecj's {@code org.eclipse.jdt.internal.compiler.ast.ASTNode} model.
 */
public class EcjTreeBuilder extends ForwardingAstVisitor {
	List<? extends ASTNode> result = null;
	
	private <T extends ASTNode> List<T> toList(Class<T> type, StrictListAccessor<?, ?> accessor) {
		List<T> result = new ArrayList<T>();
		for (Node node : accessor) {
			EcjTreeBuilder visitor = new EcjTreeBuilder();
			node.accept(visitor);
			
			List<? extends ASTNode> values;
			
			try {
				values = visitor.getAll();
				if (values.size() == 0) throw new RuntimeException();
			} catch (RuntimeException e) {
				System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
				throw e;
			}
			
			for (ASTNode value : values) {
				if (value != null && !type.isInstance(value)) {
					throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + type.getName());
				}
				result.add(type.cast(value));
			}
		}
		return result;
	}
	
	private <T extends ASTNode> List<T> toList(Class<T> type, Node node) {
		if (node == null) return new ArrayList<T>();
		EcjTreeBuilder visitor = new EcjTreeBuilder();
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
	
	private void set(Node node, ASTNode value) {
		if (result != null) {
			throw new IllegalStateException("result is already set");
		}
		
		ASTNode actualValue = value;
		//TODO Handle parens
		List<ASTNode> result = new ArrayList<ASTNode>();
		result.add(actualValue);
		this.result = result;
	}
	
	private void set(List<? extends ASTNode> values) {
		if (result != null) throw new IllegalStateException("result is already set");
		result = values;
	}
}
