package com.eugeneborshch.routecalculator;

import com.eugeneborshch.routecalculator.load.OsmImporter;
import com.eugeneborshch.routecalculator.optimize.OsmRoutingOptimizer;
import com.eugeneborshch.routecalculator.route.RouteCalculator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.tooling.GlobalGraphOperations;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class RouteCalculatorTest {


    public static final long START_NODE_ID = 569878678;
    public static final long END_NODE_ID = 569853397;

    public static final String OSM_MAP = "siem-reap.osm";
    public static final String NEO4J_FOLDER = "./db";

    @Test
    public void calculateRoute() throws XMLStreamException, IOException {

        if (new File(NEO4J_FOLDER).exists()) {
            deleteDir(NEO4J_FOLDER);
        }


        //Init Neo4j database
        GraphDatabaseService db = new EmbeddedGraphDatabase(NEO4J_FOLDER);


        //Import OSM data into the Neo4j database
        new OsmImporter(db).importXML(OSM_MAP);


        //Optimize previously loaded database
        GlobalGraphOperations graphOperations = GlobalGraphOperations.at(db);
        System.out.printf("BEFORE OPTIMIZE nodes =  %d  ways = %d  \n", nodesCount(graphOperations), relCount(graphOperations));

        new OsmRoutingOptimizer(db).optimize();
        System.out.printf("AFTER OPTIMIZE nodes =  %d  ways = %d  \n", nodesCount(graphOperations), relCount(graphOperations));


        //Find route
        List<LineString> route = new RouteCalculator().findRoute(getStartNode(db), getEndNode(db));


        //Dump route to KML
        KmlBuilder kmlBuilder = new KmlBuilder();
        kmlBuilder.start();
        kmlBuilder.addPoint(getCoordinate(getStartNode(db)), "START");
        for (LineString lineString : route) {
            kmlBuilder.addLineString(lineString, "");
        }
        kmlBuilder.addPoint(getCoordinate(getEndNode(db)), "FINISH");
        kmlBuilder.finish();
        kmlBuilder.writeToFile(new File("siem-reap.kml"));


        //Close Neo4j
        db.shutdown();

    }

    private Node getStartNode(GraphDatabaseService db) {
        return db.index().forNodes("nodes").get(OsmEntityAttributeKey.NODE_ID.name(), START_NODE_ID).getSingle();
    }

    private Node getEndNode(GraphDatabaseService db) {
        return db.index().forNodes("nodes").get(OsmEntityAttributeKey.NODE_ID.name(), END_NODE_ID).getSingle();
    }

    private Coordinate getCoordinate(Node node) {
        Double lat = (Double) node.getProperty(OsmEntityAttributeKey.NODE_LAT.name());
        Double lon = (Double) node.getProperty(OsmEntityAttributeKey.NODE_LON.name());
        return new Coordinate(lon, lat);
    }


    private long nodesCount(GlobalGraphOperations graphOperations) {
        long count = 0;
        Iterable<Node> allNodes = graphOperations.getAllNodes();
        for (Node node : allNodes) {
            count++;
        }
        return count;
    }

    private long relCount(GlobalGraphOperations graphOperations) {
        long count = 0;
        Iterable<Relationship> all = graphOperations.getAllRelationships();
        for (Relationship rel : all) {
            count++;
        }
        return count;
    }


    public void deleteDir(String name) {
        File file = new File(name);
        if (file.exists()) {
            for (File f : file.listFiles()) {
                if (f.isDirectory()) {
                    deleteDir(f.getPath());
                } else {
                    f.delete();
                }
            }
            file.delete();
        }

    }
}
