package lombok.ast.grammar;

import java.io.File;
import java.io.IOException;

import javax.tools.SimpleJavaFileObject;

class ContentBasedJavaFileObject extends SimpleJavaFileObject {
	private final String content;
	
	protected ContentBasedJavaFileObject(String name, String content) {
		super(new File(name).toURI(), Kind.SOURCE);
		this.content = content;
	}
	
	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		return content;
	}
}