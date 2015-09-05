package harvester.app;

import java.util.HashMap;
import java.util.Map;

public enum Argument {
	HELP("-help"), URL("-url"), PATH("-path"), LOGIN("-login"), PASSWORD("-password"), LIMIT("-limit");

	private static final Map<String, Argument> lookup = new HashMap<>();

	private String arg;

	static {
		for (Argument d : Argument.values())
			lookup.put(d.getArg(), d);
	}

	private Argument(String arg) {
		this.arg = arg;
	}

	public String getArg() {
		return arg;
	}

	public static Argument get(String value) {
		return lookup.get(value);
	}
}
