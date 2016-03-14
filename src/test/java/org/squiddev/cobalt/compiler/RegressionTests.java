package org.squiddev.cobalt.compiler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.lib.jse.JsePlatform;
import org.squiddev.cobalt.lib.platform.FileResourceManipulator;

import java.util.Arrays;
import java.util.Collection;

/**
 * Framework to add regression tests as problem areas are found.
 */
@RunWith(Parameterized.class)
public class RegressionTests {
	public String fileName;

	public RegressionTests(String file) {
		fileName = file;
	}

	@Before
	public void setup() throws Exception {
		JsePlatform.standardGlobals(new LuaState(new FileResourceManipulator()));
	}

	@Test
	public void compareBytecode() throws Exception {
		CompileTestHelper.compareResults("/regressions/", fileName);
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			{"modulo"},
			{"construct"},
			{"bigattr"},
			{"controlchars"},
			{"comparators"},
			{"mathrandomseed"},
			{"varargs"},
		};

		return Arrays.asList(tests);
	}
}