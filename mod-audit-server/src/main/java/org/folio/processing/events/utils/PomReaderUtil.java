package org.folio.processing.events.utils;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public enum PomReaderUtil {
  INSTANCE;

  private String moduleName = null;
  private String version = null;
  private Properties props = null;
  private List<Dependency> dependencies = null;

  private PomReaderUtil() {
    this.init("pom.xml");
  }

  void init(String pomFilename) {
    try {
      String currentRunningJar = PomReaderUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
      boolean readCurrent = currentRunningJar != null && (currentRunningJar.contains("domain-models-runtime") || currentRunningJar.contains("domain-models-interface-extensions") || currentRunningJar.contains("target"));
      if (readCurrent) {
        this.readIt(pomFilename, "META-INF/maven");
      } else {
        this.readIt((String)null, "META-INF/maven");
      }

    } catch (Exception var4) {
      throw new IllegalArgumentException(var4);
    }
  }

  void readIt(String pomFilename, String directoryName) throws IOException, XmlPullParserException {
    Model model;
    if (pomFilename != null) {
      File pomFile = new File(pomFilename);
      MavenXpp3Reader mavenReader = new MavenXpp3Reader();
      model = mavenReader.read(new FileReader(pomFile));
    } else {
      model = this.getModelFromJar(directoryName);
    }

    if (model == null) {
      throw new IOException("Can't read module name - Model is empty!");
    } else {
      if (model.getParent() != null) {
        this.moduleName = model.getParent().getArtifactId();
        this.version = model.getParent().getVersion();
      } else {
        this.moduleName = model.getArtifactId();
        this.version = model.getVersion();
      }

      this.version = this.version.replaceAll("-.*", "");
      this.moduleName = this.moduleName.replace("-", "_");
      this.props = model.getProperties();
      this.dependencies = model.getDependencies();
      this.version = this.replacePlaceHolderWithValue(this.version);
    }
  }

  private Model getModelFromJar(String directoryName) throws IOException, XmlPullParserException {
    MavenXpp3Reader mavenReader = new MavenXpp3Reader();
    Model model = null;
    URL url = Thread.currentThread().getContextClassLoader().getResource(directoryName);
    if (url.getProtocol().equals("jar")) {
      String dirname = directoryName + "/";
      String path = url.getPath();
      String jarPath = path.substring(5, path.indexOf(33));
      JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name()));
      Enumeration<JarEntry> entries = jar.entries();

      while(entries.hasMoreElements()) {
        JarEntry entry = (JarEntry)entries.nextElement();
        String name = entry.getName();
        if (name.startsWith(dirname) && !dirname.equals(name) && name.endsWith("pom.xml")) {
          InputStream pomFile = PomReaderUtil.class.getClassLoader().getResourceAsStream(name);
          model = mavenReader.read(pomFile);
          break;
        }
      }
    }

    return model;
  }

  private String replacePlaceHolderWithValue(String placeholder) {
    String[] ret = new String[]{placeholder};
    if (placeholder != null && placeholder.startsWith("${")) {
      this.props.forEach((k, v) -> {
        if (("${" + k + "}").equals(placeholder)) {
          ret[0] = (String)v;
        }

      });
    }

    return ret[0];
  }

  public String constructModuleVersionAndVersion(String moduleName, String moduleVersion) {
    String result = moduleName.replace("_", "-");
    return result + "-" + moduleVersion;
  }

  public String getVersion() {
    return this.version;
  }

  public String getModuleName() {
    return this.moduleName;
  }

  public Properties getProps() {
    return this.props;
  }

  public List<Dependency> getDependencies() {
    return this.dependencies;
  }
}
