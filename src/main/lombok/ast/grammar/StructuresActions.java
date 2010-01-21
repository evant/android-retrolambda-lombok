package lombok.ast.grammar;

import java.util.List;

import lombok.ast.Annotation;
import lombok.ast.AnnotationDeclaration;
import lombok.ast.AnnotationElement;
import lombok.ast.AnnotationMethodDeclaration;
import lombok.ast.ArrayInitializer;
import lombok.ast.ClassDeclaration;
import lombok.ast.CompilationUnit;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.EnumConstant;
import lombok.ast.EnumDeclaration;
import lombok.ast.ImportDeclaration;
import lombok.ast.InstanceInitializer;
import lombok.ast.InterfaceDeclaration;
import lombok.ast.KeywordModifier;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.PackageDeclaration;
import lombok.ast.StaticInitializer;
import lombok.ast.TypeBody;
import lombok.ast.TypeReference;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDeclarationEntry;

import org.parboiled.BaseActions;

public class StructuresActions extends BaseActions<Node> {
	public Node createMethodArguments(Node head, List<Node> tail) {
		MethodInvocation mi = new MethodInvocation();
		if (head != null) mi.arguments().addToEndRaw(head);
		if (tail != null) for (Node n : tail) mi.arguments().addToEndRaw(n);
		return mi;
	}
	
	public Node createKeywordModifier(String text) {
		return new KeywordModifier().setName(text);
	}
	
	public Node createMethodDeclaration(Node modifiers, Node typeParameters, Node resultType, Node name,
			List<Node> params, List<String> dims, Node throwsHead, List<Node> throwsTail, Node body) {
		
		MethodDeclaration decl = new MethodDeclaration().setRawMethodName(name).setRawBody(body);
		if (modifiers != null) decl.setRawModifiers(modifiers);
		int extraDims = dims == null ? 0 : dims.size();
		Node returnType = resultType;
		if (extraDims > 0 && returnType instanceof TypeReference) {
			((TypeReference)returnType).setArrayDimensions(((TypeReference)returnType).getArrayDimensions() + extraDims);
		}
		decl.setRawReturnTypeReference(returnType);
		if (typeParameters instanceof TemporaryNode.OrphanedTypeVariables) {
			TemporaryNode.OrphanedTypeVariables otv = (TemporaryNode.OrphanedTypeVariables)typeParameters;
			if (otv.variables != null) for (Node typeParameter : otv.variables) {
				if (typeParameter != null) decl.typeVariables().addToEndRaw(typeParameter);
			}
		}
		
		if (params != null) for (Node n : params) if (n != null) decl.parameters().addToEndRaw(n);
		if (throwsHead != null) decl.thrownTypeReferences().addToEndRaw(throwsHead);
		if (throwsTail != null) for (Node n : throwsTail) if (n != null) decl.thrownTypeReferences().addToEndRaw(n);
		return decl;
	}
	
	public Node createConstructorDeclaration(Node modifiers, Node typeParameters, Node name,
			List<Node> params, Node throwsHead, List<Node> throwsTail, Node body) {
		
		ConstructorDeclaration decl = new ConstructorDeclaration().setRawTypeName(name).setRawBody(body);
		if (modifiers != null) decl.setRawModifiers(modifiers);
		if (typeParameters instanceof TemporaryNode.OrphanedTypeVariables) {
			for (Node typeParameter : ((TemporaryNode.OrphanedTypeVariables)typeParameters).variables) {
				if (typeParameter != null) decl.typeVariables().addToEndRaw(typeParameter);
			}
		}
		
		if (params != null) for (Node n : params) if (n != null) decl.parameters().addToEndRaw(n);
		if (throwsHead != null) decl.thrownTypeReferences().addToEndRaw(throwsHead);
		if (throwsTail != null) for (Node n : throwsTail) if (n != null) decl.thrownTypeReferences().addToEndRaw(n);
		return decl;
	}
	
	public Node createModifiers(List<Node> values) {
		Modifiers result = new Modifiers();
		if (values != null) for (Node n : values) {
			if (n instanceof Annotation) result.annotations().addToEndRaw(n);
			if (n instanceof KeywordModifier) result.keywords().addToEndRaw(n);
		}
		return result;
	}
	
	public Node createMethodParameter(Node modifiers, Node type, String varargs, Node name, List<String> dims) {
		VariableDeclarationEntry e = new VariableDeclarationEntry().setRawName(name).setDimensions(dims == null ? 0 : dims.size());
		VariableDeclaration decl = new VariableDeclaration().setRawTypeReference(type);
		if (modifiers != null) decl.setRawModifiers(modifiers);
		if ("...".equals(varargs)) decl.setVarargs(true);
		decl.variables().addToEndRaw(e);
		return decl;
	}
	
	public Node createInstanceInitializer(Node body) {
		return new InstanceInitializer().setRawBody(body);
	}
	
	public Node createStaticInitializer(Node body) {
		return new StaticInitializer().setRawBody(body);
	}
	
