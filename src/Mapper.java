import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;

/**
 * This is the main class for the mapping program. It extends the GUI abstract
 * class and implements all the methods necessary, as well as having a main
 * function.
 * 
 * @author tony
 */
public class Mapper extends GUI {
	public static final Color NODE_COLOUR = new Color(77, 113, 255);
	public static final Color SEGMENT_COLOUR = new Color(130, 130, 130);
	public static final Color HIGHLIGHT_COLOUR = new Color(255, 219, 77);

	// these two constants define the size of the node squares at different zoom
	// levels; the equation used is node size = NODE_INTERCEPT + NODE_GRADIENT *
	// log(scale)
	public static final int NODE_INTERCEPT = 1;
	public static final double NODE_GRADIENT = 0.8;

	// defines how much you move per button press, and is dependent on scale.
	public static final double MOVE_AMOUNT = 100;
	// defines how much you zoom in/out per button press, and the maximum and
	// minimum zoom levels.
	public static final double ZOOM_FACTOR = 1.3;
	public static final double MIN_ZOOM = 1, MAX_ZOOM = 200;

	// how far away from a node you can click before it isn't counted.
	public static final double MAX_CLICKED_DISTANCE = 0.15;

	// these two define the 'view' of the program, ie. where you're looking and
	// how zoomed in you are.
	private Location origin;
	private double scale;

	// our data structures.
	private Graph graph;

	private Map<Node, List<Restriction>> restrictions;

	// whether to calculate journey by time (true) or distance (false)
	protected static boolean isTime = false;


	@Override
	protected void redraw(Graphics g) {
		if (graph != null)
			graph.draw(g, getDrawingAreaDimension(), origin, scale);
	}

	@Override
	protected void onClick(MouseEvent e) {
		if (graph == null) return;
		Location clicked = Location.newFromPoint(e.getPoint(), origin, scale);
		// find the closest node.
		double bestDist = Double.MAX_VALUE;
		Node closest = null;

		for (Node node : graph.nodes.values()) {
			double distance = clicked.distance(node.location);
			if (distance < bestDist) {
				bestDist = distance;
				closest = node;
			}
		}

		// if it's close enough, highlight it and show some information.
		if (clicked.distance(closest.location) < MAX_CLICKED_DISTANCE) {
			graph.setHighlight(closest);
			getTextOutputArea().setText(closest.toString());
		}

		// start selected and goal not selected
		if (graph.start != null && graph.goal == null) {
			graph.goal = closest;
			getTextOutputArea().setText("Journey from "+graph.start.toString()+" to "+graph.goal.toString()+":\n");
			findRoute();
		} else {
			graph.start = closest;
			graph.goal = null;
		}
	}

	/**
	 * Implements A* search over graph, taking into account one-way streets.
	 */
	private void findRoute() {
		PriorityQueue<FringeElement> fringe = new PriorityQueue<>();
		Set<Node> visited = new HashSet<>();
		fringe.add(new FringeElement(graph.start, null, heuristic(graph.start), 0));
		while (!fringe.isEmpty()) {
			FringeElement current = fringe.poll();
			Node currentNode = current.getNode();
			if (!visited.contains(currentNode)) {
				visited.add(currentNode);
				currentNode.prev = current.getPrev();

				if (currentNode.equals(graph.goal)) break;

				segmentLoop:
				for (Segment s : currentNode.segments) {
					Node next = null;
					if (s.start.equals(currentNode)) {
						next = s.end;
					} else if (s.road.oneWay == 0) { // if not one-way, allow backwards direction
						next = s.start;
					}

					if (next != null && !visited.contains(next)) {
						if (restrictions.containsKey(currentNode)) {
							for (Restriction r : restrictions.get(currentNode)) {
								if (r.notAllowed(currentNode.prev, currentNode, next, s.road)) continue segmentLoop;
							}
						}

						double costSoFar = current.getCostSoFar();

						if (isTime) costSoFar += s.length/s.road.getSpeed(true);
						else costSoFar += s.length;

						double estCost = costSoFar + heuristic(next);
						assert current.getEstCost() <= estCost : "Inconsistent heuristic";
						fringe.add(new FringeElement(next, currentNode, estCost, costSoFar));
					}
				}
			}
		}
		//graph.setVisited(visited);

		constructPath();
	}

