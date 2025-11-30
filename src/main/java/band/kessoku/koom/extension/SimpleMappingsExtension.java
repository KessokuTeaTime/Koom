package band.kessoku.koom.extension;

import net.fabricmc.loom.LoomGradleExtension;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import javax.inject.Inject;

public abstract class SimpleMappingsExtension {
	@Inject
	protected abstract Project getProject();

	/**
	 * Yarn mappings patch only support (Neo)Forge, because (Neo)Forge patches broken with yarn mappings.
	 * Support 1.20.5 and above NeoForge version, but MinecraftForge version only support 1.20.3 and above.
	 *
	 * @param patchVersion yarn mappings patch version
	 * @return yarn-mappings-patch dependency
	 */
	public Dependency yarnPatch(String patchVersion) {
		Project project = this.getProject();
		LoomGradleExtension loomExtension = LoomGradleExtension.get(project);

		if (loomExtension.isNeoForge()) {
			return project.getDependencies().create("dev.architectury:yarn-mappings-patch-neoforge:" + patchVersion);
		} else if (loomExtension.isForge()) {
			return project.getDependencies().create("dev.architectury:yarn-mappings-patch-forge:" + patchVersion);
		} else {
			throw new RuntimeException("Yarn mappings patch only support (Neo)Forge");
		}
	}
}
