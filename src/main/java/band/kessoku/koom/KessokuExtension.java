package band.kessoku.koom;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.util.ModPlatform;
import net.fabricmc.loom.util.gradle.SourceSetHelper;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;

public abstract class KessokuExtension {
	@Inject
	protected abstract Project getProject();

	public static final String[] modulePlatforms = new String[] { "fabric", "neo", "common" };

    public void platform() {
		SourceSet fabric = SourceSetHelper.createSourceSet("fabric", getProject());
		SourceSet neo = SourceSetHelper.createSourceSet("neo", getProject());
		SourceSet common = SourceSetHelper.createSourceSet("common", getProject());
    }

	public void library(String lib) {
		Project project = this.getProject();
		DependencyHandler dependencies = project.getDependencies();

		for (String plat : modulePlatforms) {
			Dependency dependency = dependencies.project(Map.of(
					"path", SourceSetHelper.createSourceSet(plat, project.project(lib)).getOutput(),
					"configuration", "namedElements"
			));
			dependencies.add("implementation", dependency);
		}
	}

	public void testModules(String[] names) {
		Arrays.stream(names).forEach(this::testModule);
	}

	public void modules(String[] names) {
		Arrays.stream(names).forEach(this::module);
	}

	@Deprecated
	public void moduleIncludes(String[] names) {
		Arrays.stream(names).forEach(this::moduleInclude);
	}

	public void testModule(String name) {
		Project project = this.getProject();
		DependencyHandler dependencies = project.getDependencies();

		for (String plat : modulePlatforms) {
			Dependency dependency = dependencies.project(Map.of(
					"path", SourceSetHelper.createSourceSet(plat, project.project(name)).getOutput(),
					"configuration", "namedElements"
			));
			dependencies.add("testImplementation", dependency);
		}
	}

	public void module(String name) {
		Project project = this.getProject();
		DependencyHandler dependencies = project.getDependencies();

		for (String plat : modulePlatforms) {
			Dependency dependency = dependencies.project(Map.of(
					"path", SourceSetHelper.createSourceSet(plat, project.project(name)).getOutput(),
					"configuration", "namedElements"
			));
			dependencies.add("api", dependency);

			LoomGradleExtensionAPI loom = project.getExtensions().getByType(LoomGradleExtensionAPI.class);
			loom.mods(mods -> mods.register("kessoku-" + name + "-" + plat, settings -> {
				Project depProject = project.project(name);
				SourceSetContainer sourceSets = depProject.getExtensions().getByType(SourceSetContainer.class);
				settings.sourceSet(sourceSets.getByName(plat), depProject);
			}));
		}
	}

	public void moduleInclude(String name) {
		Project project = this.getProject();
		DependencyHandler dependencies = project.getDependencies();

		for (String plat : modulePlatforms) {
			Dependency dependency = dependencies.project(Map.of(
					"path", SourceSetHelper.createSourceSet(plat, project.project(name)).getOutput()
			));
			dependencies.add("include", dependency);
		}
	}

	public void common(String name) {
		Project project = this.getProject();
		DependencyHandler dependencies = project.getDependencies();

		ModuleDependency dependency = (ModuleDependency) dependencies.project(Map.of(
				"path", SourceSetHelper.createSourceSet("common", project.project(name)).getOutput(),
				"configuration", "namedElements"
		));
		dependency.setTransitive(false);
		dependencies.add("compileOnly", dependency);
		dependencies.add("runtimeOnly", dependency);
		for (String plat : modulePlatforms) {
			dependencies.add("development" + Character.toUpperCase(plat.charAt(0)) + plat.substring(1), dependency);
		}
	}

	@Deprecated
	public void shadowBundle(String name, ModPlatform platform) {
		Project project = this.getProject();
		DependencyHandler dependencies = project.getDependencies();

		Dependency dependency = dependencies.project(Map.of(
				"path", SourceSetHelper.createSourceSet("common", project.project(name)).getOutput(),
				"configuration", "transformProduction" + platform.displayName()
		));
		dependencies.add("shade", dependency);
	}
}
