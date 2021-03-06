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
package org.orcid.api.t2.server.delegator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.orcid.core.manager.OrcidProfileManager;
import org.orcid.core.oauth.OrcidOAuth2Authentication;
import org.orcid.jaxb.model.message.Affiliation;
import org.orcid.jaxb.model.message.AffiliationType;
import org.orcid.jaxb.model.message.Affiliations;
import org.orcid.jaxb.model.message.ContactDetails;
import org.orcid.jaxb.model.message.CreditName;
import org.orcid.jaxb.model.message.Email;
import org.orcid.jaxb.model.message.GivenNames;
import org.orcid.jaxb.model.message.Iso3166Country;
import org.orcid.jaxb.model.message.OrcidActivities;
import org.orcid.jaxb.model.message.OrcidBio;
import org.orcid.jaxb.model.message.OrcidIdentifier;
import org.orcid.jaxb.model.message.OrcidMessage;
import org.orcid.jaxb.model.message.OrcidProfile;
import org.orcid.jaxb.model.message.OrcidWorks;
import org.orcid.jaxb.model.message.Organization;
import org.orcid.jaxb.model.message.OrganizationAddress;
import org.orcid.jaxb.model.message.PersonalDetails;
import org.orcid.jaxb.model.message.ScopePathType;
import org.orcid.jaxb.model.message.Visibility;
import org.orcid.persistence.jpa.entities.ProfileEntity;
import org.orcid.test.DBUnitTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.jersey.api.uri.UriBuilderImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:orcid-t2-web-context.xml", "classpath:orcid-t2-security-context.xml" })
public class T2OrcidApiServiceDelegatorTest extends DBUnitTest {

    private static final List<String> DATA_FILES = Arrays.asList("/data/EmptyEntityData.xml", "/data/SecurityQuestionEntityData.xml", "/data/ProfileEntityData.xml",
            "/data/WorksEntityData.xml", "/data/ProfileWorksEntityData.xml", "/data/ClientDetailsEntityData.xml", "/data/Oauth2TokenDetailsData.xml");

    @Resource(name = "t2OrcidApiServiceDelegatorLatest")
    private T2OrcidApiServiceDelegator t2OrcidApiServiceDelegator;

    @Resource
    private OrcidProfileManager orcidProfileManager;

    @Mock
    private UriInfo mockedUriInfo;

    @BeforeClass
    public static void initDBUnitData() throws Exception {
        initDBUnitData(DATA_FILES, null);
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(mockedUriInfo.getBaseUriBuilder()).thenReturn(new UriBuilderImpl());
    }

    @After
    public void after() {
        SecurityContextHolder.clearContext();
        orcidProfileManager.clearOrcidProfileCache();
    }

    @Test
    public void testFindWorksDetails() {
        setUpSecurityContext();
        Response response = t2OrcidApiServiceDelegator.findWorksDetails("4444-4444-4444-4441");
        assertNotNull(response);
    }

    @Test
    public void testAddWorks() {
        setUpSecurityContext();
        OrcidMessage orcidMessage = new OrcidMessage();
        orcidMessage.setMessageVersion("1.1");
        OrcidProfile orcidProfile = new OrcidProfile();
        orcidMessage.setOrcidProfile(orcidProfile);
        orcidProfile.setOrcidIdentifier(new OrcidIdentifier("4444-4444-4444-4441"));
        OrcidActivities orcidActivities = new OrcidActivities();
        orcidProfile.setOrcidActivities(orcidActivities);
        OrcidWorks orcidWorks = new OrcidWorks();
        orcidActivities.setOrcidWorks(orcidWorks);
        Response response = t2OrcidApiServiceDelegator.addWorks(mockedUriInfo, "4444-4444-4444-4441", orcidMessage);
        assertNotNull(response);
    }

    @Test
    public void testReadClaimedAsClientOnly() {
        setUpSecurityContextForClientOnly();
        String orcid = "4444-4444-4444-4441";

        Response readResponse = t2OrcidApiServiceDelegator.findFullDetails(orcid);

        assertNotNull(readResponse);
        assertEquals(HttpStatus.SC_OK, readResponse.getStatus());
        OrcidMessage retrievedMessage = (OrcidMessage) readResponse.getEntity();
        assertEquals(orcid, retrievedMessage.getOrcidProfile().getOrcid().getValue());
        assertEquals("S. Milligan", retrievedMessage.getOrcidProfile().getOrcidBio().getPersonalDetails().getCreditName().getContent());
    }

