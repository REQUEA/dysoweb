package com.requea.dysoweb.xmpp.impl;

import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import org.apache.felix.shell.Command;

public class XMPPCommandImpl implements Command {

	private XMPPShell fMessenger;

	public XMPPCommandImpl(XMPPShell messenger) {
		fMessenger = messenger;
	}

	public void execute(String s, PrintStream out, PrintStream err) {

		StringTokenizer st = new StringTokenizer(s, " ");
		
		// ignore the first token since it is the "xmpp" command
		st.nextToken();
		
		if(!st.hasMoreElements()) {
			// print usage since the user did not get it
			out.print(getUsage());
			return;
		}
		String command = st.nextToken();
		if(command.startsWith("set")) {
			// extract the param and value
			if(!st.hasMoreElements()) {
				out.print(getUsage());
				return;
			}
			// little manual parsing. Not ideal, but we do not have a lot to do either
			String param = st.nextToken();
			String value = null;
			// the user may have typed param = value (with spaces)
			int idx = param.indexOf('=');
			if(idx < 0) {
				if(st.hasMoreTokens() && "=".equals(st.nextToken()) && st.hasMoreTokens()) {
					value = st.nextToken();
				} else {
					// no value
					value = null;
				}
			} else if(idx < param.length()) {
				value = param.substring(idx+1).trim();
				param = param.substring(0, idx).trim(); 
			} else {
				// nothing after the =
				value = null;
				param = param.substring(0, idx).trim(); 
			}
			if(!"host".equals(param) && !"port".equals(param) && !"account".equals(param) && !"password".equals(param) && !"master".equals(param)) {
				err.print("Unknow parameter " + param);
				return;
			}
			// set the param / value combination in the preferences
			Preferences prefs = Preferences.userNodeForPackage(XMPPShell.class);
			prefs.put(param, value);
		} else if(command.equals("connect")) {
			try {
				fMessenger.connect();
				Preferences prefs = Preferences.userNodeForPackage(XMPPShell.class);
				String host = prefs.get("host", "jabber.org");
				String account = prefs.get("account", null);
				out.println("Connected to server " + host + " on account " + account);
			} catch (Exception e) {
				err.print(e.getMessage());
			}
		} else if(command.equals("register")) {
			try {
				fMessenger.register();
				Preferences prefs = Preferences.userNodeForPackage(XMPPShell.class);
				String host = prefs.get("host", "jabber.org");
				String account = prefs.get("account", null);
				out.println("Connected to server " + host + " on account " + account);
			} catch (Exception e) {
				err.print(e.getMessage());
			}
		} else if(command.equals("quit")) {
			try {
				fMessenger.quit();
			} catch (Exception e) {
				err.print(e.getMessage());
			}
		} else {
			out.println(getUsage());
		}
	}

	public String getName() {
		return "xmpp";
	}

	public String getShortDescription() {
		return "xmpp shell for dysoweb (and others)";
	}

	public String getUsage() {
		String usage = 
			"xmpp set param=value (param = host|port|account|password|master) | "+
			"xmpp (connect|register|quit)";
		return usage;
	}

}
