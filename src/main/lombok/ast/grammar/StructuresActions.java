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

import java.util.List;

import lombok.ast.Annotation;
import lombok.ast.AnnotationDeclaration;
import lombok.ast.AnnotationElement;
import lombok.ast.AnnotationMethodDeclaration;
import lombok.ast.ArrayInitializer;
import lombok.ast.ClassDeclaration;
import lombok.ast.CompilationUnit;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.DanglingNodes;
import lombok.ast.EmptyDeclaration;
import lombok.ast.EnumConstant;
import lombok.ast.EnumDeclaration;
import lombok.ast.EnumTypeBody;
import lombok.ast.ImportDeclaration;
import lombok.ast.InstanceInitializer;
import lombok.ast.InterfaceDeclaration;
import lombok.ast.KeywordModifier;
import lombok.ast.MethodDeclaration;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.NormalTypeBody;
import lombok.ast.PackageDeclaration;
import lombok.ast.Position;
import lombok.ast.StaticInitializer;
import lombok.ast.TypeBody;
import lombok.ast.TypeReference;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.grammar.TemporaryNode.MethodArguments;

public class StructuresActions extends SourceActions {
	public StructuresActions(Source source) {
		super(source);
	}
	
	public Node createMethodArguments(Node head, List<Node> tail) {
		MethodArguments ma = new MethodArguments();
		if (head != null) ma.arguments.add(head);
		if (tail != null) for (Node n : tail) ma.arguments.add(n);
		return posify(ma);
	}
	
	public Node createKeywordModifier(String text) {
		return posify(new KeywordModifier().astName(text));
	}
	
	public Node createMethodDeclaration(Node modifiers, Node typeParameters, Node resultType, Node name,
			Node params, List<org.parboiled.Node<Node>> dims, Node throwsHead, List<Node> throwsTail, Node body) {
		
		MethodDeclaration decl = new MethodDeclaration();
		
		if (params instanceof TemporaryNode.MethodParameters) {
			for (Node param : ((TemporaryNode.MethodParameters)params).parameters) {
				decl.rawParameters().addToEnd(param);
			}
		} else DanglingNodes.addDanglingNode(decl, params);
		
		decl.astMethodName(createIdentifierIfNeeded(name, currentPos())).rawBody(body);
		if (modifiers != null) decl.astModifiers(createModifiersIfNeeded(modifiers, currentPos()));
		int extraDims = dims == null ? 0 : dims.size();
		Node returnType = resultType;
		if (extraDims > 0 && returnType instanceof TypeReference) {
			((TypeReference)returnType).astArrayDimensions(((TypeReference)returnType).astArrayDimensions() + extraDims);
		}
		decl.rawReturnTypeReference(returnType);
		if (typeParameters instanceof TemporaryNode.OrphanedTypeVariables) {
			TemporaryNode.OrphanedTypeVariables otv = (TemporaryNode.OrphanedTypeVariables)typeParameters;
			if (otv.variables != null) for (Node typeParameter : otv.variables) {
				if (typeParameter != null) decl.rawTypeVariables().addToEnd(typeParameter);
			}
		}
		
		for (org.parboiled.Node<Node> dim : dims) {
			for (org.parboiled.Node<Node> dimSub : dim.getChildren()) {
				source.registerStructure(decl, dimSub);
			}
		}
		
		if (throwsHead != null) decl.rawThrownTypeReferences().addToEnd(throwsHead);
		if (throwsTail != null) for (Node n : throwsTail) if (n != null) decl.rawThrownTypeReferences().addToEnd(n);
		return posify(decl);
	}
	
	public Node createConstructorDeclaration(Node modifiers, Node typeParameters, Node name,
			Node params, Node throwsHead, List<Node> throwsTail, Node body) {
		
		ConstructorDeclaration decl = new ConstructorDeclaration().astTypeName(
				createIdentifierIfNeeded(name, currentPos())).rawBody(body);
		if (modifiers != null) decl.astModifiers(createModifiersIfNeeded(modifiers, currentPos()));
		if (typeParameters instanceof TemporaryNode.OrphanedTypeVariables) {
			for (Node typeParameter : ((TemporaryNode.OrphanedTypeVariables)typeParameters).variables) {
				decl.rawTypeVariables().addToEnd(typeParameter);
			}
		}
		
		if (params instanceof TemporaryNode.MethodParameters) {
			for (Node param : ((TemporaryNode.MethodParameters)params).parameters) {
				decl.rawParameters().addToEnd(param);
			}
		} else DanglingNodes.addDanglingNode(decl, params);
		
		if (throwsHead != null) decl.rawThrownTypeReferences().addToEnd(throwsHead);
		if (throwsTail != null) for (Node n : throwsTail) if (n != null) decl.rawThrownTypeReferences().addToEnd(n);
		return posify(decl);
	}
	
