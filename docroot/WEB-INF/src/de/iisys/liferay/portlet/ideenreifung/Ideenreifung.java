package de.iisys.liferay.portlet.ideenreifung;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.portal.kernel.bean.BeanParamUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayInputStream;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.DocumentConversionUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.LayoutTypePortletFactoryUtil;
import com.liferay.portlet.blogs.model.BlogsEntry;
import com.liferay.portlet.blogs.service.BlogsEntryLocalServiceUtil;
import com.liferay.portlet.blogs.service.BlogsEntryServiceUtil;
import com.liferay.portlet.expando.model.ExpandoBridge;
import com.liferay.portlet.expando.model.ExpandoColumnConstants;
import com.liferay.portlet.messageboards.model.MBCategory;
import com.liferay.portlet.messageboards.model.MBCategoryConstants;
import com.liferay.portlet.messageboards.model.MBMessage;
import com.liferay.portlet.messageboards.service.MBCategoryLocalServiceUtil;
import com.liferay.portlet.messageboards.service.MBMessageServiceUtil;
import com.liferay.portlet.wiki.DuplicatePageException;
import com.liferay.portlet.wiki.PageTitleException;
import com.liferay.portlet.wiki.model.WikiNode;
import com.liferay.portlet.wiki.model.WikiPage;
import com.liferay.portlet.wiki.service.WikiNodeLocalServiceUtil;
import com.liferay.portlet.wiki.service.WikiPageLocalServiceUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

public class Ideenreifung extends MVCPortlet {
	
	private final String EXPANDO_ID = "Ideen-ID";
	private final String PREF_IDEA = "theIdea";
	private final String PREF_STATE = "theState";
	private final String PREF_STARTDATE = "theStart";
	private final String PREF_WIKINODE = "wikiNodeId";
	private final String IDEA_PAGE = "Ideen";
	
	// https://www.liferay.com/de/community/wiki/-/wiki/Main/Portlet+IDs
	private final String PORTLET_ID_WIKI = "36";
	
	private long WIKI_NODE = 0;
	private boolean USE_DEFAULT_WIKI_NODE = true;

	private ResourceBundle rb;
	private ThemeDisplay themeDisplay;
	private long scopeGroupId;
	
	public Ideenreifung() { }

	
	public void render(RenderRequest request, RenderResponse response) throws PortletException, IOException {
		System.out.println("render()");
		
		PortletPreferences prefs = request.getPreferences();
		themeDisplay = (ThemeDisplay)request.getAttribute(WebKeys.THEME_DISPLAY);
		scopeGroupId = themeDisplay.getScopeGroupId();
		
/*		System.out.println("ScopeGroup: "+themeDisplay.getScopeGroup().getName());
		try {
			System.out.println("SiteGroupName: "+themeDisplay.getSiteGroupName());
		} catch (PortalException e1) {
			System.out.println("Error: SiteGroupName");
		} */
		
		if(prefs.getValue(PREF_IDEA, "").equals("")) {
			System.out.println("Creating ExpandoColumns...");
			try {
				ExpandoColumnCreator.addExpandoAttribute(themeDisplay, EXPANDO_ID, ExpandoColumnConstants.LONG, BlogsEntry.class.getName());
				ExpandoColumnCreator.addExpandoAttribute(themeDisplay, EXPANDO_ID, ExpandoColumnConstants.LONG, MBMessage.class.getName());
				ExpandoColumnCreator.addExpandoAttribute(themeDisplay, EXPANDO_ID, ExpandoColumnConstants.LONG, WikiPage.class.getName());
			} catch (SystemException | PortalException e) {
				e.printStackTrace();
			}
		} else {
			this.refreshState(prefs);
		}
		
		super.render(request, response);
	}
	
