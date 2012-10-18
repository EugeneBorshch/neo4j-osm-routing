package com.eugeneborshch.routecalculator.load;

import org.neo4j.graphdb.GraphDatabaseService;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Main SAX  handler of loading OSM data
 * User: Eugene Borshch
 */
public class MainOSMHandler extends DefaultHandler {
    private XMLReader reader;
    private GraphDatabaseService graphDb;

    public MainOSMHandler(XMLReader reader, GraphDatabaseService graphDb) {
        this.reader = reader;
        this.graphDb = graphDb;
    }

    public void startElement(String uri, String localName, String name, org.xml.sax.Attributes attributes) throws SAXException {

        if (name.equals("node")) {

            // Switch handler to parse the node element
            NodeHandler nodeHandler = new NodeHandler(reader, this);
            reader.setContentHandler(nodeHandler);
            nodeHandler.startElement(uri, localName, name, attributes);
        } else if (name.equals("way")) { //will handle "way" and it's "nd" sub-nodes

            // Switch handler to parse the way element
            WayHandler wayHandler = new WayHandler(reader, this);
            reader.setContentHandler(wayHandler);
            wayHandler.startElement(uri, localName, name, attributes);
        }
    }

    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }
}