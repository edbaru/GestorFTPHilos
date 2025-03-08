package com.mycompany.gestorftp;

import java.io.*;
import java.net.SocketException;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Key;
import org.apache.commons.net.ftp.*;

public class GestorFTP {

    private final FTPClient clienteFTP;
    private static final String SERVIDOR = "localhost";         // Servidor FTP
    private static final int PUERTO = 21;                       // Puerto FTP
    private static final String USUARIO = "Eduardo";            // Usuario FTP
    private static final String PASSWORD = "1234";              // Contraseña FTP
    private static final long INTERVALO_SINCRONIZACION = 5000;  // 5 segundos
    private static final String PASSWORDAES = "TresTristesTigresTraganTrigoEnUnTrigal";
    private static final Key claveAES = ManejarCifrado.obtenerClave(PASSWORDAES, 32);

    // Lista para almacenar las tareas de sincronización
    private final List<TareaSincronizacion> tareasSincronizacion = new ArrayList<>();

    public GestorFTP() {
        clienteFTP = new FTPClient();
    }

    public static int leerIntEntre(String mensaje, int menor, int mayor) {
        Scanner teclado = new Scanner(System.in);
        int valor;
        do {
            System.out.println(mensaje);
            while (!teclado.hasNextInt()) {
                teclado.nextLine();
                System.out.println("No se ha introducido un int. Vuelve a introducir el valor");
            }
            valor = teclado.nextInt();
            if (valor <= menor) {
                System.out.println("El valor no tiene el rango adecuado.");
            }
            if (valor > mayor) {
                System.out.println("El valor no tiene el rango adecuado");
            }
        } while (valor <= menor || valor > mayor);
        return valor;
    }

    private void conectar() throws SocketException, IOException {
        if (!clienteFTP.isConnected()) {
            clienteFTP.connect(SERVIDOR, PUERTO);
            int respuesta = clienteFTP.getReplyCode();

            if (!FTPReply.isPositiveCompletion(respuesta)) {
                clienteFTP.disconnect();
                throw new IOException("Error al conectar con el servidor FTP");
            }

            boolean credencialesOK = clienteFTP.login(USUARIO, PASSWORD);

            if (!credencialesOK) {
                throw new IOException("Error al conectar con el servidor FTP. Credenciales incorrectas.");
            }

            clienteFTP.setFileType(FTP.BINARY_FILE_TYPE);
            clienteFTP.enterLocalPassiveMode();
        }
    }

    private void desconectar() throws IOException {
        if (clienteFTP.isConnected()) {
            clienteFTP.logout();
            clienteFTP.disconnect();
            System.out.println("Desconectado del servidor FTP.");
        }
    }

    boolean subirFichero(String path, boolean esTxt) throws Exception {
        File ficheroLocal = new File(path);
        if (!ficheroLocal.exists()) {
            System.err.println("El archivo no existe: " + path);
            return false;
        }

        if (esTxt) {
            // Cifrar el archivo .txt antes de subirlo
            String contenidoOriginal = leerArchivoTexto(ficheroLocal);
            String contenidoCifrado = ManejarCifrado.cifrar(contenidoOriginal, claveAES);

            File archivoCifrado = File.createTempFile("temp_cifrado", ".aes");
            try (FileWriter fw = new FileWriter(archivoCifrado)) {
                fw.write(contenidoCifrado);
            }

            try (InputStream is = new FileInputStream(archivoCifrado)) {
                boolean enviado = clienteFTP.storeFile(ficheroLocal.getName(), is);
                if (enviado) {
                    System.out.println("Archivo cifrado y subido: " + ficheroLocal.getName());
                }
                return enviado;
            } finally {
                archivoCifrado.delete();
            }
        } else {
            // Subir el archivo sin cifrar
            try (InputStream is = new FileInputStream(ficheroLocal)) {
                boolean enviado = clienteFTP.storeFile(ficheroLocal.getName(), is);
                if (enviado) {
                    System.out.println("Archivo subido sin cifrar: " + ficheroLocal.getName());
                }
                return enviado;
            }
        }
    }

