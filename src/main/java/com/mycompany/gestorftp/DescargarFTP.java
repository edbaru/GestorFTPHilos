package com.mycompany.gestorftp;

import java.io.*;
import java.security.Key;
import java.util.*;
import org.apache.commons.net.ftp.*;

public class DescargarFTP {

    private final FTPClient clienteFTP;
    private static final String SERVIDOR = "localhost"; // Servidor FTP
    private static final int PUERTO = 21;               // Puerto FTP
    private static final String USUARIO = "Eduardo";   // Usuario FTP
    private static final String PASSWORD = "1234";     // Contraseña FTP
    private static final String passwordAES = "TresTristesTigresTraganTrigoEnUnTrigal";
    private static final Key claveAES = ManejarCifrado.obtenerClave(passwordAES, 32);

    public DescargarFTP() {
        clienteFTP = new FTPClient();
    }

    private void conectar() throws IOException {
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

    private void desconectar() throws IOException {
        if (clienteFTP.isConnected()) {
            clienteFTP.disconnect();
            System.out.println("Desconectado del servidor FTP.");
        }
    }

    private List<String> listarArchivosRemotos() throws IOException {
        FTPFile[] archivosRemotos = clienteFTP.listFiles();
        List<String> archivosRemotosList = new ArrayList<>();

        if (archivosRemotos != null) {
            for (FTPFile archivo : archivosRemotos) {
                if (archivo.isFile()) {
                    archivosRemotosList.add(archivo.getName());
                }
            }
        } else {
            System.err.println("No se pudo listar los archivos remotos.");
        }

        return archivosRemotosList;
    }

    private void descargarArchivo(String nombreArchivo, String rutaDestino) throws Exception {
        File archivoDestino = new File(rutaDestino + File.separator + nombreArchivo);

        // Verificar si el archivo es de tipo .txt.aes (cifrado)
        if (nombreArchivo.toLowerCase().endsWith(".txt")) {
            // Descargar y descifrar el archivo .txt cifrado
            File archivoTempCifrado = File.createTempFile("temp_cifrado", ".aes");
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

    public void ejecutar() {
        try (Scanner scanner = new Scanner(System.in)) {
            conectar();

            // Listar archivos remotos
            List<String> archivosRemotos = listarArchivosRemotos();
            if (archivosRemotos.isEmpty()) {
                System.out.println("No hay archivos en el servidor FTP.");
                return;
            }

            // Mostrar la lista de archivos
            System.out.println("Archivos disponibles en el servidor FTP:");
            for (int i = 0; i < archivosRemotos.size(); i++) {
                System.out.println((i + 1) + ". " + archivosRemotos.get(i));
            }

            // Pedir al usuario que seleccione un archivo
            System.out.print("Seleccione el número del archivo que desea descargar: ");
            int seleccion = scanner.nextInt();
            scanner.nextLine(); // Limpiar el buffer

            if (seleccion < 1 || seleccion > archivosRemotos.size()) {
                System.err.println("Selección inválida.");
                return;
            }

            String nombreArchivo = archivosRemotos.get(seleccion - 1);

            // Preguntar al usuario si desea usar la carpeta por defecto o una ruta personalizada
            System.out.println();
            int opcionRuta = GestorFTP.leerIntEntre("¿Usar la carpeta por defecto? 1. Sí 2. No", 0, 2);

            String rutaDestino;
            if (opcionRuta == 1) {
                // Usar la carpeta por defecto
                rutaDestino = "C:DescargasFTP";
                System.out.println("Usando la carpeta por defecto: " + rutaDestino);
            } else {
                // Pedir la ruta de destino personalizada
                System.out.print("Introduzca la ruta de destino para guardar el archivo: ");
                rutaDestino = scanner.nextLine().trim();
                System.out.println("Usando la carpeta personalizada: " + rutaDestino);
            }

            // Descargar el archivo seleccionado
            descargarArchivo(nombreArchivo, rutaDestino);

            desconectar();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        DescargarFTP descargador = new DescargarFTP();
        descargador.ejecutar();
    }
}