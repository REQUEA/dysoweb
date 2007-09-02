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

package com.requea.dysoweb.servlet.jasper;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Path;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.CompileException;
import org.codehaus.janino.Compiler;
import org.codehaus.janino.DebuggingInformation;
import org.codehaus.janino.FilterWarningHandler;
import org.codehaus.janino.Location;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.WarningHandler;
import org.codehaus.janino.util.ResourceFinderClassLoader;
import org.codehaus.janino.util.resource.PathResourceFinder;
import org.codehaus.janino.util.resource.ResourceFinder;

public class DysowebCompilerAdapter extends DefaultCompilerAdapter {

	public boolean execute() throws BuildException {
		
        // Convert source files into source file names.
        File[] sourceFiles = this.compileList;
		
        String javaEncoding = this.encoding;
        
        // Determine the class path.
        File[] classPath = pathToFiles(this.compileClasspath, new File[] { new File(".") });

        ResourceFinder classPathResourceFinder = new PathResourceFinder(classPath);
        
        // this is where Janino is useful: include classes found in the classloader into the compilation
        // context
        ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
        if(parentLoader == null) {
        	parentLoader = this.getClass().getClassLoader();
        }
        ClassLoader cl = new ResourceFinderClassLoader(classPathResourceFinder, parentLoader);

        boolean verbose = true;

        try {
	        
	        Compiler compiler = new Compiler (
	        		ResourceFinder.EMPTY_RESOURCE_FINDER,
	        		new ClassLoaderIClassLoader(cl),
	        		Compiler.FIND_NEXT_TO_SOURCE_FILE,
	        		Compiler.CREATE_NEXT_TO_SOURCE_FILE,
	        		javaEncoding,                // optionalCharacterEncoding
	                verbose,                     // verbose
	                DebuggingInformation.ALL,    // debuggingInformation
	                new FilterWarningHandler(                 // optionalWarningHandler
	                    null,
	                    new SimpleWarningHandler() // <= Anonymous class here is complicated because the enclosing instance is not fully initialized yet 
	                )
	        );
	        compiler.compile(sourceFiles);
        } catch (Scanner.ScanException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (Parser.ParseException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (CompileException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
	}
	
    public static class SimpleWarningHandler implements WarningHandler {
        public void handleWarning(String handle, String message, Location optionalLocation) {
            StringBuffer sb = new StringBuffer();
            if (optionalLocation != null) sb.append(optionalLocation).append(": ");
            sb.append("Warning ").append(handle).append(": ").append(message);
            System.err.println(sb.toString());
        }
    }
	
    /**
     * Convert a {@link org.apache.tools.ant.types.Path} into an array of
     * {@link File}.
     * @param path
     * @return The converted path, or <code>null</code> if <code>path</code> is <code>null</code>
     */
    private static File[] pathToFiles(Path path) {
        if (path == null) return null;

        String[] fileNames = path.list();
        File[] files = new File[fileNames.length];
        for (int i = 0; i < fileNames.length; ++i) files[i] = new File(fileNames[i]);
        return files;
    }

    /**
     * Convert a {@link org.apache.tools.ant.types.Path} into an array of
     * {@link File}.
     * @param path
     * @param defaultValue
     * @return The converted path, or, if <code>path</code> is <code>null</code>, the <code>defaultValue</code>
     */
    private static File[] pathToFiles(Path path, File[] defaultValue) {
        if (path == null) return defaultValue;
        return pathToFiles(path);
    }

}
