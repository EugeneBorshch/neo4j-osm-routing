package com.eugeneborshch.routecalculator.load;

import com.eugeneborshch.routecalculator.OsmEntityAttributeKey;
import org.neo4j.graphdb.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Handle OSM Node  (http://wiki.openstreetmap.org/wiki/Node)
 * <p/>
 * User: Eugene Borshch
 */

public class NodeHandler extends DefaultHandler {


    private XMLReader reader;
    private MainOSMHandler parent;
    private Map<String, String> nodeAttributes;


    public NodeHandler(XMLReader reader, MainOSMHandler parent) {
        this.reader = reader;
        this.parent = parent;
        this.nodeAttributes = new HashMap<String, String>();
    }


    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        // Get the number of attribute
        int length = attributes.getLength();

        // Process each attribute
        for (int i = 0; i < length; i++) {
            // Get names and values for each attribute
            String attrName = attributes.getQName(i);
            String attrValue = attributes.getValue(i);

            nodeAttributes.put(attrName, attrValue);
        }
    }

    public void endElement(String uri, String localName, String name) throws SAXException {
        //create new OSM node
        Node node = parent.getGraphDb().createNode();

        for (Map.Entry<String, String> props : nodeAttributes.entrySet()) {
            if (props.getKey().equals("id")) {
                node.setProperty(OsmEntityAttributeKey.NODE_ID.name(), Long.parseLong(props.getValue()));
            }
            if (props.getKey().equals("lat")) {
                node.setProperty(OsmEntityAttributeKey.NODE_LAT.name(), Double.parseDouble(props.getValue()));
            }
            if (props.getKey().equals("lon")) {
                node.setProperty(OsmEntityAttributeKey.NODE_LON.name(), Double.parseDouble(props.getValue()));
            }
        }

        //add node to index
        parent.getGraphDb().index().forNodes("nodes").add(node,
                OsmEntityAttributeKey.NODE_ID.name(),
                node.getProperty(OsmEntityAttributeKey.NODE_ID.name()));

        // Switch handler back to our parent
        reader.setContentHandler(parent);
    }
}
