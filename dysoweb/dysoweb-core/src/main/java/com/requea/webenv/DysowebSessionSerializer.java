package com.requea.webenv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.felix.framework.BundleWiringImpl;

//import org.apache.felix.framework. .searchpolicy. .ModuleImpl;
//import org.apache.felix.moduleloader.IModule;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;

import com.requea.dysoweb.DysowebServlet;


public class DysowebSessionSerializer implements Externalizable {

	private static final int SERIALIZABLE = 1;

	private static final int NOTSERIALIZABLE = 2;

	private static final int NULL = 0;
	
	private transient Serializable fObject;
	
	private byte[] fData; 

	// default 
	public DysowebSessionSerializer() {
	}

	public DysowebSessionSerializer(Serializable obj) {
		fObject = obj;
	}

	public synchronized Object getObject() throws IOException, ClassNotFoundException {
		if(fData != null && fObject == null) {
			// post deserialization process
			ByteArrayInputStream bis = new ByteArrayInputStream(fData);
			// no matter what, the data will be of no use
			fData = null;
			DysowebObjectInputStream ois = new DysowebObjectInputStream(bis);
			fObject = (Serializable)ois.readObject();
		}
		return fObject;
	}
	
	public synchronized void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		
		int type = in.readInt();
		if(type == NULL) {
			fObject = null;
			fData = null;
		} else {
			// deserialize the object
			fData = (byte[]) in.readObject();
			fObject = null;
		}
	}

	public void preSerialize() throws SecurityException, IOException {
		if(fData == null) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DysowebObjectOutputStream oos = new DysowebObjectOutputStream(bos);
			synchronized(fObject) {
				oos.writeObject(fObject);
			}
			fData = bos.toByteArray();
		}
	}
	
	public synchronized void writeExternal(ObjectOutput out) throws IOException {
		if (out instanceof OutputStream && fObject != null) {
			try {
				if(fData == null) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					DysowebObjectOutputStream oos = new DysowebObjectOutputStream(bos);
					synchronized(fObject) {
						oos.writeObject(fObject);
					}
					fData = bos.toByteArray();
				}
				out.writeInt(SERIALIZABLE);
				// serialize the data
				out.writeObject(fData);
			} catch(IOException e) {
				out.writeInt(NOTSERIALIZABLE);
				throw e;
			} catch(Exception e) {
				out.writeInt(NOTSERIALIZABLE);
				throw new IOException(e.getMessage());
			}
		} else {
			// ignore
			out.writeInt(NULL);
		}
	}

	private static class DysowebObjectOutputStream extends ObjectOutputStream {

		protected DysowebObjectOutputStream() throws IOException,
				SecurityException {
			super();
		}

		protected DysowebObjectOutputStream(OutputStream os)
				throws IOException, SecurityException {
			super(os);
		}

		protected void annotateClass(Class cls) throws IOException {

			ClassLoader cl = cls.getClassLoader();
			if(cl instanceof BundleWiringImpl.BundleClassLoader) {
				// class loaded by bundle
				Framework platform = DysowebServlet.getPlatform();
				if(platform != null) {
					Bundle b = ((BundleWiringImpl.BundleClassLoader)cl).getBundle();
					// get the symbolic name
					String symName = b == null ? null : b.getSymbolicName();
					if(symName != null) {
						writeUTF("bundle");
						writeUTF(symName);
					} else {
						writeUTF("unknown");
					}
				} else {
					writeUTF("unknown");
				}
			} else {
				// system class
				writeUTF("system");
			}
			// system class
			writeUTF("system");
		}

	}

	private class DysowebObjectInputStream extends ObjectInputStream {

		protected Class resolveClass(ObjectStreamClass desc)
				throws IOException, ClassNotFoundException {

			String strClassType = readUTF();
			if("bundle".equals(strClassType)) {
				
				String symName = readUTF();
				if(symName == null || "unknown".equals(symName) || symName.length() == 0) {
					// bundle class
					try {
						return super.resolveClass(desc);
					} catch(ClassNotFoundException e) {
						throw e;
					}
				} else {
					// retrieve the bundle
					Framework platform = DysowebServlet.getPlatform();
					BundleContext context = platform.getBundleContext();
					Bundle[] bundles = context.getBundles();
					Bundle b = null;
					for(int i=0; b == null && i<bundles.length; i++) {
						Bundle bundle = bundles[i];
						if(symName.equals(bundle.getSymbolicName()) && bundle.getState() == Bundle.ACTIVE) {
							b = bundle;
						}
					}
					if(b != null) {
						// request the bundle class loader to handle the class
						try {
							return b.loadClass(desc.getName());
						} catch(ClassNotFoundException e) {
							throw e;
						}
					} else {
						// default resolve
						try {
							return super.resolveClass(desc);
						} catch(ClassNotFoundException e) {
							throw e;
						}
					}
				}
			} else {
				// default system
				return super.resolveClass(desc);
			}
		}

		public DysowebObjectInputStream(InputStream in) throws IOException {
			super(in);
		}
	}


}
