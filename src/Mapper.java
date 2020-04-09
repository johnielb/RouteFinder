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


	@Override
	protected void redraw(Graphics g) {
		if (graph != null)
			graph.draw(g, getDrawingAreaDimension(), origin, scale);
	}

	@Override
	protected void onClick(MouseEvent e) {
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
			getTextOutputArea().setText("Start of journey: "+graph.start.toString()+"\nEnd of journey: "+ graph.goal.toString()+"\n");
			findRoute();
		} else {
			graph.start = closest;
			graph.goal = null;
		}
	}

	/**
	 * TODO: explain
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

				for (Segment s : currentNode.segments) {
					Node neigh = null;
					if (s.start.equals(currentNode)) {
						neigh = s.end;
					} else if (s.road.oneWay == 0) {
						neigh = s.start;
					}
					if (neigh != null && !visited.contains(neigh)) {
						double costSoFar = current.getCostSoFar() + s.length;
						double estCost = costSoFar + heuristic(neigh);
						fringe.add(new FringeElement(neigh, currentNode, estCost, costSoFar));
					}
				}
			}
		}
		//graph.setVisited(visited);

		List<Road> path = new ArrayList<>();
		Node current = graph.goal;
		double totalDistance = 0.0;
		while (!current.equals(graph.start)) {
			for (Segment s : current.segments) {
				if (s.start.equals(current.prev) || s.end.equals(current.prev)) {
					totalDistance += s.length;
					Road newRd = new Road(s.road.roadID, 0, s.road.name, s.road.city,
							0,0,0,0,0,0);
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

				}
			}
			current = current.prev;
		}
		Collections.reverse(path);
		graph.setHighlight(path);
		for (Road r : path) {
			getTextOutputArea().append(r.toString());
		}
		getTextOutputArea().append(String.format("Total distance: %.3f km\n", totalDistance));
	}

	private double heuristic(Node n) {
		return n.location.distance(graph.goal.location);
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
	protected void onLoad(File nodes, File roads, File segments, File polygons) {
		graph = new Graph(nodes, roads, segments, polygons);
		origin = new Location(-6, 0); // close enough
		scale = 85;
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