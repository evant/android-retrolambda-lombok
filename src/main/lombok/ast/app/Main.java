package lombok.ast.app;

import lombok.ast.Version;

public class Main {
	public static void main(String[] args) throws Exception {
		if (args.length > 0 && args[0].equals("--version")) {
			System.out.println(Version.getVersion());
			System.exit(0);
			return;
		}
		
		lombok.ast.printer.Main.main(args);
	}
}
