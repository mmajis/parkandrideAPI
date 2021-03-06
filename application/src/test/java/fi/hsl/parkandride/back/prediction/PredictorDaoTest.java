// Copyright © 2015 HSL <https://www.hsl.fi>
// This program is dual-licensed under the EUPL v1.2 and AGPLv3 licenses.

package fi.hsl.parkandride.back.prediction;

import fi.hsl.parkandride.back.AbstractDaoTest;
import fi.hsl.parkandride.back.Dummies;
import fi.hsl.parkandride.core.back.PredictorRepository;
import fi.hsl.parkandride.core.domain.CapacityType;
import fi.hsl.parkandride.core.domain.Usage;
import fi.hsl.parkandride.core.domain.UtilizationKey;
import fi.hsl.parkandride.core.domain.prediction.PredictorState;
import fi.hsl.parkandride.core.service.ValidationException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

public class PredictorDaoTest extends AbstractDaoTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Inject Dummies dummies;
    @Inject PredictorRepository predictorRepository;

    private long facilityId;
    private UtilizationKey utilizationKey;

    @Before
    public void initTestData() {
        facilityId = dummies.createFacility();
        utilizationKey = new UtilizationKey(facilityId, CapacityType.CAR, Usage.PARK_AND_RIDE);
    }


    // basics

    @Test
    public void creates_predictor_when_enabled_for_the_first_time() {
        assertThat(predictorRepository.findAllPredictors()).as("all predictors, before").isEmpty();

        PredictorState state = enablePredictor("the-type", utilizationKey);

        assertThat(predictorRepository.findAllPredictors()).as("all predictors, after").containsOnly(state);
        // identifying fields:
        assertThat(state.predictorId).as("predictorId").isNotNull();
        assertThat(state.predictorType).as("predictorType").isEqualTo("the-type");
        assertThat(state.utilizationKey.facilityId).as("facilityId").isEqualTo(utilizationKey.facilityId);
        assertThat(state.utilizationKey.capacityType).as("capacityType").isEqualTo(utilizationKey.capacityType);
        assertThat(state.utilizationKey.usage).as("usage").isEqualTo(utilizationKey.usage);
        // default values:
        assertThat(state.latestUtilization).as("latestUtilization").isEqualTo(new DateTime(0)); // safe default: before all possible utilizations
        assertThat(state.moreUtilizations).as("moreUtilizations").isTrue(); // safe default: assume there are some utilizations to process
        assertThat(state.internalState).as("internalState").isEqualTo("");
        // keep defaults in sync with Java code:
        PredictorState javaDefaults = new PredictorState(null, null, null);
        assertThat(javaDefaults.latestUtilization).as("javaDefaults.latestUtilization").isEqualTo(state.latestUtilization);
        assertThat(javaDefaults.moreUtilizations).as("javaDefaults.moreUtilizations").isEqualTo(state.moreUtilizations);
        assertThat(javaDefaults.internalState).as("javaDefaults.internalState").isEqualTo(state.internalState);
    }

    @Test
    public void returns_existing_predictor_when_enabled_for_the_second_time() {
        PredictorState state1 = enablePredictor("predictor-type", utilizationKey);
        PredictorState state2 = enablePredictor("predictor-type", utilizationKey);

        assertThat(state2.predictorId).as("state2.predictorId").isEqualTo(state1.predictorId);
    }

    @Test
    public void predictor_state_can_be_modified() {
        PredictorState expected = enablePredictor("type", utilizationKey);

        expected.latestUtilization = new DateTime();
        expected.moreUtilizations = !expected.moreUtilizations;
        expected.internalState += "updated";
        predictorRepository.save(expected);

        PredictorState actual = predictorRepository.getById(expected.predictorId);
        assertThat(actual).isNotNull();
        assertThat(actual.latestUtilization).as("latestUtilization").isEqualTo(expected.latestUtilization);
        assertThat(actual.moreUtilizations).as("moreUtilizations").isEqualTo(expected.moreUtilizations);
        assertThat(actual.internalState).as("internalState").isEqualTo(expected.internalState);
    }

    @Test
    public void predictor_state_is_validated_before_saving() {
        PredictorState state = enablePredictor("type", utilizationKey);
        state.latestUtilization = null;
        state.internalState = null;

        thrown.expect(ValidationException.class);
        thrown.expectMessage("latestUtilization (NotNull)");
        thrown.expectMessage("internalState (NotNull)");
        predictorRepository.save(state);
    }


    // finding predictors with new utilizations

    /**
     * This can be useful if the database contains utilizations from before the predictor was created,
     * for example when starting to use a new type of predictor, or if predictors are not enabled automatically.
     */
    @Test
    public void finds_recently_enabled_predictors_even_if_they_have_no_utilizations() {
        PredictorState state = enablePredictor("type", utilizationKey);

        assertThat(predictorRepository.findPredictorsNeedingUpdate()).containsExactly(state.predictorId);
    }

    @Test
    public void can_mark_predictors_to_have_processed_all_utilizations() {
        PredictorState state = enablePredictor("type", utilizationKey);
        state.moreUtilizations = false;
        predictorRepository.save(state);

        assertThat(predictorRepository.findPredictorsNeedingUpdate()).isEmpty();
    }

    @Test
    public void can_mark_predictors_to_have_received_new_utilizations_to_be_processed() {
        PredictorState state = enablePredictor("type", utilizationKey);
        state.moreUtilizations = false;
        predictorRepository.save(state);

        predictorRepository.markPredictorsNeedAnUpdate(utilizationKey);

        state.moreUtilizations = true;
        assertThat(predictorRepository.findPredictorsNeedingUpdate()).containsExactly(state.predictorId);
    }


    // uniqueness

    @Test
    public void predictors_are_predictor_type_specific() {
        PredictorState state1 = enablePredictor("type1", utilizationKey);
        PredictorState state2 = enablePredictor("type2", utilizationKey);

        assertThat(state2.predictorId).as("state2.predictorId").isNotEqualTo(state1.predictorId);
    }

    @Test
    public void predictors_are_facility_specific() {
        long facilityId2 = dummies.createFacility();
        PredictorState state1 = enablePredictor("predictor-type", new UtilizationKey(facilityId, CapacityType.CAR, Usage.PARK_AND_RIDE));
        PredictorState state2 = enablePredictor("predictor-type", new UtilizationKey(facilityId2, CapacityType.CAR, Usage.PARK_AND_RIDE));

        assertThat(state2.predictorId).as("state2.predictorId").isNotEqualTo(state1.predictorId);
    }

    @Test
    public void predictors_are_capacity_type_specific() {
        PredictorState state1 = enablePredictor("predictor-type", new UtilizationKey(facilityId, CapacityType.CAR, Usage.PARK_AND_RIDE));
        PredictorState state2 = enablePredictor("predictor-type", new UtilizationKey(facilityId, CapacityType.ELECTRIC_CAR, Usage.PARK_AND_RIDE));

        assertThat(state2.predictorId).as("state2.predictorId").isNotEqualTo(state1.predictorId);
    }

    @Test
    public void predictors_are_usage_specific() {
        PredictorState state1 = enablePredictor("predictor-type", new UtilizationKey(facilityId, CapacityType.CAR, Usage.PARK_AND_RIDE));
        PredictorState state2 = enablePredictor("predictor-type", new UtilizationKey(facilityId, CapacityType.CAR, Usage.COMMERCIAL));

        assertThat(state2.predictorId).as("state2.predictorId").isNotEqualTo(state1.predictorId);
    }

    @Test
    public void marking_predictors_is_facility_specific() {
        UtilizationKey key1 = new UtilizationKey(facilityId, CapacityType.CAR, Usage.PARK_AND_RIDE);
        UtilizationKey key2 = new UtilizationKey(dummies.createFacility(), CapacityType.CAR, Usage.PARK_AND_RIDE);
        createPredictorNotNeedingUpdate(key1);
        createPredictorNotNeedingUpdate(key2);

        predictorRepository.markPredictorsNeedAnUpdate(key1);

        assertThat(predictorRepository.findPredictorsNeedingUpdate()).hasSize(1);
    }

    @Test
    public void marking_predictors_is_capacity_type_specific() {
        UtilizationKey key1 = new UtilizationKey(facilityId, CapacityType.CAR, Usage.PARK_AND_RIDE);
        UtilizationKey key2 = new UtilizationKey(facilityId, CapacityType.ELECTRIC_CAR, Usage.PARK_AND_RIDE);
        createPredictorNotNeedingUpdate(key1);
        createPredictorNotNeedingUpdate(key2);

        predictorRepository.markPredictorsNeedAnUpdate(key1);

        assertThat(predictorRepository.findPredictorsNeedingUpdate()).hasSize(1);
    }

    @Test
    public void marking_predictors_is_usage_specific() {
        UtilizationKey key1 = new UtilizationKey(facilityId, CapacityType.CAR, Usage.PARK_AND_RIDE);
        UtilizationKey key2 = new UtilizationKey(facilityId, CapacityType.CAR, Usage.COMMERCIAL);
        createPredictorNotNeedingUpdate(key1);
        createPredictorNotNeedingUpdate(key2);

        predictorRepository.markPredictorsNeedAnUpdate(key1);

        assertThat(predictorRepository.findPredictorsNeedingUpdate()).hasSize(1);
    }

    private void createPredictorNotNeedingUpdate(UtilizationKey utilizationKey) {
        PredictorState state = enablePredictor("predictor-type", utilizationKey);
        state.moreUtilizations = false;
        predictorRepository.save(state);
    }

    private PredictorState enablePredictor(String predictorType, UtilizationKey utilizationKey) {
        Long predictorId = predictorRepository.enablePredictor(predictorType, utilizationKey);
        return predictorRepository.getById(predictorId);
    }
}
