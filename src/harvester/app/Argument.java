package harvester.app;

import java.util.HashMap;
import java.util.Map;

public enum Argument {
	HELP("help", true),
	BUY("buy"),
	ITEM("item"),
	PATH("path"),
	LOGIN("login", true),
	MAX_PRICE("max"),
	LIMIT("limit"),
	START("start"),
	BROWSER("browser.type"),
	WAIT("page.wait"),
	FIREFOX_BIN("webdriver.firefox.bin"),
	GECKO_DRIVER("webdriver.gecko.driver");

	private static final Map<String, Argument> lookup = new HashMap<>();

	private String arg;

	private boolean single;

	static {
		for (Argument d : Argument.values())
			lookup.put(d.getArg(), d);
	}

	private Argument(String arg) {
		this.arg = arg;
		this.single = false;
	}

	private Argument(String arg, boolean single) {
		this.arg = arg;
		this.single = single;
	}

	public String getArg() {
		return arg;
	}

	public static Argument get(String value) {
		return lookup.get(value);
	}

	public boolean isSingle() {
		return single;
	}
}
