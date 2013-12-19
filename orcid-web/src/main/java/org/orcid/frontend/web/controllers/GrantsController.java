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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.orcid.core.adapter.Jaxb2JpaAdapter;
import org.orcid.core.security.visibility.OrcidVisibilityDefaults;
import org.orcid.jaxb.model.message.CurrencyCode;
import org.orcid.jaxb.model.message.GrantType;
import org.orcid.jaxb.model.message.OrcidGrant;
import org.orcid.jaxb.model.message.OrcidGrants;
import org.orcid.jaxb.model.message.OrcidProfile;
import org.orcid.persistence.dao.GrantExternalIdentifierDao;
import org.orcid.persistence.dao.OrgDisambiguatedDao;
import org.orcid.persistence.dao.OrgDisambiguatedSolrDao;
import org.orcid.persistence.dao.ProfileDao;
import org.orcid.persistence.dao.ProfileGrantDao;
import org.orcid.persistence.jpa.entities.GrantExternalIdentifierEntity;
import org.orcid.persistence.jpa.entities.OrgDisambiguatedEntity;
import org.orcid.persistence.jpa.entities.ProfileEntity;
import org.orcid.persistence.jpa.entities.ProfileGrantEntity;
import org.orcid.persistence.solr.entities.OrgDisambiguatedSolrDocument;
import org.orcid.pojo.ajaxForm.Contributor;
import org.orcid.pojo.ajaxForm.Date;
import org.orcid.pojo.ajaxForm.GrantExternalIdentifierForm;
import org.orcid.pojo.ajaxForm.GrantForm;
import org.orcid.pojo.ajaxForm.PojoUtil;
import org.orcid.pojo.ajaxForm.Text;
import org.orcid.pojo.ajaxForm.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Angel Montenegro
 */
@Controller("grantsController")
@RequestMapping(value = { "/grants" })
public class GrantsController extends BaseWorkspaceController {
	private static final Logger LOGGER = LoggerFactory.getLogger(GrantsController.class);
	private static final String GRANT_MAP = "GRANT_MAP";
	
	@Resource
    private ProfileDao profileDao;
	
	@Resource
	ProfileGrantDao profileGrantDao;
	
	@Resource 
	GrantExternalIdentifierDao grantExternalIdentifierDao;
	
	@Resource
    private Jaxb2JpaAdapter jaxb2JpaAdapter;
	
	@Resource
    private OrgDisambiguatedSolrDao orgDisambiguatedSolrDao;
	
	@Resource
    private OrgDisambiguatedDao orgDisambiguatedDao;
	
	/**
     * Returns a blank grant form
     * */
    @RequestMapping(value = "/grant.json", method = RequestMethod.GET)
    public @ResponseBody
    GrantForm getGrant(HttpServletRequest request) { 
    	GrantForm result = new GrantForm();
    	result.setAmount(new Text());    	
    	result.setCurrencyCode(new Text());
    	result.setDescription(new Text());    	  	    
    	result.setGrantName(new Text());
    	result.setGrantType(new Text());
    	result.setSourceName(new String());    	
    	result.setTitle(new Text());
    	result.setUrl(new Text());    	
    	OrcidProfile profile = getEffectiveProfile();
        Visibility v = Visibility.valueOf(profile.getOrcidInternal().getPreferences().getWorkVisibilityDefault().getValue());
        result.setVisibility(v);    	
    	Date startDate = new Date();
    	result.setStartDate(startDate);
        startDate.setDay("");
        startDate.setMonth("");
        startDate.setYear("");
        Date endDate = new Date();
        result.setEndDate(endDate);
        endDate.setDay("");
        endDate.setMonth("");
        endDate.setYear("");    	
        
        // Set empty contributor
        Contributor contr = new Contributor();
        List<Contributor> contrList = new ArrayList<Contributor>();
        Text rText = new Text();
        rText.setValue("");
        contr.setContributorRole(rText);
        Text sText = new Text();
        sText.setValue("");
        contr.setContributorSequence(sText);
        contrList.add(contr);
        result.setContributors(contrList);
                
        // Set empty external identifier
        List<GrantExternalIdentifierForm> emptyExternalIdentifiers = new ArrayList<GrantExternalIdentifierForm>();
        GrantExternalIdentifierForm f = new GrantExternalIdentifierForm();
        f.setType(new Text());
        f.setUrl(new Text());
        f.setValue(new Text());
        emptyExternalIdentifiers.add(f);
        result.setExternalIdentifiers(emptyExternalIdentifiers);
        
        result.setCity(new Text());
        result.setCountry(new Text());
        result.setRegion(new Text());
        
    	return result;
    }
    