	public Node addFieldModifiers(Node variableDeclaration, Node modifiers) {
		if (modifiers != null && variableDeclaration instanceof VariableDeclaration) {
			((VariableDeclaration)variableDeclaration).setRawModifiers(modifiers);
		}
		
		return variableDeclaration;
	}
	
	public Node createVariableDelarationPart(Node varName, List<String> dims, Node initializer) {
		return new VariableDeclarationEntry().setRawName(varName).setRawInitializer(initializer).setDimensions(dims == null ? 0 : dims.size());
	}
	
	public Node createVariableDeclaration(Node type, Node head, List<Node> tail) {
		VariableDeclaration result = new VariableDeclaration().setRawTypeReference(type);
		if (head != null) result.variables().addToEndRaw(head);
		if (tail != null) for (Node n : tail) if (n != null) result.variables().addToEndRaw(n);
		return result;
	}
	
	public Node createAnnotationElementValueArrayInitializer(Node head, List<Node> tail) {
		ArrayInitializer result = new ArrayInitializer();
		if (head != null) result.expressions().addToEndRaw(head);
		if (tail != null) for (Node n : tail) if (n != null) result.expressions().addToEndRaw(n);
		return result;
	}
	
	public Node createAnnotationElement(Node name, Node value) {
		return new AnnotationElement().setRawName(name).setRawValue(value);
	}
	
	public Node createAnnotationFromElements(Node head, List<Node> tail) {
		Annotation result = new Annotation();
		if (head != null) result.elements().addToEndRaw(head);
		if (tail != null) for (Node n : tail) if (n != null) result.elements().addToEndRaw(n);
		return result;
	}
	
	public Node createAnnotationFromElement(Node value) {
		Annotation result = new Annotation();
		if (value != null) result.elements().addToEndRaw(value);
		return result;
	}
	
	public Node createAnnotation(Node type, Node annotation) {
		if (annotation instanceof Annotation) {
			return ((Annotation)annotation).setRawAnnotationTypeReference(type);
		}
		return new Annotation().setRawAnnotationTypeReference(type);
	}
	
	public Node createExtendsClause(Node head, List<Node> tail) {
		TemporaryNode.ExtendsClause result = new TemporaryNode.ExtendsClause();
		if (head != null) result.superTypes.add(head);
		if (tail != null) for (Node n : tail) if (n != null) result.superTypes.add(n);
		return result;
	}
	
	public Node createImplementsClause(Node head, List<Node> tail) {
		TemporaryNode.ImplementsClause result = new TemporaryNode.ImplementsClause();
		if (head != null) result.superInterfaces.add(head);
		if (tail != null) for (Node n : tail) if (n != null) result.superInterfaces.add(n);
		return result;
	}
	
	public Node createInterfaceDeclaration(Node modifiers, Node name, Node params, Node body, List<Node> addons) {
		InterfaceDeclaration decl = new InterfaceDeclaration().setRawName(name).setRawBody(body);
		if (modifiers != null) decl.setRawModifiers(modifiers);
		if (params instanceof TemporaryNode.OrphanedTypeVariables) {
			TemporaryNode.OrphanedTypeVariables otv = (TemporaryNode.OrphanedTypeVariables)params;
			if (otv.variables != null) for (Node typeParameter : otv.variables) {
				if (typeParameter != null) decl.typeVariables().addToEndRaw(typeParameter);
			}
		}
		
		if (addons != null) for (Node n : addons) {
			if (n instanceof TemporaryNode.ExtendsClause) {
				//if (!decl.extending().isEmpty()) //TODO add error node: multiple extends clauses.
				List<Node> superClasses = ((TemporaryNode.ExtendsClause)n).superTypes;
				if (superClasses != null) for (Node superClass : superClasses) if (superClass != null) decl.extending().addToEndRaw(n);
			}
			
			//if (n instanceof TemporaryNode.ImplementsClause) //TODO add error node: implements not allowed here.
		}
		
		return decl;
	}
	
	public Node createTypeDeclaration(String kind, Node modifiers, Node name, Node params, Node body, List<Node> addons) {
		if (kind.equals("interface")) return createInterfaceDeclaration(modifiers, name, params, body, addons);
		
		ClassDeclaration decl = new ClassDeclaration().setRawName(name).setRawBody(body);
		if (modifiers != null) decl.setRawModifiers(modifiers);
		if (params instanceof TemporaryNode.OrphanedTypeVariables) {
			TemporaryNode.OrphanedTypeVariables otv = (TemporaryNode.OrphanedTypeVariables)params;
			if (otv.variables != null) for (Node typeParameter : otv.variables) {
				if (typeParameter != null) decl.typeVariables().addToEndRaw(typeParameter);
			}
		}
		
		if (addons != null) for (Node n : addons) {
			if (n instanceof TemporaryNode.ExtendsClause) {
				//if (!decl.implementing().isEmpty()) //TODO add error node: implements must come after extends
				//if (!decl.extending().isEmpty()) //TODO add error node: multiple extends clauses.
				List<Node> superClasses = ((TemporaryNode.ExtendsClause)n).superTypes;
				if (superClasses != null && superClasses.size() > 0) {
					//if (superClasses.size() > 1) //TODO add error node: 'extends' on class can only accept 1 type.
					decl.setRawExtending(superClasses.get(0));
				}
			}
			
			if (n instanceof TemporaryNode.ImplementsClause) {
				//if (!decl.implementing().isEmpty()) //TODO add error node: multiple implements clauses.
				List<Node> interfaces = ((TemporaryNode.ImplementsClause)n).superInterfaces;
				if (interfaces != null) for (Node i : interfaces) if (i != null) decl.implementing().addToEndRaw(i);
			}
		}
		return decl;
	}
	
