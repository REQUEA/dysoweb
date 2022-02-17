/*
 *  
 */

package com.requea.dysoweb.panel;

import java.net.InetAddress;


/**
 * A class that generates unique IDs over space and time without relying on the
 * MAC address or IP address of the machine.
 */
public class GuidGenerator {

    private static int IP;
    private static short counter = (short) 0;
    private static int JVM = (int) ( System.currentTimeMillis() >>> 8 );
    
    private static int toInt( byte[] bytes ) {
        int result = 0;
        for (int i=0; i<4; i++) {
            result = ( result << 8 ) - Byte.MIN_VALUE + (int) bytes[i];
        }
        return result;
    }
    
    /**
	 * Generates a GUID
	 */
	public static String generate() {
		GuidGenerator guid = new GuidGenerator();
		String sep = "";
        return new StringBuffer(36)
        	.append( guid.format( guid.getIP() ) ).append(sep)
        	.append( guid.format( guid.getJVM() ) ).append(sep)
        	.append( guid.format( guid.getHiTime() ) ).append(sep)
        	.append( guid.format( guid.getLoTime() ) ).append(sep)
        	.append( guid.format( guid.getCount() ) )
        	.toString();
	}
    
    
    
    /**
     * Unique across JVMs on this machine (unless they load this class
     * in the same quater second - very unlikely)
     */
    private int getJVM() {
        return JVM;
    }
    
    /**
     * Unique in a millisecond for this JVM instance (unless there
     * are > Short.MAX_VALUE instances created in a millisecond)
     */
    private short getCount() {
        synchronized(GuidGenerator.class) {
            if (counter<0) counter=0;
            return counter++;
        }
    }
    
    /**
     * Unique in a local network
     */
    private int getIP() {
    	if(IP == 0) {
            int ipadd;
            try {
                ipadd = toInt( InetAddress.getLocalHost().getAddress() );
            }
            catch (Exception e) {
                // use a fix address when no ip can be retrieved
                ipadd = -8355711;
            }
            IP = ipadd;
    	}
        return IP;
    }
    
    /**
     * Unique down to millisecond
     */
    private short getHiTime() {
        return (short) ( System.currentTimeMillis() >>> 32 );
    }
    private int getLoTime() {
        return (int) System.currentTimeMillis();
    }
    
    
    private String format(int intval) {
        String formatted = Integer.toHexString(intval);
        StringBuffer buf = new StringBuffer("00000000");
        buf.replace( 8-formatted.length(), 8, formatted );
        return buf.toString();
    }
    
    private String format(short shortval) {
        String formatted = Integer.toHexString(shortval);
        StringBuffer buf = new StringBuffer("0000");
        buf.replace( 4-formatted.length(), 4, formatted );
        return buf.toString();
    }
    
    
    /**
	 * Return string representation of this VMID.
	 */
    public String toString() {
        return generate();
    }
}
