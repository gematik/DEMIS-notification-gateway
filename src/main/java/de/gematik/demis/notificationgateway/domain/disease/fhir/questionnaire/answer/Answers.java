package de.gematik.demis.notificationgateway.domain.disease.fhir.questionnaire.answer;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.notificationgateway.common.dto.QuestionnaireResponseAnswer;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TimeType;
import org.hl7.fhir.r4.model.UriType;
import org.springframework.stereotype.Service;

/** Easy handling of all the data types of questionnaire response item answer values. */
@RequiredArgsConstructor
@Service
@Slf4j
public final class Answers {

  private final QuantityDataType quantities = new QuantityDataType();
  private final BooleanDataType booleans = new BooleanDataType();
  private final DecimalDataType decimals = new DecimalDataType();
  private final IntegerDataType integers = new IntegerDataType();
  private final DateDataType dates = new DateDataType();
  private final DateTimeDataType dateTimes = new DateTimeDataType();
  private final TimeDataType times = new TimeDataType();
  private final UriDataType uris = new UriDataType();
  private final CodingDataType codings = new CodingDataType();
  private final ReferenceDataType references = new ReferenceDataType();
  private final StringDataType strings = new StringDataType();

  private List<DataType<?>> dataTypes;

  @PostConstruct
  void createDataTypesList() {
    /*
     * The order of the data types is important, because the first matching data type is used.
     * The order contains statistics of the data types in the questionnaire response items.
     * The order also covers tricky cases like date and date time. The date processor must be placed before the date time processor.
     */
    this.dataTypes =
        List.of(
            this.strings,
            this.dates,
            this.codings,
            this.references,
            this.dateTimes,
            this.uris,
            this.booleans,
            this.quantities,
            this.decimals,
            this.integers,
            this.times);
  }

  /**
   * Create a FHIR answer from a source answer.
   *
   * @param answer source answer
   * @return FHIR answer
   */
  public FhirAnswer createFhirAnswer(QuestionnaireResponseAnswer answer) {
    return new FhirAnswerImpl(answer);
  }

  /**
   * Check if answer contains a primitiv value
   *
   * @param answer answer
   * @return <code>true</code> if the answer contains a value, <code>false</code> if not
   */
  public boolean containsValue(QuestionnaireResponseAnswer answer) {
    return isPrimitiveSet(answer)
        || isDateTimeSet(answer)
        || Objects.nonNull(answer.getValueCoding())
        || Objects.nonNull(answer.getValueQuantity())
        || StringUtils.isNotBlank(answer.getValueReference())
        || StringUtils.isNotBlank(answer.getValueUri());
  }

  private boolean isDateTimeSet(QuestionnaireResponseAnswer answer) {
    return StringUtils.isNotBlank(answer.getValueDate())
        || StringUtils.isNotBlank(answer.getValueDateTime())
        || StringUtils.isNotBlank(answer.getValueTime());
  }

  private boolean isPrimitiveSet(QuestionnaireResponseAnswer answer) {
    return Objects.nonNull(answer.getValueBoolean())
        || Objects.nonNull(answer.getValueDecimal())
        || Objects.nonNull(answer.getValueInteger())
        || StringUtils.isNotBlank(answer.getValueString());
  }

  @RequiredArgsConstructor
  final class FhirAnswerImpl implements FhirAnswer, FhirAnswer.Value {

    private final QuestionnaireResponseAnswer answer;

    @Override
    public Quantity toQuantity() {
      return quantities.toFhir(this.answer);
    }

    @Override
    public BooleanType toBooleanType() {
      return booleans.toFhir(this.answer);
    }

    @Override
    public Coding toCoding() {
      return codings.toFhir(this.answer);
    }

    @Override
    public DateTimeType toDateTimeType() {
      return dateTimes.toFhir(this.answer);
    }

    @Override
    public DateType toDateType() {
      return dates.toFhir(this.answer);
    }

    @Override
    public DecimalType toDecimalType() {
      return decimals.toFhir(this.answer);
    }

    @Override
    public IntegerType toIntegerType() {
      return integers.toFhir(this.answer);
    }

    @Override
    public Reference toReference() {
      return references.toFhir(this.answer);
    }

    @Override
    public StringType toStringType() {
      return strings.toFhir(this.answer);
    }

    @Override
    public TimeType toTimeType() {
      return times.toFhir(this.answer);
    }

    @Override
    public UriType toUriType() {
      return uris.toFhir(this.answer);
    }

    @Override
    public QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent object() {
      return parse(this.answer);
    }

    @Override
    public Value value() {
      return this;
    }

    /**
     * Create a FHIR answer from a source answer.
     *
     * @param answer source answer
     * @return FHIR answer
     */
    private QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent parse(
        QuestionnaireResponseAnswer answer) {
      final var target = new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent();
      Answers.this.dataTypes.stream()
          .filter(dt -> dt.test(answer))
          .map(dt -> dt.toFhir(answer))
          .findFirst()
          .ifPresent(target::setValue);
      if ((target.getValue() == null) && containsValue(answer)) {
        throw new IllegalArgumentException(
            "Unsupported data type at questionnaire response item answer: "
                + printExceptionDataInfo(answer));
      }
      return target;
    }

    private String printExceptionDataInfo(QuestionnaireResponseAnswer source) {
      if (source == null) {
        return "null";
      }
      try {
        return new ObjectMapper().writeValueAsString(source);
      } catch (Exception e) {
        log.info("Failed to encode JSON object: {}", source, e);
        return source.toString();
      }
    }
  }
}
