FROM ubuntu:26.04 AS etimage

#update ubuntu os
RUN apt update
RUN apt upgrade -y

#install required packages
RUN apt install -y openssl sudo openssh-client curl dos2unix gettext-base netcat-openbsd unzip tzdata postgresql-client vim

#Create docker directory and copy environment variables.
WORKDIR /opt/docker
COPY ./.env webapp.env
RUN dos2unix webapp.env
RUN chmod 740 webapp.env

#create docker group
RUN export DOCKER_GID=$(cat /opt/docker/webapp.env | grep DOCKER_GID | awk -F "=" '{print $2}') && \
    groupadd --gid $DOCKER_GID docker

#Import certficates
RUN sudo install -d -g docker -m 0750 certs
COPY ./docker/private/keys/*.crt certs
RUN chmod 744 certs/*.crt
COPY ./docker/private/keys/*.key certs
RUN chmod 700 certs/*.key



################################
## MY JAVA IMAGE
###############################
FROM etimage AS etjava

#Create java user and assign java user to docker group
RUN useradd -g docker -c "Java user" --shell /bin/bash --create-home --home-dir /opt/myjava javauser

#create directory and set permissions
WORKDIR /opt/myjava

##INSTALL jdk
ARG ARCHITECTURE=amd64
ARG JAVA_VERSION=25.0.5.1

RUN --mount=type=bind,source=docker/Software,target=/tmp/sw \
  set -eux; \
  case "$ARCHITECTURE" in \
    amd64) JDK_TAR=jdk-"$JAVA_VERSION"_linux-x64_bin.tar.gz ;; \
    arm64) JDK_TAR=jdk-"$JAVA_VERSION"_linux-aarch64_bin.tar.gz ;; \
    *) echo "Unsupported architecture: $ARCHITECTURE" >&2; exit 1 ;; \
  esac; \
  tar -xzf "/tmp/sw/$JDK_TAR" -C /opt/myjava
ENV JAVA_HOME="/opt/myjava/jdk-$JAVA_VERSION"

##INSTALL jasypt
RUN sudo install -d -o javauser -g docker -m 0755 /opt/jasypt
COPY ./docker/Software/jasypt-1.9.3-dist.zip /opt/jasypt/
COPY ./docker/scripts/jasypt/jdecrypt.sh /opt/jasypt/
RUN unzip /opt/jasypt/jasypt-1.9.3-dist.zip -d /opt/jasypt
RUN chmod +x -R /opt/jasypt/jasypt-1.9.3/bin
RUN rm /opt/jasypt/jasypt-1.9.3-dist.zip
RUN chmod 755 /opt/jasypt/jdecrypt.sh

##INSTALL maven
COPY ./docker/Software/apache-maven-3.10.1-bin.tar.gz /opt/myjava/
RUN tar -xzvf apache-maven-3.10.1-bin.tar.gz
RUN rm apache-maven-3.10.1-bin.tar.gz
ENV M2_HOME=/opt/myjava/apache-maven-3.10.1
ENV M2=$M2_HOME/bin

##SET Java environment variables
ENV PATH="$JAVA_HOME/bin:$M2:/opt/jasypt/jasypt-1.9.3/bin:$PATH"

#import self signed certificates in java
RUN keytool -import -trustcacerts -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit -noprompt -alias keycloak -file /opt/docker/certs/kc.crt
RUN keytool -import -trustcacerts -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit -noprompt -alias webapp -file /opt/docker/certs/webapp.crt

##CREATE OPERATING SYSTEM USERS

WORKDIR /opt/docker

#copy private files
COPY ./docker/private/jasypt-password.txt ./

#Create administrator user
RUN export APP_USER_ADMIN_ENC_PASSWORD=$(cat /opt/docker/webapp.env | grep APP_USER_ADMIN_ENC_PASSWORD | awk -F "=" '{print $2}') && \
    ADMUSER_PASS=$(/opt/jasypt/jdecrypt.sh "$APP_USER_ADMIN_ENC_PASSWORD" "$(cat jasypt-password.txt)") && \
    ADMUSER_CYPHERED_PASSWORD=$(openssl passwd -1 "$ADMUSER_PASS") && \
    useradd -c "Administrator user" --password "$ADMUSER_CYPHERED_PASSWORD" --shell /bin/bash --create-home --home-dir /home/admuser admuser
RUN usermod -a -G sudo admuser

#Create a simple user
RUN export APP_USER_TEST_ENC_PASSWORD=$(cat /opt/docker/webapp.env | grep APP_USER_TEST_ENC_PASSWORD | awk -F "=" '{print $2}') && \
    SAMSEPI0L_PASS=$(/opt/jasypt/jdecrypt.sh "$APP_USER_TEST_ENC_PASSWORD" "$(cat jasypt-password.txt)") && \
    SAMSEPI0L_CYPHERED_PASSWORD=$(openssl passwd -1 "$SAMSEPI0L_PASS") && \
    useradd -c "Simple user." --password "$SAMSEPI0L_CYPHERED_PASSWORD" --shell /bin/bash --create-home --home-dir /home/samsepi0l samsepi0l

#remove sesitive files from image
#RUN rm admuser-password.txt
#RUN rm samsepi0l-password.txt
RUN rm jasypt-password.txt

################################
## MY POSTGRESQL IMAGE
###############################
FROM etjava AS postgreset

ENV DEBIAN_FRONTEND=noninteractive

#Create postgres user
RUN export POSTGRES_GID=$(cat /opt/docker/webapp.env | grep POSTGRES_GID | awk -F "=" '{print $2}') && \
    groupadd --gid $POSTGRES_GID postgres

RUN export POSTGRES_UID=$(cat /opt/docker/webapp.env | grep POSTGRES_UID | awk -F "=" '{print $2}') && \
    useradd -u $POSTGRES_UID -g postgres -c "PostgreSQL Administrator" --shell /bin/bash --home-dir /opt/postgresql postgres

RUN usermod -a -G docker postgres

#create app directory
WORKDIR /opt/mypostgres

RUN chown postgres:postgres /opt/mypostgres
RUN sudo -u postgres mkdir -m 700 data
RUN sudo -u postgres mkdir -m 755 logs

#Install postgres
RUN apt install -y postgresql

#copy files, scripts, etc
COPY ./docker/scripts/database/. scripts/.
RUN chown postgres:postgres -R scripts/
RUN chmod 755 -R scripts/


################################
## MY NGINX IMAGE
###############################
FROM etimage AS etnginx

#install nginx
RUN apt install -y nginx

RUN useradd -g docker -c "Nginx application user" --shell /bin/bash --create-home --home-dir /opt/nginx nginx
WORKDIR /opt/nginx

#copy sites and enable them
COPY ./docker/scripts/nginx .
RUN find ./ -maxdepth 1 -type f -name "*.sh" | xargs dos2unix
RUN chmod 740 nginx.entrypoint.sh

############################################
## Keycloak
############################################
FROM etjava AS keycloaket

RUN useradd -g docker -c "Keycloak application user" --shell /bin/bash --create-home --home-dir /opt/keycloak keycloak

COPY ./docker/Software/keycloak-26.8.2.zip /opt/keycloak

WORKDIR /opt/keycloak
COPY ./docker/scripts/keycloak .
RUN find ./ -maxdepth 1 -type f -name "*.sh" | xargs dos2unix
RUN find ./ -maxdepth 1 -type f -name "*.sh" | xargs chmod +x

#copy certificates and keys
RUN chown keycloak:docker /opt/docker/certs/kc.unsecure.key

USER keycloak

RUN unzip keycloak-26.8.2.zip
RUN rm keycloak-26.8.2.zip

ENV HOME=/opt/keycloak
ENV PATH="$HOME/keycloak-26.8.2/bin:$PATH"

############################################
## Keycloak Configuration
############################################
FROM keycloaket AS keycloaketcnf

USER root

RUN apt install -y vim

############################################
## MY webapp IMAGE
############################################
FROM etjava AS etwebapp
#USER root
RUN useradd -g docker -c "Usuario application user" --shell /bin/bash --create-home --home-dir /opt/webapp webappuser

USER webappuser
STOPSIGNAL SIGINT

#Set application folder
WORKDIR /opt/webapp
ENV HOME=/opt/webapp

RUN mkdir -m 750 src
RUN mkdir -m 750 target
RUN mkdir -m 750 API
RUN mkdir -m 750 bin
RUN mkdir -m 750 etc
RUN mkdir -m 750 lib

COPY --chmod=0750 --chown=webappuser:docker ./API/. API/.
COPY --chmod=0750 --chown=webappuser:docker ./application.yml etc/.
COPY --chmod=0750 --chown=webappuser:docker ./docker/scripts/webapp/. bin/.
COPY --chmod=0750 --chown=webappuser:docker ./docker/Software/junit-platform-console-standalone-6.0.3.jar lib/.

#Clone the application
RUN --mount=type=bind,source=library,target=src/library bin/compile.sh library
RUN --mount=type=bind,source=model,target=src/model bin/compile.sh model
RUN --mount=type=bind,source=utils,target=src/utils bin/compile.sh utils

############################################
## USER SERVICE
############################################
FROM etwebapp AS etuser

RUN --mount=type=bind,source=userKc,target=src/userKc bin/compile.sh userKc

############################################
## SCHEMA SERVICE
############################################
FROM etwebapp AS etschema

RUN --mount=type=bind,source=schema,target=src/schema bin/compile.sh schema

############################################
## MESSENGER SERVICE
############################################
FROM etwebapp AS etmail

RUN --mount=type=bind,source=messenger,target=src/messenger bin/compile.sh messenger

############################################
## COMPANY SERVICE
############################################
FROM etwebapp AS etcompany

RUN --mount=type=bind,source=company,target=src/company bin/compile.sh company

############################################
## FRONTEND SERVICE
############################################
FROM etwebapp AS frontend

RUN --mount=type=bind,source=frontend,target=src/frontend bin/compile.sh frontend

############################################
## CLAUDE
############################################

FROM etjava as claude

RUN PASSWORD=$(openssl passwd -1 secret) && \
    useradd -c "User to run claude assistant" \
    -g docker \
    --password "$PASSWORD" \
    --shell /bin/bash \
    --create-home --home-dir /home/claude \
    claudia

USER claudia
WORKDIR /home/claude
RUN curl -fsSL -O https://claude.ai/install.sh
RUN chmod +x install.sh
RUN ./install.sh
RUN rm install.sh
ENV HOME=/home/claude