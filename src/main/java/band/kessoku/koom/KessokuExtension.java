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

package band.kessoku.koom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import dev.architectury.plugin.ArchitectPluginExtension;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.language.jvm.tasks.ProcessResources;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.task.RemapJarTask;

public abstract class KessokuExtension {
	@Inject
	protected abstract Project getProject();

	Project project = getProject();
	Project root = project.getRootProject();
	ArchitectPluginExtension arch = getProject().getExtensions().getByType(ArchitectPluginExtension.class);
	DependencyHandler dependencies = project.getDependencies();

	private PlatformIdentifier platform;
	private final List<String> modules = new ArrayList<>();

	public static final String[] PLATFORMS = new String[] { "fabric", "neo", "common" };

	public void include(String module) {
		modules.add(module);
		module = ":" + module;
		String finalModule = module;
		getProject().getGradle().beforeSettings(settings -> {
			settings.include(finalModule);
			for (String platform : PLATFORMS) {
				settings.include(finalModule + ":" + platform);
			}
		});
	}

	public void version(String mod, String minecraft) {
		project.setVersion("%s+%s.%s".formatted(mod, platform.id(), minecraft));
	}

	public void name(String name) {
		project.getExtensions().getByType(BasePluginExtension.class).getArchivesName().set(name);
		project.getChildProjects().values().forEach(subproject -> subproject.getExtensions().getByType(BasePluginExtension.class).getArchivesName().set(name));
	}

	public void common(Object loader) {
		arch.common("fabric", "neoforge");

		Dependency dependency = dependencies.create(loader);
		dependencies.add("modImplementation", dependency);
	}

	public void neoforge(Object neoforge) {
		platform = PlatformIdentifier.NEO;
		arch.platformSetupLoomIde();
		arch.neoForge();

		Dependency dependency = dependencies.create(neoforge);
		dependencies.add("neoForge", dependency);

		settingResource();
	}

	public void fabric(Object... fabric) {
		platform = PlatformIdentifier.FABRIC;
		arch.platformSetupLoomIde();
		arch.fabric();

		for (Object dep : fabric) {
			Dependency dependency = dependencies.create(dep);
			dependencies.add("modImplementation", dependency);
		}

		settingResource();
	}

	private void settingResource() {
		Task processResources = project.getTasks().getByName("processResources");
		processResources.getInputs().property("version", project.getVersion());
		if (platform == PlatformIdentifier.FABRIC) {
			((ProcessResources) processResources).filesMatching("fabric.mod.json", fileCopy -> {
				fileCopy.expand(Map.of("version", project.getVersion()));
			});
		} else if (platform == PlatformIdentifier.NEO) {
			((ProcessResources) processResources).filesMatching("META-INF/neoforge.mods.toml", fileCopy -> {
				fileCopy.expand(Map.of("version", project.getVersion()));
			});
		}
	}

	public void settingsShade() {
		Configuration shade = project.getConfigurations().getByName("shade");
		shade.setCanBeResolved(true);
		shade.setCanBeConsumed(true);

		ShadowJar shadowJar = (ShadowJar) project.getTasks().getByName("shadowJar");
		shadowJar.setConfigurations(Collections.singletonList(project.getConfigurations().getByName("shade")));
		shadowJar.getArchiveClassifier().set("dev-shadow");

		RemapJarTask remapJar = (RemapJarTask) project.getTasks().getByName("remapJar");
		remapJar.getInputFile().set(shadowJar.getArchiveFile());
	}

	public void library(String lib) {
		Project project = this.getProject();
		DependencyHandler dependencies = project.getDependencies();

		Dependency dependency = dependencies.project(Map.of(
				"path", lib,
				"configuration", "namedElements"
		));
		dependencies.add("implementation", dependency);
	}

	public void testModules(List<String> names, String plat) {
		names.forEach(name -> testModule(name, plat));
	}

	public void modules(List<String> names, String plat) {
		names.forEach(name -> module(name, plat));
	}

	public void moduleIncludes(List<String> names, String plat) {
		names.forEach(name -> moduleInclude(name, plat));
	}

	public void testModule(String name, String plat) {
		Dependency dependency = dependencies.project(Map.of(
				"path", ":" + name + ":" + plat,
				"configuration", "namedElements"
		));
		dependencies.add("testImplementation", dependency);
	}

	public void module(String name, String plat) {
		Dependency dependency = dependencies.project(Map.of(
				"path", ":" + name + ":" + plat,
				"configuration", "namedElements"
		));
		dependencies.add("api", dependency);

		LoomGradleExtensionAPI loom = project.getExtensions().getByType(LoomGradleExtensionAPI.class);
		loom.mods(mods -> mods.register("kessoku-" + name + ":" + plat, settings -> {
			Project depProject = project.project(":" + name + ":" + plat);
			SourceSetContainer sourceSets = depProject.getExtensions().getByType(SourceSetContainer.class);
			settings.sourceSet(sourceSets.getByName("main"), depProject);
		}));
	}

	public void moduleInclude(String name, String plat) {
		Dependency dependency = dependencies.project(Map.of(
				"path", ":" + name + ":" + plat
		));
		dependencies.add("include", dependency);
	}

	public void common(String name, PlatformIdentifier platform) {
		ModuleDependency dependency = (ModuleDependency) dependencies.project(Map.of(
				"path", ":" + name + ":common",
				"configuration", "namedElements"
		));
		dependency.setTransitive(false);
		dependencies.add("compileOnly", dependency);
		dependencies.add("runtimeOnly", dependency);
		dependencies.add("development" + platform.platform().displayName(), dependency);
	}

	public void shadowBundle(String name, PlatformIdentifier platform) {
		Dependency dependency = dependencies.project(Map.of(
				"path", ":" + name + ":common",
				"configuration", "transformProduction" + platform.platform().displayName()
		));
		dependencies.add("shade", dependency);
	}

	public PlatformIdentifier getPlatform() {
		return platform;
	}

	public List<String> getModules() {
		return modules;
	}
}
