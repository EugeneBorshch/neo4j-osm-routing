package com.eugeneborshch.routecalculator.optimize;

import com.eugeneborshch.routecalculator.OsmEntityAttributeKey;
import com.eugeneborshch.routecalculator.OsmRelation;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import scala.Option;
import scala.collection.immutable.Map;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Optimize loaded in Neo4j OSM map that it would be suitable for route calculations.
 * <p/>
 * User: Eugene Borshch
 */
public class OsmRoutingOptimizer {

    private GeometryFactory geometryFactory = new GeometryFactory();
    private GraphDatabaseService graphDb;
    private WayStartNodeFinder startNodeFinder;
    private WayMerger wayMerger;

    public OsmRoutingOptimizer(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
        this.startNodeFinder = new WayStartNodeFinder(graphDb);
        this.wayMerger = new WayMerger();
    }

    public void optimize() {

        ExecutionEngine engine = new ExecutionEngine(graphDb);
        ExecutionResult result = engine.execute("START n=relationship:ways('WAY_ID: *') RETURN distinct n.WAY_ID");
        while (result.hasNext()) {

            Map<String, Object> objectMap = result.next();
            Option<Object> option = objectMap.get("n.WAY_ID");
            Long wayId = (Long) option.get();

            optimizeWay(wayId);


        }
    }

    private void optimizeWay(Long wayId) {
        Node startNode = startNodeFinder.getStartNode(wayId);

        if (startNode == null) {
            System.err.println("Optimization skipped for way id " + wayId + "  start node = " + null);
            return;
        }

        List<List<Relationship>> roadPartsToMerge = wayMerger.merge(wayId, startNode);

        dumpPartsToLog(wayId, roadPartsToMerge);


        Transaction tx = graphDb.beginTx();

        optimize(wayId, roadPartsToMerge);

        tx.success();
        tx.finish();

    }

    private void optimize(Long wayId, List<List<Relationship>> roadPartsToMerge) {
        for (List<Relationship> partToMerge : roadPartsToMerge) {

            LinkedHashSet<Coordinate> coordinates = new LinkedHashSet<Coordinate>();

            String wayName = null;
            OsmRelation wayRelation = null;

            for (int i = 0; i < partToMerge.size(); i++) {
                Relationship chunk = partToMerge.get(i);

                //Create Coordinate objects for the nodes. Sequence matters!
                Node startNode = chunk.getStartNode();
                Node endNode = chunk.getEndNode();

                Double startLat = (Double) startNode.getProperty(OsmEntityAttributeKey.NODE_LAT.name());
                Double startLon = (Double) startNode.getProperty(OsmEntityAttributeKey.NODE_LON.name());

                Double endLat = (Double) endNode.getProperty(OsmEntityAttributeKey.NODE_LAT.name());
                Double endLon = (Double) endNode.getProperty(OsmEntityAttributeKey.NODE_LON.name());

                coordinates.add(new Coordinate(startLon, startLat));
                coordinates.add(new Coordinate(endLon, endLat));

                //merge
                if (partToMerge.size() == 1) {
                    //TODO nothing to merge
                    continue;
                }

                if (i == 0) {
                    wayRelation = chunk.isType(OsmRelation.BIDIRECTIONAL) ? OsmRelation.BIDIRECTIONAL : OsmRelation.ONE_WAY;
                    wayName = chunk.hasProperty(OsmEntityAttributeKey.WAY_NAME.name()) ?
                            (String) chunk.getProperty(OsmEntityAttributeKey.WAY_NAME.name()) : null;

                    //Start relationship -> only start node survives
                    graphDb.index().forRelationships("ways").remove(chunk);
                    chunk.delete();

                } else {
                    graphDb.index().forRelationships("ways").remove(chunk);
                    chunk.delete();

                    graphDb.index().forNodes("nodes").remove(startNode);
                    startNode.delete();

                }
            }


            Coordinate[] coords = getCoordinates(coordinates);
            if (partToMerge.size() == 1) {
                Relationship relationship = partToMerge.get(0);
                double length = getDistanceInMeters(coords);

                relationship.setProperty(OsmEntityAttributeKey.WAY_DISTANCE.name(), length);

            } else if (partToMerge.size() > 1) {

                //Create new relationship
                Node startNode = partToMerge.get(0).getStartNode();
                Node endNode = partToMerge.get(partToMerge.size() - 1).getEndNode();
                Relationship newRelationship = startNode.createRelationshipTo(endNode, wayRelation);

                //Set relationship properties
                newRelationship.setProperty(OsmEntityAttributeKey.WAY_ID.name(), wayId);
                if (wayName != null) {
                    newRelationship.setProperty(OsmEntityAttributeKey.WAY_NAME.name(), wayName);
                }

                //Set geometry and distance
                LineString lineString = geometryFactory.createLineString(coords);

                double length = getDistanceInMeters(coords);
                String textGeometry = lineString.toText();

                newRelationship.setProperty(OsmEntityAttributeKey.WAY_DISTANCE.name(), length);
                newRelationship.setProperty(OsmEntityAttributeKey.WAY_GEOMETRY.name(), textGeometry);

                //Add to index
                graphDb.index().forRelationships("ways").add(newRelationship,
                        OsmEntityAttributeKey.WAY_ID.name(),
                        wayId);
            }
        }
    }

    private Coordinate[] getCoordinates(LinkedHashSet<Coordinate> coordinates) {
        Coordinate[] coords = new Coordinate[coordinates.size()];
        int i = 0;
        for (Coordinate c : coordinates) {
            coords[i] = c;
            i++;
        }
        return coords;
    }


    public double getDistanceInMeters(Coordinate[] coord) {
        double distance = 0.0;

        if (coord.length < 2) {
            return distance;
        }

        Coordinate first = coord[0];

        for (Coordinate next : coord) {

            distance += DefaultEllipsoid.WGS84.orthodromicDistance(first.x, first.y, next.x, next.y);
            first = next;

        }

        return distance;
    }

    private void dumpPartsToLog(Long wayId, List<List<Relationship>> roadPartsToMerge) {
        String tmp = "wayId = " + wayId + " parts :";

        for (List<Relationship> chunk : roadPartsToMerge) {
            List<Long> nodesss = new ArrayList<Long>();
            for (Relationship elem : chunk) {
                nodesss.add((Long) elem.getStartNode().getProperty(OsmEntityAttributeKey.NODE_ID.name()));
                if (chunk.indexOf(elem) == chunk.size() - 1) {
                    nodesss.add((Long) elem.getEndNode().getProperty(OsmEntityAttributeKey.NODE_ID.name()));
                }
            }
            tmp += nodesss;
        }
        System.out.println(tmp);
    }
}