	/**
	 * Some global values like scopeGroupId and themeDisplay are set.
	 * If the "newIdea"-parameter is given, the values for the new idea are set.
	 */
	public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) 
		throws IOException, PortletException {
		
		System.out.println("processAction()");
		
		rb = ResourceBundle.getBundle("content.Language", actionRequest.getLocale());
		themeDisplay = (ThemeDisplay)actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
		scopeGroupId = themeDisplay.getScopeGroupId();
		
		PortletPreferences prefs = actionRequest.getPreferences();
		String newIdea = actionRequest.getParameter("newIdea");
		
//		Format dateFormatDate = FastDateFormatFactoryUtil.getDate(LocaleUtil.getDefault());
		
		// Create new idea:
		if (newIdea != null) {
			SimpleDateFormat dateFormatOut = new SimpleDateFormat("dd.MM.yyyy");
            prefs.setValue(PREF_IDEA, newIdea);
            prefs.setValue(PREF_STARTDATE, dateFormatOut.format(new Date()) );
            prefs.setValue(PREF_STATE, "0");
            prefs.setValue(EXPANDO_ID, "0");
            prefs.store();
        }
		
		// Check for wiki node:
		/*
		if(prefs.getValue( PREF_WIKINODE, "0").equals("0") ) {
			IdeaPageCreator ipCreator = new IdeaPageCreator(themeDisplay, IDEA_PAGE, scopeGroupId);
			prefs.setValue( PREF_WIKINODE, Long.toString(ipCreator.getIdeaWikiNode(true)) );
			prefs.store();
		} */
		if(prefs.getValue( PREF_WIKINODE, "0").equals("0") ) {
			
		}
//		WIKI_NODE = Long.parseLong(prefs.getValue(PREF_WIKINODE, "0"));
		
		super.processAction(actionRequest, actionResponse);
	}
	
	/**
	 * Request for creating a new blog entry.
	 * <ul>
	 * <li>Creates a blog entry for the current idea.</li>
	 * <li>Uses the entry's ID as idea-ID and saves it as portlet preference.</li>
	 * <li>Sets the idea's status to 1.</li>
	 * </ul>
	 * @param request
	 * @param response
	 * @throws PortletException
	 * @throws IOException
	 */
	public void blogAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
		PortletPreferences prefs = request.getPreferences();
		
		try {
			BlogsEntry entry = createBlogEntry(prefs.getValue(PREF_IDEA, "error"), request.getLocale());
			
			prefs.setValue(EXPANDO_ID, Long.toString(entry.getEntryId()));
			prefs.setValue(PREF_STATE, "1");
			prefs.store();
			
			entry.getExpandoBridge().setAttribute(EXPANDO_ID, Long.toString(entry.getEntryId()));
			
		} catch (PortalException pe) {
			pe.printStackTrace();
			System.out.println("Error: Could not create new entry in blog!");
		}
	}
	
	/**
	 * Request for creating a new thread in the message board
	 * <ul>
	 * <li>Creates a message board thread for the current idea.</li>
	 * <li>Uses the idea-ID (from preferences) and tries to save it as a custom field for the message board message. 
	 * If there is no such custom field, it will be created.</li>
	 * <li>Sets the idea's status to 2.</li>
	 * </ul>
	 * @param request
	 * @param response
	 * @throws PortletException
	 * @throws IOException
	 */
	public void messageBoardAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
		PortletPreferences prefs = request.getPreferences();
		
		try {
			MBMessage mbMsg = createForumPost(prefs.getValue(PREF_IDEA, "error"), "Das ist ein Test.");
			
			prefs.setValue(PREF_STATE, "2");
			prefs.store();
			
			if(mbMsg!=null) {
				mbMsg.getExpandoBridge().setAttribute(EXPANDO_ID, prefs.getValue(EXPANDO_ID, "0"));
				LinkedAssetCreator.addMBAssetToBlogEntry(themeDisplay, Long.parseLong(prefs.getValue(EXPANDO_ID, "0")), mbMsg);
			}
			
		} catch (PortalException e) {
			e.printStackTrace();
			System.out.println("Error: Could not create new thread in message board!");
		}
	}
	
	/**
	 * Request for creating a new wiki page
	 * <ul>
	 * <li>Creates a wiki page for the current idea. Uses idea-template as content.</li>
	 * <li>Uses the idea-ID (from preferences) and tries to save it as a custom field for the wiki page. 
	 * If there is no such custom field, it will be created.</li>
	 * <li>Sets the idea's status to 3.</li>
	 * </ul>
	 * @param request
	 * @param response
	 * @throws PortletException
	 * @throws IOException
	 */
	public void wikiAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
		PortletPreferences prefs = request.getPreferences();
		String createDate = prefs.getValue(PREF_STARTDATE, "");
		
		long wikiNodeId = getWikiNodeForThisPage();
		if(wikiNodeId!=0) {
			try {
				// Attention at the ! in front of USE_DEFAULT_WIKI_NODE
				WikiPage wp = createWikiPage(prefs.getValue(PREF_IDEA, "error"), createDate, wikiNodeId, !USE_DEFAULT_WIKI_NODE);
				
				prefs.setValue(PREF_STATE, "3");
				prefs.store();
				
				if(wp!=null) {
					wp.getExpandoBridge().setAttribute(EXPANDO_ID, prefs.getValue(EXPANDO_ID, "0"));
					LinkedAssetCreator.addWikiAssetToBlogEntry(themeDisplay, Long.parseLong(prefs.getValue(EXPANDO_ID, "0")), wp);
				}
				return;
			} catch (PortalException e) {
				e.printStackTrace();		
			}
		}
		System.out.println("Error: Could not create wiki page!");
	}
	
	/*
	public void documentAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
		PortletPreferences prefs = request.getPreferences();
		Long ideaID = Long.parseLong( prefs.getValue(EXPANDO_ID, "0") );
		
		File docFile = null;
		String fileName = "Article";
		String SRC_TYPE = "txt"; 
		String TARGET_TYPE = "doc";
		
		List<WikiPage> pages = WikiPageLocalServiceUtil.getPages(WIKI_NODE, 0, Integer.MAX_VALUE);
		for(WikiPage wp : pages) {
			ExpandoBridge exBr = wp.getExpandoBridge();
			if(exBr.getAttribute(EXPANDO_ID)!=null) {
				Long wpID = (Long)exBr.getAttribute(EXPANDO_ID);
				if(ideaID.equals(wpID.longValue())) {
					fileName = wp.getTitle();
					System.out.println(wp.toXmlString());
					
//					docFile = this.openOfficeConvert(SRC_TYPE, TARGET_TYPE, prefs.getValue(EXPANDO_ID, "0"), wp.getContent());
					break;
				}
			} else {
				System.out.println("No custom fields created for WikiPage yet.");
				break;
			}
		}
		
		if(docFile != null) {
			InputStream fileIs = new FileInputStream(docFile);
			try {
				ServletResponseUtil.sendFile(PortalUtil.getHttpServletRequest(request), PortalUtil.getHttpServletResponse(response), fileName, fileIs, MimeTypesUtil.getContentType(docFile));
				System.out.println("Successfully created File "+fileName+".");
			} catch (IOException ioE) {
				ioE.printStackTrace();
			}
		}
	} */
	
	/**
	 * Sets the idea's state to 0 and checks all possible states again:
	 * <ul>
	 * <li>State 0: New idea is started.</li>
	 * <li>State 1: Blog entry is existing.</li>
	 * <li>State 2: Message board thread is existing.</li>
	 * <li>State 3: Wiki page is existing.</li>
	 * </ul>
	 * State 2 and state 3 imply state 0.
	 * @param request
	 * @param response
	 * @throws PortletException
	 * @throws IOException
	 */
	public void refreshAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
		PortletPreferences prefs = request.getPreferences();
		prefs.setValue(PREF_STATE, "0");
		prefs.store();
		refreshState(prefs);
	}
	
	/**
	 * Returns the wiki nodeId either for the own scope of the page, this portlet belongs to.
	 * Or uses the default scope, this page and portlet belong to.
	 * @return
	 * 		Returns 0 if no wiki nodeId was found.
	 */
	private long getWikiNodeForThisPage() {
		long layoutScopeGroupId = 0;
		boolean useDefault;
		
		// checks if this layout/page has its own scope:
		try {
			layoutScopeGroupId = themeDisplay.getLayout().getScopeGroup().getPrimaryKey();
			useDefault = false;
			// if yes, receive wiki nodes for this scope:
			List<WikiNode> nodes = WikiNodeLocalServiceUtil.getNodes(layoutScopeGroupId);
			for(WikiNode node : nodes) {
				this.USE_DEFAULT_WIKI_NODE = useDefault;
				return node.getNodeId();
			}
		} catch(PortalException | SystemException e) {
			e.printStackTrace();
		}

		useDefault = true;
		// if this layout/page has no scope, or there is no wikiNode with this scope,
		// then use the main scope, this layout/page belongs to
		// and try it once again:
		try {
			List<WikiNode> nodes = WikiNodeLocalServiceUtil.getNodes(themeDisplay.getLayout().getGroupId());
			for(WikiNode node : nodes) {
				this.USE_DEFAULT_WIKI_NODE = useDefault;
				return node.getNodeId();
			}
		} catch (PortalException | SystemException e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	
	/**
	 * Checks if the current status is higher than the saved status, and saves it to the portlet-preferences.
	 * <ul>
	 * <li>State 0: New idea is started.</li>
	 * <li>State 1: Blog entry is existing.</li>
	 * <li>State 2: Message board thread is existing.</li>
	 * <li>State 3: Wiki page is existing.</li>
	 * </ul>
	 * @param prefs
	 * @throws PortletException
	 * @throws IOException
	 */
	private void refreshState(PortletPreferences prefs) throws PortletException, IOException {
		System.out.println("refreshState()");
		
		Long ideaID = Long.parseLong( prefs.getValue(EXPANDO_ID, "0") );
		int state = Integer.parseInt( prefs.getValue(PREF_STATE, "0") );
		System.out.println("\tState: "+state+" (before check)");
		
		//expandoCheck:
		try {
			ExpandoColumnCreator.addExpandoAttribute(themeDisplay, EXPANDO_ID, ExpandoColumnConstants.LONG, BlogsEntry.class.getName());
			ExpandoColumnCreator.addExpandoAttribute(themeDisplay, EXPANDO_ID, ExpandoColumnConstants.LONG, MBMessage.class.getName());
			ExpandoColumnCreator.addExpandoAttribute(themeDisplay, EXPANDO_ID, ExpandoColumnConstants.LONG, WikiPage.class.getName());
		} catch (SystemException | PortalException e) {
			System.out.println("ExpandoCheck: Columns were not created.");
		}
		
		if(ideaID!=0) {
			if(state<1) {
				List<BlogsEntry> entries = new ArrayList<BlogsEntry>();
				try {
					entries = BlogsEntryLocalServiceUtil.getBlogsEntries(0, Integer.MAX_VALUE);
				} catch (SystemException e) {
					e.printStackTrace();
				}
				for(BlogsEntry entry : entries) {
						if(ideaID.equals(entry.getEntryId())) {
							state = 1;
							break;
						}
				}
			}
			
			if(state>0 && state<2) {
				ServiceContext serviceContext = new ServiceContext();
			    serviceContext.setScopeGroupId(scopeGroupId);
			    int msgStatus = 0;
				
				try {
					List<MBMessage> messages = MBMessageServiceUtil.getCategoryMessages(scopeGroupId, this.getForumCategoryId(serviceContext), msgStatus, 0, Integer.MAX_VALUE);
					for(MBMessage msg : messages) {
						ExpandoBridge exBr = msg.getExpandoBridge();
						if(exBr.getAttribute(EXPANDO_ID)!=null) {
							Long msgID = (Long)exBr.getAttribute(EXPANDO_ID);
							if( ideaID.equals(msgID.longValue()) ) {
								state = 2;
								break;
							}
						} else {
							System.out.println("No custom fields created for MessageBoardMessage yet.");
							System.out.println("\t at de.iisys.liferay.portlet.ideenreifung.Ideenreifung.refreshState");
							break;
						}
					}
				} catch (PortalException | SystemException pe) {
					pe.printStackTrace();
				}
			}
			
			if(state>0 && state<3) {
				List<WikiPage> pages = new ArrayList<WikiPage>();
				try {
					pages = WikiPageLocalServiceUtil.getPages(WIKI_NODE, 0, Integer.MAX_VALUE);
				} catch (SystemException e) {
					e.printStackTrace();
				}
				for(WikiPage wp : pages) {
					ExpandoBridge exBr = wp.getExpandoBridge();
					if(exBr!=null && exBr.getAttribute(EXPANDO_ID)!=null) {
						Long wpID = (Long)exBr.getAttribute(EXPANDO_ID);
						if(ideaID.equals(wpID.longValue())) {
							state = 3;
							break;
						}
					} else {
						System.out.println("No custom fields created for WikiPage yet.");
						System.out.println("\t at de.iisys.liferay.portlet.ideenreifung.Ideenreifung.refreshState");
						break;
					}
				}
			}
		}
		
		System.out.println("\tState: "+state+" (after check)");
		prefs.setValue(PREF_STATE, Integer.toString(state));
		prefs.store();
	}
	
	/**
	 * Creates a blog entry in the current user's personal blog.
	 * @param title
	 * @return
	 * @throws PortalException
	 */
	private BlogsEntry createBlogEntry(String title, Locale locale) throws PortalException {
		System.out.println("createBlogEntry()");
		
/*		Liferay 7:
 		String theTitle = LanguageUtil.get(rb, "ideen_idea")+": "+title;
		String subTitle = LanguageUtil.get(rb, "ideen_blogsentry-subtitle");
		String theContent = LanguageUtil.get(rb, "ideen_blogsentry-content");
		*/
		String theTitle = title;
		String theContent = LanguageUtil.get(getPortletConfig(), themeDisplay.getLocale(), "ideen_blogsentry-content");
		
		Calendar cal = Calendar.getInstance();
		boolean allowPingbacks = true;
		boolean allowTrackbacks = true;
		boolean smallImage = false;
		
//		ServiceContext serviceContext = new ServiceContext();
//	    serviceContext.setScopeGroupId(scopeGroupId);
	    
	    // Find the proper blog:
 		ServiceContext serviceContext = new ServiceContext();
 		long blogScopeGroupId = 0;
 		try {
 			blogScopeGroupId = themeDisplay.getLayout().getScopeGroup().getPrimaryKey();
 		} catch(PortalException | SystemException e) {
 			e.printStackTrace();
 		}
 		
 		BlogsEntry entry = null;
 		// use the scope of the page this portlet belongs to:
		if(blogScopeGroupId!=0) {
			serviceContext.setScopeGroupId(blogScopeGroupId);
			try {
				entry = BlogsEntryServiceUtil.addEntry(theTitle, "", theContent, 
						cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.YEAR), 
						cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 
						allowPingbacks, allowTrackbacks, null, 
						smallImage, null, null, null, 
						serviceContext);
				setPermissionsForStandardUser(BlogsEntry.class.getName(), 
						new String[]{ActionKeys.VIEW}, 
						entry.getPrimaryKey(), 
						null, null, themeDisplay);
			} catch (SystemException e) {
				e.printStackTrace();
			}
		}
		
		// if that approach failed (eg. because this page has no scope or the blog on this page uses another scope),
		// then use the default/main scope and try again:
		if(entry==null) {
			serviceContext.setScopeGroupId(themeDisplay.getLayout().getGroupId());
			try {
				entry = BlogsEntryServiceUtil.addEntry(theTitle, "", theContent, 
						cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.YEAR), 
						cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 
						allowPingbacks, allowTrackbacks, null, 
						smallImage, null, null, null, 
						serviceContext);
			} catch (SystemException e) {
				e.printStackTrace();
			}
		}
		
	    return entry;
	}
	
	/**
	 * Creates a thread in the message board's idea-category (defined in the Language properties)
	 * @param title: The thread's title
	 * @param message: The thread's message/body
	 * @return the newly created thread
	 * @throws PortalException 
	 */
	private MBMessage createForumPost(String title, String message) throws PortalException {
		System.out.println("createForumPost()");
		
		MBMessage mbMsg = null;
//		String theTitle = titleCheck(title);
		/* Liferay 7:
		String theMessage = "<< "+LanguageUtil.get(rb, "ideen_mbthread-msg1")+" >>\n\n"+LanguageUtil.format(rb, "ideen_mbthread-msg2", theTitle)+":\n\n"+message;
		*/		
		String theMessage = "<< "+LanguageUtil.get(getPortletConfig(), themeDisplay.getLocale(), "ideen_mbthread-msg1")+" >>\n\n"+
					LanguageUtil.format(themeDisplay.getLocale(), "ideen_mbthread-msg2", title)+":\n\n"+message;
		
		
		// Find the proper forum:
		ServiceContext serviceContext = new ServiceContext();
		long forumScopeGroupId = 0;
		try {
			forumScopeGroupId = themeDisplay.getLayout().getScopeGroup().getPrimaryKey();
		} catch(PortalException | SystemException e) {
			e.printStackTrace();
		}
		
		long forumCategoryId = 0;
		if(forumScopeGroupId!=0) {
			serviceContext.setScopeGroupId(forumScopeGroupId);
			forumCategoryId = getForumCategoryId(serviceContext);
		}
		
		if(forumCategoryId==0) {
			serviceContext.setScopeGroupId(themeDisplay.getLayout().getGroupId());
			forumCategoryId = getForumCategoryId(serviceContext);
		}
		
		try {
			mbMsg = MBMessageServiceUtil.addMessage(forumCategoryId, title, theMessage, serviceContext);
			System.out.println("Successfully created "+title+" with status "+mbMsg.getStatus());
			setPermissionsForStandardUser(MBMessage.class.getName(), 
					new String[]{ActionKeys.VIEW, ActionKeys.UPDATE}, 
					mbMsg.getPrimaryKey(), null, null, themeDisplay);
		} catch (PortalException | SystemException e) {
			e.printStackTrace();
			System.out.println("Error: Could not create new thread in message board!");
		}
		return mbMsg;
	}
	
	/**
	 * Returns the message board category-id of the specified idea-category (defined in the Language properties)
	 * If there is no such category, it will be created.
	 * @param serviceContext
	 * @return category id of the category with the name CATEGORY_NAME
	 * @throws PortalException
	 */
	private long getForumCategoryId(ServiceContext serviceContext) throws PortalException {
		List<MBCategory> categories;
		try {
			categories = MBCategoryLocalServiceUtil.getCategories(serviceContext.getScopeGroupId());
			for(MBCategory category : categories ){
				/* Liferay 7:
			    if(category.getName().equals( LanguageUtil.get(rb, "ideen_mb-category-name") )) */
				if(category.getName().equals( LanguageUtil.get(getPortletConfig(), themeDisplay.getLocale(), "ideen_mb-category-name") ))
			    	return category.getCategoryId();
			}
			MBCategory createdCategory = MBCategoryLocalServiceUtil.addCategory(themeDisplay.getUserId(), 
					MBCategoryConstants.DEFAULT_PARENT_CATEGORY_ID, 
					/* Liferay 7:
					LanguageUtil.get(rb, "ideen_mb-category-name"), 
					LanguageUtil.get(rb, "ideen_mb-category-desc"), */
					LanguageUtil.get(getPortletConfig(), themeDisplay.getLocale(), "ideen_mb-category-name"), 
					LanguageUtil.get(getPortletConfig(), themeDisplay.getLocale(), "ideen_mb-category-desc"),
					serviceContext);
			return createdCategory.getCategoryId();
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	/**
	 * Creates a new page in the wiki with the node: WIKI_NODE.
	 * @param title
	 * @param createDate
	 * @return the newly created wiki page
	 * @throws PortalException
	 */
	private WikiPage createWikiPage(String title, String createDate, long wikiNodeId, boolean editFrontPage) throws PortalException {   
	    ServiceContext serviceContext = new ServiceContext();
	    serviceContext.setScopeGroupId(scopeGroupId);
	    
	    Object[] contentArgs = {titleCheck(title), themeDisplay.getUser().getFullName(), createDate, title};
//	    String content = LanguageUtil.format(rb, "ideen_wikipage-content1", contentArgs)
//				+ LanguageUtil.get(rb, "ideen_wikipage-content2");
	    String content = themeDisplay.translate("ideen_wikipage-content1", contentArgs)
				+ themeDisplay.translate("ideen_wikipage-content2");
//	    String summary = LanguageUtil.format(rb, "ideen_wikipage-summary", titleCheck(title));
	    String summary = themeDisplay.translate("ideen_wikipage-summary", titleCheck(title));
	    boolean minorEdit = false;
	    
		try {
			WikiPage wp = WikiPageLocalServiceUtil.addPage(themeDisplay.getUserId(), 
					wikiNodeId, wikiTitleCheck(title), content, summary, minorEdit, 
					serviceContext);
			
			// set permissions for page:
			setPermissionsForStandardUser(WikiPage.class.getName(), 
					new String[]{ActionKeys.VIEW, ActionKeys.UPDATE},  
					wp.getResourcePrimKey(),
					null, null, themeDisplay);
			
			// add new page to frontpage:
			if(editFrontPage) {
				try {
					WikiPage frontPage = WikiPageLocalServiceUtil.getPage(wikiNodeId, "FrontPage");
					if(frontPage!=null) {
						frontPage.setContent("*"+createDate+": [["+wp.getTitle()+"]]\n"+frontPage.getContent());
						WikiPageLocalServiceUtil.updateWikiPage(frontPage);
					}
				} catch(SystemException e) {
					System.out.println("Idea Maturing: Could not add link on wiki FrontPage");
				}
			}
			
			System.out.println("Success: Wiki page "+title+" created.");
			return wp;
		} catch (PageTitleException ex) {
			ex.printStackTrace();
			System.out.println("Warning: PageTitleException!");
//			createWikiPage(wikiTitleCheck(title));
		} catch (DuplicatePageException dpe) {
			System.out.println("DuplicatePageException: Wiki page already exists.");
		} catch (SystemException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void setPermissionsForStandardUser(String className, String[] actionKeys, 
			long layoutPrimKey, String portletId, String portletInstanceId) {		
		setPermissionsForStandardUser(className, actionKeys, layoutPrimKey, portletId, portletInstanceId, themeDisplay);
	}
	
	public static void setPermissionsForStandardUser(String className, String[] actionKeys, 
			long layoutPrimKey, String portletId, String portletInstanceId, ThemeDisplay td) {
		try {
			long companyId = td.getCompanyId();
			Role roleUser = RoleLocalServiceUtil.getRole(companyId, RoleConstants.USER);
			
			/* Individual scope - If the permission applies to a model instance, primkey will be the primary key of the instance. 
			 * If the permission is for a portlet, primKey will contain the primary key of the layout containing the portlet, 
			 * followed by "_LAYOUT_" and the portlet ID. The instance ID will also be present for instanceable portlets, 
			 * preceded by "_INSTANCE_".
			 * https://docs.liferay.com/portal/6.2/javadocs-all/com/liferay/portal/model/impl/ResourcePermissionImpl.html
			 */
			StringBuffer primKey = new StringBuffer(String.valueOf(layoutPrimKey));
			if(portletId!=null)
				primKey.append("_LAYOUT_"+portletId);
			if(portletInstanceId != null)
				primKey.append("_INSTANCE_"+portletInstanceId);
			
			ResourcePermissionLocalServiceUtil.setResourcePermissions(companyId, className, 
					ResourceConstants.SCOPE_INDIVIDUAL, primKey.toString(), 
					roleUser.getRoleId(), 
					actionKeys
			);
		} catch(SystemException | PortalException e) {
			e.printStackTrace();
		}
	}
	
	private String titleCheck(String formerTitle) {
		if(formerTitle.startsWith("Idee:") || formerTitle.startsWith("Idea:"))
			return formerTitle;
		else {
			/* Liferay 7:
			return LanguageUtil.get(rb, "ideen_idea")+": "+formerTitle; */
			return LanguageUtil.get(getPortletConfig(), themeDisplay.getLocale(), "ideen_idea")+": "+formerTitle;
		}
	}
	
	private String wikiTitleCheck(String title) {
		return title.replace(":", "");
//		return HtmlUtil.escape(title);
	}
	
	/* Use AssetExport Tool instead:
	private File openOfficeConvert(String sourceExtension, String targetExtension, String id, String content) {
		File file = null;
		
		try {
			InputStream is = new UnsyncByteArrayInputStream(content.getBytes(StringPool.UTF8));
			
			file = DocumentConversionUtil.convert(id, is, sourceExtension, targetExtension);
		} catch (UnsupportedEncodingException uEE) {
			uEE.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return file;
	}
	*/
	
}
