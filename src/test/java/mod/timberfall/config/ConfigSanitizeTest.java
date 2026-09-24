package mod.timberfall.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConfigSanitizeTest {

	@Test
	void clampsNegativeAndZeroValuesUp() {
		Config config = new Config();
		config.minLogsToChop = 0;
		config.maxLogsPerTree = -50;
		config.heightLimit = 0;
		config.radiusLimit = -1;
		config.blocksPerTick = 0;
		config.speedLimitConnectedLogs = 0;
		config.leafDecayPerTick = -3;

		config.sanitize();

		assertEquals(1, config.minLogsToChop);
		assertEquals(1, config.maxLogsPerTree);
		assertEquals(1, config.heightLimit);
		assertEquals(1, config.radiusLimit);
		assertEquals(1, config.blocksPerTick);
		assertEquals(1, config.speedLimitConnectedLogs);
		assertEquals(1, config.leafDecayPerTick);
	}

	@Test
	void clampsBreakSpeedFactorToUnitInterval() {
		Config config = new Config();
		config.breakSpeedFactor = 7.0f;
		config.sanitize();
		assertEquals(1.0f, config.breakSpeedFactor);

		config.breakSpeedFactor = -2.0f;
		config.sanitize();
		assertEquals(0.0f, config.breakSpeedFactor);
	}

	@Test
	void saneDefaultsSurviveSanitize() {
		Config config = new Config();
		config.sanitize();

		assertTrue(config.minLogsToChop >= 1);
		assertTrue(config.blocksPerTick >= 1);
		assertTrue(config.breakSpeedFactor >= 0.0f && config.breakSpeedFactor <= 1.0f);
	}
}