package lombok.ast.ecj;

import java.lang.reflect.Array;

import lombok.ast.StringLiteral;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.Clinit;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

public class EcjAstPrinter extends EcjAstVisitor {
	private final StringBuilder output = new StringBuilder();
	private int indent;
	private String rel;
	
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
	
	private void child(String rel, ASTNode node) {
		this.rel = rel;
		if (node != null) {
			visitEcjNode(node);
		} else {
			printNode("NULL");
			indent--;
		}
	}
	
	private void children(String rel, Object nodes) {
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
				child(String.format("%s[%d]", rel, i), (ASTNode)Array.get(nodes, i));
			}
		}
	}
	
	@Override
	public void visitBlock(Block node) {
		printNode(node);
		children("statements", node.statements);
		indent--;
	}
	
	@Override
	public void visitClinit(Clinit node) {
		printNode(node);
		indent--;
	}
	
	@Override
	public void visitCompilationUnitDeclaration(CompilationUnitDeclaration compilationUnitDeclaration) {
		printNode(compilationUnitDeclaration);
		child("currentPackage", compilationUnitDeclaration.currentPackage);
		children("imports", compilationUnitDeclaration.imports);
		children("types", compilationUnitDeclaration.types);
		indent--;
	}
	
	@Override
	public void visitConstructorDeclaration(ConstructorDeclaration constructorDeclaration) {
		printNode(constructorDeclaration);
		child("javadoc", constructorDeclaration.javadoc);
		children("annotations", constructorDeclaration.annotations);
		children("typeParameters", constructorDeclaration.typeParameters);
		children("arguments", constructorDeclaration.arguments);
		children("thrownExceptions", constructorDeclaration.thrownExceptions);
		child("constructorCall", constructorDeclaration.constructorCall);
		children("statements", constructorDeclaration.statements);
		indent--;
	}
	
	@Override
	public void visitTypeDeclaration(TypeDeclaration typeDeclaration) {
		printNode(typeDeclaration);
		child("javadoc", typeDeclaration.javadoc);
		children("annotations", typeDeclaration.annotations);
		child("superclass", typeDeclaration.superclass);
		children("superInterfaces", typeDeclaration.superInterfaces);
		children("typeParameters", typeDeclaration.typeParameters);
		children("memberTypes", typeDeclaration.memberTypes);
		children("fields", typeDeclaration.fields);
		children("methods", typeDeclaration.methods);
		indent--;
	}
	
	@Override
	public void visitImportReference(ImportReference importRef) {
		printNode(importRef);
		property("tokens", importRef.tokens);
		indent--;
	}
	
	@Override
	public void visitInitializer(Initializer initializer) {
		printNode(initializer);
		property("modifiers", initializer.modifiers);
		child("block", initializer.block);
		indent--;
	}
	
	@Override
	public void visitExplicitConstructorCall(ExplicitConstructorCall explicitConstructor) {
		printNode(explicitConstructor);
		property("accessMode", explicitConstructor.accessMode);
		child("qualification", explicitConstructor.qualification);
		children("typeArguments", explicitConstructor.typeArguments);
		children("arguments", explicitConstructor.arguments);
		indent--;
	}
	
	@Override
	public void visitEmptyStatement(EmptyStatement emptyStatement) {
		printNode(emptyStatement);
		indent--;
	}
}
