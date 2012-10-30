package com.eugeneborshch.routecalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Dump route to KML . Quick and dirty :)
 * <p/>
 * User: Eugene Borshch
 */
public class KmlBuilder {
    StringBuilder kml;

    public void start() {
        kml = new StringBuilder();
        kml.append("<?xml version='1.0' encoding='UTF-8'?>                                                            \n"
                + "<kml xmlns='http://www.opengis.net/kml/2.2'>                                                       \n"
                + "<Document>                                                                                         \n"
                + "     <name>Paths</name>                                                                            \n"
                + "     <Style id='route'>                                                                            \n"
                + "         <LineStyle>                                                                               \n"
                + "             <color>7D26CDFF</color>                                                               \n"
                + "             <width>5</width>                                                                      \n"
                + "         </LineStyle>                                                                              \n"
                + "         <PolyStyle>                                                                               \n"
                + "             <color>7D26CDFF</color>                                                               \n"
                + "         </PolyStyle>                                                                              \n"
                + "     </Style>                                                                                      \n");
    }

    public void addPoint(Coordinate coordinate, String name) {
        kml.append("    <Placemark>                                                                                   \n"
                + "         <name>" + name + "</name>                                                                 \n"
                + "         <Point>                                                                                   \n"
                + "             <coordinates>                                                                           ");
        kml.append("                " + coordinate.x + "," + coordinate.y + ",0." + "\n");
        kml.append("            </coordinates>                                                                        \n"
                + "         </Point>                                                                                  \n"
                + "     </Placemark>                                                                                  \n");
    }

    public void addLineString(LineString lineString, String name) {
        kml.append("    <Placemark>                                                                                   \n"
                + "         <name>" + name + "</name>                                                                 \n"
                + "         <styleUrl>route</styleUrl>                                                                \n"
                + "         <LineString>                                                                              \n"
                + "             <extrude>1</extrude>                                                                  \n"
                + "             <tessellate>1</tessellate>                                                            \n"
                + "             <coordinates>                                                                         \n");


        for (Coordinate coordinate : lineString.getCoordinates()) {
            kml.append("                " + coordinate.x + "," + coordinate.y + ",0." + "\n");
        }


        kml.append("            </coordinates>                                                                        \n"
                + "         </LineString>                                                                             \n"
                + "     </Placemark>                                                                                  \n");

    }


    public void finish() {
        kml.append("</Document>                                                                                       \n"
                + "</kml>                                                                                             \n");
    }

    public void writeToFile(File file) throws IOException {
        if (file.exists()) {
            file.delete();
        }

        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(kml.toString());
        fileWriter.close();

    }
}
