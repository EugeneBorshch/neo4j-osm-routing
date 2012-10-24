package com.eugeneborshch.routecalculator.optimize;

import com.eugeneborshch.routecalculator.OsmEntityAttributeKey;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * OSM nodes doesn't always correspond to physical road junctions. This leads to the fact that
 * Neo4j graph is not optimal for route calculation. Because route will be calculated using nodes that are shape points
 * of the road rather than a junctions.
 * <p/>
 * So the idea is to merge graph in a such way that in the result:
 * - all nodes will represent real road junctions
 * - nodes and relations that doesn't relate to a physical junctions will be removed
 * - new relation between junction nodes will be created instead of merged nodes\relations
 * - new relation will receive merged shape points coordinates as a property (in a correct sequence)
 * <p/>
 * User: Eugene Borshch
 */
public class WayMerger {


    private List<List<Relationship>> roadParts;

    private Set<Relationship> visited;


    public List<List<Relationship>> merge(Long wayId, Node startNode) {
        visited = new HashSet<Relationship>();
        roadParts = new ArrayList<List<Relationship>>();

        List<Relationship> relationships = getOutRelationships(wayId, startNode);


        for (Relationship rel : relationships) {

            List<Relationship> mergeRels = new ArrayList<Relationship>();
            visited.add(rel);
            mergeRels.add(rel);

            //recursively traverse each possible branch outgoing from the start node
            //In most cases there would be only 1 branch.
            //And only in cases when there are loops there could be more.
            merge(wayId, rel.getEndNode(), mergeRels);
        }

        return roadParts;
    }

    private void merge(Long wayId, Node startNode, List<Relationship> accum) {
        List<Relationship> relationships = getOutRelationships(wayId, startNode);

        //is end of a way
        boolean isEnd = relationships.isEmpty();
        //if it is physical road junction
        boolean isJunction = getDegreeOf(startNode) > 2;

        boolean isFlushed = false;

        if (isEnd || isJunction) {
            if (accum.size() > 1) {
                roadParts.add(accum);
                isFlushed = true;
            }
        }

        List<Relationship> nextAccum = isFlushed ? new ArrayList<Relationship>() : accum;
        for (Relationship rel : relationships) {
            nextAccum.add(rel);
            visited.add(rel);
            merge(wayId, rel.getEndNode(), nextAccum);

        }
    }

    /**
     * Get OUTGOING relations from the given node that belong tho the specific way and hasn't been visited before.
     */
    private List<Relationship> getOutRelationships(Long wayId, Node node) {
        List<Relationship> result = new ArrayList<Relationship>();
        Iterable<Relationship> relationships = node.getRelationships(Direction.OUTGOING);

        for (Relationship relationship : relationships) {
            if (wayId.equals(relationship.getProperty(OsmEntityAttributeKey.WAY_ID.name()))
                    && !visited.contains(relationship)) {
                result.add(relationship);
            }
        }
        return result;
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
