package lombok.ast;

public interface JavadocContainer {
	Comment getJavadoc();
	Node getRawJavadoc();
	Node setJavadoc(Comment javadoc);
	Node setRawJavadoc(Node javadoc);
}
