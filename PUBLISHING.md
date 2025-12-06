# Publishing to Maven Central

This document describes how to publish this library to Maven Central via Sonatype OSSRH.

## Prerequisites

Before you can publish to Maven Central, you need to complete the following one-time setup:

### 1. Create a Sonatype JIRA Account

1. Go to https://issues.sonatype.org/secure/Signup!default.jspa
2. Create an account
3. Create a new Project ticket for your `com.shaibachar` groupId
   - Follow instructions at: https://central.sonatype.org/publish/publish-guide/#initial-setup

### 2. Generate a GPG Key

GPG signing is required for all artifacts published to Maven Central.

```bash
# Generate a new GPG key
gpg --gen-key

# List your keys to find the key ID
gpg --list-keys

# Publish your public key to a key server
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

### 3. Configure Maven Settings

Add your Sonatype credentials and GPG passphrase to `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>YOUR_SONATYPE_USERNAME</username>
      <password>YOUR_SONATYPE_PASSWORD</password>
    </server>
  </servers>
  
  <profiles>
    <profile>
      <id>ossrh</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>YOUR_GPG_PASSPHRASE</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
```

## Publishing Process

### 1. Update Version

Before publishing, update the version in `pom.xml` from a SNAPSHOT to a release version:

```xml
<version>1.0.0</version>
```

### 2. Build and Deploy

The project is now configured with all the necessary plugins for Maven Central deployment:

- **maven-source-plugin**: Generates sources JAR
- **maven-javadoc-plugin**: Generates javadoc JAR
- **maven-gpg-plugin**: Signs artifacts with GPG
- **nexus-staging-maven-plugin**: Handles staging and release to Maven Central

To deploy to Maven Central:

```bash
# Clean build with signing enabled
mvn clean deploy -Dgpg.skip=false

# Or use the release profile if you create one
mvn clean deploy -P release
```

### 3. Release Process

The `nexus-staging-maven-plugin` is configured with `autoReleaseAfterClose=true`, which means:

1. Artifacts are uploaded to the staging repository
2. The staging repository is automatically closed
3. If all validations pass, it's automatically released to Maven Central
4. Within ~2 hours, it will be available on Maven Central
5. Within ~4 hours, it will be searchable on https://search.maven.org

If you prefer manual control, set `autoReleaseAfterClose=false` in `pom.xml` and use:

```bash
mvn nexus-staging:release
```

## Development Builds (SNAPSHOT)

For development builds, you can deploy SNAPSHOT versions without GPG signing:

```bash
mvn clean deploy -Dgpg.skip=true
```

SNAPSHOT versions are deployed to:
- https://s01.oss.sonatype.org/content/repositories/snapshots

## Troubleshooting

### GPG Signing Issues

If you encounter GPG signing issues:

```bash
# Test GPG signing manually
gpg --sign test.txt

# Check your GPG keys
gpg --list-secret-keys

# Export your key if needed
gpg --armor --export YOUR_KEY_ID > public-key.asc
```

### Build Without Signing (for testing)

The project is configured to skip GPG signing by default. This allows you to:

```bash
# Build and verify locally without GPG
mvn clean verify

# This will generate:
# - spring-boot-mcp-lib-1.0.0-SNAPSHOT.jar
# - spring-boot-mcp-lib-1.0.0-SNAPSHOT-sources.jar
# - spring-boot-mcp-lib-1.0.0-SNAPSHOT-javadoc.jar
```

To enable signing for release, use `-Dgpg.skip=false`.

## Requirements Met

This project's POM includes all requirements for Maven Central:

- ✅ `<name>`, `<description>`, `<url>`
- ✅ `<licenses>` (MIT License)
- ✅ `<developers>` information
- ✅ `<scm>` (Source Code Management) details
- ✅ `<distributionManagement>` for Sonatype OSSRH
- ✅ Source JAR generation (maven-source-plugin)
- ✅ Javadoc JAR generation (maven-javadoc-plugin)
- ✅ GPG signing capability (maven-gpg-plugin)
- ✅ Nexus staging (nexus-staging-maven-plugin)

## Additional Resources

- Maven Central Publishing Guide: https://central.sonatype.org/publish/publish-guide/
- Sonatype OSSRH Guide: https://central.sonatype.org/publish/publish-maven/
- GPG Guide: https://central.sonatype.org/publish/requirements/gpg/
