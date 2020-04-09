import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Road represents ... a road ... in our graph, which is some metadata and a
 * collection of Segments. We have lots of information about Roads, but don't
 * use much of it.
 * 
 * @author tony
 */
public class Road {
	public final int roadID;
	public final String name, city;
	public final Collection<Segment> components;
	public final int oneWay;
	public final int speed;
	public final int roadClass;
	public final int noCar;
	public final int noPed;
	public final int noBike;

	public Road(int roadID, int type, String label, String city, int oneway,
			int speed, int roadclass, int notforcar, int notforpede,
			int notforbicy) {
		this.roadID = roadID;
		this.city = city;
		this.name = label;
		this.components = new HashSet<Segment>();
		this.oneWay = oneway;
		this.speed = speed;
		roadClass = roadclass;
		noCar = notforcar;
		noPed = notforpede;
		noBike = notforbicy;
	}

	public void addSegment(Segment seg) {
		components.add(seg);
	}

	/**
	 * WARNING: the test for equivalency looks for the same road name.
	 * This is acceptable as equivalency is only tested between contiguous Segments, so it
	 * is very unlikely a street joins to a separate street with the same name.
	 * @param o object to compare
	 * @return if objects are equal
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Road road = (Road) o;
		return Objects.equals(name, road.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		double sum = 0.0;
		for (Segment s : components) sum += s.length;
		return String.format("%s: %.3f km\n", name, sum);
	}
}

// code for COMP261 assignments