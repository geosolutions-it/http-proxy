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

import java.util.LinkedHashSet;

/**
 * ProxyConfig class to define the proxy configuration.
 * 
 * @author Tobia Di Pisa at tobia.dipisa@geo-solutions.it
 */
final class ProxyConfig {

/**
 * A list of regular expressions describing hostnames the proxy is permitted to
 * forward to
 */
private LinkedHashSet<String> hostnameWhitelist;

/**
 * A list of regular expressions describing MIMETypes the proxy is permitted to
 * forward
 */
private LinkedHashSet<String> mimetypeWhitelist;

/**
 * A list of regular expressions describing Request Types the proxy is permitted
 * to forward
 */
private LinkedHashSet<String> reqtypeWhitelist;

/**
 * Default constructor.
 */
public ProxyConfig() {
    super();
}

/**
 * @param hostnameWhitelist
 * @param mimetypeWhitelist
 */
public ProxyConfig(LinkedHashSet<String> hostnameWhitelist,
        LinkedHashSet<String> mimetypeWhitelist) {
    super();
    this.hostnameWhitelist = hostnameWhitelist;
    this.mimetypeWhitelist = mimetypeWhitelist;
}

/**
 * @return the hostnameWhitelist
 */
public LinkedHashSet<String> getHostnameWhitelist() {
    return hostnameWhitelist;
}

/**
 * @param hostnameWhitelist the hostnameWhitelist to set
 */
public void setHostnameWhitelist(LinkedHashSet<String> hostnameWhitelist) {
    this.hostnameWhitelist = hostnameWhitelist;
}

/**
 * @return the mimetypeWhitelist
 */
public LinkedHashSet<String> getMimetypeWhitelist() {
    return mimetypeWhitelist;
}

/**
 * @param mimetypeWhitelist the mimetypeWhitelist to set
 */
public void setMimetypeWhitelist(LinkedHashSet<String> mimetypeWhitelist) {
    this.mimetypeWhitelist = mimetypeWhitelist;
}

/**
 * @return the reqtypeWhitelist
 */
public LinkedHashSet<String> getReqtypeWhitelist() {
    return reqtypeWhitelist;
}

/**
 * @param reqtypeWhitelist the reqtypeWhitelist to set
 */
public void setReqtypeWhitelist(LinkedHashSet<String> reqtypeWhitelist) {
    this.reqtypeWhitelist = reqtypeWhitelist;
}

}
