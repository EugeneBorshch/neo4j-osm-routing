package com.eugeneborshch.routecalculator.load;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;

/**
 * Imports OSM data in Neo4j database.
 * <p/>
 * It only process information required for routing calculation(i.e. without user, timestamp, changelist,area... information.)
 * User: Eugene Borshch
 */
public class OsmImporter {

    private GraphDatabaseService graphDb;

    public OsmImporter(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }


    public void importXML(String xml) {

        Transaction tx = graphDb.beginTx();

        try {

            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            xmlReader.setContentHandler ( new MainOSMHandler(xmlReader, graphDb)) ;
            xmlReader.parse ( xml );

            // Hallelujah
            tx.success();

        } catch (SAXException e) {
            System.err.println(e);
            tx.failure();
        } catch (IOException e) {
            System.err.println(e);
            tx.failure();
        } finally {
            tx.finish();
        }
    }


}
