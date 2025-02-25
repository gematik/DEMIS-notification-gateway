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
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Diagnosis, just faked for testing */
@Setter
public class Diagnosis {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime receivedDate;

  private String comment;

  /**
   * Test type of the Sars-Cov-2: Antigenschnelltest PCR-Schnelltest Nukleinsäurenachweis, z.B. PCR
   * Variantenspezifische PCR Sequenzierung
   */
  public enum TestTypeEnum {
    ANTIGEN_RAPID_TEST("ANTIGEN_RAPID_TEST"),

    PCR_RAPID_TEST("PCR_RAPID_TEST"),

    NUCLEIC_ACID_CERTIFICATE("NUCLEIC_ACID_CERTIFICATE"),

    VARIANT_SPECIFIC_PCR("VARIANT_SPECIFIC_PCR"),

    SEQUENCING("SEQUENCING");

    private String value;

    TestTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TestTypeEnum fromValue(String value) {
      for (TestTypeEnum b : TestTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private TestTypeEnum testType;

  /** VOC: Variant of Concern. */
  public enum VocEnum {
    ALPHA("ALPHA"),

    BETA("BETA"),

    GAMMA("GAMMA"),

    DELTA("DELTA"),

    OMIKRON("OMIKRON");

    private String value;

    VocEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static VocEnum fromValue(String value) {
      for (VocEnum b : VocEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private VocEnum voc;

  private String oneTimeCode;

  public Diagnosis() {
    super();
  }

  public Diagnosis receivedDate(OffsetDateTime receivedDate) {
    this.receivedDate = receivedDate;
    return this;
  }

  /**
   * Get receivedDate
   *
   * @return receivedDate
   */
  @NotNull
  @Valid
  @Schema(name = "receivedDate", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("receivedDate")
  public OffsetDateTime getReceivedDate() {
    return receivedDate;
  }

  public Diagnosis comment(String comment) {
    this.comment = comment;
    return this;
  }

  /**
   * Get comment
   *
   * @return comment
   */
  @Schema(name = "comment", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("comment")
  public String getComment() {
    return comment;
  }

  public Diagnosis testType(TestTypeEnum testType) {
    this.testType = testType;
    return this;
  }

  /**
   * Test type of the Sars-Cov-2: Antigenschnelltest PCR-Schnelltest Nukleinsäurenachweis, z.B. PCR
   * Variantenspezifische PCR Sequenzierung
   *
   * @return testType
   */
  @Schema(
      name = "testType",
      accessMode = Schema.AccessMode.READ_ONLY,
      description =
          "Test type of the Sars-Cov-2:    Antigenschnelltest  PCR-Schnelltest  Nukleinsäurenachweis, z.B. PCR  Variantenspezifische PCR  Sequenzierung",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("testType")
  public TestTypeEnum getTestType() {
    return testType;
  }

  public Diagnosis voc(VocEnum voc) {
    this.voc = voc;
    return this;
  }

  /**
   * VOC: Variant of Concern.
   *
   * @return voc
   */
  @Schema(
      name = "voc",
      accessMode = Schema.AccessMode.READ_ONLY,
      description = "VOC: Variant of Concern.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("voc")
  public VocEnum getVoc() {
    return voc;
  }

  public Diagnosis oneTimeCode(String oneTimeCode) {
    this.oneTimeCode = oneTimeCode;
    return this;
  }

  /**
   * Feld wird nur als Honeypot verwendet
   *
   * @return oneTimeCode
   */
  @Schema(
      name = "oneTimeCode",
      description = "Feld wird nur als Honeypot verwendet",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("oneTimeCode")
  public String getOneTimeCode() {
    return oneTimeCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Diagnosis diagnosis = (Diagnosis) o;
    return Objects.equals(this.receivedDate, diagnosis.receivedDate)
        && Objects.equals(this.comment, diagnosis.comment)
        && Objects.equals(this.testType, diagnosis.testType)
        && Objects.equals(this.voc, diagnosis.voc)
        && Objects.equals(this.oneTimeCode, diagnosis.oneTimeCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(receivedDate, comment, testType, voc, oneTimeCode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Diagnosis {\n");
    sb.append("    receivedDate: ").append(toIndentedString(receivedDate)).append("\n");
    sb.append("    comment: ").append(toIndentedString(comment)).append("\n");
    sb.append("    testType: ").append(toIndentedString(testType)).append("\n");
    sb.append("    voc: ").append(toIndentedString(voc)).append("\n");
    sb.append("    oneTimeCode: ").append(toIndentedString(oneTimeCode)).append("\n");
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
