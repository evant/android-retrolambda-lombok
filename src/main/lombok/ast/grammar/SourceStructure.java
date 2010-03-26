package lombok.ast.grammar;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
import lombok.ast.Position;

@Data
public final class SourceStructure {
	@Setter(AccessLevel.PACKAGE)
	@NonNull private Position position;
	@NonNull private final String content;
}
