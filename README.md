# GestorFTPHilos

GestorFTP es una aplicación basada en java que permite la sincronización de archivos entre una carpeta de nuestra maquina y un servidor FTP.  
Incluye cifrado AES de archivos.txt, comparación de hashes para detectar cambios y un sistema de ejecución en hilos para optimizar el rendimiento, ademas cuenta con un historial, donde se almacenan todos los archivos que son borrados/renombrados.

## Características principales
- Conexión automática a un servidor FTP.
- Sincronización de archivos entre la carpeta local y remota.
- Cifrado y descifrado de archivos `.txt` con AES.
- Comparación de archivos mediante hashes MD5.
- Descarga y eliminación automática de archivos según el estado de sincronización.
- Guardado de un historial para los archivos borrados

---

## 📂 Clases principales

### **1. GestorFTP**
Esta clase gestiona la conexión con el servidor FTP y maneja la sincronización de archivos.  
**Responsabilidades principales:**
- Conectar y desconectar del servidor FTP.
- Subir, descargar y eliminar archivos.
- Comprobar la sincronización entre local y remoto.

### **2. ManejarCifrado**
Clase encargada del cifrado y descifrado de archivos `.txt`.  
**Responsabilidades principales:**
- Generar claves AES.
- Cifrar archivos antes de subirlos al servidor.
- Descifrar archivos al descargarlos.

### **3. TareaSincronizacion**
Clase que ejecuta las funciones necesarias para la sincronización.  
**Responsabilidades principales:**
- Llamar a subirFichero cuando no se encuentra en el servidor
- Llamar a descargar para descargar el contenido cifrado, descifrarlo y guardarlo

### **4. DescargarFTP**
Clase ejecutabke que lista los archivos del cliente FTP para elegir y descargar.
**Responsabilidades principales:**
- Listar remotos para darte a elegir.
- Descomprimir y descargar el archivo elegido en una carpeta por defecto o una dada.

---

## 🚀 Cómo usar la aplicación

### **1. Instalar Docker (si no lo tienes)**
Descarga e instala Docker desde el siguiente enlace:  
🔗 [Descargar Docker](https://www.docker.com/get-started)

### **2. Crear el contenedor FTP**
Ejecuta el siguiente comando para crear un contenedor FTP usando Docker:  
```sh
docker run -d --name Servidor-ftp -p 21:21 -p 30000-30009:30000-30009 -e "PUBLICHOST=localhost" stilliard/pure-ftpd:latest
```
- Este comando creara un contenedor de nombre Servidor-ftp que usara el puerto 21 para la maquina host y permitira conexiones entre los puertos 30000 y 30009

### **3. Crear el usuario**
Ejecutar los siguientes comandos para crear el usuario
```sh
docker exec -it Servidor-ftp /bin/bash
```
- Nos conectamos al contenedor que acabamos de crear

```sh
pure-pw useradd Eduardo -u ftpuser -d /home/ftpusers/Eduardo
```
- Añadimos al usuario junto a su carpeta al contenedor

```sh
pure-pw mkdb
```
- Le damos todos los permisos

```sh
exit
```
- Nos salimos del contenedor
- Una vez configurado el contenedor en la clase principal GestorFTP debemos cambiar usuario y password si cambiamos algo.

### **4. Ejecucion**
- Debemos de tener el contenedor activo en todo momento
- La clase GestorFTP en la principal, se encarga de la conexion, comprobar que haya sincronia, llamada de hilos, cifrado y validación.
- La clase DescargarFTP es secundaria, al ejecutarse te mostrara los archivos en el contenedor y te pedira un numero para descifrarlo y descargarlo.

### **5. Comandos**
- Escribir "parar" en la terminal sin importar mayusculas dentendra el proyecto en cuanto pueda
