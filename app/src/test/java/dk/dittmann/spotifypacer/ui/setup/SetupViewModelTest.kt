package dk.dittmann.spotifypacer.ui.setup

import dk.dittmann.spotifypacer.pacing.PaceStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupViewModelTest {

    @Test
    fun initial_state_is_idle_and_cannot_submit() {
        val vm = SetupViewModel()
        val state = vm.state.value

        assertEquals("", state.distanceText)
        assertEquals("", state.targetTimeText)
        assertEquals(StrategyChoice.LinearRamp, state.strategy)
        assertNull(state.distanceError)
        assertNull(state.targetTimeError)
        assertNull(state.paceWarning)
        assertTrue(!state.canSubmit)
    }

    @Test
    fun typing_valid_distance_clears_errors() {
        val vm = SetupViewModel()
        vm.onDistanceChanged("5.0")

        assertEquals("5.0", vm.state.value.distanceText)
        assertNull(vm.state.value.distanceError)
    }

    @Test
    fun comma_decimal_separator_is_accepted() {
        val vm = SetupViewModel()
        vm.onDistanceChanged("5,0")

        assertNull(vm.state.value.distanceError)
    }

    @Test
    fun non_numeric_distance_surfaces_error() {
        val vm = SetupViewModel()
        vm.onDistanceChanged("five")

        assertEquals(DistanceError.NotANumber, vm.state.value.distanceError)
    }

    @Test
    fun zero_or_negative_distance_surfaces_error() {
        val vm = SetupViewModel()

        vm.onDistanceChanged("0")
        assertEquals(DistanceError.NotPositive, vm.state.value.distanceError)

        vm.onDistanceChanged("-1")
        assertEquals(DistanceError.NotPositive, vm.state.value.distanceError)
    }

    @Test
    fun mm_ss_target_time_parses() {
        val vm = SetupViewModel()
        vm.onTargetTimeChanged("28:00")

        assertNull(vm.state.value.targetTimeError)
    }

    @Test
    fun hh_mm_ss_target_time_parses() {
        val vm = SetupViewModel()
        vm.onTargetTimeChanged("1:05:00")

        assertNull(vm.state.value.targetTimeError)
    }

    @Test
    fun malformed_target_time_surfaces_error() {
        val vm = SetupViewModel()
        vm.onTargetTimeChanged("abc")

        assertEquals(TargetTimeError.Malformed, vm.state.value.targetTimeError)
    }

    @Test
    fun seconds_segment_over_60_is_malformed() {
        val vm = SetupViewModel()
        vm.onTargetTimeChanged("28:75")

        assertEquals(TargetTimeError.Malformed, vm.state.value.targetTimeError)
    }

    @Test
    fun zero_target_time_surfaces_not_positive() {
        val vm = SetupViewModel()
        vm.onTargetTimeChanged("0:00")

        assertEquals(TargetTimeError.NotPositive, vm.state.value.targetTimeError)
    }

    @Test
    fun pace_warning_appears_when_pace_under_3_min_per_km() {
        val vm = SetupViewModel()
        vm.onDistanceChanged("5")
        vm.onTargetTimeChanged("10:00")

        assertEquals(PaceWarning.TooFast, vm.state.value.paceWarning)
    }

    @Test
    fun pace_warning_appears_when_pace_over_10_min_per_km() {
        val vm = SetupViewModel()
        vm.onDistanceChanged("5")
        vm.onTargetTimeChanged("1:00:00")

        assertEquals(PaceWarning.TooSlow, vm.state.value.paceWarning)
    }

    @Test
    fun no_pace_warning_inside_band() {
        val vm = SetupViewModel()
        vm.onDistanceChanged("5")
        vm.onTargetTimeChanged("28:00")

        assertNull(vm.state.value.paceWarning)
    }

    @Test
    fun pace_warning_clears_when_either_input_invalidated() {
        val vm = SetupViewModel()
        vm.onDistanceChanged("5")
        vm.onTargetTimeChanged("10:00")
        assertEquals(PaceWarning.TooFast, vm.state.value.paceWarning)

        vm.onDistanceChanged("")
        assertNull(vm.state.value.paceWarning)
    }

    @Test
    fun submit_disabled_until_both_inputs_valid() {
        val vm = SetupViewModel()
        assertTrue(!vm.state.value.canSubmit)

        vm.onDistanceChanged("5")
        assertTrue(!vm.state.value.canSubmit)

        vm.onTargetTimeChanged("28:00")
        assertTrue(vm.state.value.canSubmit)

        vm.onTargetTimeChanged("bad")
        assertTrue(!vm.state.value.canSubmit)
    }

    @Test
    fun toRunConfig_returns_parsed_config_when_valid() {
        val vm = SetupViewModel()
        vm.onDistanceChanged("5.0")
        vm.onTargetTimeChanged("28:00")
        vm.onStrategyChanged(StrategyChoice.DelayedExponential)

        val config = vm.toRunConfig()

        assertEquals(RunConfig(5.0, 28 * 60, PaceStrategy.DelayedExponential()), config)
    }

    @Test
    fun toRunConfig_returns_null_when_inputs_invalid() {
        val vm = SetupViewModel()
        vm.onDistanceChanged("5.0")

        assertNull(vm.toRunConfig())
    }
}
