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
package lombok.ast.syntaxChecks;

import static lombok.ast.syntaxChecks.MessageKey.*;
import static lombok.ast.Message.*;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import lombok.ast.AnnotationDeclaration;
import lombok.ast.AnnotationMethodDeclaration;
import lombok.ast.Block;
import lombok.ast.ClassDeclaration;
import lombok.ast.CompilationUnit;
import lombok.ast.EmptyDeclaration;
import lombok.ast.EnumDeclaration;
import lombok.ast.For;
import lombok.ast.ForEach;
import lombok.ast.InterfaceDeclaration;
import lombok.ast.KeywordModifier;
import lombok.ast.Message;
import lombok.ast.MethodDeclaration;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.StaticInitializer;
import lombok.ast.TypeDeclaration;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class KeywordChecks {
	private static final int K_PUBLIC       = 0x0001;
	private static final int K_PRIVATE      = 0x0002;
	private static final int K_PROTECTED    = 0x0004;
	private static final int K_STATIC       = 0x0008;
	private static final int K_FINAL        = 0x0010;
	private static final int K_SYNCHRONIZED = 0x0020;
	private static final int K_VOLATILE     = 0x0040;
	private static final int K_TRANSIENT    = 0x0080;
	private static final int K_NATIVE       = 0x0100;
	private static final int K_ABSTRACT     = 0x0400;
	private static final int K_STRICTFP     = 0x0800;
	private static final Map<String, Integer> TO_FLAG_MAP = ImmutableMap.<String, Integer>builder()
		.put("private", K_PRIVATE)
		.put("protected", K_PROTECTED)
		.put("public", K_PUBLIC)
		.put("final", K_FINAL)
		.put("native", K_NATIVE)
		.put("strictfp", K_STRICTFP)
		.put("synchronized", K_SYNCHRONIZED)
		.put("abstract", K_ABSTRACT)
		.put("static", K_STATIC)
		.put("transient", K_TRANSIENT)
		.put("volatile", K_VOLATILE)
		.build();
	
	private static final int[] METHOD_MODIFIERS_EXCLUSIVITY = {
		K_PRIVATE   | K_PROTECTED,
		K_PRIVATE   | K_PUBLIC,
		K_PROTECTED | K_PUBLIC,
		K_NATIVE    | K_STRICTFP,
		K_ABSTRACT  | K_FINAL,
		K_ABSTRACT  | K_NATIVE,
		K_ABSTRACT  | K_STATIC,
		K_ABSTRACT  | K_STRICTFP,
		K_ABSTRACT  | K_SYNCHRONIZED,
	};
	
	private static final int METHOD_MODIFIERS_LEGAL =
		K_PRIVATE | K_PROTECTED | K_PUBLIC | K_ABSTRACT | K_FINAL | K_NATIVE |
		K_STATIC | K_STRICTFP | K_SYNCHRONIZED
	;
	
	private static final int[] FIELD_MODIFIERS_EXCLUSIVITY = {
		K_PRIVATE | K_PROTECTED,
		K_PRIVATE | K_PUBLIC,
		K_PROTECTED | K_PUBLIC,
		K_VOLATILE | K_FINAL,
	};
	
	private static final int FIELD_MODIFIERS_LEGAL =
		K_PRIVATE | K_PROTECTED | K_PUBLIC | K_STATIC | K_FINAL | K_TRANSIENT | K_VOLATILE;
	
	private static final int[] TYPE_MODIFIERS_EXCLUSIVITY = {
		K_PRIVATE | K_PROTECTED,
		K_PRIVATE | K_PUBLIC,
		K_PROTECTED | K_PUBLIC,
		K_FINAL | K_ABSTRACT,
	};
	
	private static final int TYPE_MODIFIERS_LEGAL =
		K_PRIVATE | K_PROTECTED | K_PUBLIC | K_STATIC | K_FINAL | K_ABSTRACT | K_STRICTFP;
	
	public void duplicateKeywordModifierCheck(Modifiers modifiers) {
		List<String> keywords = Lists.newArrayList();
		
		for (KeywordModifier n : modifiers.astKeywords()) {
			String k = n.astName();
			if (k != null && !k.isEmpty() ) {
				if (keywords.contains(k)) {
					n.addMessage(Message.error(MODIFIERS_DUPLICATE_KEYWORD, "Duplicate modifier: " + k));
				} else {
					keywords.add(k);
				}
			}
		}
	}
	
	public void methodModifiersCheck(MethodDeclaration md) {
		modifiersCheck(md.astModifiers(),
				METHOD_MODIFIERS_EXCLUSIVITY, METHOD_MODIFIERS_LEGAL, "method declarations");
		checkStaticChain(md.astModifiers());
	}
	
	public void annotationMethodModifiersCheck(AnnotationMethodDeclaration md) {
		modifiersCheck(md.astModifiers(),
				METHOD_MODIFIERS_EXCLUSIVITY, METHOD_MODIFIERS_LEGAL & ~K_STRICTFP,
				"annotation method declarations");
		checkStaticChain(md.astModifiers());
	}
	
	public void fieldModifiersCheck(VariableDeclaration vd) {
		if (!(vd.getParent() instanceof TypeDeclaration)) return;
		VariableDefinition def = vd.astDefinition();
		if (def != null) {
			Modifiers m = def.astModifiers();
			modifiersCheck(m, FIELD_MODIFIERS_EXCLUSIVITY, FIELD_MODIFIERS_LEGAL, "field declarations");
			boolean allFieldsHaveInitializers = true;
			for (VariableDefinitionEntry entry : def.astVariables()) {
				if (entry.rawInitializer() == null) {
					allFieldsHaveInitializers = false;
					break;
				}
			}
			
			if (m.isStatic() && !m.isFinal() && !allFieldsHaveInitializers) {
				// Initialized static final fields, assuming the initializer expression is a compile time constant, are 'special' and
				// do not need to adhere to the static chain rule. However, we can't determine CTC nature without resolution.
				checkStaticChain(m);
			}
		}
	}
	
	public void localVariableModifiersCheck(VariableDefinition vd) {
		boolean applies = vd.getParent() instanceof VariableDeclaration && vd.getParent().getParent() instanceof Block;
		if (!applies) applies = vd.upToForEach() != null;
		if (!applies) applies = vd.upToFor() != null;
		if (!applies) return;
		
		modifiersCheck(vd.astModifiers(), new int[0], K_FINAL, "local variable declarations");
	}
	
	public void classDeclarationModifiersCheck(ClassDeclaration cd) {
		typeModifiersCheck(cd, "class declarations");
	}
	
	public void interfaceDeclarationModifiersCheck(InterfaceDeclaration id) {
		int flags = typeModifiersCheck(id, "interface declarations");
		generateNotAllowedKeywordError(id.astModifiers(), flags, K_FINAL, "final", "Interfaces cannot be final");
	}
	
	public void annotationDeclarationModifiersCheck(AnnotationDeclaration ad) {
		int flags = typeModifiersCheck(ad, "annotation declarations");
		generateNotAllowedKeywordError(ad.astModifiers(), flags, K_FINAL, "final", "Annotations cannot be final");
	}
	
	public void enumDeclarationModifiersCheck(EnumDeclaration ad) {
		int flags = typeModifiersCheck(ad, "enum declarations");
		generateNotAllowedKeywordError(ad.astModifiers(), flags, K_FINAL, "final", "Enums cannot be marked final");
	}
	
	private int typeModifiersCheck(TypeDeclaration td, String desc) {
		int flags = modifiersCheck(td.astModifiers(),
				TYPE_MODIFIERS_EXCLUSIVITY, TYPE_MODIFIERS_LEGAL, desc);
		boolean staticWarningEmitted = false;
		if (td.getParent() instanceof CompilationUnit) {
			generateNotAllowedKeywordError(td.astModifiers(), flags, K_PRIVATE, "private", "Top-level types cannot be private.");
			staticWarningEmitted |= generateNotAllowedKeywordError(td.astModifiers(), flags, K_STATIC, "static", "Top-level types cannot be static.");
		} else if (td.getParent() instanceof Block) {
			generateNotAllowedKeywordError(td.astModifiers(), flags, K_PRIVATE, "private", "Method-local types cannot be private.");
			generateNotAllowedKeywordError(td.astModifiers(), flags, K_PROTECTED, "protected", "Method-local types cannot be protected.");
			generateNotAllowedKeywordError(td.astModifiers(), flags, K_PUBLIC, "public", "Method-local types cannot be public.");
			staticWarningEmitted |= generateNotAllowedKeywordError(td.astModifiers(), flags, K_STATIC, "static", "Method-local types cannot be static.");
		}
		
		if (!staticWarningEmitted) checkStaticChain(td.astModifiers());
		
		return flags;
	}
	
	public void checkStaticInitializerInNonStaticType(StaticInitializer node) {
		if (node.getParent() instanceof TypeDeclaration) {
			Modifiers pMods = ((TypeDeclaration)node.getParent()).astModifiers();
			if (!pMods.isStatic()) {
				node.addMessage(Message.error(INITIALIZER_STATIC_IN_NON_STATIC_TYPE,
						"static initializers are only allowed in top-level or static types declarations."));
			}
		}
	}
	
	private void checkStaticChain(Modifiers modifiers) {
		if (!modifiers.isStatic()) return;
		
		Node p = modifiers.getParent();
		while (p != null) {
			if (p instanceof CompilationUnit) return;
			if (p instanceof TypeDeclaration) {
				Modifiers pMods = ((TypeDeclaration)p).astModifiers();
				if (!pMods.isStatic()) {
					modifiers.getParent().addMessage(Message.error(MODIFIERS_STATIC_CHAIN,
							"This declaration is (effectively) static; static declarations or only legal in top-level and static declarations."));
				}
			}
			p = p.getParent();
		}
	}
	
	private int modifiersCheck(Node rawModifiers, int[] exclusivity, int legality, String desc) {
		if (!(rawModifiers instanceof Modifiers)) return 0;
		Modifiers modifiers = (Modifiers) rawModifiers;
		int flags = modifiers.getEffectiveModifierFlags();
		int implicits = flags & ~modifiers.getExplicitModifierFlags();
		
		for (Node n : ((Modifiers)rawModifiers).rawKeywords()) {
			if (n instanceof KeywordModifier) {
				String k = ((KeywordModifier)n).astName();
				if (k == null || k.isEmpty()) {
					n.addMessage(Message.error(MODIFIERS_EMPTY_MODIFIER, "Empty/null modifier."));
				}
				
				if (!TO_FLAG_MAP.containsKey(k)) {
					n.addMessage(Message.error(MODIFIERS_UNKNOWN_MODIFIER, "Unknown modifier: " + k));
					continue;
				}
				
				int flag = TO_FLAG_MAP.get(k);
				if ((legality & flag) == 0) {
					n.addMessage(Message.error(MODIFIERS_MODIFIER_NOT_ALLOWED, "Modifier not allowed on " + desc + ": " + k));
					continue;
				}
				
				flags |= flag;
			}
		}
		
		for (int exclusive : exclusivity) {
			if ((flags & exclusive) == exclusive) {
				generateExclusivityError(implicits, exclusive, rawModifiers);
			}
		}
		
		return flags;
	}
	
	private boolean generateNotAllowedKeywordError(Modifiers modifiers, int flags, int flag, String keyword, String error) {
		if ((flags & flag) == 0) return false;
		
		for (KeywordModifier n : modifiers.astKeywords()) {
			if (keyword.equals(n.astName())) {
				n.addMessage(Message.error(MessageKey.MODIFIERS_MODIFIER_NOT_ALLOWED, error));
				return true;
			}
		}
		
		return false;
	}
	
	public void emptyDeclarationMustHaveNoModifiers(EmptyDeclaration node) {
		Node rawModifiers = node.astModifiers();
		if (!(rawModifiers instanceof Modifiers)) return;
		Modifiers modifiers = (Modifiers)rawModifiers;
		
		if (!modifiers.astKeywords().isEmpty() || !modifiers.astAnnotations().isEmpty()) {
			problems.add(new SyntaxProblem(node, "Empty Declarations cannot have modifiers."));
		}
	}
	
	private void generateExclusivityError(int implicit, int exclusive, Node rawModifiers) {
		if (!(rawModifiers instanceof Modifiers)) return;
		
		String hit = null;
		
		int responsibleImplicit = implicit & exclusive;
		
		if (responsibleImplicit != 0) {
			String nameOfResponsibleImplicit = "(unknown)";
			for (Map.Entry<String, Integer> x : TO_FLAG_MAP.entrySet()) {
				if (x.getValue() == responsibleImplicit) nameOfResponsibleImplicit = x.getKey();
			}
			
			for (Node n : ((Modifiers)rawModifiers).rawKeywords()) {
				if (n instanceof KeywordModifier) {
					String k = ((KeywordModifier)n).astName();
					if (!TO_FLAG_MAP.containsKey(k)) continue;
					int f = TO_FLAG_MAP.get(k);
					if ((f & exclusive) == 0) continue;
					
					problems.add(new SyntaxProblem(n, String.format(
							"Modifier %s cannot be used together with %s here", k, nameOfResponsibleImplicit)));
				}
			}
		} else {
			for (Node n : ((Modifiers)rawModifiers).rawKeywords()) {
				if (n instanceof KeywordModifier) {
					String k = ((KeywordModifier)n).astName();
					if (!TO_FLAG_MAP.containsKey(k)) continue;
					int f = TO_FLAG_MAP.get(k);
					if ((f & exclusive) == 0) continue;
					
					if (hit == null) {
						hit = k;
						continue;
					}
					
					problems.add(new SyntaxProblem(n, String.format(
							"Modifier %s cannot be used together with %s here", k, hit)));
				}
			}
		}
	}
}
