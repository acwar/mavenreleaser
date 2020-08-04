# MercuryTFS Maven Releaser
## Visión general
Maven Releaser es una herramienta desarrollada en Mercury para cubrir la necesidad de liberaciones rapidas de todas la maraña de dependencias que se desprenden de los proyectos Maven.

Permite el analisis de un pom de partida en que encuentra todas sus dependencias y de forma escalonada liberarlas en el orden que lo exija el arbol de dependencias.

## Fucionamiento general liberación
Una vez invocado, se explora el _pom.xml_ del proyecto indicado en busca de dependencias que se encuentren subfijadas con el apellido _SNAPSHOT_. 
Para cada una de estas que se encuentren se busca dicho artefacto en el repositorio de MercuryTFS ([artifactory](http://192.168.10.2:8081/artifactory/webapp/login.html?0)) en lo repositorios de versiones cerradas en busca de una version liberada: 
- De no encontrarse se trata de buscar una version en el repositorio de versiones SNAPSHOT
  - De encontrarse en el repositorio de _SNAPSHOTS_ de Artifactory, se descarga y analiza de la misma manera que al padre que lo encontró
  - De no encontrarse en el repo _SNAPSHOTS_ se le supone una dependencia externa y se le obvia.
- De encontrarse se actualiza el pom eliminando el sufijo _SNAPSHOT_ en tanto se entiende no procede.

Con el resultado de estas búsquedas se actualiza el POM analizado y __se respalda__ en el servidor para proceder a lanzar el plugin __maven-release__, este baja una copia del código para comprobar que compila correctamente, hace un tag en el repositorio de código y avanza el numero de versión del pom. Este numero nuevo de version será preguntado al operador conforme se continuen las liberaciones. Se puede indicar la version del artefacto en ciernes a liberar ademas de la version siguiente separadas por @
```sh
1.10.10@1.9.1
```
Indica que la siguiente version será la 1.10.0 y que el pom que está procesando se debe cerrar y desplegar como 1.9.1


## Uso
Para lanzarlo se debe ejecutar como _jar_ en un sistema en que se tenga correctamente configurado la invacion de Maven (esto es, con un settings.xml que se conozca es correcto)
```sh
$ java -jar mavenreleaser-4.0.0.jar --username aUser --url http://gitlab.mercurytfs.com/aMightyArtifact -- artefactName aMightyArtifact --action prepare
```
El ejecutable cuenta con 4 parámetros obligatorios, a saber
- __url__ La ruta en que se aloja el codigo fuente del artefacto a liberar
- __username__ El usuario con que conectar al repositorio de código
- __artefactName__ El nombre que se le va a dar al artefacto en la sesión (no será el que se le aplique en la subida)
- __action__ Que tipo de acción se desea llevar a cabo, __prepare__ para analizar el arbol de dependencias o __release__ para liberar el artefacto completo y todas las dependencias que de el se desprenden.

A mayores de estas se puede indicar como parámetros opcionales 
- __password__ La contraseña del usuario indicado, útil para labores de autimatizacion
- __jira__ Jira asociado al artefacto para el tratamiento del flujo operativo
 

