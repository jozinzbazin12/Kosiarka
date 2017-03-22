package harvester.app.harvesters;

public class Item {

	private String id;

	private String link;

	public String getId() {
		return id;
	}

	public String getLink() {
		return link;
	}

	public Item(String id, String link) {
		this.id = id;
		this.link = link;
	}

	@Override
	public String toString() {
		return String.format("%s - %s", id, link);
	}

	@Override
	public int hashCode() {
		return id.hashCode() + link.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Item)) {
			return false;
		}
		Item i = (Item) obj;
		return i.id.equals(id) && i.link.equals(link);
	}

}
