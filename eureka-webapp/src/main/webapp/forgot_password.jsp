<%--
  #%L
  Eureka WebApp
  %%
  Copyright (C) 2012 Emory University
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
       http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
  --%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/tlds/template.tld" prefix="template"%>


<template:insert template="/templates/eureka_main.jsp">

	<template:content name="sidebar">
		<img
			src="${pageContext.request.contextPath}/images/clinical_research.jpg" />
	</template:content>

	<template:content name="content">
		<h3>Forgot Password</h3>
<form id="ResetPsdForm" action="#" method="post">
        <div class="pad_btm fltlft">
			<p><br /> 
			  Having trouble logging in?
			<p>Remember that your user name is your email address you used when you registered.<br />
            If you cannot remember your password, please enter your email address.            
<p>
		<div>
 			<table class="white">
	  		  <tr>
	  		  	<td class=" white"><label id="lemail" for="email">Email Address</label></td>
	  		  	<td class="field white"><input id="email" name="email" type="text"  class="email" value="" /></td>
	  		  	<td class="status white shift_left error"></td>
	  		  </tr>
	  		  </table>
			 &nbsp;&nbsp;&nbsp;
			<button id="submit" type="submit" class="btn btn-primary submit">Reset Password</button>
			</div>
		</p>
		<div id="passwordresetComplete"></div>
          </div>
          </form>
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