package com.mycompany.gestorftp;

import java.io.File;

public class TareaSincronizacion {
    private final GestorFTP gestorFTP;
    private final String accion;
    private final String archivo;
    private final String ruta;

    public TareaSincronizacion(GestorFTP gestorFTP, String accion, String archivo, String ruta) {
        this.gestorFTP = gestorFTP;
        this.accion = accion;
        this.archivo = archivo;
        this.ruta = ruta;
    }

    /**
     * Ejecuta la tarea de sincronización según la acción especificada.
     * @throws java.lang.Exception
     */
    public void ejecutar() throws Exception {
        switch (accion) {
            case "subir" -> gestorFTP.subirFichero(ruta + File.separator + archivo, archivo.toLowerCase().endsWith(".txt"));
            case "descargar" -> gestorFTP.descargarArchivo(archivo, ruta);
            case "eliminar" -> gestorFTP.eliminarFicheroRemoto(archivo, ruta);
            default -> System.err.println("Acción no válida: " + accion);
        }
    }
}