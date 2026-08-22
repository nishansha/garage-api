package com.triasoft.garage;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Enforces module boundaries for the explicitly-annotated application modules
 * (ledger, company, servicesale, hrm). Sibling packages that predate the
 * modular refactor (controller, service, entity, ...) are not annotated and
 * are therefore not part of any declared module — they are unaffected by
 * this check and may keep calling into module internals freely. Only the
 * declared modules are held to "depend on another module's API only".
 */
class ApplicationModulesVerificationTest {

	@Test
	void verifyModuleStructure() {
		ApplicationModules.of(GarageApplication.class).verify();
	}
}
