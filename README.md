#Route calculations using Neo4j graph database over OSM data

The goal of the project is to prove that [Neo4j](http://neo4j.org)  can be successfully used as
a route calculation engine for [OSM](http://www.openstreetmap.org) data.


##Problem

Talking about route calculation we want to think in terms of roads ([OSM  Way](http://wiki.openstreetmap.org/wiki/Way) )
and road junctions ([OSM  Node](http://wiki.openstreetmap.org/wiki/Node) ) .

However *OSM Nodes* represents road shape points and does not always corresponds to real-world road junctions.

This leads to the situation when resulting road graph is overloaded with redundant nodes and relations.

##Implementation

##OSM Data Loading

```OsmImporter``` class is responsible for loading of OSM *nodes* and only those *ways* that are marked as 'highway' with
respect to *ONEWAY/BIDERECTIONAL* attribution.

#Graph Optimization
```OsmRoutingOptimizer``` does the following:
 * removes nodes and corresponding relations that doesn't model real-world road junctions or start\end point of a road
 * creates new relations between survived nodes
 * each new relation will receive :
 ** *WAY_DISTANCE* attribute that will hold relation's distance in meters value
 ** *WAY_GEOMETRY* attribute that will contain WKT representation of road geometry(merged\removed nodes will go there). E.g.
```
LINESTRING (103.9740925 13.3479685, 103.9741521 13.3459453, 103.9741086 13.3455537, 103.974126 13.3444397, 103.9741195 13.3438257)
```

#Route Calculation And Test
```RouteCalculator``` can be used to calculate route between two *nodes* :

```
  List<LineString> route = new RouteCalculator().findRoute(getStartNode(db), getEndNode(db));
```

Calculated route could be visualized using *KML* and *Google Earth* application.

Route calculated somewhere in [Siem Reap](http://www.openstreetmap.org/?lat=13.361778259277344&lon=103.86045455932617&zoom=12) :)

