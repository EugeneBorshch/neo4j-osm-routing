package com.eugeneborshch.routecalculator.optimize;

import com.eugeneborshch.routecalculator.OsmEntityAttributeKey;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Search for the start node of the way.
 * <p/>
 * The idea is to find the start nodes algorithmically rather then updating Neo4j graph with special marks.
 * <p/>
 * User: Eugene Borshch
 */
public class WayStartNodeFinder {


    private GraphDatabaseService db;

    public WayStartNodeFinder(GraphDatabaseService db) {
        this.db = db;
    }

    public Node getStartNode(Long wayId) {

        IndexHits<Relationship> ways = db.index().forRelationships("ways").get(OsmEntityAttributeKey.WAY_ID.name(), wayId);
        while (ways.hasNext()) {
            Relationship relationship = ways.next();
            Iterator<Relationship> incoming = relationship.getStartNode().getRelationships(Direction.INCOMING).iterator();

            boolean isStartNode = true;
            while (incoming.hasNext()) {
                Relationship incomingRel = incoming.next();
                Long incomingWayId = (Long) incomingRel.getProperty(OsmEntityAttributeKey.WAY_ID.name());
                if (wayId.equals(incomingWayId)) {
                    isStartNode = false;
                    break;
                }
            }

            if (isStartNode) {
                return relationship.getStartNode();
            }
        }
        //Try roundabout
        return getStartNodeForRoundabout(db, wayId);
    }


    /**
     * In case of roundabout -> first node with degree of 3 would be taken as start node.
     */
    private Node getStartNodeForRoundabout(GraphDatabaseService db, Long wayId) {

        Set<Long> startNodes = new HashSet<Long>();
        Set<Long> endNodes = new HashSet<Long>();
        Node startNodeCandidate = null;
        boolean isRoundabout = false;

        IndexHits<Relationship> ways = db.index().forRelationships("ways").get(OsmEntityAttributeKey.WAY_ID.name(), wayId);
        while (ways.hasNext()) {
            Relationship relationship = ways.next();

            Node startNode = relationship.getStartNode();
            Node endNode = relationship.getEndNode();

            Long startNodeId = (Long) startNode.getProperty(OsmEntityAttributeKey.NODE_ID.name());
            Long endNodeId = (Long) endNode.getProperty(OsmEntityAttributeKey.NODE_ID.name());

            //put start\end nodes in cache
            startNodes.add(startNodeId);
            endNodes.add(endNodeId);

            //take the first node with degree > 2 as candidate for the start
            if (startNodeCandidate == null && getDegreeOf(startNode) > 2) {
                startNodeCandidate = startNode;
            } else if (startNodeCandidate == null && getDegreeOf(endNode) > 2) {
                startNodeCandidate = endNode;
            }

        }
        //check if we have a roundabout
        if (startNodes.containsAll(endNodes)) {
            isRoundabout = true;
        }


        return isRoundabout ? startNodeCandidate : null;
    }

    private int getDegreeOf(Node node) {

        int degree = 0;
        Iterable<Relationship> relationships = node.getRelationships();
        for (Relationship rel : relationships) {
            degree++;
        }
        return degree;
    }

}
