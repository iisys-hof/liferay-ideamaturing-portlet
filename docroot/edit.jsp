<%
/**
 * Copyright (c) 2014 Institute for Information Systems
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
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme"%>

<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="com.liferay.util.portlet.PortletProps" %>

<%@ page import="java.util.Map,java.util.HashMap,org.apache.shindig.common.crypto.BasicBlobCrypter,java.io.File" %>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%
	PortletPreferences prefs = renderRequest.getPreferences();
	String idea = (String)prefs.getValue("theIdea", "Keine Idee ausgewählt");
	
	// Shindig: 
	String userScreenName = user.getScreenName();
	String userFullName = user.getFullName();
	
	//see AbstractSecurityToken for keys
	Map<String, String> token = new HashMap<String, String>();
	//application
	token.put("i", "Ideenreifung-portlet");
	//viewer
	token.put("v", userScreenName);
	
//	String shindigToken = "default:" + new BasicBlobCrypter(new File(PortletProps.get("token_secret"))).wrap(token);
	String shindigToken = "";
	
%>

<div class="portlet-msg-success" style="display:none;" id="<portlet:namespace/>successMsg"></div>

<div class="navbar navbar-default" style="clear:both; margin-bottom:15px;">
	<div class="container-fluid">
		<p style="margin-top:14px;"><liferay-ui:message key="ideen_current-idea" />: <strong><%= idea %></strong></p>
	</div>
</div>

<portlet:actionURL var="actionURL">
    <portlet:param name="mvcPath" value="/view.jsp" />
</portlet:actionURL>

<portlet:renderURL var="viewIdeenreifungURL">
    <portlet:param name="mvcPath" value="/view.jsp" />
</portlet:renderURL>

<aui:form action="<%= actionURL %>" method="post">
        <aui:input id="idea-text" label="ideen_new-idea" name="newIdea" type="text" value="" />
        
        <aui:button-row>
        	<aui:button type="submit" onClick='<%= renderResponse.getNamespace() + "createShindigActivity();"%>' />
        	<aui:button type="cancel" onClick="<%= viewIdeenreifungURL %>" />
        </aui:button-row>
</aui:form>


<aui:script>

	var <portlet:namespace/>LIFERAY_URL = '<%= PortletProps.get("liferay_url") %>';
	var <portlet:namespace/>SHINDIG_URL = '<%= PortletProps.get("shindig_url") %>';
	var <portlet:namespace/>ACTIVITY_FRAG = "/social/rest/activitystreams/";
	var <portlet:namespace/>USER_ID = '<%= userScreenName %>';
	var <portlet:namespace/>USER_NAME = '<%= userFullName %>';
	var <portlet:namespace/>SHINDIG_TOKEN = '<%= shindigToken %>';


	function <portlet:namespace/>sendAsyncRequest(method, url, callback, payload, callbackValue) {			
		AUI().use('aui-io-request', function(A)
		{
//			<portlet:namespace/>animationOnOff(true, A);
			
			if(payload && payload!=="") {
			  A.io.request(url, {
				  dataType: 'json',
				  method : method,
				  headers: {
					  'Content-Type': 'application/json; charset=utf-8'
				  },
				  data : JSON.stringify(payload),
				  on: {
					success: function() {
//						<portlet:namespace/>animationOnOff(false, A);
						if(callbackValue)
							callback(this.get('responseData'),callbackValue);
						else
					  		callback(this.get('responseData'));
					},
					failure: function() {
						<portlet:namespace/>showError(this.get('responseData'));
//						<portlet:namespace/>animationOnOff(false, A);
					}
				  }
			  });
			} else {
				A.io.request(url, {
				  dataType: 'json',
				  method : method,
				  on: {
					success: function() {
//						<portlet:namespace/>animationOnOff(false, A);
						if(callbackValue)
							callback(this.get('responseData'),callbackValue);
						else
					  		callback(this.get('responseData'));
					},
					failure: function() {
						<portlet:namespace/>showError(this.get('responseData'));
//						<portlet:namespace/>animationOnOff(false, A);
					}
				  }
				});
			}
		});
	}


	function <portlet:namespace/>createShindigActivity() {
		var theIdea = document.getElementById('<portlet:namespace/>idea-text').value;
	  
		var url = <portlet:namespace/>SHINDIG_URL + <portlet:namespace/>ACTIVITY_FRAG
			+ <portlet:namespace/>USER_ID + '/@self';
	  
		if(<portlet:namespace/>SHINDIG_TOKEN != null) 
			url += '?st=' + <portlet:namespace/>SHINDIG_TOKEN;
	  
		var activityJson = <portlet:namespace/>createIdeaActivityJSON(theIdea);
	
	  //send request
	  <portlet:namespace/>sendAsyncRequest('POST', url, <portlet:namespace/>nopSuccess, activityJson);
	}
	
	function <portlet:namespace/>nopSuccess(data) {
	    console.log(JSON.stringify(data));
	}
	
	function <portlet:namespace/>createIdeaActivityJSON(ideaTitle) {
		var ideaMaturingUrl = <portlet:namespace/>LIFERAY_URL + '/web/guest/ideenreifung';
		
		var json = {
				"actor" : {
					"id" : <portlet:namespace/>USER_ID,
					"displayName" : <portlet:namespace/>USER_NAME,
					"objectType" : "person"
				},
				"generator" : {
					"id" : "Ideenreifung-portlet",
					"displayName" : "Liferay "+'<liferay-ui:message key="ideen_idea-maturing" />',
					"objectType" : "application"
				},
				"object" : {
//					"id" : ideaId,
//					"displayName" : '<liferay-ui:message key="ideen_shindig-activity-title" />',
					"displayName" : ideaTitle,
					"objectType" : "idea",
					"url" : ideaMaturingUrl
				},
				"verb" : "create",
				"title" : '<liferay-ui:message key="ideen_idea" />: '+ideaTitle
				};
		
		return json;
	}
	
	function <portlet:namespace/>showError(data) {
		console.log("Request-Error!");
		var successElement = document.getElementById('<portlet:namespace/>'+'successMsg');
		successElement.className = 'portlet-msg-error';
		successElement.innerHTML = 'Request-Error';
		if(data && data.type)
			successElement.innerHTML += ' ('+data.type+')';
		if(data && data.message)
			successElement.innerHTML += ': '+data.message;
		
		successElement.style.display = "block";
	}
	
</aui:script>