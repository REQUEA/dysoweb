// ========================================================================
// Copyright 2007 Requea.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package com.requea.dysoweb.defaultservlet;


import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Date;
import java.util.Hashtable;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.naming.resources.BaseDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;

import com.requea.dysoweb.processor.RequestProcessor;
import com.requea.dysoweb.processor.RequestProcessor.EntryInfo;

/**
 * BundleDirContext Directory Context implementation helper class.
 * 
 * @author Pierre Dubois
 */

public class BundleDirContext extends BaseDirContext {

	// -------------------------------------------------------------- Constants

	/**
	 * The descriptive information string for this implementation.
	 */
	protected static final int BUFFER_SIZE = 2048;

	private RequestProcessor fRequestProcessor;

	// ----------------------------------------------------------- Constructors

	/**
	 * Builds a file directory context using the given environment.
	 */
	public BundleDirContext(RequestProcessor w) {
		super();
		fRequestProcessor = w;
	}

	/**
	 * Builds a file directory context using the given environment.
	 */
	public BundleDirContext(RequestProcessor w, Hashtable env) {
		super(env);
		fRequestProcessor = w;
	}

	// ----------------------------------------------------- Instance Variables

	/**
	 * Absolute normalized filename of the base.
	 */
	protected String absoluteBase = null;

	/**
	 * Case sensitivity.
	 */
	protected boolean caseSensitive = true;

	/**
	 * Allow linking.
	 */
	protected boolean allowLinking = false;

	// ------------------------------------------------------------- Properties

	/**
	 * Set case sensitivity.
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Is case sensitive ?
	 */
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	/**
	 * Set allow linking.
	 */
	public void setAllowLinking(boolean allowLinking) {
		this.allowLinking = allowLinking;
	}

	/**
	 * Is linking allowed.
	 */
	public boolean getAllowLinking() {
		return allowLinking;
	}

	// --------------------------------------------------------- Public Methods

	/**
	 * Release any resources allocated for this directory context.
	 */
	public void release() {

		caseSensitive = true;
		allowLinking = false;
		absoluteBase = null;
		super.release();

	}

	// -------------------------------------------------------- Context Methods