	public Node createTypeBody(List<Node> values) {
		TypeBody body = new TypeBody();
		if (values != null) for (Node n : values) if (n != null) body.members().addToEndRaw(n);
		return body;
	}
	
	public Node createEnumConstant(List<Node> annotations, Node name, Node arguments, Node body) {
		EnumConstant result = new EnumConstant().setRawName(name).setRawBody(body);
		if (annotations != null) for (Node n : annotations) if (n != null) result.annotations().addToEndRaw(n);
		if (arguments instanceof MethodInvocation) result.arguments().migrateAllFromRaw(((MethodInvocation)arguments).arguments());
		return result;
	}
	
	public Node createEnumFromContents(Node head, List<Node> tail, Node body) {
		EnumDeclaration decl = new EnumDeclaration().setRawBody(body);
		if (head != null) decl.constants().addToEndRaw(head);
		if (tail != null) for (Node n : tail) if (n != null) decl.constants().addToEndRaw(n);
		return decl;
	}
	
	public Node createEnumDeclaration(Node modifiers, Node name, Node enumDeclaration, List<Node> addons) {
		if (enumDeclaration != null && (enumDeclaration instanceof EnumDeclaration)) return enumDeclaration;
		EnumDeclaration decl = enumDeclaration == null ? new EnumDeclaration() : (EnumDeclaration)enumDeclaration;
		decl.setRawName(name);
		if (modifiers != null) decl.setRawModifiers(modifiers);
		if (addons != null) for (Node n : addons) {
			//if (n instanceof ExtendsClause) //TODO add error node: implements not allowed here.
			if (n instanceof TemporaryNode.ImplementsClause) {
				//if (!decl.implementing().isEmpty()) //TODO add error node: multiple implements clauses.
				List<Node> interfaces = ((TemporaryNode.ImplementsClause)n).superInterfaces;
				if (interfaces != null) for (Node i : interfaces) if (i != null) decl.implementing().addToEndRaw(i);
			}
		}
		return decl;
	}
	
	public Node createAnnotationDeclaration(Node modifiers, Node name, List<Node> members) {
		AnnotationDeclaration decl = new AnnotationDeclaration().setRawName(name).setRawBody(createTypeBody(members));
		if (modifiers != null) decl.setRawModifiers(modifiers);
		return decl;
	}
	
	public Node createAnnotationMethodDeclaration(Node modifiers, Node typeReference, Node name, Node defaultValue) {
		AnnotationMethodDeclaration decl = new AnnotationMethodDeclaration().setRawMethodName(name).setRawDefaultValue(defaultValue).setRawReturnTypeReference(typeReference);
		if (modifiers != null) decl.setRawModifiers(modifiers);
		return decl;
	}
	
	public Node createPackageDeclaration(List<Node> annotations, Node head, List<Node> tail) {
		PackageDeclaration decl = new PackageDeclaration();
		if (annotations != null) for (Node n : annotations) if (n != null) decl.annotations().addToEndRaw(n);
		if (head != null) decl.parts().addToEndRaw(head);
		if (tail != null) for (Node n : tail) if (n != null) decl.parts().addToEndRaw(n);
		return decl;
	}
	
	public Node createImportDeclaration(String staticKeyword, Node head, List<Node> tail, String dotStar) {
		ImportDeclaration decl = new ImportDeclaration();
		if (head != null) decl.parts().addToEndRaw(head);
		if (tail != null) for (Node n : tail) if (n != null) decl.parts().addToEndRaw(n);
		if (staticKeyword != null && staticKeyword.length() > 0) decl.setStaticImport(true);
		if (dotStar != null && dotStar.length() > 0) decl.setStarImport(true);
		return decl;
	}
	
	public Node createCompilationUnit(Node packageDeclaration, List<Node> importDeclarations, List<Node> typeDeclarations) {
		CompilationUnit unit = new CompilationUnit().setRawPackageDeclaration(packageDeclaration);
		if (importDeclarations != null) for (Node n : importDeclarations) if (n != null) unit.importDeclarations().addToEndRaw(n);
		if (typeDeclarations != null) for (Node n : typeDeclarations) if (n != null) unit.typeDeclarations().addToEndRaw(n);
		return unit;
	}
}
