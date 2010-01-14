package lombok.ast.grammar;

import org.parboiled.Parboiled;

public class ParserGroup {
	public final BasicsParser basics = Parboiled.createParser(BasicsParser.class, this);
	public final LiteralsParser literals = Parboiled.createParser(LiteralsParser.class, this);
	public final ExpressionsParser operators = Parboiled.createParser(ExpressionsParser.class, this);
	public final StructuresParser structures = Parboiled.createParser(StructuresParser.class, this);
	public final TypesParser types = Parboiled.createParser(TypesParser.class, this);
	public final JavaParser java = Parboiled.createParser(JavaParser.class, this);
}
