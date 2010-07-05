package lombok.ast;

import lombok.ast.template.ParentAccessor;

public interface TypeBody extends Node {
	@ParentAccessor
	StrictListAccessor<TypeMember, ? extends TypeBody> astMembers();
	RawListAccessor<TypeMember, ? extends TypeBody> rawMembers();
	
	ConstructorInvocation upIfAnonymousClassToConstructorInvocation();
	EnumConstant upToEnumConstant();
	TypeDeclaration upToTypeDeclaration();
}