    @Test
    public void testCreateAndReadOwnCreation() {
        setUpSecurityContextForClientOnly();
        OrcidMessage orcidMessage = createStubOrcidMessage();
        Email email = new Email("madeupemail@semantico.com");
        orcidMessage.getOrcidProfile().getOrcidBio().getContactDetails().getEmail().add(email);

        Response createResponse = t2OrcidApiServiceDelegator.createProfile(mockedUriInfo, orcidMessage);

        assertNotNull(createResponse);
        assertEquals(HttpStatus.SC_CREATED, createResponse.getStatus());
        String location = ((URI) createResponse.getMetadata().getFirst("Location")).getPath();
        assertNotNull(location);
        String orcid = location.substring(1, 20);

        Response readResponse = t2OrcidApiServiceDelegator.findFullDetails(orcid);
        assertNotNull(readResponse);
        assertEquals(HttpStatus.SC_OK, readResponse.getStatus());
        OrcidMessage retrievedMessage = (OrcidMessage) readResponse.getEntity();
        assertEquals(orcid, retrievedMessage.getOrcidProfile().getOrcid().getValue());
        assertEquals("Test credit name", retrievedMessage.getOrcidProfile().getOrcidBio().getPersonalDetails().getCreditName().getContent());
    }

    @Test
    public void testReadUnclaimedWhenNotOwnCreation() {
        setUpSecurityContextForClientOnly();
        OrcidMessage orcidMessage = createStubOrcidMessage();
        Email email = new Email("madeupemail2@semantico.com");
        orcidMessage.getOrcidProfile().getOrcidBio().getContactDetails().getEmail().add(email);

        Response createResponse = t2OrcidApiServiceDelegator.createProfile(mockedUriInfo, orcidMessage);

        assertNotNull(createResponse);
        assertEquals(HttpStatus.SC_CREATED, createResponse.getStatus());
        String location = ((URI) createResponse.getMetadata().getFirst("Location")).getPath();
        assertNotNull(location);
        String orcid = location.substring(1, 20);

        setUpSecurityContextForClientOnly("4444-4444-4444-4448");

        Response readResponse = t2OrcidApiServiceDelegator.findFullDetails(orcid);
        assertNotNull(readResponse);
        assertEquals(HttpStatus.SC_OK, readResponse.getStatus());
        OrcidMessage retrievedMessage = (OrcidMessage) readResponse.getEntity();
        assertEquals(orcid, retrievedMessage.getOrcidProfile().getOrcid().getValue());
        GivenNames givenNames = retrievedMessage.getOrcidProfile().getOrcidBio().getPersonalDetails().getGivenNames();
        assertNotNull(givenNames);
        assertEquals("Reserved For Claim", givenNames.getContent());
    }

    @Test
    public void testAddAffilliations() {
        setUpSecurityContext(ScopePathType.AFFILIATIONS_CREATE);
        OrcidMessage orcidMessage = new OrcidMessage();
        orcidMessage.setMessageVersion("1.1");
        OrcidProfile orcidProfile = new OrcidProfile();
        orcidMessage.setOrcidProfile(orcidProfile);
        orcidProfile.setOrcidIdentifier(new OrcidIdentifier("4444-4444-4444-4441"));
        OrcidActivities orcidActivities = new OrcidActivities();
        orcidProfile.setOrcidActivities(orcidActivities);
        Affiliations affiliations = new Affiliations();
        orcidActivities.setAffiliations(affiliations);
        Affiliation affiliation1 = new Affiliation();
        affiliations.getAffiliation().add(affiliation1);
        affiliation1.setType(AffiliationType.EDUCATION);
        Organization organization1 = new Organization();
        affiliation1.setOrganization(organization1);
        organization1.setName("A new affiliation");
        OrganizationAddress organizationAddress = new OrganizationAddress();
        organization1.setAddress(organizationAddress);
        organizationAddress.setCity("Edinburgh");
        organizationAddress.setCountry(Iso3166Country.GB);
        Response response = t2OrcidApiServiceDelegator.addAffiliations(mockedUriInfo, "4444-4444-4444-4441", orcidMessage);
        assertNotNull(response);
        assertEquals(HttpStatus.SC_CREATED, response.getStatus());
        String location = ((URI) response.getMetadata().getFirst("Location")).getPath();
        assertNotNull(location);

        OrcidProfile retrievedProfile = orcidProfileManager.retrieveOrcidProfile("4444-4444-4444-4441");
        List<Affiliation> affiliationsList = retrievedProfile.getOrcidActivities().getAffiliations().getAffiliation();
        assertEquals(1, affiliationsList.size());
        Affiliation affiliation = affiliationsList.get(0);
        assertEquals("A new affiliation", affiliation.getOrganization().getName());
        assertEquals("4444-4444-4444-4447", affiliation.getSource().getSourceOrcid().getPath());
    }

