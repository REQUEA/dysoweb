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


package com.requea.dysoweb.util.xml;

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