	public Node createModifiers(List<Node> values) {
		Modifiers result = new Modifiers();
		if (values != null) for (Node n : values) {
			if (n instanceof Annotation) result.rawAnnotations().addToEnd(n);
			if (n instanceof KeywordModifier) result.rawKeywords().addToEnd(n);
		}
		return posify(result);
	}
	
	public Node createMethodParameter(
			Node modifiers, Node type, String varargs, Node name,
			List<org.parboiled.Node<Node>> dimOpen, List<org.parboiled.Node<Node>> dimClosed) {
		
		VariableDefinitionEntry e = new VariableDefinitionEntry().astName(createIdentifierIfNeeded(name, currentPos()))
				.astArrayDimensions(dimOpen == null ? 0 : dimOpen.size());
		if (dimOpen != null) for (org.parboiled.Node<Node> pNode : dimOpen) {
			source.registerStructure(e, pNode);
		}
		if (dimClosed != null) for (org.parboiled.Node<Node> pNode : dimClosed) {
			source.registerStructure(e, pNode);
		}
		if (name != null) e.setPosition(new Position(name.getPosition().getStart(), currentPos()));
		VariableDefinition decl = new VariableDefinition().rawTypeReference(type);
		if (modifiers != null) decl.astModifiers(createModifiersIfNeeded(modifiers, currentPos()));
		if (varargs != null && !varargs.trim().isEmpty()) decl.astVarargs(true);
		decl.rawVariables().addToEnd(e);
		return posify(decl);
	}
	
	public Node createInstanceInitializer(Node body) {
		return posify(new InstanceInitializer().rawBody(body));
	}
	
	public Node createStaticInitializer(Node body) {
		return posify(new StaticInitializer().rawBody(body));
	}
	
	public Node createFieldDeclaration(Node variableDefinition, Node modifiers) {
		if (modifiers != null && variableDefinition instanceof VariableDefinition) {
			((VariableDefinition)variableDefinition).astModifiers(createModifiersIfNeeded(modifiers, currentPos()));
		}
		
		return posify(new VariableDeclaration().rawDefinition(variableDefinition));
	}
	
	public Node createVariableDefinitionPart(Node varName, List<String> dims, Node initializer) {
		return posify(new VariableDefinitionEntry().astName(createIdentifierIfNeeded(varName, currentPos()))
				.rawInitializer(initializer).astArrayDimensions(dims == null ? 0 : dims.size()));
	}
	
	public Node createVariableDefinition(Node type, Node head, List<Node> tail) {
		VariableDefinition result = new VariableDefinition().rawTypeReference(type);
		if (head != null) result.rawVariables().addToEnd(head);
		if (tail != null) for (Node n : tail) if (n != null) result.rawVariables().addToEnd(n);
		return posify(result);
	}
	
	public Node createAnnotationElementValueArrayInitializer(Node head, List<Node> tail) {
		ArrayInitializer result = new ArrayInitializer();
		if (head != null) result.rawExpressions().addToEnd(head);
		if (tail != null) for (Node n : tail) if (n != null) result.rawExpressions().addToEnd(n);
		return posify(result);
	}
	
	public Node createAnnotationElement(Node name, Node value) {
		return posify(new AnnotationElement().astName(createIdentifierIfNeeded(name, currentPos())).rawValue(value));
	}
	
	public Node createAnnotationFromElements(Node head, List<Node> tail) {
		Annotation result = new Annotation();
		if (head != null) result.rawElements().addToEnd(head);
		if (tail != null) for (Node n : tail) if (n != null) result.rawElements().addToEnd(n);
		return posify(result);
	}
	
	public Node createAnnotationFromElement(Node value) {
		Annotation result = new Annotation();
		if (value != null) {
			result.rawElements().addToEnd(posify(new AnnotationElement().rawValue(value)));
		}
		return posify(result);
	}
	
	public Node createAnnotation(Node type, Node annotation) {
		if (annotation instanceof Annotation) {
			return posify(((Annotation)annotation).rawAnnotationTypeReference(type));
		}
		return posify(new Annotation().rawAnnotationTypeReference(type));
	}
	
	public Node createExtendsClause(Node head, List<Node> tail) {
		TemporaryNode.ExtendsClause result = new TemporaryNode.ExtendsClause();
		if (head != null) result.superTypes.add(head);
		if (tail != null) for (Node n : tail) if (n != null) result.superTypes.add(n);
		return posify(result);
	}
	
