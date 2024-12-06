/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2024 FabricMC
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

package net.fabricmc.loom.util;

import javax.inject.Inject;

import org.apache.commons.io.output.NullOutputStream;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.configuration.ShowStacktrace;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.process.JavaExecSpec;
import org.jetbrains.annotations.Nullable;

/**
 * Contains helpers for executing Forge's command line tools
 * with suppressed output streams to prevent annoying log spam.
 */
public abstract class ForgeToolExecutor {
	@Inject
	protected abstract JavaToolchainService getToolchainService();

	@Inject
	protected abstract ExecOperations getExecOperations();

	@Inject
	protected abstract Project getProject();

	public static boolean shouldShowVerboseStdout(Project project) {
		// if running with INFO or DEBUG logging
		return project.getGradle().getStartParameter().getLogLevel().compareTo(LogLevel.LIFECYCLE) < 0;
	}

	public static boolean shouldShowVerboseStderr(Project project) {
		// if stdout is shown or stacktraces are visible so that errors printed to stderr show up
		return shouldShowVerboseStdout(project) || project.getGradle().getStartParameter().getShowStacktrace() != ShowStacktrace.INTERNAL_EXCEPTIONS;
	}

	/**
	 * Executes a {@link ExecOperations#javaexec(Action) javaexec} action with suppressed output.
	 *
	 * @param project      the project
	 * @param configurator the {@code javaexec} configuration action
	 * @return the execution result
	 */
	public static ExecResult exec(Project project, Action<? super JavaExecSpec> configurator) {
		return project.getObjects().newInstance(ForgeToolExecutor.class)
				.exec(configurator);
	}

	private ExecResult exec(Action<? super JavaExecSpec> configurator) {
		final Project project = getProject();
		return getExecOperations().javaexec(spec -> {
			configurator.execute(spec);

			if (shouldShowVerboseStdout(project)) {
				spec.setStandardOutput(System.out);
			} else {
				spec.setStandardOutput(NullOutputStream.INSTANCE);
			}

			if (shouldShowVerboseStderr(project)) {
				spec.setErrorOutput(System.err);
			} else {
				spec.setErrorOutput(NullOutputStream.INSTANCE);
			}

			// Use project toolchain for executing if possible.
			// Note: This feature cannot be tested using the test kit since
			//  - Gradle disables native services in test kit environments.
			//  - The only resolver plugin I could find, foojay-resolver,
			//    requires the services for finding the OS architecture.
			final @Nullable String executable = findJavaToolchainExecutable(project);

			if (executable != null) {
				spec.setExecutable(executable);
			}
		});
	}

	private @Nullable String findJavaToolchainExecutable(Project project) {
		final JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
		final JavaToolchainSpec toolchain = java.getToolchain();

		if (!toolchain.getLanguageVersion().isPresent()) {
			// Toolchain not configured, we'll use the runtime Java version.
			return null;
		}

		final JavaLauncher launcher = getToolchainService().launcherFor(toolchain).get();
		return launcher.getExecutablePath().getAsFile().getAbsolutePath();
	}
}
