/*
 * Copyright [2023], gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package de.gematik.demis.notificationgateway.common.services.fhir;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 gematik GmbH
 * %%
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 * #L%
 */

import de.gematik.demis.notification.builder.demis.fhir.notification.utils.Utils;
import de.gematik.demis.notificationgateway.common.constants.FhirConstants;
import de.gematik.demis.notificationgateway.common.utils.ReferenceUtils;
import java.util.Date;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.lang.Nullable;

public abstract class BundleCreationService {

  protected Bundle createBundle(String profile) {
    Bundle bundle = new Bundle();

    final Date timestamp = new Date();
    final Meta meta = new Meta().addProfile(profile).setLastUpdated(timestamp);
    bundle.setMeta(meta);

    addIdentifier(bundle);

    bundle.setType(BundleType.DOCUMENT);
    bundle.setTimestamp(timestamp);

    return bundle;
  }

  protected void addEntry(Bundle bundle, @Nullable Resource resource) {
    if (resource == null) {
      return;
    }
    bundle.addEntry().setResource(resource).setFullUrl(ReferenceUtils.getFullUrl(resource));
  }

  private void addIdentifier(Bundle bundle) {
    Identifier identifier =
        new Identifier()
            .setSystem(FhirConstants.NAMING_SYSTEM_NOTIFICATION_BUNDLE_ID)
            .setValue(Utils.generateUuidString());
    bundle.setIdentifier(identifier);
  }
}