    private OrcidMessage createStubOrcidMessage() {
        OrcidMessage orcidMessage = new OrcidMessage();
        orcidMessage.setMessageVersion("1.1");
        OrcidProfile orcidProfile = new OrcidProfile();
        orcidMessage.setOrcidProfile(orcidProfile);
        OrcidBio orcidBio = new OrcidBio();
        orcidProfile.setOrcidBio(orcidBio);
        PersonalDetails personalDetails = new PersonalDetails();
        orcidBio.setPersonalDetails(personalDetails);
        GivenNames givenNames = new GivenNames("Test given names");
        personalDetails.setGivenNames(givenNames);
        CreditName creditName = new CreditName("Test credit name");
        personalDetails.setCreditName(creditName);
        creditName.setVisibility(Visibility.LIMITED);
        ContactDetails contactDetails = new ContactDetails();
        orcidBio.setContactDetails(contactDetails);
        return orcidMessage;
    }

    private void setUpSecurityContext() {
        setUpSecurityContext(ScopePathType.ORCID_WORKS_CREATE);
    }

    private void setUpSecurityContext(ScopePathType... scopePathTypes) {
        SecurityContextImpl securityContext = new SecurityContextImpl();
        OrcidOAuth2Authentication mockedAuthentication = mock(OrcidOAuth2Authentication.class);
        securityContext.setAuthentication(mockedAuthentication);
        SecurityContextHolder.setContext(securityContext);
        when(mockedAuthentication.getPrincipal()).thenReturn(new ProfileEntity("4444-4444-4444-4441"));
        AuthorizationRequest authorizationRequest = mock(AuthorizationRequest.class);
        Set<String> scopes = new HashSet<String>();
        for (ScopePathType scopePathType : scopePathTypes) {
            scopes.add(scopePathType.value());
        }
        when(authorizationRequest.getClientId()).thenReturn("4444-4444-4444-4447");
        when(authorizationRequest.getScope()).thenReturn(scopes);
        when(mockedAuthentication.getAuthorizationRequest()).thenReturn(authorizationRequest);
    }

    private void setUpSecurityContextForClientOnly() {
        setUpSecurityContextForClientOnly("4444-4444-4444-4445");
    }

    private void setUpSecurityContextForClientOnly(String clientId) {
        Set<String> scopes = new HashSet<String>();
        scopes.add(ScopePathType.ORCID_PROFILE_CREATE.value());
        setUpSecurityContextForClientOnly(clientId, scopes);
    }

    private void setUpSecurityContextForClientOnly(String clientId, Set<String> scopes) {
        SecurityContextImpl securityContext = new SecurityContextImpl();
        OrcidOAuth2Authentication mockedAuthentication = mock(OrcidOAuth2Authentication.class);
        securityContext.setAuthentication(mockedAuthentication);
        SecurityContextHolder.setContext(securityContext);
        when(mockedAuthentication.getPrincipal()).thenReturn(new ProfileEntity(clientId));
        when(mockedAuthentication.isClientOnly()).thenReturn(true);
        AuthorizationRequest authorizationRequest = mock(AuthorizationRequest.class);
        when(authorizationRequest.getClientId()).thenReturn(clientId);
        when(authorizationRequest.getScope()).thenReturn(scopes);
        when(mockedAuthentication.getAuthorizationRequest()).thenReturn(authorizationRequest);
    }

    @Test
    public void testRegisterAndUnregisterWebhook() {
        Set<String> scopes = new HashSet<String>();
        scopes.add(ScopePathType.WEBHOOK.value());
        setUpSecurityContextForClientOnly("4444-4444-4444-4445", scopes);
        Response response = t2OrcidApiServiceDelegator.registerWebhook(mockedUriInfo, "4444-4444-4444-4447", "www.webhook.com");
        assertNotNull(response);
        assertEquals(HttpStatus.SC_CREATED, response.getStatus());
        response = t2OrcidApiServiceDelegator.unregisterWebhook(mockedUriInfo, "4444-4444-4444-4447", "www.webhook.com");
        assertNotNull(response);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());
    }

}