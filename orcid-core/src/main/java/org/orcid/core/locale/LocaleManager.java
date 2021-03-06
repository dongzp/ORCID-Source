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
package org.orcid.core.locale;

import java.util.Locale;

public interface LocaleManager {

    /**
     * @return The currently active locale
     */
    Locale getLocale();

    /**
     * @param messageCode
     *            The code of the message in the messages properties file
     * @param messageParams
     *            Values to use in {} placeholders in the message
     * @return The localized message (using the locale for the current thread)
     */
    String resolveMessage(String messageCode, Object... messageParams);

}
