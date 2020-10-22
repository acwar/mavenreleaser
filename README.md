# MercuryTFS Maven Releaser
## Visión general
Maven Releaser es una herramienta opensource desarrollada por empleados de Mercury en su tiempo libre para cubrir la necesidad de liberaciones rapidas de todas la maraña de dependencias que se desprenden de los proyectos Maven.

Permite el analisis de un pom de partida en que encuentra todas sus dependencias y de forma escalonada liberarlas en el orden que lo exija el arbol de dependencias.

También permite verificar los commits que se hacen a svn o git para que el codigo fuente que se sube está correctamente relacionado en jira con los artefactos correspondientes

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


## Uso Maven Releaser
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
- 


#MercuryTFS SvnChecker
Dentro de este paquete se encuenta la clase de utilidad SVNChecker que comprueba la correccion de la metodologia aplciada 
a los commit en SVN y su contrapartida en JIRA con sus enlaces a tareas tipo _Artefacto Maven_

Se incluye una invocacion de ejemplo en el Hook *pre-commit* de SVN actualmente en uso (192.168.10.2:/var/lib/svn/mercury/hooks)

```
#!/bin/sh

REPOS="$1"
TXN="$2"

SVNLOOK=/usr/bin/svnlook
JAVA=/opt/mavenreleaser/mavenreleaser-4.3.3.jar
CAMBIOS=`$SVNLOOK changed -t $TXN $REPOS`
COMMIT_MESSAGE=`$SVNLOOK log -t $TXN $REPOS` 
USER=`$SVNLOOK author -t $TXN $REPOS`
i=1
for aux in $CAMBIOS
do
  
if [ `expr $i % 2` -eq 0 ]
then
    if [ $FICHEROS ]
    then 
    	FICHEROS="$FICHEROS;${aux}"
    else
        FICHEROS=${aux}
    fi
fi
i=$((i+1))
done
java -cp $JAVA  -Dloader.main=com.mercurytfs.mercury.mavenreleaser.SVNChecker org.springframework.boot.loader.PropertiesLauncher $FICHEROS "$COMMIT_MESSAGE" $USER

VALIDA=$?

if [ "$VALIDA" -eq "1" ]
then
	echo "ERROR_WRONG_PARAMETERS" 1>&2

elif [ "$VALIDA" -eq "2" ]
then
        echo "ERROR_COMMIT_MESSAGE_FORMAT" 1>&2

elif [ "$VALIDA" -eq "3" ]
then
        echo "ERROR_SVN_FILE_HAS_NOT_OPEN_ARTEfACT_OR_DONT_EXIST" 1>&2
elif [ "$VALIDA" -eq "4" ]
then
        echo "ERROR_SVN_FILE_ARTEFACT_IS_NOT_LINKED_WITH_COMMIT_ISSUE" 1>&2

elif [ "$VALIDA" -eq "5" ]
then
        echo "ERROR_JIRA_ISSUE_IS_RESOLVED_CANNOT_COMMIT_TO_THIS_ISSUE" 1>&2
fi

exit $VALIDA
```

Para su configuracion se usa el fichero __config.properties__ por defecto incluido en este paquete, aunqe se puede indicar
por parametros de la maquina virtual una ruta alternativa __-Dconfigfile.path=__[ruta fichero].

Las propiedades que debe contener minimas dicho fichero son
- __notcheck.token__=#NOTCHECK
- __jira.user__=admin1
- __jira.password__=########
- __projects.list__=MERCURY,BANORTE,PRUEB,IN....