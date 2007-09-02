/*
 * Created on Sep 4, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package com.requea.dysoweb.portlet.util;

/**
 * @author Pierre Dubois
 */
public class XMLException extends Exception {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = -2461632203507677008L;

    /**
     * Constructor for a chained exception.
     */
    public XMLException(Exception e) {
        super(e);
    }

    /**
     * Consturctor with a given message.
     */
    public XMLException(String msg) {
        super(msg);
    }
}