	/**
	 * Retrieves the named object.
	 * 
	 * @param name
	 *            the name of the object to look up
	 * @return the object bound to name
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public Object lookup(String name) throws NamingException {
		Object result = null;

		try {
			EntryInfo entry = fRequestProcessor.getEntryInfo(name);
			if (entry == null)
				throw new NamingException(sm.getString("resources.notFound", name));
	
			if (name.endsWith("/")) {
				BundleDirContext tempContext = new BundleDirContext(
						fRequestProcessor, env);
				tempContext.setAllowLinking(getAllowLinking());
				tempContext.setCaseSensitive(isCaseSensitive());
				result = tempContext;
			} else {
				result = new BundleEntryResource(entry);
			}
		} catch(IOException e) {
			throw new NamingException(e.getMessage());
		}

		return result;

	}

	/**
	 * Unbinds the named object. Removes the terminal atomic name in name from
	 * the target context--that named by all but the terminal atomic part of
	 * name.
	 * <p>
	 * This method is idempotent. It succeeds even if the terminal atomic name
	 * is not bound in the target context, but throws NameNotFoundException if
	 * any of the intermediate contexts do not exist.
	 * 
	 * @param name
	 *            the name to bind; may not be empty
	 * @exception NameNotFoundException
	 *                if an intermediate context does not exist
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public void unbind(String name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	/**
	 * Binds a new name to the object bound to an old name, and unbinds the old
	 * name. Both names are relative to this context. Any attributes associated
	 * with the old name become associated with the new name. Intermediate
	 * contexts of the old name are not changed.
	 * 
	 * @param oldName
	 *            the name of the existing binding; may not be empty
	 * @param newName
	 *            the name of the new binding; may not be empty
	 * @exception NameAlreadyBoundException
	 *                if newName is already bound
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public void rename(String oldName, String newName) throws NamingException {
		throw new OperationNotSupportedException();
	}

	/**
	 * Enumerates the names bound in the named context, along with the class
	 * names of objects bound to them. The contents of any subcontexts are not
	 * included.
	 * <p>
	 * If a binding is added to or removed from this context, its effect on an
	 * enumeration previously returned is undefined.
	 * 
	 * @param name
	 *            the name of the context to list
	 * @return an enumeration of the names and class names of the bindings in
	 *         this context. Each element of the enumeration is of type
	 *         NameClassPair.
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public NamingEnumeration list(String name) throws NamingException {

		throw new OperationNotSupportedException();
	}

	/**
	 * Enumerates the names bound in the named context, along with the objects
	 * bound to them. The contents of any subcontexts are not included.
	 * <p>
	 * If a binding is added to or removed from this context, its effect on an
	 * enumeration previously returned is undefined.
	 * 
	 * @param name
	 *            the name of the context to list
	 * @return an enumeration of the bindings in this context. Each element of
	 *         the enumeration is of type Binding.
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public NamingEnumeration listBindings(String name) throws NamingException {

		throw new OperationNotSupportedException();
	}

	/**
	 * Destroys the named context and removes it from the namespace. Any
	 * attributes associated with the name are also removed. Intermediate
	 * contexts are not destroyed.
	 * <p>
	 * This method is idempotent. It succeeds even if the terminal atomic name
	 * is not bound in the target context, but throws NameNotFoundException if
	 * any of the intermediate contexts do not exist.
	 * 
	 * In a federated naming system, a context from one naming system may be
	 * bound to a name in another. One can subsequently look up and perform
	 * operations on the foreign context using a composite name. However, an
	 * attempt destroy the context using this composite name will fail with
	 * NotContextException, because the foreign context is not a "subcontext" of
	 * the context in which it is bound. Instead, use unbind() to remove the
	 * binding of the foreign context. Destroying the foreign context requires
	 * that the destroySubcontext() be performed on a context from the foreign
	 * context's "native" naming system.
	 * 
	 * @param name
	 *            the name of the context to be destroyed; may not be empty
	 * @exception NameNotFoundException
	 *                if an intermediate context does not exist
	 * @exception NotContextException
	 *                if the name is bound but does not name a context, or does
	 *                not name a context of the appropriate type
	 */
	public void destroySubcontext(String name) throws NamingException {
		unbind(name);
	}

