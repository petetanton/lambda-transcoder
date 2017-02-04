#!/usr/bin/env bash
mvn clean
mvn --show-version --batch-mode clean org.jacoco:jacoco-maven-plugin:prepare-agent verify org.jacoco:jacoco-maven-plugin:report install org.codehaus.mojo:sonar-maven-plugin:2.5:sonar -Dsonar.host.url=http://localhost:9000/ -Dsonar.scm.disabled=true
mvn clean
./deploy.sh