#Route calculations using Neo4j graph database over OSM data

The goal of the project is to prove that [Neo4j](http://neo4j.org)  can be successfully used as
a route calculation engine for [OSM](http://www.openstreetmap.org) data.


##Problem

Talking about route calculation we want to think in terms of roads ([OSM  Way](http://wiki.openstreetmap.org/wiki/Way) )
and road junctions ([OSM  Node](http://wiki.openstreetmap.org/wiki/Node) ) .

However __OSM Nodes__ represents road shape points and does not always corresponds to real-world road junctions.

This leads to the situation when resulting road graph is overloaded with redundant nodes and relations.

##Implementation

####OSM Data Loading

```OsmImporter``` class is responsible for loading of OSM __nodes__ and only those __ways__ that are marked as 'highway' with
respect to __ONEWAY/BIDERECTIONAL__ attribution.

###Graph Optimization
```OsmRoutingOptimizer``` does the following:
 * removes nodes and corresponding relations that doesn't model real-world road junctions or start\end point of a road
 * creates new relations between survived nodes
 * each new relation will receive :
     * __WAY_DISTANCE__ attribute that will hold relation's distance in meters value
     * __WAY_GEOMETRY__ attribute that will contain WKT representation of road geometry(merged\removed nodes will go there). E.g.
```
LINESTRING (103.9740925 13.3479685, 103.9741521 13.3459453, 103.9741086 13.3455537)
```

![Google Earth](https://raw.github.com/EugeneBorshch/neo4j-osm-routing/gh-pages/graph.png)

####Route Calculation And Test
```RouteCalculator``` can be used to calculate route between two __nodes__ :

```
  List<LineString> route = new RouteCalculator().findRoute(getStartNode(db), getEndNode(db));
```

Calculated route could be visualized using __KML__ and __Google Earth__ application.

Route calculated somewhere in [Siem Reap](http://www.openstreetmap.org/?lat=13.361778259277344&lon=103.86045455932617&zoom=12) :)
![Google Earth](https://raw.github.com/EugeneBorshch/neo4j-osm-routing/gh-pages/googleearth.png)
