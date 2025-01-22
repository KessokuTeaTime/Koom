/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2025 FabricMC
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

package net.fabricmc.loom.configuration.fabricapi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.fabricapi.GameTestSettings;
import net.fabricmc.loom.configuration.ide.RunConfigSettings;
import net.fabricmc.loom.task.AbstractLoomTask;
import net.fabricmc.loom.task.LoomTasks;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.gradle.SourceSetHelper;

public abstract class FabricApiTesting extends FabricApiAbstractSourceSet {
	@Inject
	protected abstract Project getProject();

	@Inject
	public FabricApiTesting() {
	}

	@Override
	protected String getSourceSetName() {
		return "gametest";
	}

	void configureTests(Action<GameTestSettings> action) {
		final LoomGradleExtension extension = LoomGradleExtension.get(getProject());
		final TaskContainer tasks = getProject().getTasks();

		GameTestSettings settings = getProject().getObjects().newInstance(GameTestSettings.class);
		settings.getCreateSourceSet().convention(false);
		settings.getEnableGameTests().convention(true);
		settings.getEnableClientGameTests().convention(true);
		settings.getEula().convention(false);
		settings.getClearRunDirectory().convention(true);
		settings.getUsername().convention("Player0");

		action.execute(settings);

		final SourceSet testSourceSet;

		if (settings.getCreateSourceSet().get()) {
			testSourceSet = configureSourceSet(settings.getModId(), true);
		} else {
			testSourceSet = SourceSetHelper.getMainSourceSet(getProject());
		}

		Consumer<RunConfigSettings> configureBase = run -> {
			if (settings.getCreateSourceSet().get()) {
				run.source(getSourceSetName());
			}
		};

		if (settings.getEnableGameTests().get()) {
			RunConfigSettings gameTest = extension.getRunConfigs().create("gameTest", run -> {
				run.inherit(extension.getRunConfigs().getByName("server"));
				run.property("fabric-api.gametest");
				run.runDir("build/run/gameTest");
				configureBase.accept(run);
			});

			tasks.named("test", task -> task.dependsOn(LoomTasks.getRunConfigTaskName(gameTest)));
		}

		if (settings.getEnableClientGameTests().get()) {
			// Not ideal as there may be multiple resources directories, if this isnt correct the mod will need to override this.
			final File resourcesDir = testSourceSet.getResources().getFiles().stream().findAny().orElse(null);

			RunConfigSettings clientGameTest = extension.getRunConfigs().create("clientGameTest", run -> {
				run.inherit(extension.getRunConfigs().getByName("client"));
				run.property("fabric.client.gametest");

				if (resourcesDir != null) {
					run.property("fabric.client.gametest.testModResourcesPath", resourcesDir.getAbsolutePath());
				}

				run.runDir("build/run/clientGameTest");

				if (settings.getUsername().isPresent()) {
					run.programArgs("--username", settings.getUsername().get());
				}

				configureBase.accept(run);
			});

			if (settings.getClearRunDirectory().get()) {
				var deleteGameTestRunDir = tasks.register("deleteGameTestRunDir", Delete.class, task -> {
					task.setGroup(Constants.TaskGroup.FABRIC);
					task.delete(clientGameTest.getRunDir());
				});

				tasks.named(LoomTasks.getRunConfigTaskName(clientGameTest), task -> task.dependsOn(deleteGameTestRunDir));
			}

			if (settings.getEula().get()) {
				var acceptEula = tasks.register("acceptGameTestEula", AcceptEulaTask.class, task -> {
					task.getEulaFile().set(getProject().file(clientGameTest.getRunDir() + "/eula.txt"));

					if (settings.getClearRunDirectory().get()) {
						// Ensure that the eula is accepted after the run directory is cleared
						task.dependsOn(tasks.named("deleteGameTestRunDir"));
					}
				});

				tasks.named("configureLaunch", task -> task.dependsOn(acceptEula));
			}
		}
	}

	public abstract static class AcceptEulaTask extends AbstractLoomTask {
		@OutputFile
		public abstract RegularFileProperty getEulaFile();

		@TaskAction
		public void acceptEula() throws IOException {
			final Path eula = getEulaFile().get().getAsFile().toPath();

			if (Files.notExists(eula)) {
				Files.writeString(eula, """
						#This file was generated by the Fabric Loom Gradle plugin. As the user opted into accepting the EULA.
						eula=true
						""");
			}
		}
	}
}
