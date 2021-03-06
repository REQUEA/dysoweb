/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.requea.dysoweb.bundlerepository;

import java.util.Enumeration;
import java.util.Iterator;

public class IteratorToEnumeration implements Enumeration
{
    private Iterator m_iter = null;

    public IteratorToEnumeration(Iterator iter)
    {
        m_iter = iter;
    }

    public boolean hasMoreElements()
    {
        if (m_iter == null)
            return false;
        return m_iter.hasNext();
    }

    public Object nextElement()
    {
        if (m_iter == null)
            return null;
        return m_iter.next();
    }
}
