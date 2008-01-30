package com.requea.dysoweb.xmpp.impl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.prefs.Preferences;

import org.apache.felix.shell.ShellService;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

public class XMPPShell {

	private boolean fActive;
	private Thread fHeartBeat;
	private XMPPConnection fCnx;
	private String fMaster;
	private boolean fParamsValid;
	private ShellService fShell;
	
    class HeartBeat implements Runnable {
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            while(fActive) {
                // check if the connexion is available on a regular basis
            	synchronized (XMPPShell.this) {
            		// check that we are still active
            		if(!fActive) {
            			// exit
            			return;
            		}
                    if(fCnx != null) {
                    	sendAvailablePresence("available");
                    } else if(fParamsValid) {
                    	// try to reconnect, if the parameters are valid
                    	try {
                    		connect();
                    	} catch(Exception e) {
                    		// ignore: will try later
                    	}
                    }
				}
                try {
                    Thread.sleep(10*1000);
                } catch (InterruptedException e) {
                    // the heart beat is dead
                    return;
                }
            }
            // thread has terminated
            fHeartBeat = null;
        }
    }

    
	
	public XMPPShell(ShellService shell) {
		fShell = shell;
	}

	public synchronized void start() {
		fActive = true;
		// try to reconnect with previous parameters
		try {
			connect();
		} catch(Exception e) {
			// ignore
		}
		// create the heartbeat thread (this thread will send regular available packets
		// and try to reconnect if connection is lost)
        fHeartBeat = new Thread(new HeartBeat(), "XMPP Heartbeat");
        fHeartBeat.start();
	}

	public synchronized void stop() {
		try {
			quit();
		} catch (Exception e) {
			// ignore
		}
		fActive = false;
	}
	
	public synchronized void connect() throws Exception {
		// check the connection params
		Preferences prefs = Preferences.userNodeForPackage(XMPPShell.class);
		if(prefs == null) {
			throw new Exception("No connection params defined. Please set the params first");
		}
		String host = prefs.get("host", "jabber.org");
		String port = prefs.get("port", "5222");
		String account = prefs.get("account", null);
		if(account == null) {
			throw new Exception("account missing. Please use set account=xxx");
		}
		String password = prefs.get("password", null);
		String master = prefs.get("master", null);
		
		fParamsValid = true;
		// if we are here, try to connect
		ConnectionConfiguration cfg = new ConnectionConfiguration(host, Integer.parseInt(port));
		XMPPConnection cnx = new XMPPConnection(cfg);
		cnx.connect();
		
		// then try to login
		cnx.login(account, password);
		
		// register the packet listener
		cnx.addPacketListener(new PacketListener() {

			public void processPacket(Packet packet) {
				if(packet instanceof Message) {
					Message msg = (Message)packet;
					String from = msg.getFrom();
					if(fMaster != null && !from.startsWith(fMaster)) {
						// not coming from master
						return;
					}
					// get the command from the body
					String cmd = msg.getBody();
					if(cmd == null || cmd.length() == 0) {
						return;
					}
					// other wise, executes the command and returns the response
                    try
                    {
                    	ByteArrayOutputStream bosOut = new ByteArrayOutputStream();
                    	PrintStream out = new PrintStream(bosOut);
                    	
                    	ByteArrayOutputStream bosErr = new ByteArrayOutputStream();
                    	PrintStream err = new PrintStream(bosErr);
                    	
                        fShell.executeCommand(cmd, out, err);
                        
                        String strOut = bosOut.toString();
                        String strErr = bosErr.toString();
                        // return
                        StringBuffer sb = new StringBuffer();
                        // error?
                        if(strErr.length() > 0) {
                        	sb.append("ERROR\n");
                        	sb.append(strErr);
                        	sb.append("\n");
                        }
                        if(strOut.length() > 0) {
                        	sb.append(strOut);
                        }
                        // provides a feeback if everything is empty
                        if(strOut.length() == 0 && strErr.length() == 0) {
                        	sb.append("Command executed");
                        }
                        
                        // sends back the response
                        Message msgResponse = new Message();
                        msgResponse.setThread(msg.getThread());
                        msgResponse.setTo(msg.getFrom());
                        msgResponse.setBody(sb.toString());
                        // send the response
                        fCnx.sendPacket(msgResponse);
                    }
                    catch (Exception ex)
                    {
                        System.err.println("XMPP Shell: " + ex);
                    }
				}
				
			}
			
		}, new PacketFilter() {

			public boolean accept(Packet packet) {
                // accept all messages packets
                return (packet instanceof Message);
			}
			
		});
		
		// we are connected
		if(fCnx != null) {
			try {
				fCnx.disconnect();
			} catch(Exception e) {
				// ignore
			}
		}
		fCnx = cnx;
		fMaster = master;
		// we are available
		sendAvailablePresence("available");
	}

	
	public synchronized void register() throws Exception {
		// check the connection params
		Preferences prefs = Preferences.userNodeForPackage(XMPPShell.class);
		if(prefs == null) {
			throw new Exception("No connection params defined. Please set the params first");
		}
		String host = prefs.get("host", "jabber.org");
		String port = prefs.get("port", "5222");
		String account = prefs.get("account", null);
		if(account == null) {
			throw new Exception("account missing. Please use set account=xxx");
		}
		String password = prefs.get("password", null);
		// open a connection to the server
		ConnectionConfiguration cfg = new ConnectionConfiguration(host, Integer.parseInt(port));
		XMPPConnection cnx = new XMPPConnection(cfg);
		cnx.connect();
		
		AccountManager mgr = cnx.getAccountManager();
		// try to create the account
		mgr.createAccount(account, password);

		// then connect to the server
		connect();
	}
	
	public synchronized void quit() throws Exception {
		fParamsValid = false;
		if(fCnx != null) {
			fCnx.disconnect(new Presence(Presence.Type.unavailable, "gone", 0, Presence.Mode.away));
		}
		fCnx = null;
	}
	
	private synchronized void sendAvailablePresence(String status) {
		if(fCnx != null) {
	        Packet p = new Presence(Presence.Type.available, status, 0, Presence.Mode.available);
	        fCnx.sendPacket(p);
		}
	}
	
}
