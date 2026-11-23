# Webapp - Java code snippets 

## Libraries

```bash
# Compile code
mvn clean install -Daether.installer.hardLink=false -f library/pom.xml
mvn clean install -Daether.installer.hardLink=false -f model/pom.xml
mvn clean install -Daether.installer.hardLink=false -f utils/pom.xml
```
+ Option description:
    - **aether.installer.hardLink** - Needs to be disabled because The project tree and the local Maven repo live on different filesystems.

## User
```bash
# Compile code
mvn clean install -Daether.installer.hardLink=false -f userKc/pom.xml

# Run application
```