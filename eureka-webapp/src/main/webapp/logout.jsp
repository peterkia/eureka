<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/tlds/template.tld" prefix="template"%>

<template:insert template="/templates/eureka_main.jsp">

	<template:content name="sidebar">
		<img src="${pageContext.request.contextPath}/images/scientific_research.jpg" />
	</template:content>

	<template:content name="content">
		<h3>Logout</h3>
		<div id="msg" class="success">
			<p>You have successfully logged out of Eureka! Clinical Analytics.</p>
			<p>For security reasons, exit your web browser.</p>
		</div>
	</template:content>

	<template:content name="subcontent">
		<div id="release_notes">
			<h3>
				<img src="${pageContext.request.contextPath}/images/rss.png"
					 border="0" /> Related News <a href="xml/rss_news.xml" class="rss"></a>
			</h3>
			<script language="JavaScript"
					src="http://feed2js.org//feed2js.php?src=http%3A%2F%2Fwhsc.emory.edu%2Fhome%2Fnews%2Freleases%2Fresearch.rss&chan=y&num=4&desc=1&utf=y"
			charset="UTF-8" type="text/javascript"></script>

			<noscript>
			<a style="padding-left: 35px"
			   href="http://feed2js.org//feed2js.php?src=http%3A%2F%2Fwhsc.emory.edu%2Fhome%2Fnews%2Freleases%2Fresearch.rss&chan=y&num=4&desc=200>1&utf=y&html=y">View
				RSS feed</a>
			</noscript>
		</div>
	</template:content>
</template:insert>