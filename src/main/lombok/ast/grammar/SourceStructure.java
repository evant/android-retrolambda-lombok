package lombok.ast.grammar;

import lombok.Data;
import lombok.ast.Position;

@Data
public final class SourceStructure {
	private final Position position;
	private final String content;
}
