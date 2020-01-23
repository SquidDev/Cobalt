/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.squiddev.cobalt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.function.OneArgFunction;
import org.squiddev.cobalt.function.ZeroArgFunction;
import org.squiddev.cobalt.lib.Bit32Lib;
import org.squiddev.cobalt.lib.Utf8Lib;

import java.io.File;
import java.io.IOException;

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Just runs various libraries in the main test suite
 */
public class AssertTests {
	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {"table-hash-01", "table-hash-02", "table-hash-03"})
	public void tables(String name) throws IOException, CompileException, LuaError, InterruptedException {
		ScriptHelper helpers = new ScriptHelper("/assert/table/");
		helpers.setup();
		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {
		"base-issues", "string-issues", "debug", "debug-coroutine-hook", "gc", "immutable", "invalid-tailcall",
		"load-error", "no-unwind", "time",
	})
	public void main(String name) throws IOException, CompileException, LuaError, InterruptedException {
		ScriptHelper helpers = new ScriptHelper("/assert/");
		helpers.setup();
		helpers.runWithDump(name);
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {
		// Skip all, api, big and main
		"attrib",
		"calls",
		"checktable",
		"closure",
		"code",
		"constructs",
		"db",
		"errors",
		"events",
		"files",
		"gc",
		"literals",
		"locals",
		"math",
		"nextvar",
		"pm",
		"sort",
		"strings",
		"vararg",
		"verybig",
	})
	public void lua51(String name) throws Exception {
		ScriptHelper helpers = new ScriptHelper("/assert/lua5.1/");
		helpers.setup();
		helpers.globals.rawset("mkdir", new OneArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
				return valueOf(new File(arg.checkString()).mkdirs());
			}
		});

		// TODO: Move this into the debug library
		((LuaTable) helpers.globals.rawget("debug")).rawset("debug", new ZeroArgFunction() {
			@Override
			public LuaValue call(LuaState state) {
				return Constants.NONE;
			}
		});

		helpers.runWithDump(name);
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {
		"bitwise",
	})
	public void lua52(String name) throws Exception {
		ScriptHelper helpers = new ScriptHelper("/assert/lua5.2/");
		helpers.setup();
		helpers.globals.load(helpers.state, new Bit32Lib());

		helpers.runWithDump(name);
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {
		"utf8",
	})
	public void lua53(String name) throws Exception {
		ScriptHelper helpers = new ScriptHelper("/assert/lua5.3/");
		helpers.setup();
		helpers.globals.load(helpers.state, new Utf8Lib());

		helpers.runWithDump(name);
	}

	@Test
	public void foo() {
		Buffer sb = new Buffer(1);
		byte[] buffer = new byte[8];
		// 27721,23383,47,28450,23383
		int codepoint = 23383;
		if (codepoint < 0x80) {
			sb.append((byte) codepoint);
		} else {
			int mfb = 0x3f;
			int j = 1;
			do {
				buffer[8 - j++] = ((byte) (0x80 | (codepoint & 0x3f)));
				codepoint >>= 6;
				mfb >>= 1;
			} while (codepoint > mfb);
			buffer[8 - j] = (byte) ((~mfb << 1) | codepoint);
			sb.append(buffer, 8 - j, j);
		}

		// 27721: 230     177     137
		// 23383: 229     173     151
		// 47   : 47
		// 28450: 230     188     162
		// 23383: 229     173     151
		for (int i : buffer) System.out.printf("%d ", i & 0xFF);
		System.out.println();

		LuaString st = sb.toLuaString();
		for (int i = 0; i < st.length; i++) System.out.printf("%d ", st.luaByte(i));
		System.out.println();

		System.out.println("\u5b57");
		System.out.println(st.toString());
	}
}
