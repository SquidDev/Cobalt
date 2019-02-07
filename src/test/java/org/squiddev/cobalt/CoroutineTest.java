package org.squiddev.cobalt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHelpers;
import org.squiddev.cobalt.function.ResumableVarArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Tests yielding in a whole load of places
 */
@RunWith(Parameterized.class)
public class CoroutineTest {
	private final String name;
	private ScriptDrivenHelpers helpers;

	public CoroutineTest(String name) {
		this.name = name;
		this.helpers = new ScriptDrivenHelpers("/coroutine/");
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			{"basic"},
			{"debug"},
			{"gsub"},
			{"ops"},
			{"pcall"},
			{"table"},
			{"tail"},
			{"xpcall"},
		};

		return Arrays.asList(tests);
	}

	private void setup() {
		helpers.state.stdout = new CapturingPrintStream();
		helpers.globals.load(helpers.state, new CoroutineFunctions());
	}

	@Test
	public void runNormal() throws IOException, CompileException, LuaError {
		helpers.setup();
		setup();
		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}

	@Test(timeout = 1_000L)
	public void runBlocking() throws IOException, CompileException, LuaError {
		helpers.setup(LuaState.Builder::yieldThreader);
		setup();
		((LuaTable) helpers.globals.rawget("coroutine")).rawset("yield", new VarArgFunction() {
			@Override
			public Varargs invoke(LuaState state, Varargs args) throws LuaError {
				try {
					return LuaThread.yieldBlocking(state, args);
				} catch (InterruptedException e) {
					throw new OrphanedThread();
				}
			}
		});

		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}

	private static final class CapturingPrintStream extends PrintStream {
		private final ByteArrayOutputStream output;

		public CapturingPrintStream() {
			super(new ByteArrayOutputStream());
			output = (ByteArrayOutputStream) out;
		}

		@Override
		public void println() {
			super.println();
		}

		@Override
		public void write(int b) {
			super.write(b);
			System.out.write(b);
		}

		@Override
		public void write(byte[] buf, int off, int len) {
			super.write(buf, off, len);
			System.out.write(buf, off, len);
		}

		public String getOutput() {
			return output.toString().replace("\r\n", "\n").replace('\r', '\n');
		}
	}

	private static class CoroutineFunctions extends ResumableVarArgFunction<LuaThread> implements LuaLibrary {
		@Override
		public LuaValue add(LuaState state, LuaTable environment) {
			bind(environment, CoroutineFunctions.class, new String[]{"suspend", "run", "assertEquals", "fail", "getOutput", "id"});
			return environment;
		}

		@Override
		public Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			switch (opcode) {
				case 0:
					throw UnwindThrowable.suspend();
				case 1: {
					LuaThread thread = new LuaThread(state, args.first().checkFunction(), getfenv());
					di.state = thread;
					throw LuaThread.resume(state, thread, Constants.NONE);
				}
				case 2: {
					String traceback = DebugHelpers.traceback(state.getCurrentThread(), 0);
					Assert.assertEquals(traceback, args.arg(1), args.arg(2));
					return Constants.NONE;
				}
				case 3: {
					String traceback = DebugHelpers.traceback(state.getCurrentThread(), 0);
					Assert.fail(args.first().toString() + ":\n" + traceback);
					return Constants.NONE;
				}
				case 4: {
					return valueOf(((CapturingPrintStream) state.stdout).getOutput());
				}
				case 5:
					return args;
				default:
					return Constants.NONE;
			}
		}

		@Override
		public Varargs resumeThis(LuaState state, LuaThread thread, Varargs value) throws LuaError, UnwindThrowable {
			switch (opcode) {
				case 0:
					return value;
				case 1:
					if (!thread.isAlive()) return value;
					throw LuaThread.resume(state, thread, value);
				default:
					throw new NonResumableException("Cannot resume " + debugName());
			}
		}
	}

}