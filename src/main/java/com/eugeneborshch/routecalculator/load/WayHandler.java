package com.eugeneborshch.routecalculator.load;

import com.eugeneborshch.routecalculator.OsmEntityAttributeKey;
import com.eugeneborshch.routecalculator.OsmRelation;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Handle OSM ways (http://wiki.openstreetmap.org/wiki/Way) ant it's child tags
 * <p/>
 * User: Eugene Borshch
 */
public class WayHandler extends DefaultHandler {


    private XMLReader reader;
    private MainOSMHandler parent;

    private Long wayId;
    private List<Node> nodes;
    private boolean highWay;
    private boolean oneWay;
    private String wayName;


    public WayHandler(XMLReader reader, MainOSMHandler parent) {
        this.reader = reader;
        this.parent = parent;
        this.nodes = new LinkedList<Node>();
    }


    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

        if (!name.equals("nd") && !name.equals("way") && !name.equals("tag")) {
            throw new IllegalArgumentException("Try to parse unsupported tag = " + name);
        }

        //Fetch WAY name
        if (name.equals("way")) {
            wayId = Long.parseLong(attributes.getValue("id"));
        }

        //Fetch WAY nodes
        if (name.equals("nd")) {
            Long nodeId = Long.parseLong(attributes.getValue("ref"));
            Index<Node> nodeIndex = parent.getGraphDb().index().forNodes("nodes");
            IndexHits<Node> idxHits = nodeIndex.get(OsmEntityAttributeKey.NODE_ID.name(), nodeId);
            if (idxHits.size() != 1) {
                // throw new IllegalStateException("Can't find node in index = " + nodeId + " for way =" + wayId);
                System.err.println("Can't find node in index = " + nodeId + " for way =" + wayId);
            } else {
                nodes.add(idxHits.getSingle());
            }
        }

        //Fetch WAY tags
        if (name.equals("tag")) {
            highWay |= "highway".equals(attributes.getValue("k"));

            wayName = "name".equals(attributes.getValue("k")) ? attributes.getValue("v") : wayName;

            oneWay |= "oneway".equals(attributes.getValue("k")) && "yes".equalsIgnoreCase(attributes.getValue("v"));
        }
    }

    public void endElement(String uri, String localName, String name) throws SAXException {


        //Wait until we get closing <way> tag , so that <nd> and <tag> are already loaded
        if (name.equals("way")) {

            //we need only routing enabled ways
            if (highWay) {

                Node prevNode = null;
                for (Node node : nodes) {

                    if (prevNode != null) {

                        // According to OSM spec ONE_WAY attribution is set in the direction starting from the first node
                        Relationship wayPartRel = prevNode.createRelationshipTo(node, oneWay ? OsmRelation.ONE_WAY : OsmRelation.BIDIRECTIONAL);
                        wayPartRel.setProperty(OsmEntityAttributeKey.WAY_ID.name(), wayId);
                        //Add WAY name if exists
                        if (wayName != null && !wayName.isEmpty()) {
                            wayPartRel.setProperty(OsmEntityAttributeKey.WAY_NAME.name(), wayName);
                        }

                        //add way/relation to index
                        parent.getGraphDb().index().forRelationships("ways").add(wayPartRel,
                                OsmEntityAttributeKey.WAY_ID.name(),
                                wayId);
                    }
                    prevNode = node;
                }
            }
            // Switch handler back to our parent
            reader.setContentHandler(parent);
        }


    }


}
