# About

# Installation

## Install ```docker```
```bash
apt update
apt upgrade
```

## Get code
```bash
git clone git@github.com:davidrcuervo/webapp.git
```

## Crete private folders and keys.

````
 $APP_ROOT_FOLDER
 ├──  docker
    ├── private
      ├── Software
      └── Keys
        ├── [ ] kc.private.key
        └── [ ] kc.unsecure.key
        └── [ ] kc.crt
        └── [ ] webapp.crt
        └── [ ] webapp.private.key
        └── [ ] webapp.unsecure.key
        └── [ ] jasypt-password.txt
        └── [ ] samsepi0l-password.txt
        └── [ ] admuser-password.txt
````

## Download software

Download software to folder ```$APP_ROOT_FOLDER/docker/Software```

+ [ ] ```jdk-25.0.5.0.3_linux-x64_bin.tar.gz``` | Link: [Java SE Development Kit 25.0.2 downloads](https://www.oracle.com/ca-en/java/technologies/downloads/#jdk25-linux)
+ [ ] ```apache-maven-3.10.0-bin.tar.gz``` | Link: [Downloading Apache Maven 3.9.14](https://maven.apache.org/download.cgi)
+ [ ] ```jasypt-1.9.3-dist.zip``` | Link: [jasypt 1.9.3 (binaries and javadocs)](https://github.com/jasypt/jasypt/releases/download/jasypt-1.9.3/jasypt-1.9.3-dist.zip)
+ [ ] ```keycloak-26.8.2.zip``` | Link: [Keycloak Downloads 26.5.5](https://www.keycloak.org/downloads)
+ [ ] ```junit-platform-console-standalone-6.1.3.jar``` | Link: [Maven Repository | org/junit/platform/junit-platform-console-standalone/6.1.3](https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.1.3/)

## Create key

+ [ ] Copy certificates and copy them to ```$APP_ROOT_FOLDER/docker/private/keys```

## create passwords

+ [ ] admuser-password.txt
+ [ ] samsepi0l-password.txt
+ [ ] jasypt-password.txt

## Edit environmental file ```$APP_ROOT_FOLDER/docker/.env```

## Build docker images
```bash
#find docker group id
cat /etc/group | grep docker
    
#build docker images
docker copose build --no-cache --build-arg DOCKER_GID=$(cat /etc/group | grep docker | cut -d: -f3)
```

<a id="org927a2e4"></a>

# Test and Run
## Run
```bash
#0. Ideally it should works by only running the following command, but that is not ready. So that, steps 1. 2. and 3 must be completed instead
dockerm compose up -dx
    
#1. Run database and keycloak container
docker compose up -d keycloaketcnf
    
#2. Test if keycloak is up and ready to accept connections
kcadm.sh config credentials --server https://auth.webapp.com --realm master --user etadmuser
```

## Security

## Data Layer

# API

API is defined on file ```$APP_ROOT_FOLDER/API/Application.ylm``` 

# Appendix
## Install Java
```bash
#Extract java
    
#Set java environmental variables
export JAVA_HOME=/opt/java/jdk-21.0.6
export PATH=$JAVA_HOME/bin:$PATH
```
## Install Maven
```bash
#1. Uncompress maven
tar -xzvf Software/apache-maven-3.9.9-bin.tar.gz -C /opt/maven/
    
#2. Set envrionment variables
export M2_HOME=/opt/myjava/apache-maven-3.9.9
export M2=$M2_HOME/bin
export PATH="$M2:$PATH"
```

## Install keycloak
```bash
#1. Uncompress keycloak
unzip Software/keycloak-26.1.5.zip -d /opt/keycloak
    
#2. Add bin to path env variable
export KC_HOME=/opt/keycloak/keycloak-26.1.5
export PATH="$KC_HOME/bin:$PATH"
```

## Encrypt passwords
### Encrypt by using jasypt bin script
```bash
#ENCRYPT
/opt/jasypt/jasypt-1.9.3/bin/encrypt.sh algorithm="PBEWITHHMACSHA512ANDAES_256" saltGeneratorClassName="org.jasypt.salt.RandomSaltGenerator" ivGeneratorClassName="org.jasypt.iv.RandomIvGenerator" \
password="password" input="plainsecretpassword"
    
#DECRYPT
/opt/jasypt/jasypt-1.9.3/bin/decrypt.sh algorithm="PBEWITHHMACSHA512ANDAES_256" saltGeneratorClassName="org.jasypt.salt.RandomSaltGenerator" ivGeneratorClassName="org.jasypt.iv.RandomIvGenerator" \
password="password" input="encryptedsecretpassword"
```

### Encrypt by using maven plugin
```bash
#ENCRYPT
mvn jasypt:encrypt-value -Djasypt.encryptor.password="password" -Djasypt.plugin.value="DEC(plain)" -f pom.xml
    
#DECRYPT
mvn jasypt:decrypt-value -Djasypt.encryptor.password="password" -Djasypt.plugin.value="ENC(encrypted)" -f pom.xml
```

## Create a self-signed certificate
### Commands
```bash
# Create private key with password
openssl genrsa -aes256 -out private.key 4096
    
# Remove password protection from private key
openssl rsa -in private.key -out unsecure.key
   
# Create certificate
openssl req -x509 -key unsecure.key -config config.cfg -out certificate.crt
```

### Example of config.cfg file
```text
[req]
distinguished_name=req_distinguished_name
x509_extensions=v3_req
prompt=no
    
[req_distinguished_name]
C=CA
ST=Quebec
L=Montreal
O=La eTienda
OU=Webapp IT
CN=webapp.com
    
[v3_req]
subjectAltName=@alt_names
    
[alt_names]
DNS.1=www.webapp.com
DNS.2=frontend
DNS.3=localhost
IP.1=127.0.0.1
```

### Add self-signed certificate to java keystore
```bash
# Add certificate to java trusted certs
%JAVA_HOME%\bin\keytool.exe -importcert -file 'C:\path\to\cert\cert.crt' -alias aliasName -keystore '%JAVA_HOME%\lib\security\cacerts'
    
# List certificate by alias
%JAVA_HOME%\bin\keytool.exe -list -alias webapp -keystore '%JAVA_HOME%\lib\security\cacerts'
    
# Delete certificate by alias
%JAVA_HOME%\bin\keytool.exe -delete -alias webapp -keystore '%JAVA_HOME%\lib\security\cacerts
```

# References

+ Bootstrap: [Bootstrap 5.1 Documentation](https://getbootstrap.com/docs/5.1/getting-started/introduction/)