	public Node createImplementsClause(Node head, List<Node> tail) {
		TemporaryNode.ImplementsClause result = new TemporaryNode.ImplementsClause();
		if (head != null) result.superInterfaces.add(head);
		if (tail != null) for (Node n : tail) if (n != null) result.superInterfaces.add(n);
		return posify(result);
	}
	
	public Node createInterfaceDeclaration(Node modifiers, Node name, Node params, Node body, List<Node> addons) {
		InterfaceDeclaration decl = new InterfaceDeclaration().astName(createIdentifierIfNeeded(name, currentPos())).rawBody(body);
		if (modifiers != null) decl.astModifiers(createModifiersIfNeeded(modifiers, currentPos()));
		if (params instanceof TemporaryNode.OrphanedTypeVariables) {
			TemporaryNode.OrphanedTypeVariables otv = (TemporaryNode.OrphanedTypeVariables)params;
			if (otv.variables != null) for (Node typeParameter : otv.variables) {
				if (typeParameter != null) decl.rawTypeVariables().addToEnd(typeParameter);
			}
		}
		
		if (addons != null) for (Node n : addons) {
			if (n instanceof TemporaryNode.ExtendsClause) {
				//if (!decl.extending().isEmpty()) //TODO add error node: multiple extends clauses.
				List<Node> superClasses = ((TemporaryNode.ExtendsClause)n).superTypes;
				if (superClasses != null) for (Node superClass : superClasses) if (superClass != null) decl.rawExtending().addToEnd(superClass);
			}
			
			//if (n instanceof TemporaryNode.ImplementsClause) //TODO add error node: implements not allowed here.
		}
		
		return posify(decl);
	}
	
	public Node createTypeDeclaration(String kind, Node modifiers, Node name, Node params, Node body, List<Node> addons) {
		if (kind.equals("interface")) return createInterfaceDeclaration(modifiers, name, params, body, addons);
		
		ClassDeclaration decl = new ClassDeclaration().astName(createIdentifierIfNeeded(name, currentPos())).rawBody(body);
		if (modifiers != null) decl.astModifiers(createModifiersIfNeeded(modifiers, currentPos()));
		if (params instanceof TemporaryNode.OrphanedTypeVariables) {
			TemporaryNode.OrphanedTypeVariables otv = (TemporaryNode.OrphanedTypeVariables)params;
			if (otv.variables != null) for (Node typeParameter : otv.variables) {
				if (typeParameter != null) decl.rawTypeVariables().addToEnd(typeParameter);
			}
		}
		
		if (addons != null) for (Node n : addons) {
			if (n instanceof TemporaryNode.ExtendsClause) {
				//if (!decl.implementing().isEmpty()) //TODO add error node: implements must come after extends
				//if (!decl.extending().isEmpty()) //TODO add error node: multiple extends clauses.
				List<Node> superClasses = ((TemporaryNode.ExtendsClause)n).superTypes;
				if (superClasses != null && superClasses.size() > 0) {
					//if (superClasses.size() > 1) //TODO add error node: 'extends' on class can only accept 1 type.
					decl.rawExtending(superClasses.get(0));
				}
			}
			
			if (n instanceof TemporaryNode.ImplementsClause) {
				//if (!decl.implementing().isEmpty()) //TODO add error node: multiple implements clauses.
				List<Node> interfaces = ((TemporaryNode.ImplementsClause)n).superInterfaces;
				if (interfaces != null) for (Node i : interfaces) decl.rawImplementing().addToEnd(i);
			}
		}
		return posify(decl);
	}
	
	public Node createNormalTypeBody(List<Node> values) {
		NormalTypeBody body = new NormalTypeBody();
		if (values != null) for (Node n : values) body.rawMembers().addToEnd(n);
		return posify(body);
	}
	
	public Node createEnumConstant(List<Node> annotations, Node name, Node arguments, Node body) {
		EnumConstant result = new EnumConstant().astName(createIdentifierIfNeeded(name, currentPos())).rawBody(body);
		if (annotations != null) for (Node n : annotations) if (n != null) result.rawAnnotations().addToEnd(n);
		if (arguments instanceof TemporaryNode.MethodArguments) {
			for (Node arg : ((TemporaryNode.MethodArguments)arguments).arguments) {
				result.rawArguments().addToEnd(arg);
			}
		}
		return posify(result);
	}
	
