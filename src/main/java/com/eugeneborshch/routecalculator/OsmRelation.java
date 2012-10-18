package com.eugeneborshch.routecalculator;

import org.neo4j.graphdb.RelationshipType;

/**
 * Supported OSM relations
 * User: Eugene Borshch
 */
public enum OsmRelation implements RelationshipType {

    BIDIRECTIONAL,
    ONE_WAY
}
