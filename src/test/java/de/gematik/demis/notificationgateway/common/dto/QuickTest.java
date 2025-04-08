package de.gematik.demis.notificationgateway.common.dto;

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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.Setter;

/** QuickTest, just faked for testing */
@Setter
public class QuickTest {

  private NotifiedPerson notifiedPerson;

  private NotifierFacility notifierFacility;

  private String pathogen;

  private Diagnosis diagnostic;

  public QuickTest notifiedPerson(NotifiedPerson notifiedPerson) {
    this.notifiedPerson = notifiedPerson;
    return this;
  }

  /**
   * Get notifiedPerson
   *
   * @return notifiedPerson
   */
  @NotNull
  @Valid
  @Schema(name = "notifiedPerson", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("notifiedPerson")
  public NotifiedPerson getNotifiedPerson() {
    return notifiedPerson;
  }

  public QuickTest notifierFacility(NotifierFacility notifierFacility) {
    this.notifierFacility = notifierFacility;
    return this;
  }

  /**
   * Get notifierFacility
   *
   * @return notifierFacility
   */
  @NotNull
  @Valid
  @Schema(name = "notifierFacility", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("notifierFacility")
  public NotifierFacility getNotifierFacility() {
    return notifierFacility;
  }

  public QuickTest pathogen(String pathogen) {
    this.pathogen = pathogen;
    return this;
  }

  /**
   * Krankheitserreger
   *
   * @return pathogen
   */
  @NotNull
  @Schema(
      name = "pathogen",
      description = "Krankheitserreger",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("pathogen")
  public String getPathogen() {
    return pathogen;
  }

  public QuickTest diagnostic(Diagnosis diagnostic) {
    this.diagnostic = diagnostic;
    return this;
  }

  /**
   * Get diagnostic
   *
   * @return diagnostic
   */
  @NotNull
  @Valid
  @Schema(name = "diagnostic", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("diagnostic")
  public Diagnosis getDiagnostic() {
    return diagnostic;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuickTest quickTest = (QuickTest) o;
    return Objects.equals(this.notifiedPerson, quickTest.notifiedPerson)
        && Objects.equals(this.notifierFacility, quickTest.notifierFacility)
        && Objects.equals(this.pathogen, quickTest.pathogen)
        && Objects.equals(this.diagnostic, quickTest.diagnostic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(notifiedPerson, notifierFacility, pathogen, diagnostic);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuickTest {\n");
    sb.append("    notifiedPerson: ").append(toIndentedString(notifiedPerson)).append("\n");
    sb.append("    notifierFacility: ").append(toIndentedString(notifierFacility)).append("\n");
    sb.append("    pathogen: ").append(toIndentedString(pathogen)).append("\n");
    sb.append("    diagnostic: ").append(toIndentedString(diagnostic)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
