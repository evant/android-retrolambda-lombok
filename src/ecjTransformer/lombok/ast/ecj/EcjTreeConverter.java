package lombok.ast.ecj;

import java.util.List;
import java.util.Map;

import lombok.ast.Node;
import lombok.ast.RawListAccessor;
import lombok.ast.StrictListAccessor;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class EcjTreeConverter extends EcjTreeVisitor {
	private enum FlagKey {
		IMPORTDECLARATION_IS_PACKAGE;
	}
	
	private List<? extends Node> result = null;
	private Map<FlagKey, Object> params;
	
	private boolean hasFlag(FlagKey key) {
		return params.containsKey(key);
	}
	
	private Object getFlag(FlagKey key) {
		return params.get(key);
	}
	
	List<? extends Node> getAll() {
		return result;
	}
	
	Node get() {
		if (result.isEmpty()) {
			return null;
		}
		if (result.size() == 1) {
			return result.get(0);
		}
		throw new RuntimeException("Expected only one result but got " + result.size());
	}
	
	private void set(ASTNode node, Node value) {
		if (result != null) throw new IllegalStateException("result is already set");
		
		List<Node> result = Lists.newArrayList();
		if (value != null) result.add(value);
		this.result = result;
	}
	
	private void set(ASTNode node, List<? extends Node> values) {
		if (values.isEmpty()) System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
		
		if (result != null) throw new IllegalStateException("result is already set");
		this.result = values;
	}
	
	private Node toTree(ASTNode node, FlagKey... keys) {
		Map<FlagKey, Object> map = Maps.newEnumMap(FlagKey.class);
		for (FlagKey key : keys) map.put(key, key);
		return toTree(node, map);
	}
	
	private Node toTree(ASTNode node, Map<FlagKey, Object> params) {
		if (node == null) return null;
		EcjTreeConverter visitor = new EcjTreeConverter();
		visitor.params = params;
		visitor.visitEcjNode(node);
		try {
			return visitor.get();
		} catch (RuntimeException e) {
			System.err.printf("Node '%s' (%s) did not produce any results\n", node, node.getClass().getSimpleName());
			throw e;
		}
	}
	
	private void fillList(ASTNode[] nodes, RawListAccessor<?, ?> list) {
		if (nodes == null) return;
		
		for (ASTNode node : nodes) list.addToEnd(toTree(node));
	}
	
	public static Node convert(ASTNode node) {
		return new EcjTreeConverter().toTree(node);
	}
	
	private lombok.ast.Identifier toIdentifier(char[] token) {
		return lombok.ast.Identifier.of(token == null ? "" : new String(token));
	}
	
	private void fillIdentifiers(char[][] tokens, StrictListAccessor<lombok.ast.Identifier, ?> list) {
		if (tokens == null) return;
		for (char[] token : tokens) list.addToEnd(toIdentifier(token));
	}
	
	@Override public void visitCompilationUnitDeclaration(CompilationUnitDeclaration node) {
		lombok.ast.CompilationUnit unit = new lombok.ast.CompilationUnit();
		unit.rawPackageDeclaration(toTree(node.currentPackage, FlagKey.IMPORTDECLARATION_IS_PACKAGE));
		fillList(node.imports, unit.rawImportDeclarations());
		fillList(node.types, unit.rawTypeDeclarations());
		set(node, unit);
	}
	
	@Override public void visitImportReference(ImportReference node) {
		if (hasFlag(FlagKey.IMPORTDECLARATION_IS_PACKAGE)) {
			lombok.ast.PackageDeclaration pkg = new lombok.ast.PackageDeclaration();
			fillIdentifiers(node.tokens, pkg.astParts());
			fillList(node.annotations, pkg.rawAnnotations());
			set(node, pkg);
			return;
		}
		
		lombok.ast.ImportDeclaration imp = new lombok.ast.ImportDeclaration();
		fillIdentifiers(node.tokens, imp.astParts());
		imp.astStarImport((node.bits & ASTNode.OnDemand) != 0);
		imp.astStaticImport((node.modifiers & ClassFileConstants.AccStatic) != 0);
		set(node, imp);
	}
}
