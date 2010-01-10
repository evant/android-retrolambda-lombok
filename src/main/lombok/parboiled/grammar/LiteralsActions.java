package lombok.parboiled.grammar;

import lombok.ast.BooleanLiteral;
import lombok.ast.CharLiteral;
import lombok.ast.FloatingPointLiteral;
import lombok.ast.IntegralLiteral;
import lombok.ast.Node;
import lombok.ast.NullLiteral;
import lombok.ast.StringLiteral;

import org.parboiled.BaseActions;

public class LiteralsActions extends BaseActions<Node> {
	public Node createNullLiteral(String raw) {
		return new NullLiteral().setRawValue(raw);
	}
	
	public Node createStringLiteral(String raw) {
		return new StringLiteral().setRawValue(raw);
	}
	
	public Node createCharLiteral(String raw) {
		return new CharLiteral().setRawValue(raw);
	}
	
	public Node createBooleanLiteral(String raw) {
		return new BooleanLiteral().setRawValue(raw);
	}
	
	public Node createNumberLiteral(String raw) {
		if (raw == null) return new IntegralLiteral();
		
		String v = raw.trim().toLowerCase();
		
		if (v.startsWith("0x")) {
			if (v.contains("p")) return new FloatingPointLiteral().setRawValue(raw);
			return new IntegralLiteral().setRawValue(raw);
		}
		
		if (v.contains(".") || v.endsWith("d") || v.endsWith("f")) return new FloatingPointLiteral().setRawValue(raw);
		else return new IntegralLiteral().setRawValue(raw);
	}
}
