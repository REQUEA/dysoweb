/* Track-B Phase-0 spike (throwaway): a tag whose TLD lives INSIDE an embedded jar. */
package com.requea.dysoweb.spike.tags;

import java.io.IOException;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;

public class EmbedTag extends SimpleTagSupport {
    private String x;

    public void setX(String x) { this.x = x; }

    @Override
    public void doTag() throws JspException, IOException {
        getJspContext().getOut().write("EMBED[x=" + x + "]");
    }
}