    /**
     * Returns a blank grant form
     * */
    @RequestMapping(value = "/grant.json", method = RequestMethod.DELETE)
    public @ResponseBody GrantForm deleteGrantJson(HttpServletRequest request, GrantForm grant) {
    	OrcidProfile currentProfile = getEffectiveProfile();
        OrcidGrants grants = currentProfile.getOrcidActivities() == null ? null : currentProfile.getOrcidActivities().getOrcidGrants();
        if(grants != null) {
        	List<OrcidGrant> grantList = grants.getOrcidGrant();
        	Iterator<OrcidGrant> iterator = grantList.iterator();
        	while(iterator.hasNext()){
        		OrcidGrant orcidGrant = iterator.next();
        		if(grant.getPutCode().getValue().equals(orcidGrant.getPutCode())){
        			iterator.remove();
        		}
        	}
        }
    	return grant;
    }
    
    /**
     * List grants associated with a profile
     * */
    @RequestMapping(value = "/grantIds.json", method = RequestMethod.GET)
    public @ResponseBody
    List<String> getGrantsJson(HttpServletRequest request) {
        // Get cached profile
        List<String> grantIds = createGrantIdList(request);
        return grantIds;
    }

    /**
     * Create a grant id list and sorts a map associated with the list in
     * in the session
     * 
     */
    private List<String> createGrantIdList(HttpServletRequest request) {
        OrcidProfile currentProfile = getEffectiveProfile();
        OrcidGrants grants = currentProfile.getOrcidActivities() == null ? null : currentProfile.getOrcidActivities().getOrcidGrants();

        HashMap<String, GrantForm> grantsMap = new HashMap<>();
        List<String> grantIds = new ArrayList<String>();
        if (grants != null) {
            for (OrcidGrant grant : grants.getOrcidGrant()) {
                try {
                    GrantForm form = GrantForm.valueOf(grant);
                    if (grant.getType() != null) {
                        form.setGrantTypeForDisplay(getMessage(buildInternationalizationKey(GrantType.class, grant.getType().value())));
                    }
                    grantsMap.put(grant.getPutCode(), form);
                    grantIds.add(grant.getPutCode());
                } catch (Exception e) {
                    LOGGER.error("Failed to parse as Grant. Put code" + grant.getPutCode());
                }
            }
            request.getSession().setAttribute(GRANT_MAP, grantsMap);
        }
        return grantIds;
    }
    
