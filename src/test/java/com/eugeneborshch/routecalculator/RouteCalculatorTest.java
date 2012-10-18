package com.eugeneborshch.routecalculator;

import com.eugeneborshch.routecalculator.load.OsmImporter;
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

        GlobalGraphOperations graphOperations = GlobalGraphOperations.at(db);
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
        }

        db.shutdown();

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