	/**
	 * Retrieves the named object, following links except for the terminal
	 * atomic component of the name. If the object bound to name is not a link,
	 * returns the object itself.
	 * 
	 * @param name
	 *            the name of the object to look up
	 * @return the object bound to name, not following the terminal link (if
	 *         any).
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public Object lookupLink(String name) throws NamingException {
		// Note : Links are not supported
		return lookup(name);
	}

	/**
	 * Retrieves the full name of this context within its own namespace.
	 * <p>
	 * Many naming services have a notion of a "full name" for objects in their
	 * respective namespaces. For example, an LDAP entry has a distinguished
	 * name, and a DNS record has a fully qualified name. This method allows the
	 * client application to retrieve this name. The string returned by this
	 * method is not a JNDI composite name and should not be passed directly to
	 * context methods. In naming systems for which the notion of full name does
	 * not make sense, OperationNotSupportedException is thrown.
	 * 
	 * @return this context's name in its own namespace; never null
	 * @exception OperationNotSupportedException
	 *                if the naming system does not have the notion of a full
	 *                name
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public String getNameInNamespace() throws NamingException {
		return docBase;
	}

	// ----------------------------------------------------- DirContext Methods

	/**
	 * Retrieves selected attributes associated with a named object. See the
	 * class description regarding attribute models, attribute type names, and
	 * operational attributes.
	 * 
	 * @return the requested attributes; never null
	 * @param name
	 *            the name of the object from which to retrieve attributes
	 * @param attrIds
	 *            the identifiers of the attributes to retrieve. null indicates
	 *            that all attributes should be retrieved; an empty array
	 *            indicates that none should be retrieved
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public Attributes getAttributes(String name, String[] attrIds)
			throws NamingException {

		// Building attribute list
		try {
			EntryInfo entry = fRequestProcessor.getEntryInfo(name);
			if (entry == null)
				throw new NamingException(sm.getString("resources.notFound", name));
	
			return new BundleEntryResourceAttributes(entry);
		} catch(IOException e) {
			throw new NamingException(e.getMessage());
		}
	}

	/**
	 * Modifies the attributes associated with a named object. The order of the
	 * modifications is not specified. Where possible, the modifications are
	 * performed atomically.
	 * 
	 * @param name
	 *            the name of the object whose attributes will be updated
	 * @param mod_op
	 *            the modification operation, one of: ADD_ATTRIBUTE,
	 *            REPLACE_ATTRIBUTE, REMOVE_ATTRIBUTE
	 * @param attrs
	 *            the attributes to be used for the modification; may not be
	 *            null
	 * @exception AttributeModificationException
	 *                if the modification cannot be completed successfully
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public void modifyAttributes(String name, int mod_op, Attributes attrs)
			throws NamingException {

	}

	/**
	 * Modifies the attributes associated with a named object using an an
	 * ordered list of modifications. The modifications are performed in the
	 * order specified. Each modification specifies a modification operation
	 * code and an attribute on which to operate. Where possible, the
	 * modifications are performed atomically.
	 * 
	 * @param name
	 *            the name of the object whose attributes will be updated
	 * @param mods
	 *            an ordered sequence of modifications to be performed; may not
	 *            be null
	 * @exception AttributeModificationException
	 *                if the modification cannot be completed successfully
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public void modifyAttributes(String name, ModificationItem[] mods)
			throws NamingException {

	}

	/**
	 * Binds a name to an object, along with associated attributes. If attrs is
	 * null, the resulting binding will have the attributes associated with obj
	 * if obj is a DirContext, and no attributes otherwise. If attrs is
	 * non-null, the resulting binding will have attrs as its attributes; any
	 * attributes associated with obj are ignored.
	 * 
	 * @param name
	 *            the name to bind; may not be empty
	 * @param obj
	 *            the object to bind; possibly null
	 * @param attrs
	 *            the attributes to associate with the binding
	 * @exception NameAlreadyBoundException
	 *                if name is already bound
	 * @exception InvalidAttributesException
	 *                if some "mandatory" attributes of the binding are not
	 *                supplied
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public void bind(String name, Object obj, Attributes attrs)
			throws NamingException {

		throw new OperationNotSupportedException();
	}

	/**
	 * Binds a name to an object, along with associated attributes, overwriting
	 * any existing binding. If attrs is null and obj is a DirContext, the
	 * attributes from obj are used. If attrs is null and obj is not a
	 * DirContext, any existing attributes associated with the object already
	 * bound in the directory remain unchanged. If attrs is non-null, any
	 * existing attributes associated with the object already bound in the
	 * directory are removed and attrs is associated with the named object. If
	 * obj is a DirContext and attrs is non-null, the attributes of obj are
	 * ignored.
	 * 
	 * @param name
	 *            the name to bind; may not be empty
	 * @param obj
	 *            the object to bind; possibly null
	 * @param attrs
	 *            the attributes to associate with the binding
	 * @exception InvalidAttributesException
	 *                if some "mandatory" attributes of the binding are not
	 *                supplied
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public void rebind(String name, Object obj, Attributes attrs)
			throws NamingException {
		throw new OperationNotSupportedException();
	}

	/**
	 * Creates and binds a new context, along with associated attributes. This
	 * method creates a new subcontext with the given name, binds it in the
	 * target context (that named by all but terminal atomic component of the
	 * name), and associates the supplied attributes with the newly created
	 * object. All intermediate and target contexts must already exist. If attrs
	 * is null, this method is equivalent to Context.createSubcontext().
	 * 
	 * @param name
	 *            the name of the context to create; may not be empty
	 * @param attrs
	 *            the attributes to associate with the newly created context
	 * @return the newly created context
	 * @exception NameAlreadyBoundException
	 *                if the name is already bound
	 * @exception InvalidAttributesException
	 *                if attrs does not contain all the mandatory attributes
	 *                required for creation
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public DirContext createSubcontext(String name, Attributes attrs)
			throws NamingException {

		throw new OperationNotSupportedException();
	}

	/**
	 * Retrieves the schema associated with the named object. The schema
	 * describes rules regarding the structure of the namespace and the
	 * attributes stored within it. The schema specifies what types of objects
	 * can be added to the directory and where they can be added; what mandatory
	 * and optional attributes an object can have. The range of support for
	 * schemas is directory-specific.
	 * 
	 * @param name
	 *            the name of the object whose schema is to be retrieved
	 * @return the schema associated with the context; never null
	 * @exception OperationNotSupportedException
	 *                if schema not supported
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public DirContext getSchema(String name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	/**
	 * Retrieves a context containing the schema objects of the named object's
	 * class definitions.
	 * 
	 * @param name
	 *            the name of the object whose object class definition is to be
	 *            retrieved
	 * @return the DirContext containing the named object's class definitions;
	 *         never null
	 * @exception OperationNotSupportedException
	 *                if schema not supported
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public DirContext getSchemaClassDefinition(String name)
			throws NamingException {
		throw new OperationNotSupportedException();
	}

	/**
	 * Searches in a single context for objects that contain a specified set of
	 * attributes, and retrieves selected attributes. The search is performed
	 * using the default SearchControls settings.
	 * 
	 * @param name
	 *            the name of the context to search
	 * @param matchingAttributes
	 *            the attributes to search for. If empty or null, all objects in
	 *            the target context are returned.
	 * @param attributesToReturn
	 *            the attributes to return. null indicates that all attributes
	 *            are to be returned; an empty array indicates that none are to
	 *            be returned.
	 * @return a non-null enumeration of SearchResult objects. Each SearchResult
	 *         contains the attributes identified by attributesToReturn and the
	 *         name of the corresponding object, named relative to the context
	 *         named by name.
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public NamingEnumeration search(String name, Attributes matchingAttributes,
			String[] attributesToReturn) throws NamingException {
		return null;
	}

	/**
	 * Searches in a single context for objects that contain a specified set of
	 * attributes. This method returns all the attributes of such objects. It is
	 * equivalent to supplying null as the atributesToReturn parameter to the
	 * method search(Name, Attributes, String[]).
	 * 
	 * @param name
	 *            the name of the context to search
	 * @param matchingAttributes
	 *            the attributes to search for. If empty or null, all objects in
	 *            the target context are returned.
	 * @return a non-null enumeration of SearchResult objects. Each SearchResult
	 *         contains the attributes identified by attributesToReturn and the
	 *         name of the corresponding object, named relative to the context
	 *         named by name.
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public NamingEnumeration search(String name, Attributes matchingAttributes)
			throws NamingException {
		return null;
	}

	/**
	 * Searches in the named context or object for entries that satisfy the
	 * given search filter. Performs the search as specified by the search
	 * controls.
	 * 
	 * @param name
	 *            the name of the context or object to search
	 * @param filter
	 *            the filter expression to use for the search; may not be null
	 * @param cons
	 *            the search controls that control the search. If null, the
	 *            default search controls are used (equivalent to (new
	 *            SearchControls())).
	 * @return an enumeration of SearchResults of the objects that satisfy the
	 *         filter; never null
	 * @exception InvalidSearchFilterException
	 *                if the search filter specified is not supported or
	 *                understood by the underlying directory
	 * @exception InvalidSearchControlsException
	 *                if the search controls contain invalid settings
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public NamingEnumeration search(String name, String filter,
			SearchControls cons) throws NamingException {
		return null;
	}

	/**
	 * Searches in the named context or object for entries that satisfy the
	 * given search filter. Performs the search as specified by the search
	 * controls.
	 * 
	 * @param name
	 *            the name of the context or object to search
	 * @param filterExpr
	 *            the filter expression to use for the search. The expression
	 *            may contain variables of the form "{i}" where i is a
	 *            nonnegative integer. May not be null.
	 * @param filterArgs
	 *            the array of arguments to substitute for the variables in
	 *            filterExpr. The value of filterArgs[i] will replace each
	 *            occurrence of "{i}". If null, equivalent to an empty array.
	 * @param cons
	 *            the search controls that control the search. If null, the
	 *            default search controls are used (equivalent to (new
	 *            SearchControls())).
	 * @return an enumeration of SearchResults of the objects that satisy the
	 *         filter; never null
	 * @exception ArrayIndexOutOfBoundsException
	 *                if filterExpr contains {i} expressions where i is outside
	 *                the bounds of the array filterArgs
	 * @exception InvalidSearchControlsException
	 *                if cons contains invalid settings
	 * @exception InvalidSearchFilterException
	 *                if filterExpr with filterArgs represents an invalid search
	 *                filter
	 * @exception NamingException
	 *                if a naming exception is encountered
	 */
	public NamingEnumeration search(String name, String filterExpr,
			Object[] filterArgs, SearchControls cons) throws NamingException {
		return null;
	}