	public Node createEnumBody(Node head, List<Node> tail, Node typeBody) {
		EnumTypeBody body = new EnumTypeBody();
		if (head != null) body.rawConstants().addToEnd(head);
		if (tail != null) for (Node n : tail) body.rawConstants().addToEnd(n);
		if (typeBody instanceof TypeBody) {
			body.rawMembers().migrateAllFrom(((TypeBody)typeBody).rawMembers());
		}
		return posify(body);
	}
	
	public Node createEnumDeclaration(Node modifiers, Node name, Node body, List<Node> addons) {
		EnumDeclaration decl = new EnumDeclaration();
		decl.astName(createIdentifierIfNeeded(name, currentPos())).rawBody(body);
		if (modifiers != null) decl.astModifiers(createModifiersIfNeeded(modifiers, currentPos()));
		if (addons != null) for (Node n : addons) {
			//if (n instanceof ExtendsClause) //TODO add error node: implements not allowed here.
			if (n instanceof TemporaryNode.ImplementsClause) {
				//if (!decl.implementing().isEmpty()) //TODO add error node: multiple implements clauses.
				List<Node> interfaces = ((TemporaryNode.ImplementsClause)n).superInterfaces;
				if (interfaces != null) for (Node i : interfaces) decl.rawImplementing().addToEnd(i);
			}
		}
		return posify(decl);
	}
	
	public Node createAnnotationDeclaration(Node modifiers, Node name, List<Node> members, org.parboiled.Node<Node> typeOpen, org.parboiled.Node<Node> typeClose) {
		Node typeBody = createNormalTypeBody(members);
		if (typeOpen != null && typeClose != null) {
			typeBody.setPosition(new Position(typeOpen.getStartIndex(), typeClose.getEndIndex()));
		}
		AnnotationDeclaration decl = new AnnotationDeclaration().astName(createIdentifierIfNeeded(name, currentPos())).rawBody(typeBody);
		if (modifiers != null) decl.astModifiers(createModifiersIfNeeded(modifiers, currentPos()));
		return posify(decl);
	}
	
	public Node createAnnotationMethodDeclaration(Node modifiers, Node typeReference, Node name, List<org.parboiled.Node<Node>> dims, Node defaultValue) {
		AnnotationMethodDeclaration decl = new AnnotationMethodDeclaration()
				.astMethodName(createIdentifierIfNeeded(name, currentPos())).rawDefaultValue(defaultValue);
		if (modifiers != null) decl.astModifiers(createModifiersIfNeeded(modifiers, currentPos()));
		int extraDims = dims == null ? 0 : dims.size();
		Node returnType = typeReference;
		if (extraDims > 0 && returnType instanceof TypeReference) {
			((TypeReference)returnType).astArrayDimensions(((TypeReference)returnType).astArrayDimensions() + extraDims);
		}
		decl.rawReturnTypeReference(returnType);
		return posify(decl);
	}
	
	public Node createPackageDeclaration(List<Node> annotations, Node head, List<Node> tail) {
		PackageDeclaration decl = new PackageDeclaration();
		if (annotations != null) for (Node n : annotations) if (n != null) decl.rawAnnotations().addToEnd(n);
		if (head != null) decl.rawParts().addToEnd(head);
		if (tail != null) for (Node n : tail) if (n != null) decl.rawParts().addToEnd(n);
		return posify(decl);
	}
	
	public Node createImportDeclaration(String staticKeyword, Node head, List<Node> tail, String dotStar) {
		ImportDeclaration decl = new ImportDeclaration();
		if (head != null) decl.rawParts().addToEnd(head);
		if (tail != null) for (Node n : tail) decl.rawParts().addToEnd(n);
		if (staticKeyword != null && staticKeyword.length() > 0) decl.astStaticImport(true);
		if (dotStar != null && dotStar.length() > 0) decl.astStarImport(true);
		return posify(decl);
	}
	
	public Node createCompilationUnit(Node packageDeclaration, List<Node> importDeclarations, List<Node> typeDeclarations) {
		CompilationUnit unit = new CompilationUnit().rawPackageDeclaration(packageDeclaration);
		if (importDeclarations != null) for (Node n : importDeclarations) if (n != null) unit.rawImportDeclarations().addToEnd(n);
		if (typeDeclarations != null) for (Node n : typeDeclarations) if (n != null) unit.rawTypeDeclarations().addToEnd(n);
		return posify(unit);
	}
	
	public Node createMethodParameters(Node head, List<Node> tail) {
		TemporaryNode.MethodParameters params = new TemporaryNode.MethodParameters();
		if (head != null) params.parameters.add(head);
		if (tail != null) for (Node n : tail) if (n != null) params.parameters.add(n);
		
		return params;
	}
	
	public Node createEmptyDeclaration() {
		return posify(new EmptyDeclaration());
	}
}
