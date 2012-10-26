package com.eugeneborshch.routecalculator;

import com.eugeneborshch.routecalculator.load.OsmImporter;
import com.eugeneborshch.routecalculator.optimize.OsmRoutingOptimizer;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.tooling.GlobalGraphOperations;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

public class RouteCalculatorTest {


    @Test
    public void calculateRoute() throws XMLStreamException, IOException {
        String directory = "./db";
        String mapFileName = "siem-reap.osm";

        if (new File(directory).exists()) {

            deleteDir(directory);


        }
        GraphDatabaseService db = new EmbeddedGraphDatabase(directory);


        new OsmImporter(db).importXML(mapFileName);

        //OsmRouteCalculator osmRouteCalculator = new OsmRouteCalculator(db, layerName);
        //osmRouteCalculator.findRoute();

        /*   GlobalGraphOperations graphOperations = GlobalGraphOperations.at(db);
        Iterable<Node> allNodes = graphOperations.getAllNodes();
        for (Node node : allNodes) {
            String dump = "" + node.getId() + " ";
            for (String key : node.getPropertyKeys()) {
                dump += key + "->" + node.getProperty(key) + "  ";
            }

            dump += " ||| ";
            for (Relationship rel : node.getRelationships()) {

                dump += rel.getId() + "   " + rel.getType() + "  ";
                for (String key : rel.getPropertyKeys()) {
                    dump += "(" + key + "->" + rel.getProperty(key) + ")  ";
                }
                dump += " || ";
            }


            System.out.println(dump);
        }*/


        GlobalGraphOperations graphOperations = GlobalGraphOperations.at(db);

        System.out.printf("BEFORE OPTIMIZE nodes =  %d  ways = %d  \n", nodesCount(graphOperations), relCount(graphOperations));

        new OsmRoutingOptimizer(db).optimize();

        System.out.printf("AFTER OPTIMIZE nodes =  %d  ways = %d  \n", nodesCount(graphOperations), relCount(graphOperations));

        db.shutdown();

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