	// ----------------------------------------------- FileResource Inner Class

	/**
	 * This specialized resource implementation avoids opening the InputStream
	 * to the file right away (which would put a lock on the file).
	 */
	protected class BundleEntryResource extends Resource {

		// -------------------------------------------------------- Constructor

		public BundleEntryResource(EntryInfo entry) {
			this.entry = entry;
		}

		// --------------------------------------------------- Member Variables

		/**
		 * Associated entry object.
		 */
		protected EntryInfo entry;

		/**
		 * File length.
		 */
		protected long length = -1L;

		// --------------------------------------------------- Resource Methods

		/**
		 * Content accessor.
		 * 
		 * @return InputStream
		 */
		public InputStream streamContent() throws IOException {
			if (binaryContent == null) {
				URLConnection cnx = entry.getURL().openConnection();
				inputStream = cnx.getInputStream();
			}
			return super.streamContent();
		}

	}

	// ------------------------------------- FileResourceAttributes Inner Class

	/**
	 * This specialized resource attribute implementation does some lazy reading
	 * (to speed up simple checks, like checking the last modified date).
	 */
	protected class BundleEntryResourceAttributes extends ResourceAttributes {

		// -------------------------------------------------------- Constructor

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private EntryInfo entry;

		public BundleEntryResourceAttributes(EntryInfo entry) {
			this.entry = entry;
		}