    /**
     * List grants associated with a profile
     * */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/grants.json", method = RequestMethod.GET)
    public @ResponseBody
    List<GrantForm> getGrantJson(HttpServletRequest request, @RequestParam(value = "grantIds") String grantIdsStr) {
        List<GrantForm> grantList = new ArrayList<>();
        GrantForm grant = null;
        String[] grantIds = grantIdsStr.split(",");

        if (grantIds != null) {
            HashMap<String, GrantForm> grantsMap = (HashMap<String, GrantForm>) request.getSession().getAttribute(GRANT_MAP);
            // this should never happen, but just in case.
            if (grantsMap == null) {
                createGrantIdList(request);
                grantsMap = (HashMap<String, GrantForm>) request.getSession().getAttribute(GRANT_MAP);
            }
            for (String grantId : grantIds) {
            	grant = grantsMap.get(grantId);
                grantList.add(grant);
            }
        }

        return grantList;
    }
    
    
    /**
     * Persist a grant object on database
     * */
    @RequestMapping(value = "/grant.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm postGrant(HttpServletRequest request, @RequestBody GrantForm grant) {
    	validateName(grant);
    	validateAmount(grant);
    	validateCurrency(grant);
    	validateTitle(grant);
    	validateDescription(grant);   
    	validateUrl(grant);
    	validateDates(grant);
    	validateExternalIdentifiers(grant);
    	validateType(grant);
    	validateCity(grant);
    	validateRegion(grant);
    	validateCountry(grant);
    	
    	copyErrors(grant.getGrantName(), grant);
    	copyErrors(grant.getAmount(), grant);
    	copyErrors(grant.getCurrencyCode(), grant);
    	copyErrors(grant.getTitle(), grant);
    	copyErrors(grant.getDescription(), grant);
    	copyErrors(grant.getUrl(), grant);
    	copyErrors(grant.getEndDate(), grant);
    	copyErrors(grant.getGrantType(), grant);
    	
    	for(GrantExternalIdentifierForm extId : grant.getExternalIdentifiers()){
    		copyErrors(extId.getType(), grant);
    		copyErrors(extId.getUrl(), grant);
    		copyErrors(extId.getValue(), grant);
    	}
    	
    	// If there are no errors, persist to DB
    	if (grant.getErrors().isEmpty()) {
    		//Set the credit name
    		setContributorsCreditName(grant);
    		//Update on database
    		ProfileEntity userProfile = profileDao.find(getEffectiveUserOrcid());
    		ProfileGrantEntity profileGrantEntity = jaxb2JpaAdapter.getNewProfileGrantEntity(grant.toOrcidGrant(), userProfile);
    		profileGrantEntity.setSource(userProfile);
    		// Persists the profile grant object
    		ProfileGrantEntity newProfileGrant = profileGrantDao.createProfileGrant(profileGrantEntity);
    		
    		// Persist the external identifiers
    		SortedSet<GrantExternalIdentifierEntity> externalIdentifiers = profileGrantEntity.getExternalIdentifiers();
    		
    		if(externalIdentifiers != null && !externalIdentifiers.isEmpty()) {
    			for(GrantExternalIdentifierEntity externalIdentifier : externalIdentifiers) {
    				externalIdentifier.setProfileGrant(newProfileGrant);    				
    				grantExternalIdentifierDao.createGrantExternalIdentifier(externalIdentifier);
    			}
    		}
    	}
    	
    	return grant;
    }
    
    private void setContributorsCreditName(GrantForm grant) {
    	OrcidProfile profile = getEffectiveProfile();
    	String creditName = null;
    	Visibility creditNameVisibility = null;
    	if(profile.getOrcidBio() != null && profile.getOrcidBio().getPersonalDetails() != null && profile.getOrcidBio().getPersonalDetails().getCreditName() != null) {
    		creditName = profile.getOrcidBio().getPersonalDetails().getCreditName().getContent();
    		creditNameVisibility = Visibility.valueOf(profile.getOrcidBio().getPersonalDetails().getCreditName().getVisibility());
    	}
    	if(grant != null && grant.getContributors() != null && !grant.getContributors().isEmpty()) {
    		for(Contributor contributor : grant.getContributors()){
    			if(!PojoUtil.isEmpty(creditName))
    				contributor.setCreditName(Text.valueOf(creditName));
    			if(creditNameVisibility != null) {
    				contributor.setCreditNameVisibility(creditNameVisibility);
    			} else {
    				contributor.setCreditNameVisibility(Visibility.valueOf(OrcidVisibilityDefaults.CREDIT_NAME_DEFAULT.getVisibility()));
    			}
    		}
    	}
    }
    
    /**
     * Validators
     * */
    @RequestMapping(value = "/grant/amountValidate.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm validateAmount(@RequestBody GrantForm grant) {    	
    	grant.getAmount().setErrors(new ArrayList<String>());
    	if(PojoUtil.isEmpty(grant.getAmount())) { 
    		setError(grant.getAmount(), "NotBlank.grant.amount");
    	} else {
    		String amount = grant.getAmount().getValue();
    		long lAmount = 0;
    		try {
    			lAmount = Long.valueOf(amount);
    		} catch(NumberFormatException nfe) {
    			setError(grant.getAmount(), "Invalid.grant.amount");
    		}
    		
    		if(lAmount < 0)
    			setError(grant.getAmount(), "Invalid.grant.amount");
    	}
    	return grant;
    }
    
