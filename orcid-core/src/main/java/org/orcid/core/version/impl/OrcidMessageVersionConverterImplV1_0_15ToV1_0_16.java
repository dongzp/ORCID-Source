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
package org.orcid.core.version.impl;

import javax.annotation.Resource;

import org.orcid.core.manager.impl.OrcidUrlManager;
import org.orcid.core.version.OrcidMessageVersionConverter;
import org.orcid.jaxb.model.message.Orcid;
import org.orcid.jaxb.model.message.OrcidMessage;
import org.orcid.jaxb.model.message.OrcidProfile;
import org.orcid.jaxb.model.message.OrcidSearchResult;
import org.orcid.jaxb.model.message.OrcidSearchResults;

/**
 * 
 * @author Will Simpson
 * 
 */
public class OrcidMessageVersionConverterImplV1_0_15ToV1_0_16 implements OrcidMessageVersionConverter {

    @Resource
    private OrcidUrlManager orcidUrlManager;

    private static final String FROM_VERSION = "1.0.15";
    private static final String TARGET_VERSION = "1.0.16";

    @Override
    public String getFromVersion() {
        return FROM_VERSION;
    }

    @Override
    public String getToVersion() {
        return TARGET_VERSION;
    }

    @Override
    public OrcidMessage downgradeMessage(OrcidMessage orcidMessage) {
        if (orcidMessage == null) {
            return null;
        }
        orcidMessage.setMessageVersion(FROM_VERSION);
        downgradeProfile(orcidMessage.getOrcidProfile());
        downgradeSearchResults(orcidMessage.getOrcidSearchResults());
        return orcidMessage;
    }

    private void downgradeSearchResults(OrcidSearchResults orcidSearchResults) {
        if (orcidSearchResults != null) {
            for (OrcidSearchResult searchResult : orcidSearchResults.getOrcidSearchResult()) {
                downgradeProfile(searchResult.getOrcidProfile());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void downgradeProfile(OrcidProfile orcidProfile) {
        if (orcidProfile != null) {
            String orcid = orcidProfile.extractOrcidNumber();
            orcidProfile.setOrcid(orcid);
        }
    }

    @Override
    public OrcidMessage upgradeMessage(OrcidMessage orcidMessage) {
        if (orcidMessage == null) {
            return null;
        }
        orcidMessage.setMessageVersion(TARGET_VERSION);
        upgradeProfile(orcidMessage.getOrcidProfile());
        upgradeSearchResults(orcidMessage.getOrcidSearchResults());
        return orcidMessage;
    }

    private void upgradeSearchResults(OrcidSearchResults orcidSearchResults) {
        if (orcidSearchResults != null) {
            for (OrcidSearchResult searchResult : orcidSearchResults.getOrcidSearchResult()) {
                upgradeProfile(searchResult.getOrcidProfile());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void upgradeProfile(OrcidProfile orcidProfile) {
        if (orcidProfile != null) {
            Orcid orcid = orcidProfile.getOrcid();
            String orcidId = orcidProfile.getOrcidId();
            if (orcidId == null && !(orcid == null)) {
                orcidProfile.setOrcidId(orcidUrlManager.orcidNumberToOrcidId(orcid.getValue()));
            }
            orcidProfile.setOrcid((Orcid) null);
        }
    }

}