    private String leerArchivoTexto(File archivo) throws IOException {
        StringBuilder contenido = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                contenido.append(linea).append("\n");
            }
        }
        return contenido.toString().trim();
    }

    private Set<String> listarArchivosLocales(String carpetaLocal) {
        File carpeta = new File(carpetaLocal);
        File[] archivos = carpeta.listFiles();
        Set<String> archivosLocales = new HashSet<>();

        if (archivos != null) {
            for (File archivo : archivos) {
                if (archivo.isFile()) {
                    archivosLocales.add(archivo.getName());
                }
            }
        } else {
            System.err.println("La carpeta local no existe o no es accesible: " + carpetaLocal);
        }

        return archivosLocales;
    }

    private Set<String> listarArchivosRemotos(String carpetaRemota) throws IOException {
        FTPFile[] archivosRemotos = clienteFTP.listFiles();
        Set<String> archivosRemotosSet = new HashSet<>();

        if (archivosRemotos != null) {
            for (FTPFile archivo : archivosRemotos) {
                if (archivo.isFile()) {
                    archivosRemotosSet.add(archivo.getName());
                }
            }
        } else {
            System.err.println("No se pudo listar los archivos remotos en: " + carpetaRemota);
        }

        return archivosRemotosSet;
    }

    private String obtenerHashLocal(String path) throws IOException {
        File archivoLocal = new File(path);
        return calcularHashMD5(archivoLocal);
    }

    private String obtenerHashRemoto(String nombreFichero) throws IOException {
        FTPFile[] archivos = clienteFTP.listFiles(nombreFichero);
        if (archivos.length > 0) {
            // Crear archivo temporal para almacenar el archivo remoto
            File archivoTemp = File.createTempFile("temp", ".tmp");
            try (FileOutputStream fos = new FileOutputStream(archivoTemp)) {
                // Descargar el archivo remoto y escribirlo en el archivo temporal
                if (clienteFTP.retrieveFile(nombreFichero, fos)) {
                    return calcularHashMD5(archivoTemp);
                } else {
                    System.err.println("No se pudo descargar el archivo remoto: " + nombreFichero);
                }
            }
        }
        return null;
    }

    private String calcularHashMD5(File archivo) throws IOException {
        try (FileInputStream fis = new FileInputStream(archivo)) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md5.update(buffer, 0, bytesRead);
            }
            byte[] digest = md5.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Error al calcular el hash MD5", e);
        }
    }
    
    private int extraerVersion(String nombreArchivo) {
        try {
            String[] partes = nombreArchivo.split("_v");
            if (partes.length > 1) {
                String versionStr = partes[1].replace(".txt", "");
                return Integer.parseInt(versionStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error al extraer la versión del archivo: " + nombreArchivo);
        }
        return 0;
    }
    
    private int obtenerUltimaVersion(String nombreArchivo, String carpetaHistory) {
        File carpeta = new File(carpetaHistory);
        File[] archivos = carpeta.listFiles((dir, name) -> name.startsWith(nombreArchivo.replace(".txt", "")));

        int maxVersion = 0;
        if (archivos != null) {
            for (File archivo : archivos) {
                String nombre = archivo.getName();
                int version = extraerVersion(nombre);
                if (version > maxVersion) {
                    maxVersion = version;
                }
            }
        }
        return maxVersion;
    }
    
    private String generarNombreVersionado(String nombreArchivo, int version) {
        String nombreBase = nombreArchivo.replace(".txt", "");
        return nombreBase + "_v" + version + ".txt";
    }

    boolean eliminarFicheroRemoto(String nombreFichero, String carpetaHistory) throws IOException {
        if (nombreFichero.toLowerCase().endsWith(".txt")) {
            // Mover el archivo a la carpeta history antes de eliminarlo
            File archivoRemoto = new File(carpetaHistory + File.separator + nombreFichero);
            if (archivoRemoto.exists()) {
                // Obtener el número de versión más alto para este archivo
                int version = obtenerUltimaVersion(nombreFichero, carpetaHistory) + 1;
                String nuevoNombre = generarNombreVersionado(nombreFichero, version);

                // Descargar el archivo remoto y guardarlo en la carpeta history con el nuevo nombre
                File archivoHistory = new File(carpetaHistory + File.separator + nuevoNombre);
                try (FileOutputStream fos = new FileOutputStream(archivoHistory)) {
                    if (clienteFTP.retrieveFile(nombreFichero, fos)) {
                        System.out.println("Archivo movido a 'history': " + nuevoNombre);
                    } else {
                        System.err.println("Error al descargar el archivo remoto para moverlo a 'history': " + nombreFichero);
                        return false;
                    }
                }
            } else {
                // Si no existe, moverlo directamente con la versión 1
                String nuevoNombre = generarNombreVersionado(nombreFichero, 1);
                File archivoHistory = new File(carpetaHistory + File.separator + nuevoNombre);
                try (FileOutputStream fos = new FileOutputStream(archivoHistory)) {
                    if (clienteFTP.retrieveFile(nombreFichero, fos)) {
                        System.out.println("Archivo movido a 'history': " + nuevoNombre);
                    } else {
                        System.err.println("Error al descargar el archivo remoto para moverlo a 'history': " + nombreFichero);
                        return false;
                    }
                }
            }
        }

        // Eliminar el archivo remoto
        return clienteFTP.deleteFile(nombreFichero);
    }

    void descargarArchivo(String nombreArchivo, String rutaDestino) throws Exception {
        File archivoDestino = new File(rutaDestino + File.separator + nombreArchivo);

        // Verificar si el archivo es de tipo .txt (cifrado)
        if (nombreArchivo.toLowerCase().endsWith(".txt")) {
            // Descargar y descifrar el archivo .txt cifrado
            File archivoTempCifrado = File.createTempFile("temp_cifrado", ".txt");
            try (FileOutputStream fos = new FileOutputStream(archivoTempCifrado)) {
                if (clienteFTP.retrieveFile(nombreArchivo, fos)) {
                    // Leer el contenido cifrado del archivo temporal
                    String contenidoCifrado = leerArchivoTexto(archivoTempCifrado);

                    // Descifrar el contenido
                    String contenidoDescifrado = ManejarCifrado.descifrar(contenidoCifrado, claveAES);

                    // Guardar el contenido descifrado en el archivo de destino
                    try (FileWriter writer = new FileWriter(archivoDestino)) {
                        writer.write(contenidoDescifrado);
                    }

                    System.out.println("Archivo descargado y descifrado: " + archivoDestino.getAbsolutePath());
                } else {
                    System.err.println("Error al descargar el archivo: " + nombreArchivo);
                }
            } finally {
                archivoTempCifrado.delete();
            }
        } else {
            // Descargar el archivo sin cifrar
            try (FileOutputStream fos = new FileOutputStream(archivoDestino)) {
                if (clienteFTP.retrieveFile(nombreArchivo, fos)) {
                    System.out.println("Archivo descargado sin cifrar: " + archivoDestino.getAbsolutePath());
                } else {
                    System.err.println("Error al descargar el archivo: " + nombreArchivo);
                }
            }
        }
    }

    private void verificarConexion() throws IOException {
        if (!clienteFTP.isConnected()) {
            conectar();
        }
    }

    // Método para agregar tareas de sincronización a la lista
    private synchronized void agregarTareaSincronizacion(TareaSincronizacion tarea) {
        tareasSincronizacion.add(tarea);
    }

    // Método para procesar las tareas de sincronización
    private void procesarTareasSincronizacion() {
        synchronized (tareasSincronizacion) {
            for (TareaSincronizacion tarea : tareasSincronizacion) {
                try {
                    tarea.ejecutar();
                } catch (Exception e) {
                    System.err.println("Error al ejecutar la tarea de sincronización: " + e.getMessage());
                }
            }
            tareasSincronizacion.clear(); // Limpiar la lista después de procesar las tareas
        }
    }
    
    private void crearCarpetaHistory(String carpetaHistory) {
        File carpeta = new File(carpetaHistory);
        if (!carpeta.exists()) {
            if (carpeta.mkdirs()) {
                System.out.println("Carpeta 'history' creada: " + carpetaHistory);
            } else {
                System.err.println("No se pudo crear la carpeta 'history': " + carpetaHistory);
            }
        }
    }
    
    public void comprobarSincronia(String carpetaLocal, String carpetaRemota) {
        try {
            // Crear la carpeta history si no existe
            String carpetaHistory = carpetaLocal + File.separator + "history";
            crearCarpetaHistory(carpetaHistory);

            verificarConexion(); // Verificar y reconectar si es necesario
            Set<String> archivosLocales = listarArchivosLocales(carpetaLocal);
            Set<String> archivosRemotos = listarArchivosRemotos(carpetaRemota);

            // Subir archivos locales que no están en el servidor remoto o están desactualizados
            for (String archivoLocal : archivosLocales) {
                String pathLocal = carpetaLocal + File.separator + archivoLocal;
                boolean esTxt = archivoLocal.toLowerCase().endsWith(".txt");

                if (archivosRemotos.contains(archivoLocal)) {
                    if (esTxt) {
                        // Crear un archivo temporal con el contenido del archivo local
                        File archivoTempLocal = File.createTempFile("temp_local", ".txt");
                        try (FileWriter writer = new FileWriter(archivoTempLocal)) {
                            String contenidoOriginal = leerArchivoTexto(new File(pathLocal));
                            writer.write(contenidoOriginal);
                        }

                        // Cifrar el archivo temporal local
                        File archivoTempCifrado = File.createTempFile("temp_cifrado", ".aes");
                        String contenidoCifrado = ManejarCifrado.cifrar(leerArchivoTexto(archivoTempLocal), claveAES);
                        try (FileWriter writer = new FileWriter(archivoTempCifrado)) {
                            writer.write(contenidoCifrado);
                        }

                        // Descargar el archivo remoto
                        File archivoTempRemoto = File.createTempFile("temp_remoto", ".aes");
                        try (FileOutputStream fos = new FileOutputStream(archivoTempRemoto)) {
                            if (clienteFTP.retrieveFile(archivoLocal, fos)) {
                                // Comparar el contenido cifrado del archivo local con el remoto
                                String contenidoCifradoRemoto = leerArchivoTexto(archivoTempRemoto);
                                String contenidoCifradoLocal = leerArchivoTexto(archivoTempCifrado);

                                if (!contenidoCifradoLocal.equals(contenidoCifradoRemoto)) {
                                    // Si los contenidos cifrados son diferentes, el archivo está desactualizado
                                    System.out.println("Archivo desactualizado, subiendo: " + archivoLocal);
                                    agregarTareaSincronizacion(new TareaSincronizacion(this, "subir", archivoLocal, carpetaLocal));
                                } else {
                                    // Si los contenidos cifrados son iguales, el archivo ya está sincronizado
                                    System.out.println("Ya sincronizado: " + archivoLocal);
                                }
                            } else {
                                System.err.println("Error al descargar el archivo remoto: " + archivoLocal);
                            }
                        } finally {
                            // Eliminar archivos temporales
                            archivoTempLocal.delete();
                            archivoTempCifrado.delete();
                            archivoTempRemoto.delete();
                        }
                    } else {
                        // Para archivos no .txt, comparar hashes (si es necesario)
                        String hashLocal = obtenerHashLocal(pathLocal);
                        String hashRemoto = obtenerHashRemoto(archivoLocal);

                        if (!Objects.equals(hashLocal, hashRemoto)) {
                            System.out.println("Archivo desactualizado, subiendo: " + archivoLocal);
                            agregarTareaSincronizacion(new TareaSincronizacion(this, "subir", archivoLocal, carpetaLocal));
                        } else {
                            System.out.println("Ya sincronizado: " + archivoLocal);
                        }
                    }
                } else {
                    // El archivo no existe en el servidor remoto, subirlo
                    System.out.println("Archivo no encontrado en el servidor, subiendo: " + archivoLocal);
                    agregarTareaSincronizacion(new TareaSincronizacion(this, "subir", archivoLocal, carpetaLocal));
                }
            }

            // Eliminar archivos remotos que no están en la carpeta local
            for (String archivoRemoto : archivosRemotos) {
                if (!archivosLocales.contains(archivoRemoto)) {
                    System.out.println("Archivo remoto no existe localmente, eliminando: " + archivoRemoto);
                    agregarTareaSincronizacion(new TareaSincronizacion(this, "eliminar", archivoRemoto, carpetaHistory)); // Pasar carpetaHistory
                }
            }

            // Procesar las tareas de sincronización
            procesarTareasSincronizacion();
        } catch (Exception e) {
            System.err.println("Error en la comprobación de sincronización: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        GestorFTP gestorFTP = new GestorFTP();
        try (Scanner scanner = new Scanner(System.in)) {
            String rutaPorDefecto = "C:ServidorFTP";
            System.out.println();
            int respuesta = leerIntEntre("¿Usar la carpeta por defecto? 1. Sí 2. No", 0, 2);
            String carpetaLocal;

            if (respuesta == 1) {
                carpetaLocal = rutaPorDefecto;
                System.out.println("Usando la carpeta por defecto: " + carpetaLocal);
            } else {
                System.out.println("Introduzca la ruta completa de la carpeta local:");
                scanner.nextLine();
                carpetaLocal = scanner.nextLine().trim();
                System.out.println("Usando la carpeta personalizada: " + carpetaLocal);
            }

            String carpetaRemota = "/home/ftpusers/Eduardo";
            System.out.println("Sincronización automática iniciada. Escriba 'Parar' para finalizar.");

            // Bucle principal
            boolean ejecutando = true;
            while (ejecutando) {
                gestorFTP.comprobarSincronia(carpetaLocal, carpetaRemota);

                // Verificar si el usuario ha escrito "Parar"
                if (System.in.available() > 0) {
                    String comando = scanner.nextLine().trim();
                    if (comando.equalsIgnoreCase("Parar")) {
                        ejecutando = false;
                        System.out.println("Sincronización detenida.");
                    }
                }

                // Esperar antes de la próxima sincronización
                try {
                    Thread.sleep(INTERVALO_SINCRONIZACION);
                } catch (InterruptedException e) {
                    System.err.println("Error en la espera: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error de entrada/salida: " + e.getMessage());
        }
    }
}