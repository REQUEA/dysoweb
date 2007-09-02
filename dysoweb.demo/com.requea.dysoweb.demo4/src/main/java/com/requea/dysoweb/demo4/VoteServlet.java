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

package com.requea.dysoweb.demo4;

import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

public class VoteServlet extends HttpServlet {

	private static final String RESULTS = "com.requea.dysoweb.demo4.results";

	private static final String VOTE = "com.requea.dynsoa.demo4.vote";

	private static final long serialVersionUID = 1L;

	private static final Paint[] STANDARD_COLORS = new Paint[] {
    	new Color(0x106dbd), 
    	new Color(0xf77d10),
        new Color(0xb5c318), 
        new Color(0x8c554a), 
        new Color(0x9cdf84),
        new Color(0xc69e94), 
        new Color(0x9c65bd), 
        new Color(0x299a21), 
        new Color(0xb52810), 
    	new Color(0xb5c7f7), 
        new Color(0xffa2a5),
        new Color(0x7b707b) 
    };

	/**
     * A custom renderer that returns a different color for each item in a single series.
     */
    class CustomBarRenderer extends BarRenderer {

		private static final long serialVersionUID = -1L;
        private Paint[] colors;

        public CustomBarRenderer() {
            this.colors = STANDARD_COLORS;
        }

        public Paint getSeriesPaint(int idx) {
        	return this.colors[idx % this.colors.length];
        }
        
        public Paint getItemPaint(final int row, final int column) {
            return this.colors[column % this.colors.length];
        }
    }


	public void init(ServletConfig cfg) throws ServletException {
		if(cfg.getServletContext().getAttribute(RESULTS) == null) {
			Map results = new HashMap();
			results.put("modular", new Integer(0));
			results.put("dynamic", new Integer(0));
			results.put("standard", new Integer(0));
			cfg.getServletContext().setAttribute(RESULTS, results);
		}
		super.init(cfg);
	}

	
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String op = request.getParameter("op");
		if(op == null) {
			// check if the user has already voted
			Object obj = request.getSession().getAttribute(VOTE);
			if(Boolean.TRUE.equals(obj)) {
				// renders the result page
	            RequestDispatcher rd = request.getRequestDispatcher("/demo4/result.jsp");
	            rd.include(request, response);
			} else {
				// renders the vote page
	            RequestDispatcher rd = request.getRequestDispatcher("/demo4/vote.jsp");
	            rd.include(request, response);
			}
		} else if("vote".equals(op)) {
			// check if the user has already voted
			Object obj = request.getSession().getAttribute(VOTE);
			
			// record the vote and redirect to the referer
			if(obj == null) {
				// 
				Map results = (Map)request.getSession().getServletContext().getAttribute(RESULTS);
				synchronized (results) {
					String resp = request.getParameter("response");
					Integer val = (Integer)results.get(resp);
					if(val != null) {
						// increment the value
						val = new Integer(val.intValue()+1);
						results.put(resp, val);
					}
				}
			}

			// indicates that the user has voted
			request.getSession().setAttribute(VOTE, Boolean.TRUE);
			
			// redirect to the refererer (if any)
			String ru = request.getParameter("ru");
			if(ru == null)
				ru = request.getHeader("Referer");
			if(ru == null)
				ru = request.getContextPath() + "/index.jsp";
			// redirect to the ru
	    	response.sendRedirect(ru);
	    	return;
		} else if("img".equals(op)) {
			// renders the chart
			// creates a jfreechart
			OutputStream out = response.getOutputStream();
			try {
				DefaultCategoryDataset dataset = new DefaultCategoryDataset();
				Map results = (Map)request.getSession().getServletContext().getAttribute(RESULTS);
				synchronized (results) {
					int modular = ((Integer)results.get("modular")).intValue();
					int dynamic = ((Integer)results.get("dynamic")).intValue();
					int standard = ((Integer)results.get("standard")).intValue();
					int total = modular+dynamic+standard;
					if(total > 0) {
						dataset.addValue((float)modular/(float)total, "S1", "Modular");
						dataset.addValue((float)dynamic/(float)total, "S1", "Dynamic");
						dataset.addValue((float)standard/(float)total, "S1", "Standard");
					}
				}

				JFreeChart chart = ChartFactory.createBarChart("Most important functionality",
						null, null, dataset, PlotOrientation.HORIZONTAL,
						false, false, false);
				chart.setBackgroundPaint(new Color(0xffffff));
				response.setContentType("image/png");
				CategoryPlot plot = (CategoryPlot)chart.getPlot();
				CustomBarRenderer renderer = new CustomBarRenderer();
				plot.setRenderer(renderer);
				// percentage
				NumberAxis va = (NumberAxis)plot.getRangeAxis();
				va.setNumberFormatOverride(NumberFormat.getPercentInstance());
				// set the maximum width to 20%
				renderer.setMaximumBarWidth(0.20);

				// then renders the chart
				ChartUtilities.writeChartAsPNG(out, chart, 300, 180);
			} catch (Exception e) {
				System.err.println(e.toString());
			} finally {
				out.close();
			}
		}

	}
}
