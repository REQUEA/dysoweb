/* Track-B Phase-0 spike (throwaway): a trivial custom tag to exercise the TLD path. */
package com.requea.dysoweb.spike.tags;

import java.io.IOException;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;

public class HelloTag extends SimpleTagSupport {
    private String name;

    public void setName(String name) { this.name = name; }

    @Override
    public void doTag() throws JspException, IOException {
        getJspContext().getOut().write("TAG[name=" + name + "]");
    }
}
