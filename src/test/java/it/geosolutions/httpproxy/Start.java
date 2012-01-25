/*
 *  Copyright (C) 2007 - 2011 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.httpproxy;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.thread.BoundedThreadPool;

/**
 * Start class for test using JETTY server.
 * 
 * @author Tobia di Pisa at tobia.dipisa@geo-solutions.it
 */
@SuppressWarnings("deprecation")
public class Start {

private final static Logger LOGGER = Logger.getLogger(Start.class.toString());

/**
 * @param args
 */
public static void main(String[] args) {
    Server jettyServer = null;

    try {
        jettyServer = new Server();

        // /////////////////////////////////////////////////////
        // don't even think of serving more than XX requests
        // in parallel... we have a limit in our processing
        // and memory capacities.
        // /////////////////////////////////////////////////////

        BoundedThreadPool tp = new BoundedThreadPool();
        tp.setMaxThreads(50);

        SocketConnector conn = new SocketConnector();
        String portVariable = System.getProperty("jetty.port");
        int port = parsePort(portVariable);

        if (port <= 0) {
            port = 8082;
        }

        conn.setPort(port);
        conn.setThreadPool(tp);
        conn.setAcceptQueueSize(100);
        jettyServer.setConnectors(new Connector[] { conn });

        WebAppContext wah = new WebAppContext();
        wah.setContextPath("/http_proxy");
        wah.setWar("src/main/webapp");
        jettyServer.setHandler(wah);
        wah.setTempDirectory(new File("target/work"));

        jettyServer.start();

        // ////////////////////////////////////////////////////////////////////////
        // use this to test normal stop behavior, that is, to check stuff that
        // need to be done on container shutdown (and yes, this will make
        // jetty stop just after you started it...)
        // jettyServer.stop();
        // ////////////////////////////////////////////////////////////////////////

    } catch (Exception e) {
        if (LOGGER.isLoggable(Level.SEVERE))
            LOGGER.log(Level.SEVERE,
                    "Could not start the Jetty server: " + e.getMessage(), e);

        if (jettyServer != null) {
            try {
                jettyServer.stop();
            } catch (Exception e1) {
                if (LOGGER.isLoggable(Level.SEVERE))
                    LOGGER.log(Level.SEVERE, "Unable to stop the "
                            + "Jetty server:" + e1.getMessage(), e1);
            }
        }
    }
}

/**
 * @param portVariable
 * @return int
 */
private static int parsePort(String portVariable) {
    if (portVariable == null) {
        return -1;
    }

    try {
        return Integer.valueOf(portVariable).intValue();
    } catch (NumberFormatException e) {
        return -1;
    }
}
}
