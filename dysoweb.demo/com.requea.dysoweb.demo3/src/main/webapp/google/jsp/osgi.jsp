<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<div class="plugin">
<script src="http://www.google.com/uds/api?file=uds.js&amp;v=1.0&amp;key=ABQIAAAAHAMrWhP7Xp1ZEfesvfekvxRehcWnftuajKO0jvi9RQuTk1wvcxTQa8GurY27AXJlwO0qgjdctIbSdg" type="text/javascript"></script>
<script language="Javascript" type="text/javascript">
//<![CDATA[

function OnLoad() {
  // Create a search control
  var searchControl = new GSearchControl();

  // Add in a full set of searchers
  searchControl.addSearcher(new GwebSearch());
  searchControl.addSearcher(new GblogSearch());
  searchControl.addSearcher(new GnewsSearch());
  // Tell the searcher to draw itself and tell it where to attach
  searchControl.draw(document.getElementById("searchcontrol"));
  // Execute an inital search
  searchControl.execute("OSGI");
}
GSearch.setOnLoadCallback(OnLoad);

//]]>
</script>
<div id="searchcontrol"/>
</div>
