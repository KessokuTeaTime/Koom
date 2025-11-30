package band.kessoku.koom.extension;

import java.io.File;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.util.download.DownloadException;

/**
 * A gradle extension with specific functionality related to Forgified Fabric API.
 */
public abstract class ForgifiedFabricApiExtension {
	private final HashMap<String, Map<String, String>> moduleVersionCache = new HashMap<>();
	private boolean isSinytraVersion;

	@Inject
	protected abstract Project getProject();

	/**
	 * Get a {@link Dependency} for a given Forgified Fabric API module.
	 *
	 * @param moduleName       The name of the module.
	 * @param fabricApiVersion The main Forgified Fabric API version.
	 * @param isSinytraVersion Use the Sinytra version or the ThinkingStudio version.
	 * @return A {@link Dependency} for the module.
	 */
	public Dependency module(String moduleName, String fabricApiVersion, boolean isSinytraVersion) {
		this.isSinytraVersion = isSinytraVersion;
		return getProject().getDependencies()
				.create(getDependencyNotation(moduleName, fabricApiVersion));
	}

	/**
	 * Get the version of a Forgified Fabric API module.
	 *
	 * @param moduleName       The name of the module.
	 * @param fabricApiVersion The main Forgified Fabric API version.
	 * @return The version of the module.
	 */
	public String moduleVersion(String moduleName, String fabricApiVersion) {
		String moduleVersion = moduleVersionCache
				.computeIfAbsent(fabricApiVersion, this::getApiModuleVersions)
				.get(moduleName);

		if (moduleVersion == null) {
			throw new RuntimeException("Failed to find module version for module: " + moduleName);
		}

		return moduleVersion;
	}

	private String getDependencyNotation(String moduleName, String fabricApiVersion) {
		return String.format("org.sinytra.forgified-fabric-api:%s:%s", moduleName, moduleVersion(moduleName, fabricApiVersion));
	}

	private Map<String, String> getApiModuleVersions(String fabricApiVersion) {
		try {
			return populateModuleVersionMap(getApiMavenPom(fabricApiVersion));
		} catch (PomNotFoundException e) {
			throw new RuntimeException("Could not find forgified-fabric-api version: " + fabricApiVersion);
		}
	}

	private Map<String, String> populateModuleVersionMap(File pomFile) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document pom = docBuilder.parse(pomFile);

			Map<String, String> versionMap = new HashMap<>();

			NodeList dependencies = ((Element) pom.getElementsByTagName("dependencies").item(0)).getElementsByTagName("dependency");

			for (int i = 0; i < dependencies.getLength(); i++) {
				Element dep = (Element) dependencies.item(i);
				Element artifact = (Element) dep.getElementsByTagName("artifactId").item(0);
				Element version = (Element) dep.getElementsByTagName("version").item(0);

				if (artifact == null || version == null) {
					throw new RuntimeException("Failed to find artifact or version");
				}

				versionMap.put(artifact.getTextContent(), version.getTextContent());
			}

			return versionMap;
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse " + pomFile.getName(), e);
		}
	}

	private File getApiMavenPom(String fabricApiVersion) throws PomNotFoundException {
		return getPom("forgified-fabric-api", fabricApiVersion);
	}

	private File getPom(String name, String version) throws PomNotFoundException {
		final LoomGradleExtension extension = LoomGradleExtension.get(getProject());
		final var mavenPom = new File(extension.getFiles().getUserCache(), "forgified-fabric-api/%s-%s.pom".formatted(name, version));

		try {
			if (this.isSinytraVersion) {
				extension.download(String.format("https://maven.su5ed.dev/releases/org/sinytra/forgified-fabric-api/%2$s/%1$s/%2$s-%1$s.pom", version, name))
						.defaultCache()
						.downloadPath(mavenPom.toPath());
			} else {
				extension.download(String.format("https://maven.kessokuteatime.work/snapshots/org/sinytra/forgified-fabric-api/%2$s/%1$s/%2$s-%1$s.pom", version, name))
						.defaultCache()
						.downloadPath(mavenPom.toPath());
			}
		} catch (DownloadException e) {
			if (e.getStatusCode() == 404) {
				throw new PomNotFoundException(e);
			}

			throw new UncheckedIOException("Failed to download maven info to " + mavenPom.getName(), e);
		}

		return mavenPom;
	}

	private static class PomNotFoundException extends Exception {
		PomNotFoundException(Throwable cause) {
			super(cause);
		}
	}
}
