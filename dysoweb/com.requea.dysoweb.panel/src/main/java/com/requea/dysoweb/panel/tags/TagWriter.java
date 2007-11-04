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

package com.requea.dysoweb.panel.tags;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

/**
 * A util class to append content to response in an efficient way.
 * The content is appended to a list, and then the list is writen directly
 * to the JspWriter. Therefore, the writing is efficient, and there is
 * no additional string manipulation.
 * @author Pierre Dubois
 */
public class TagWriter {
    
    private ArrayList fContents = new ArrayList();
    
    /**
     * Appends a string
     */
    public void append(String str) {
        fContents.add(str);
    }
    
    /**
     * Appends an integer
     * @param i
     */
    public void append(int i) {
        fContents.add(Integer.toString(i));
    }
    
    /**
     * Appends a long
     * @param l
     */
    public void append(long l) {
        fContents.add(Long.toString(l));
    }
    
    /**
     * Write the content to the servlet response. 
     */
    public void writeTo(PageContext pageContext)
    	throws JspException {
        
        JspWriter writer = pageContext.getOut();
        try {
            for(int i=0; i<fContents.size(); i++) {
                Object o = fContents.get(i);
                if(o != null) {
                    // should be a string anyway
                    writer.print(o.toString());
                }
            }
            // flush the content
            fContents.clear();
        } catch (IOException e) {
            throw new JspException("Unable to write response"+e.getMessage());
        }
    }

}