    @RequestMapping(value = "/grant/currencyValidate.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm validateCurrency(@RequestBody GrantForm grant) {
    	grant.getCurrencyCode().setErrors(new ArrayList<String>());
    	if(PojoUtil.isEmpty(grant.getCurrencyCode())) {
    		setError(grant.getCurrencyCode(), "NotBlank.grant.currency");
    	} else {
    		try {
    			CurrencyCode.fromValue(grant.getCurrencyCode().getValue());
    		} catch(IllegalArgumentException iae) {
    			setError(grant.getCurrencyCode(), "Invalid.grant.currency");
    		}
    	}
    	return grant;
    }
    
    @RequestMapping(value = "/grant/nameValidate.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm validateName(@RequestBody GrantForm grant) {
    	grant.getGrantName().setErrors(new ArrayList<String>());
        if (grant.getGrantName().getValue() == null || grant.getGrantName().getValue().trim().length() == 0) {
            setError(grant.getGrantName(), "NotBlank.grant.name");
        } else {
            if (grant.getGrantName().getValue().trim().length() > 1000) {
                setError(grant.getGrantName(), "grant.length_less_1000");
            }
        }
        return grant;
    }
    
    @RequestMapping(value = "/grant/titleValidate.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm validateTitle(@RequestBody GrantForm grant) {
    	grant.getTitle().setErrors(new ArrayList<String>());
    	if(PojoUtil.isEmpty(grant.getTitle())) {
    		setError(grant.getTitle(), "NotBlank.grant.title");
    	} else {
    		if(grant.getTitle().getValue().length() > 1000)
    			setError(grant.getTitle(), "grant.length_less_1000");
    	}
    	return grant;
    }
    
    @RequestMapping(value = "/grant/descriptionValidate.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm validateDescription(@RequestBody GrantForm grant) {
    	grant.getDescription().setErrors(new ArrayList<String>());    	
   		if(!PojoUtil.isEmpty(grant.getDescription()) && grant.getDescription().getValue().length() > 5000)
   			setError(grant.getDescription(), "grant.length_less_5000");
   		return grant;
    }
    
    @RequestMapping(value = "/grant/urlValidate.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm validateUrl(@RequestBody GrantForm grant) {
    	grant.getUrl().setErrors(new ArrayList<String>());
    	if(!PojoUtil.isEmpty(grant.getUrl()) && grant.getUrl().getValue().length() > 350)
    		setError(grant.getUrl(), "grant.length_less_350");
    	return grant;
    }
    
    @RequestMapping(value = "/grant/datesValidate.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm validateDates(@RequestBody GrantForm grant) {
    	grant.getStartDate().setErrors(new ArrayList<String>());
    	grant.getEndDate().setErrors(new ArrayList<String>());
        if (!PojoUtil.isEmpty(grant.getStartDate()) && !PojoUtil.isEmpty(grant.getEndDate())) {
            if (grant.getStartDate().toJavaDate().after(grant.getEndDate().toJavaDate()))
                setError(grant.getEndDate(), "grant.endDate.after");
        }
        return grant;
    }
    
    @RequestMapping(value = "/grant/externalIdentifiersValidate.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm validateExternalIdentifiers(@RequestBody GrantForm grant) {
    	if(grant.getExternalIdentifiers() != null && !grant.getExternalIdentifiers().isEmpty()) {
    		for(GrantExternalIdentifierForm extId : grant.getExternalIdentifiers()) {
    			if(!PojoUtil.isEmpty(extId.getType()) && extId.getType().getValue().length() > 255)
    				setError(extId.getType(), "grant.lenght_less_255");
    			if(!PojoUtil.isEmpty(extId.getUrl()) && extId.getUrl().getValue().length() > 350)
    				setError(extId.getUrl(), "grant.length_less_350");
    			if(!PojoUtil.isEmpty(extId.getValue()) && extId.getValue().getValue().length() > 2084)
    				setError(extId.getValue(), "grant.length_less_2084");
    		}
    	}
    	return grant;
    }
    
