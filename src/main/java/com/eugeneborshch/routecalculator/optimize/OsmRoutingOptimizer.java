package com.eugeneborshch.routecalculator.optimize;

import com.eugeneborshch.routecalculator.OsmEntityAttributeKey;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import scala.Option;
import scala.collection.immutable.Map;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimize loaded in Neo4j OSM map that it would be suitable for route calculations.
 * <p/>
 * User: Eugene Borshch
 */
public class OsmRoutingOptimizer {

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

        //TODO Merge in Neo4j itself

    }
}