		// --------------------------------------------------- Member Variables

		protected boolean accessed = false;

		protected String canonicalPath = null;

		// ----------------------------------------- ResourceAttributes Methods

		/**
		 * Is collection.
		 */
		public boolean isCollection() {
			if (!accessed) {
				collection = entry.getURL().getPath().endsWith("/");
				accessed = true;
			}
			return super.isCollection();
		}

		/**
		 * Get content length.
		 * 
		 * @return content length value
		 * @throws IOException
		 */
		public long getContentLength() {
			if (contentLength != -1L)
				return contentLength;

			try {
				URLConnection cnx = entry.getURL().openConnection();
				contentLength = cnx.getContentLength();
				return contentLength;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * Get creation time.
		 * 
		 * @return creation time value
		 */
		public long getCreation() {
			if (creation != -1L)
				return creation;
			creation = entry.getLastModified();
			return creation;
		}

		/**
		 * Get creation date.
		 * 
		 * @return Creation date value
		 */
		public Date getCreationDate() {
			if (creation == -1L) {
				creation = entry.getLastModified();
			}
			return super.getCreationDate();
		}

		/**
		 * Get last modified time.
		 * 
		 * @return lastModified time value
		 */
		public long getLastModified() {
			if (lastModified != -1L)
				return lastModified;
			lastModified = entry.getLastModified();
			return lastModified;
		}

		/**
		 * Get lastModified date.
		 * 
		 * @return LastModified date value
		 */
		public Date getLastModifiedDate() {
			if (lastModified == -1L) {
				lastModified = entry.getLastModified();
			}
			return super.getLastModifiedDate();
		}

		/**
		 * Get name.
		 * 
		 * @return Name value
		 */
		public String getName() {
			if (name == null)
				name = entry.getURL().getPath();
			return name;
		}

		/**
		 * Get resource type.
		 * 
		 * @return String resource type
		 */
		public String getResourceType() {
			if (!accessed) {
				collection = entry.getURL().getPath().endsWith("/");
				accessed = true;
			}
			return super.getResourceType();
		}

		/**
		 * Get canonical path.
		 * 
		 * @return String the file's canonical path
		 */
		public String getCanonicalPath() {
			if (canonicalPath == null) {
				canonicalPath = entry.getURL().getPath();
			}
			return canonicalPath;
		}

	}

}
