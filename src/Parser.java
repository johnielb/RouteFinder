import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * This is the Stream version of the Parser.java file. 
 * The two files are essentially the same, so feel free to use either of them.
 * This file is an alternative of Parser.java.
 * It implements the methods using the Stream functionality.
 * It is faster, and help you learn a bit of recent and fancy Java functionalities.
 */
public class Parser {

    private Parser(){
        //empty private constructor
    }

	private static final Function<String, String[]> splitByTab =
			line -> line.split("[\t]+");

	public static Map<Integer, Node> parseNodes(
			File nodes,
			Graph graph)
	{
		try {
			final Function<Node, Integer> key = node -> node.nodeID;
			final Function<Node, Node> value = node -> node;
			final Function<String[], Node> nodeFrom =
                    tokens -> {
                        int nodeID = asInt(tokens[0]);
                        double lat = asDouble(tokens[1]),
                               lon = asDouble(tokens[2]);
                        return new Node(nodeID, lat, lon);
                    };

			return Files.lines(nodes.toPath())
						.parallel()
						.map(splitByTab)
						.map(nodeFrom)
                        .collect(Collectors.toMap(key, value));
		} catch (IOException | NumberFormatException e) {
			throw new RuntimeException("file reading failed.");
		}
	}

	public static Map<Integer, Road> parseRoads(
			File roads,
			Graph graph)
	{
		try{
			final Function<Road, Integer> key = road -> road.roadID;
			final Function<Road, Road> value = road -> road;
			final Function<String[], Road> toRoad =
                    tokens -> {
                        int roadID = asInt(tokens[0]),
                            type = asInt(tokens[1]);
                        String label = tokens[2],
                               city = tokens[3];
                        int oneway = asInt(tokens[4]),
                            speed = asInt(tokens[5]),
                            road_class = asInt(tokens[6]);

                        return new Road(
                                roadID, type, label,
                                city, oneway, speed,
                                road_class
                        );
                    };

			return Files.lines(roads.toPath())
						.skip(1)	//skip header
						.parallel()
						.map(splitByTab)
						.map(toRoad)
                        .collect(Collectors.toMap(key,value));
		}catch (IOException | NumberFormatException e){
			throw new RuntimeException("file reading failed.");
		}
	}

	public static Collection<Segment> parseSegments(
			File segments,
			Graph graph)
	{
		try{
		    final Function<String[], Segment> toSegment =
                    tokens -> {
                        int roadID = asInt(tokens[0]);
                        double length = asDouble(tokens[1]);
                        int node1ID = asInt(tokens[2]),
                            node2ID = asInt(tokens[3]);
                        double[] coords = asDouble(tokens,4);

                        return new Segment(
                                graph, roadID,
                                length, node1ID,
                                node2ID, coords
                        );
                    };

			return Files.lines(segments.toPath())
                        .skip(1)	//skip header
						.parallel()
                        .map(splitByTab)
                        .map(toSegment)
                        .collect(Collectors.toSet());
		}catch (IOException | NumberFormatException e){
			throw new RuntimeException("file reading failed.");
		}
	}

	public static Map<Node, List<Restriction>> parseRestrictions(
			File restrictions,
			Graph graph
	)
	{
		try{
			final Function<Restriction, Node> key = res -> res.curr;
			final Function<Restriction, List<Restriction>> value = res -> {
				List<Restriction> l = new ArrayList<>();
				l.add(res);
				return l;
			};
			final BinaryOperator<List<Restriction>> merge = (old, latest) -> {
				old.addAll(latest);
				return old;
			};
			final Function<String[], Restriction> toRestriction =
					tokens -> {
						int prevNodeID = asInt(tokens[0]);
						Node prev = graph.nodes.get(prevNodeID);
						int prevRdID = asInt(tokens[1]);
						Road prevRd = graph.roads.get(prevRdID);
						int currNodeID = asInt(tokens[2]);
						Node curr = graph.nodes.get(currNodeID);
						int nextRdID = asInt(tokens[3]);
						Road nextRd = graph.roads.get(nextRdID);
						int nextNodeID = asInt(tokens[4]);
						Node next = graph.nodes.get(nextNodeID);

						return new Restriction(
								prevRd, nextRd,
								prev, curr, next
						);
					};

			return Files.lines(restrictions.toPath())
					.skip(1)	//skip header
					.parallel()
					.map(splitByTab)
					.map(toRestriction)
					.collect(Collectors.toMap(key, value, merge));
		}catch (IOException | NumberFormatException e){
			throw new RuntimeException("file reading failed.");
		}
	}

	private static int asInt(String str)
	{
		return Integer.parseInt(str);
	}

	private static double asDouble(String str)
	{
		return Double.parseDouble(str);
	}

    private static double[] asDouble(
            String[] tokens,
            int skip)
    {
        if(skip < 0) skip = 0;
        return Stream.of(tokens)
                     .skip(skip)	//skip to read the coordinates
                     .mapToDouble(Parser::asDouble)
                     .toArray();
    }
}
// code for COMP261 assignments