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
mvn clean install -f userKc/pom.xml \
-Djasypt.encryptor.password=$(cat /run/secrets/jasypt-password) \
-Dspring.config.additional-location=$(pwd)/API/,$(pwd)/ \
-Daether.installer.hardLink=false \
-Dmaven.test.failure.ignore=true 
```

```bash
# Run application
java -ea -Djasypt.encryptor.password=$(cat /run/secrets/jasypt-password) \
-Dspring.config.additional-location=$(pwd)/API/,$(pwd)/ \
-jar /home/claude/.m2/repository/com/laetienda/userKc/1.0-SNAPSHOT/userKc-1.0-SNAPSHOT.jar & \
PID_WEBAPP_USER=$!
```

## Frontend
```bash
mvn clean install -f frontend/pom.xml \
-Djasypt.encryptor.password=$(cat /run/secrets/jasypt-password) \
-Dspring.config.additional-location=$(pwd)/API/,$(pwd)/ \
-Daether.installer.hardLink=false
```