	/**
	 * Constructs path of Roads starting from the goal back to the finish, and
	 * highlights it on the GUI and displays information about determined journey.
	 */
	private void constructPath() {
		List<Road> path = new ArrayList<>();
		//List<Node> nodes = new ArrayList<>();
		graph.setHighlight(path);
		if (graph == null || graph.goal == null) return;
		Node current = graph.goal;
		double total = 0.0;
		while (!current.equals(graph.start)) {
			for (Segment s : current.segments) {
				if (s.start.equals(current.prev) || s.end.equals(current.prev)) {
					if (isTime) total += s.length / s.road.getSpeed(false);
					else total += s.length;

					Road newRd = s.road.copyOf();
					newRd.addSegment(s);
					if (path.isEmpty()) {
						path.add(newRd);
					} else {
						Road currentRd = path.get(path.size()-1);
						if (s.road.equals(currentRd)) {
							currentRd.addSegment(s);
						} else {
							path.add(newRd);
						}
					}
					break;
				}
			}
			//nodes.add(current);
			current = current.prev;
			if (current == null) {
				path.clear();
				getTextOutputArea().append("No path found.");
				return;
			}
		}
		//nodes.add(graph.start);
		//Collections.reverse(nodes);
		Collections.reverse(path);
		for (Road r : path) {
			getTextOutputArea().append(" - "+r.toString());
		}
		if (Mapper.isTime) getTextOutputArea().append("Total time: "+parseTime(total));
		else getTextOutputArea().append(String.format("Total distance: %.3f km\n", total));
	}

	private double heuristic(Node n) {
		if (isTime) return n.location.distance(graph.goal.location)/Road.MAX_SPEED;
		return n.location.distance(graph.goal.location);
	}

	public static String parseTime(double h) {
		double m = h % 1 * 60;
		int hours = (int) h;
		if (h < 1) {
			return String.format("%.3f minutes\n", m);
		}
		return String.format("%d hours %.3f minutes\n", hours, m);
	}

	@Override
	protected void onUnitChange(boolean newIsTime) {
		if (newIsTime && !isTime) {
			isTime = true;
			if (graph == null || graph.goal == null) return;
			getTextOutputArea().setText("Journey from "+graph.start.toString()+" to "+graph.goal.toString()+":\n");
			findRoute();
			redraw();
		} else if (!newIsTime && isTime) {
			isTime = false;
			if (graph == null || graph.goal == null) return;
			getTextOutputArea().setText("Journey from "+graph.start.toString()+" to "+graph.goal.toString()+":\n");
			findRoute();
		}
	}

	@Override
	protected void onSearch() {
		// Does nothing
	}

	@Override
	protected void onMove(Move m) {
		if (m == GUI.Move.NORTH) {
			origin = origin.moveBy(0, MOVE_AMOUNT / scale);
		} else if (m == GUI.Move.SOUTH) {
			origin = origin.moveBy(0, -MOVE_AMOUNT / scale);
		} else if (m == GUI.Move.EAST) {
			origin = origin.moveBy(MOVE_AMOUNT / scale, 0);
		} else if (m == GUI.Move.WEST) {
			origin = origin.moveBy(-MOVE_AMOUNT / scale, 0);
		} else if (m == GUI.Move.ZOOM_IN) {
			if (scale < MAX_ZOOM) {
				// yes, this does allow you to go slightly over/under the
				// max/min scale, but it means that we always zoom exactly to
				// the centre.
				scaleOrigin(true);
				scale *= ZOOM_FACTOR;
			}
		} else if (m == GUI.Move.ZOOM_OUT) {
			if (scale > MIN_ZOOM) {
				scaleOrigin(false);
				scale /= ZOOM_FACTOR;
			}
		}
	}

	@Override
	protected void onLoad(File nodes, File roads, File segments, File polygons, File rests) {
		graph = new Graph(nodes, roads, segments, polygons);
		origin = new Location(-6, 0); // close enough
		scale = 85;
		if (rests != null) restrictions = Parser.parseRestrictions(rests, graph);
		else restrictions = new HashMap<>();
		getTextOutputArea().setText("Click on a node to set the starting position, click again to set the goal position.");
	}

	/**
	 * This method does the nasty logic of making sure we always zoom into/out
	 * of the centre of the screen. It assumes that scale has just been updated
	 * to be either scale * ZOOM_FACTOR (zooming in) or scale / ZOOM_FACTOR
	 * (zooming out). The passed boolean should correspond to this, ie. be true
	 * if the scale was just increased.
	 */
	private void scaleOrigin(boolean zoomIn) {
		Dimension area = getDrawingAreaDimension();
		double zoom = zoomIn ? 1 / ZOOM_FACTOR : ZOOM_FACTOR;

		int dx = (int) ((area.width - (area.width * zoom)) / 2);
		int dy = (int) ((area.height - (area.height * zoom)) / 2);

		origin = Location.newFromPoint(new Point(dx, dy), origin, scale);
	}

	public static void main(String[] args) {
		new Mapper();
	}
}

// code for COMP261 assignments