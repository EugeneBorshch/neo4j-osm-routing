package com.eugeneborshch.routecalculator.route;

import com.eugeneborshch.routecalculator.OsmEntityAttributeKey;
import com.eugeneborshch.routecalculator.OsmRelation;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.EstimateEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Expander;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.Traversal;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculate route between two nodes using OSM data loaded into Neo4j database.
 * <p/>
 * User: Eugene Borshch
 */
public class RouteCalculator {


    private GeometryFactory geometryFactory = new GeometryFactory();

    public List<LineString> findRoute(Node startNode, Node endNode) {

        EstimateEvaluator<Double> estimateEval = CommonEvaluators.geoEstimateEvaluator(OsmEntityAttributeKey.NODE_LAT.name(),
                OsmEntityAttributeKey.NODE_LON.name());

        CostEvaluator<Double> costEval = CommonEvaluators.doubleCostEvaluator(OsmEntityAttributeKey.WAY_DISTANCE.name());

        Expander relExpander = Traversal.expanderForTypes(
                OsmRelation.ONE_WAY, Direction.OUTGOING,
                OsmRelation.BIDIRECTIONAL, Direction.BOTH);


        PathFinder<WeightedPath> finder = GraphAlgoFactory.aStar(relExpander, costEval, estimateEval);

        Path route = finder.findSinglePath(startNode, endNode);
        return getPathGeometries(route.relationships());
    }


    private List<LineString> getPathGeometries(Iterable<Relationship> relationships) {
        List<LineString> result = new ArrayList<LineString>();

        for (Relationship relationship : relationships) {

            boolean hasGeometry = relationship.hasProperty(OsmEntityAttributeKey.WAY_GEOMETRY.name());

            if (!hasGeometry) {
                result.add(createLineStringUsingNodes(relationship));

            } else {
                String geometryWKT = (String) relationship.getProperty(OsmEntityAttributeKey.WAY_GEOMETRY.name());

                WKTReader reader = new WKTReader();

                try {
                    result.add((LineString) reader.read(geometryWKT));
                } catch (ParseException e) {
                    Object wayId = relationship.getProperty(OsmEntityAttributeKey.WAY_ID.name());
                    System.out.printf("Failed to parse geometry for way = %s and wkt = %s \n ", wayId, geometryWKT);
                    result.add(createLineStringUsingNodes(relationship));

                }
            }

        }
        return result;
    }

    private LineString createLineStringUsingNodes(Relationship relationship) {
        Node startNode = relationship.getStartNode();
        Node endNode = relationship.getEndNode();

        Double startLat = (Double) startNode.getProperty(OsmEntityAttributeKey.NODE_LAT.name());
        Double startLon = (Double) startNode.getProperty(OsmEntityAttributeKey.NODE_LON.name());

        Double endLat = (Double) endNode.getProperty(OsmEntityAttributeKey.NODE_LAT.name());
        Double endLon = (Double) endNode.getProperty(OsmEntityAttributeKey.NODE_LON.name());

        Coordinate[] coords = new Coordinate[2];
        coords[0] = new Coordinate(startLon, startLat);
        coords[1] = new Coordinate(endLon, endLat);

        return geometryFactory.createLineString(coords);
    }
}