    @RequestMapping(value = "/grant/typeValidate.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm validateType(@RequestBody GrantForm grant) {
    	grant.getGrantType().setErrors(new ArrayList<String>());
    	if(PojoUtil.isEmpty(grant.getGrantType())) {
    		setError(grant.getGrantType(), "NotBlank.grant.type");
    	} else {
    		try {
    			GrantType.fromValue(grant.getGrantType().getValue());
    		} catch(IllegalArgumentException iae) {
    			setError(grant.getGrantType(), "Invalid.grant.type");
    		}
    	}
    	return grant;
    }
    
    @RequestMapping(value = "/grant/cityValidate.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm validateCity(@RequestBody GrantForm grant) {
    	grant.getCity().setErrors(new ArrayList<String>());
        if (grant.getCity().getValue() == null || grant.getCity().getValue().trim().length() == 0) {
            setError(grant.getCity(), "NotBlank.grant.city");
        } else {
            if (grant.getCity().getValue().trim().length() > 1000) {
                setError(grant.getCity(), "grant.length_less_1000");
            }
        }
        return grant;
    }
    
    @RequestMapping(value = "/grant/regionValidate.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm validateRegion(@RequestBody GrantForm grant) {
    	grant.getRegion().setErrors(new ArrayList<String>());
        if (grant.getRegion().getValue() != null && grant.getRegion().getValue().trim().length() > 1000) {
            setError(grant.getRegion(), "grant.length_less_1000");
        }
        return grant;
    }

    @RequestMapping(value = "/grant/countryValidate.json", method = RequestMethod.POST)
    public @ResponseBody
    GrantForm validateCountry(@RequestBody GrantForm grant) {
    	grant.getCountry().setErrors(new ArrayList<String>());
        if (grant.getCountry().getValue() == null || grant.getCountry().getValue().trim().length() == 0) {
            setError(grant.getCountry(), "NotBlank.grant.country");
        }
        return grant;
    }
    
    /**
     * Typeahead
     * */
    
    /**
     * Search DB for disambiguated affiliations to suggest to user
     */
    @RequestMapping(value = "/disambiguated/name/{query}", method = RequestMethod.GET)
    public @ResponseBody
    List<Map<String, String>> searchDisambiguated(@PathVariable("query") String query, @RequestParam(value = "limit") int limit) {
        List<Map<String, String>> datums = new ArrayList<>();
        for (OrgDisambiguatedSolrDocument orgDisambiguatedDocument : orgDisambiguatedSolrDao.getOrgs(query, 0, limit)) {
            Map<String, String> datum = createDatumFromOrgDisambiguated(orgDisambiguatedDocument);
            datums.add(datum);
        }
        return datums;
    }
    
    private Map<String, String> createDatumFromOrgDisambiguated(OrgDisambiguatedSolrDocument orgDisambiguatedDocument) {
        Map<String, String> datum = new HashMap<>();
        datum.put("value", orgDisambiguatedDocument.getOrgDisambiguatedName());
        datum.put("city", orgDisambiguatedDocument.getOrgDisambiguatedCity());
        datum.put("region", orgDisambiguatedDocument.getOrgDisambiguatedRegion());
        datum.put("country", orgDisambiguatedDocument.getOrgDisambiguatedCountry());
        datum.put("disambiguatedAffiliationIdentifier", Long.toString(orgDisambiguatedDocument.getOrgDisambiguatedId()));
        return datum;
    }
    
    /**
     * fetch disambiguated by id
     */
    @RequestMapping(value = "/disambiguated/id/{id}", method = RequestMethod.GET)
    public @ResponseBody
    Map<String, String> getDisambiguated(@PathVariable("id") Long id) {
        OrgDisambiguatedEntity orgDisambiguatedEntity = orgDisambiguatedDao.find(id);
        Map<String, String> datum = new HashMap<>();
        datum.put("value", orgDisambiguatedEntity.getName()); 
        datum.put("city", orgDisambiguatedEntity.getCity());
        datum.put("region", orgDisambiguatedEntity.getRegion());
        datum.put("country", orgDisambiguatedEntity.getCountry().value());
        datum.put("sourceId", orgDisambiguatedEntity.getSourceId());
        datum.put("sourceType", orgDisambiguatedEntity.getSourceType());
        return datum;
    }
}





