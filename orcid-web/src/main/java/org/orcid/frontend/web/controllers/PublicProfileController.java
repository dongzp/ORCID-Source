/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2013 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.frontend.web.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.orcid.core.locale.LocaleManager;
import org.orcid.core.manager.WorkManager;
import org.orcid.frontend.web.util.LanguagesMap;
import org.orcid.jaxb.model.message.Affiliation;
import org.orcid.jaxb.model.message.OrcidProfile;
import org.orcid.jaxb.model.message.OrcidWork;
import org.orcid.jaxb.model.message.Visibility;
import org.orcid.persistence.jpa.entities.custom.MinimizedWorkEntity;
import org.orcid.persistence.jpa.entities.custom.WorkInfoEntity;
import org.orcid.pojo.ajaxForm.AffiliationForm;
import org.orcid.pojo.ajaxForm.PojoUtil;
import org.orcid.pojo.ajaxForm.Text;
import org.orcid.pojo.ajaxForm.Work;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PublicProfileController extends BaseWorkspaceController {

    @Resource
    private LocaleManager localeManager;
    
    @Resource
    private WorkManager workManager;

    @RequestMapping(value = "/{orcid:(?:\\d{4}-){3,}\\d{3}[\\dX]}")
    public ModelAndView publicPreview(HttpServletRequest request, @RequestParam(value = "page", defaultValue = "1") int pageNo,
            @RequestParam(value = "maxResults", defaultValue = "15") int maxResults, @PathVariable("orcid") String orcid) {
        ModelAndView mav = new ModelAndView("public_profile");
        mav.addObject("isPublicProfile", true);

        request.getSession().removeAttribute(PUBLIC_WORKS_RESULTS_ATTRIBUTE);
        OrcidProfile profile = orcidProfileManager.retrievePublicOrcidProfile(orcid);

        mav.addObject("profile", profile);

        List<Work> works = new ArrayList<Work>();
        List<String> workIds = new ArrayList<String>();

        List<Affiliation> affilations = new ArrayList<Affiliation>();
        List<String> affiliationIds = new ArrayList<String>();

        if (profile.getOrcidDeprecated() != null) {
            String primaryRecord = profile.getOrcidDeprecated().getPrimaryRecord().getOrcid().getValue();
            mav.addObject("deprecated", true);
            mav.addObject("primaryRecord", primaryRecord);
        } else {
        	
        	List<MinimizedWorkEntity> publicWorks = workManager.findPublicWorks(orcid);
        	if(publicWorks != null && publicWorks.size() > 0) {
        		for(MinimizedWorkEntity minimizedWork : publicWorks){
        			works.add(Work.valueOf(minimizedWork));
        			workIds.add(String.valueOf(minimizedWork.getId()));
        		}        		
        		 if (!works.isEmpty()) {
                     mav.addObject("works", works);
                 }
        	}            

            if (profile.getOrcidActivities() != null && profile.getOrcidActivities().getAffiliations() != null) {
                for (Affiliation affiliation : profile.getOrcidActivities().getAffiliations().getAffiliation()) {
                    affilations.add(affiliation);
                    affiliationIds.add(affiliation.getPutCode());
                }
                if (!affilations.isEmpty()) {
                    mav.addObject("affilations", affilations);
                }
            }
        }
        ObjectMapper mapper = new ObjectMapper();

        try {
            String worksIdsJson = mapper.writeValueAsString(workIds);
            String affiliationIdsJson = mapper.writeValueAsString(affiliationIds);
            mav.addObject("workIdsJson", StringEscapeUtils.escapeEcmaScript(worksIdsJson));
            mav.addObject("affiliationIdsJson", StringEscapeUtils.escapeEcmaScript(affiliationIdsJson));
        } catch (JsonGenerationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return mav;
    }
    

    @RequestMapping(value = "/{orcid:(?:\\d{4}-){3,}\\d{3}[\\dX]}/affiliations.json")
    public @ResponseBody
    List<AffiliationForm> getAffiliationsJson(HttpServletRequest request, @PathVariable("orcid") String orcid, @RequestParam(value = "affiliationIds") String workIdsStr) {
        List<AffiliationForm> affs = new ArrayList<AffiliationForm>();
        OrcidProfile profile = orcidProfileManager.retrievePublicOrcidProfile(orcid);
        Map<String, Affiliation> affMap = profile.getOrcidActivities().getAffiliations().retrieveAffiliationAsMap();
        String[] affIds = workIdsStr.split(",");
        for (String id: affIds) {
            Affiliation aff = affMap.get(id);
            // ONLY SHARE THE PUBLIC AFFILIATIONS! 
            if (aff != null && aff.getVisibility().equals(Visibility.PUBLIC)) {
                affs.add(AffiliationForm.valueOf(aff));
            }
        }
        return affs;
    }

    @RequestMapping(value = "/{orcid:(?:\\d{4}-){3,}\\d{3}[\\dX]}/works.json")
    public @ResponseBody
    List<Work> getWorkJson(HttpServletRequest request, @PathVariable("orcid") String orcid, @RequestParam(value = "workIds") String workIdsStr) {
        Map<String, String> countries = retrieveIsoCountries();
        Map<String, String> languages = LanguagesMap.buildLanguageMap(localeManager.getLocale(), false);
        OrcidProfile profile = orcidProfileManager.retrievePublicOrcidProfile(orcid);
        Map<String, OrcidWork> workMap = profile.getOrcidActivities().getOrcidWorks().retrieveOrcidWorksAsMap();
        List<Work> works = new ArrayList<Work>();
        String[] workIds = workIdsStr.split(",");
        for (String workId : workIds) {
            OrcidWork orcidWork = workMap.get(workId);
            if (orcidWork != null) {
                // ONLY SHARE THE PUBLIC WORKS!
                if (orcidWork.getVisibility().equals(Visibility.PUBLIC)) {
                    Work work = Work.valueOf(orcidWork);
                    if (!PojoUtil.isEmpty(work.getCountryCode())) {
                        Text countryName = Text.valueOf(countries.get(work.getCountryCode().getValue()));
                        work.setCountryName(countryName);
                    }
                    // Set language name
                    if (!PojoUtil.isEmpty(work.getLanguageCode())) {
                        Text languageName = Text.valueOf(languages.get(work.getLanguageCode().getValue()));
                        work.setLanguageName(languageName);
                    }
                    // Set translated title language name
                    if (!(work.getWorkTitle().getTranslatedTitle() == null) && !StringUtils.isEmpty(work.getWorkTitle().getTranslatedTitle().getLanguageCode())) {
                        String languageName = languages.get(work.getWorkTitle().getTranslatedTitle().getLanguageCode());
                        work.getWorkTitle().getTranslatedTitle().setLanguageName(languageName);
                    }
                    works.add(work);
                }
            }
        }
        return works;
    }
    
    /**
     * Returns a blank work
     * */
    @RequestMapping(value = "/{orcid:(?:\\d{4}-){3,}\\d{3}[\\dX]}/getWorkInfo.json", method = RequestMethod.GET)
    public @ResponseBody
    Work getWorkInfo(@PathVariable("orcid") String orcid, @RequestParam(value = "workId") String workId) {
    	Map<String, String> countries = retrieveIsoCountries();
        Map<String, String> languages = LanguagesMap.buildLanguageMap(localeManager.getLocale(), false);
    	if(StringUtils.isEmpty(workId))
    		return null;    	    
    	
    	WorkInfoEntity workInfo = workManager.loadWorkInfo(orcid, workId);
    	
    	Work work = Work.valueOf(workInfo);
    	
    	//Set country name
        if(!PojoUtil.isEmpty(work.getCountryCode())) {            
            Text countryName = Text.valueOf(countries.get(work.getCountryCode().getValue()));
            work.setCountryName(countryName);
        }
        //Set language name
        if(!PojoUtil.isEmpty(work.getLanguageCode())) {
            Text languageName = Text.valueOf(languages.get(work.getLanguageCode().getValue()));
            work.setLanguageName(languageName);
        }
        //Set translated title language name
        if(!(work.getWorkTitle().getTranslatedTitle() == null) && !StringUtils.isEmpty(work.getWorkTitle().getTranslatedTitle().getLanguageCode())) {
            String languageName = languages.get(work.getWorkTitle().getTranslatedTitle().getLanguageCode());
            work.getWorkTitle().getTranslatedTitle().setLanguageName(languageName);
        }
    	
    	return work;
    }
}
