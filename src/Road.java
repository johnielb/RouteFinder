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
	public final String name;
	public final int type;
	public final String city;
	public final Collection<Segment> components;
	public final int oneWay;
	public final int speed;
	public final int roadClass;
	public static final double MAX_SPEED = 110.0;

	public Road(int roadID, int type, String label, String city, int oneway,
			int speed, int roadclass) {
		this.roadID = roadID;
		this.type = type;
		this.city = city;
		this.name = label;
		this.components = new HashSet<>();
		this.oneWay = oneway;
		this.speed = speed;
		roadClass = roadclass;
	}

	public void addSegment(Segment seg) {
		components.add(seg);
	}

	/**
	 * Convert speed classification into actual speed.
	 * Classification of 7 (no speed limit) follows maximum speed limit 6 = 110 km/h
	 * @return speed of road in km/h
	 */
	public double getSpeed(boolean isSearching) {
		double weight = 0.06; // how much influence road class has
		double handicap = 1.0;
		if (isSearching) handicap -= weight*(roadClass+1);
		assert handicap > 0 : "Speed reduction parameters are too strong, results in negative speed.";
		switch (speed) {
			case 0:
				return 5.0*handicap;
			case 1:
				return 20.0*handicap;
			case 2:
				return 40.0*handicap;
			case 3:
				return 60.0*handicap;
			case 4:
				return 80.0*handicap;
			case 5:
				return 100.0*handicap;
			default:
				return MAX_SPEED;
		}
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
		if (Mapper.isTime) {
			for (Segment s : components) sum += s.length / s.road.getSpeed(false);
			return name+": "+Mapper.parseTime(sum);
		}
		for (Segment s : components) sum += s.length;
		return String.format("%s: %.3f km\n", name, sum);
	}
}

// code for COMP261 assignments