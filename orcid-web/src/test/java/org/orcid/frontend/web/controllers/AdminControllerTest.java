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

/**
 * Copyright 2012-2013 ORCID
 * 
 * @author Angel Montenegro (amontenegro) Date: 29/08/2013
 */
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.orcid.frontend.web.util.BaseControllerTest;
import org.orcid.persistence.dao.ProfileDao;
import org.orcid.persistence.jpa.entities.EmailEntity;
import org.orcid.persistence.jpa.entities.ProfileEntity;
import org.orcid.pojo.ProfileDeprecationRequest;
import org.orcid.pojo.ProfileDetails;
import org.orcid.pojo.ajaxForm.Group;
import org.orcid.pojo.ajaxForm.PojoUtil;
import org.orcid.pojo.ajaxForm.Text;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:orcid-core-context.xml", "classpath:orcid-frontend-web-servlet.xml" })
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class AdminControllerTest extends BaseControllerTest {

    @Resource(name = "adminController")
    AdminController adminController;

    @Resource
    private ProfileDao profileDao;

    @Before
    public void init() {
        assertNotNull(adminController);
        assertNotNull(profileDao);
    }

    @Test
    @Transactional("transactionManager")
    public void testCheckOrcid() throws Exception {
        ProfileDetails profileDetails = adminController.checkOrcid("4444-4444-4444-4441");
        assertNotNull(profileDetails);
        assertEquals(0, profileDetails.getErrors().size());
        assertEquals("spike@milligan.com", profileDetails.getEmail());
        assertEquals("Milligan", profileDetails.getFamilyName());
        assertEquals("Spike", profileDetails.getGivenNames());
        assertEquals("4444-4444-4444-4441", profileDetails.getOrcid());

        profileDetails = adminController.checkOrcid("4444-4444-4444-4411");
        assertNotNull(profileDetails);
        assertEquals(1, profileDetails.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.inexisting_orcid", "4444-4444-4444-4411"), profileDetails.getErrors().get(0));
    }

    @Test
    @Transactional("transactionManager")
    @Rollback(true)
    public void testDeprecateProfile() throws Exception {
        ProfileEntity toDeprecate = profileDao.find("4444-4444-4444-4441");
        ProfileEntity primary = profileDao.find("4444-4444-4444-4442");

        boolean containsEmail = false;

        assertNull(toDeprecate.getPrimaryRecord());

        Set<EmailEntity> emails1 = toDeprecate.getEmails();
        assertNotNull(emails1);
        assertEquals(3, emails1.size());

        for (EmailEntity email : emails1) {
            if (email.getId().equals("1@deprecate.com")) {
                if (email.getCurrent() == false && email.getVerified() == true) {
                    containsEmail = true;
                } else {
                    containsEmail = false;
                    break;
                }
            } else if (email.getId().equals("2@deprecate.com")) {
                if (email.getCurrent() == false && email.getVerified() == false) {
                    containsEmail = true;
                } else {
                    containsEmail = false;
                    break;
                }
            } else if (email.getId().equals("spike@milligan.com")) {
                if (email.getCurrent() == true && email.getVerified() == true && email.getPrimary() == true) {
                    containsEmail = true;
                } else {
                    containsEmail = false;
                    break;
                }
            }
        }

        assertTrue(containsEmail);

        Set<EmailEntity> emails2 = primary.getEmails();
        assertNotNull(emails2);
        assertEquals(1, emails2.size());

        ProfileDeprecationRequest result = adminController.deprecateProfile("4444-4444-4444-4441", "4444-4444-4444-4442");

        assertEquals(0, result.getErrors().size());

        profileDao.refresh(toDeprecate);
        profileDao.refresh(primary);

        assertNotNull(toDeprecate.getPrimaryRecord());

        emails1 = toDeprecate.getEmails();
        assertNotNull(emails1);
        assertEquals(0, emails1.size());

        emails2 = primary.getEmails();
        assertNotNull(emails2);
        assertEquals(4, emails2.size());

        containsEmail = false;

        for (EmailEntity email : emails2) {
            if (email.getId().equals("1@deprecate.com")) {
                if (email.getCurrent() == false && email.getVerified() == true) {
                    containsEmail = true;
                } else {
                    containsEmail = false;
                    break;
                }
            } else if (email.getId().equals("2@deprecate.com")) {
                if (email.getCurrent() == false && email.getVerified() == false) {
                    containsEmail = true;
                } else {
                    containsEmail = false;
                    break;
                }
            } else if (email.getId().equals("spike@milligan.com")) {
                if (email.getCurrent() == true && email.getVerified() == true && email.getPrimary() == false) {
                    containsEmail = true;
                } else {
                    containsEmail = false;
                    break;
                }
            } else if (email.getId().equals("michael@bentine.com")) {
                if (email.getCurrent() == true && email.getVerified() == true && email.getPrimary() == true) {
                    containsEmail = true;
                } else {
                    containsEmail = false;
                    break;
                }
            }
        }

        assertTrue(containsEmail);
    }

    @Test
    @Transactional("transactionManager")
    @Rollback(true)
    public void tryToDeprecateDeprecatedProfile() throws Exception {
        ProfileDeprecationRequest result = adminController.deprecateProfile("4444-4444-4444-4441", "4444-4444-4444-4442");
        assertEquals(0, result.getErrors().size());

        profileDao.refresh(profileDao.find("4444-4444-4444-4441"));
        profileDao.refresh(profileDao.find("4444-4444-4444-4442"));

        // Test deprecating a deprecated account
        result = adminController.deprecateProfile("4444-4444-4444-4441", "4444-4444-4444-4443");
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.already_deprecated", "4444-4444-4444-4441"), result.getErrors().get(0));

        // Test deprecating account with himself
        result = adminController.deprecateProfile("4444-4444-4444-4441", "4444-4444-4444-4441");
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.deprecated_equals_primary"), result.getErrors().get(0));

        // Test set deprecated account as a primary account
        result = adminController.deprecateProfile("4444-4444-4444-4442", "4444-4444-4444-4441");
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.primary_account_deprecated", "4444-4444-4444-4441"), result.getErrors().get(0));

        // Test deprecating an invalid orcid
        result = adminController.deprecateProfile("4444-4444-4444-444", "4444-4444-4444-4442");
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.invalid_orcid", "4444-4444-4444-444"), result.getErrors().get(0));

        // Test use invalid orcid as primary
        result = adminController.deprecateProfile("4444-4444-4444-4441", "4444-4444-4444-444");
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.invalid_orcid", "4444-4444-4444-444"), result.getErrors().get(0));

        ProfileEntity deactiveProfile = profileDao.find("4444-4444-4444-4443");
        deactiveProfile.setDeactivationDate(new Date());
        profileDao.merge(deactiveProfile);
        profileDao.flush();
        profileDao.refresh(deactiveProfile);

        // Test set deactive primary account
        result = adminController.deprecateProfile("4444-4444-4444-4442", "4444-4444-4444-4443");
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.primary_account_is_deactivated", "4444-4444-4444-4443"), result.getErrors().get(0));
    }

    @Test
    @Transactional("transactionManager")
    @Rollback(true)
    public void deactivateAndReactivateProfileTest() throws Exception {
        // Test deactivate
        ProfileDetails result = adminController.confirmDeactivateOrcidAccount("4444-4444-4444-4441");
        assertEquals(0, result.getErrors().size());

        profileDao.refresh(profileDao.find("4444-4444-4444-4441"));
        ProfileEntity deactivated = profileDao.find("4444-4444-4444-4441");
        assertNotNull(deactivated.getDeactivationDate());
        assertEquals(deactivated.getFamilyName(), "Family Name Deactivated");
        assertEquals(deactivated.getGivenNames(), "Given Names Deactivated");

        // Test try to deactivate an already deactive account
        result = adminController.confirmDeactivateOrcidAccount("4444-4444-4444-4441");
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deactivation.errors.already_deactivated", new ArrayList<String>()), result.getErrors().get(0));

        // Test reactivate
        result = adminController.confirmReactivateOrcidAccount("4444-4444-4444-4441");
        assertEquals(0, result.getErrors().size());

        profileDao.refresh(profileDao.find("4444-4444-4444-4441"));
        deactivated = profileDao.find("4444-4444-4444-4441");
        assertNull(deactivated.getDeactivationDate());

        // Try to reactivate an already active account
        result = adminController.confirmReactivateOrcidAccount("4444-4444-4444-4441");
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_reactivation.errors.already_active", new ArrayList<String>()), result.getErrors().get(0));
    }

    @Test
    @Transactional("transactionManager")
    @Rollback(true)
    public void createGroupProfileWithInvalidEmailsTest() throws Exception {
        ProfileEntity profile = profileDao.find("4444-4444-4444-4441");
        assertNotNull(profile);
        assertNotNull(profile.getPrimaryEmail());
        String existingEmail = profile.getPrimaryEmail().getId();
        assertNotNull(existingEmail);
        Group group = new Group();
        group.setGroupName(Text.valueOf("Group Name"));
        group.setType(Text.valueOf("basic"));

        // Validate already existing email address
        group.setEmail(Text.valueOf(existingEmail));
        group = adminController.createGroup(group);
        assertEquals(1, group.getErrors().size());
        assertEquals(adminController.getMessage("group.email.already_used", new ArrayList<String>()), group.getErrors().get(0));

        // Validate empty email address
        group.setEmail(Text.valueOf(""));
        group = adminController.createGroup(group);
        assertEquals(1, group.getErrors().size());
        assertEquals(adminController.getMessage("NotBlank.group.email", new ArrayList<String>()), group.getErrors().get(0));

        // Validate invalid email address
        group.setEmail(Text.valueOf("invalidemail"));
        group = adminController.createGroup(group);
        assertEquals(1, group.getErrors().size());
        assertEquals(adminController.getMessage("group.email.invalid_email", new ArrayList<String>()), group.getErrors().get(0));
    }

    @Test
    @Transactional("transactionManager")
    @Rollback(true)
    public void createGroupProfileWithInvalidGroupNameTest() throws Exception {
        Group group = new Group();
        group.setEmail(Text.valueOf("group@email.com"));
        group.setType(Text.valueOf("basic"));

        // Validate empty group name
        group.setGroupName(Text.valueOf(""));
        group = adminController.createGroup(group);
        assertEquals(1, group.getErrors().size());
        assertEquals(adminController.getMessage("NotBlank.group.name", new ArrayList<String>()), group.getErrors().get(0));

        // validate too long group name - 151 chars
        group.setGroupName(Text
                .valueOf("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901"));
        group = adminController.createGroup(group);
        assertEquals(1, group.getErrors().size());
        assertEquals(adminController.getMessage("group.name.too_long", new ArrayList<String>()), group.getErrors().get(0));
    }

    @Test
    @Transactional("transactionManager")
    @Rollback(true)
    public void createGroupProfileWithInvalidTypeTest() throws Exception {
        Group group = new Group();
        group.setEmail(Text.valueOf("group@email.com"));
        group.setGroupName(Text.valueOf("Group Name"));

        // Validate empty type
        group.setType(Text.valueOf(""));
        group = adminController.createGroup(group);
        assertEquals(1, group.getErrors().size());
        assertEquals(adminController.getMessage("NotBlank.group.type", new ArrayList<String>()), group.getErrors().get(0));

        // Validate invalid type
        group.setType(Text.valueOf("invalid"));
        group = adminController.createGroup(group);
        assertEquals(1, group.getErrors().size());
        assertEquals(adminController.getMessage("group.type.invalid", new ArrayList<String>()), group.getErrors().get(0));
    }

    @Test
    @Transactional("transactionManager")
    @Rollback(true)
    public void createGroupProfileTest() throws Exception {
        Group group = new Group();
        group.setEmail(Text.valueOf("group@email.com"));
        group.setGroupName(Text.valueOf("Group Name"));
        group.setType(Text.valueOf("premium-institution"));
        group = adminController.createGroup(group);
        assertEquals(0, group.getErrors().size());
        assertFalse(PojoUtil.isEmpty(group.getGroupOrcid()));
    }
}