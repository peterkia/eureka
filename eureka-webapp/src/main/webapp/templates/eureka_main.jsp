<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tlds/template.tld" prefix="template" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<meta name="keywords" content="informatics, i2b2, biomedical, clinical research, research, de-identification, clinical data analysis, analytics, medical research, data analysis tool, clinical database, eureka!, eureka, scientific research, temporal patterns, bioinformatics, ontology, ontologies, ontology editor, data mining, etl, cvrg, CardioVascular Research Grid" />

<title>Eureka! Clinical Analytics</title>

<!--[if IE 7]>
<style>
.content { margin-right: -1px; }
ul.nav a { zoom: 1; }
#submit { border:none;}
</style>
<![endif]-->
<link href="css/styles.css" rel="stylesheet" type="text/css" />
<link rel="SHORTCUT ICON" href="favicon.ico">
<meta name="Description" content="A Clinical Analysis Tool for biomedical informatics and data" />
</head>

<body>

<div class="container">
<div>
    <div id="login">
	   <label>User Name
	   <input id="login_field" type="text" name="textfield" />
	   </label>
	   	   <label>Password
	   <input id="login_field" type="text" name="textfield" />
	   </label>
 	       <input id="submit" type="submit" name="Submit" value="Log In" />
 <br />
 	<span class="sub_text"><a href="forgot_password.html">Login Help</a></span>
<br />
    </div>
  <div class="header">
    <span><a href="index.html"><img src="images/tag_line.gif" alt="Data Analysis Tool" width="238" align="absmiddle" /></a></span></div>
 </div>

 <div>    
  <ul class="nav">
      <li><a href="about.html"><img src="images/about_icon.gif" alt="About" width="30" height="30" align="absmiddle" />About</a></li>
      <img src="images/reg_icon.gif" alt="Register" width="30" height="30" align="absmiddle" />
      <li><a href="register.jsp">Register</a></li>
      <img src="images/acct_icon.gif" alt="Account" width="30" height="30" align="absmiddle" />
      <li><a href="acct.html">Account</a></li>
      <img src="images/contact_icon.gif" alt="Contact" width="30" height="30" align="absmiddle" />
      <li><a href="contact.html">Contact</a></li>
      <img src="images/help_icon.gif" alt="Help" width="30" height="30" align="absmiddle" />
      <li><a href="help.html">Help</a></li>
            <img src="images/admin_icon.gif" alt="Administration" width="30" height="30" align="absmiddle" />
      <li><a href="admin.html">Administration</a></li>
      <img style="padding-left:475px" src="images/i2b2_icon.gif" alt="i2b2" width="30" height="30" align="absmiddle" />
      <li><a href="i2b2workbench.exe">i2b2</a></li>
         <img src="images/rsch_icon.gif" alt="Upload Data" width="30" height="30" align="absmiddle" />
      <li><a href="Tool.html">Upload Data</a></li>
    </ul>
  </div>

  <div class="sidebar1">

    <img src="images/analytics.jpg" />

 <br />
  <br />
  </div>
  <div class="content">
		<template:get name="content" />
  </div>
  
   <div class="sub-content">
		<template:get name="subcontent" />
  </div>
  
  <div class="footer">
    <p>Copyright 2011 . CCI . Center for Comprehensive Informatics  </p>
    <!-- end .footer --></div>
  <!-- end .container --></div>
</body>
</html>
