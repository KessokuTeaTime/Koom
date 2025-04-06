package band.kessoku.koom;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.ModPlatform;

public abstract class KoomGradleExtension {
	@Inject
	protected abstract Project getProject();

	private ModPlatform platform = null;
	private final DependencyHandler dependencyHandler = getProject().getDependencies();
	private Runnable addDependency = () -> { };

	public void common() {
		platform = ModPlatform.FABRIC;
	}

	public void fabric(String version) {
		platform = ModPlatform.FABRIC;
		addDependency = () -> dependencyHandler.add("modImplementation", "net.fabricmc:fabric-loader:" + version);
	}

	public void forge(String version) {
		platform = ModPlatform.FORGE;
		addDependency = () -> dependencyHandler.add(Constants.Configurations.FORGE, "net.minecraftforge:forge:" + version);
	}

	public void neoforge(String version) {
		platform = ModPlatform.NEOFORGE;
		addDependency = () -> dependencyHandler.add(Constants.Configurations.NEOFORGE, "net.neoforged:neoforge:" + version);
	}

	public void quilt(String version) {
		platform = ModPlatform.QUILT;
		addDependency = () -> dependencyHandler.add("modImplementation", "org.quiltmc:quilt-loader:" + version);
	}

	@Nullable
	public ModPlatform getPlatform() {
		return platform;
	}

	@ApiStatus.Internal
	public void registerDependency() {
		addDependency.run();
	}
}
