<%
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://alloy.liferay.com/tld/aui" prefix="aui" %>

<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="com.liferay.portal.theme.ThemeDisplay" %>
<%@ page import="com.liferay.portal.kernel.util.WebKeys" %>

<portlet:defineObjects />

<%! String siteName = "Diese Seite"; %>
<%
	PortletPreferences prefs = renderRequest.getPreferences();
	String idea = (String)prefs.getValue("theIdea", " - ");
	String startDate = prefs.getValue("theStart",  "");
	int state = Integer.parseInt(prefs.getValue("theState", "-1"));
	
	ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(WebKeys.THEME_DISPLAY);
	siteName = themeDisplay.getSiteGroupName();
%>

<portlet:renderURL var="editIdeenreifungURL">
    <portlet:param name="mvcPath" value="/edit.jsp" />
</portlet:renderURL>

<portlet:actionURL name="refreshAction" var="refreshActionURL">
    	<portlet:param name="mvcPath" value="/view.jsp" />
	</portlet:actionURL>

<div class="icon-calendar" style="float:left; color:#999;">
	Start: <%= startDate %>
</div>

<p style="float:right;">
	<span style="margin-right:10px;">
	<liferay-ui:icon
		iconCssClass="icon-refresh"
		label="<%= true %>"
		message="Refresh"
		url="<%= refreshActionURL %>"
	/></span>

	<liferay-ui:icon
		iconCssClass="icon-edit"
		label="<%= true %>"
		message="ideen_new-idea"
		url="<%= editIdeenreifungURL %>"
	/>
</p>

<div class="navbar navbar-default" style="clear:both; margin-bottom:15px;">
	<div class="container-fluid">
		<p style="margin-top:14px;"><liferay-ui:message key="ideen_current-idea" />: <strong><%= idea %></strong></p>
	</div>
</div>

<ol>

	<portlet:actionURL name="blogAction" var="blogActionURL">
    	<portlet:param name="mvcPath" value="/view.jsp" />
	</portlet:actionURL>

	<li>
	<% if(state>0) { %>
	<liferay-ui:icon
			image="check"
			label="<%= false %>"
			message="done"
			url=""
		/> 
		<% } else if(state==0) { %>
		<liferay-ui:icon
			image="submit"
			label="<%= false %>"
			message=""
			url=""
		/> <% } %>
		<strong><liferay-ui:message key="ideen_1draft" />:</strong> <% if(state==0) { %><a href="<%= blogActionURL %>"><% } %><liferay-ui:message key="ideen_1draft-text" /><% if(state==0) { %></a><% } %>.
	</li>
	
	<portlet:actionURL name="messageBoardAction" var="messageBoardActionURL">
    	<portlet:param name="mvcPath" value="/view.jsp" />
	</portlet:actionURL>
	
	<li><% if(state>1) { %>
		<liferay-ui:icon
				image="check"
				label="<%= false %>"
				message="done"
				url=""
			/> 
			<% } else if(state==1) { %>
			<liferay-ui:icon
				image="submit"
				label="<%= false %>"
				message=""
				url=""
			/> <% } %> 
		<strong><liferay-ui:message key="ideen_2discussion" />:</strong>
		<ul>
			<li><liferay-ui:message key="ideen_2discussion-text1" /></li>
			<li><% if(state>0 && state<3) { %><a href="<%= messageBoardActionURL %>"><% } %><liferay-ui:message key="ideen_2discussion-text3" /><% if(state>0 && state<3) { %></a><% } %></li>
		</ul>
	</li>
	
	<portlet:actionURL name="wikiAction" var="wikiActionURL">
    	<portlet:param name="mvcPath" value="/view.jsp" />
	</portlet:actionURL>
	
	<li><% if(state>2) { %>
		<liferay-ui:icon
				image="check"
				label="<%= false %>"
				message="done"
				url=""
			/> 
			<% } else if(state==2) { %>
			<liferay-ui:icon
				image="submit"
				label="<%= false %>"
				message=""
				url=""
			/> <% } %>
		<strong><liferay-ui:message key="ideen_3community" />:</strong> <% if(state>0 && state<3) { %><a href="<%= wikiActionURL %>"><% } %><liferay-ui:message key="ideen_3community-text" /><% if(state>0 && state<3) { %></a><% } %></li>
	
	<%-- 
	<portlet:actionURL name="documentAction" var="documentActionURL">
    	<portlet:param name="mvcPath" value="/view.jsp" />
	</portlet:actionURL>
	--%>
	
	<li><% if(state>3) { %>
		<liferay-ui:icon
				image="check"
				label="<%= false %>"
				message="done"
				url=""
			/> 
			<% } else if(state==3) { %>
			<liferay-ui:icon
				image="submit"
				label="<%= false %>"
				message=""
				url=""
			/> <% } %>
		<strong><liferay-ui:message key="ideen_4submit" />:</strong> <liferay-ui:message key="ideen_4submit-text1" /><ul>
		<li><liferay-ui:message key="ideen_4submit-text2" /></li></ul>
	</li>
</ol>