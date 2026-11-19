
package modelo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class OperacionesArchivo {
    private Bitacora bitacora;

    public OperacionesArchivo(Bitacora bitacora) {
        this.bitacora = bitacora;
    }

    
    public void validarNombre(String nombre) throws RutaInvalidaException {
        if(nombre == null || nombre.trim().isEmpty()){
            throw new RutaInvalidaException("El nombre no puede estar vacio.");
        }
        if(nombre.length() > 255){
            throw new RutaInvalidaException("El nombre es demasiado largo.");
        }
        String invalidos = "\\/:*?\"<>|";
        for(int i = 0; i < nombre.length(); i++){
            if(invalidos.indexOf(nombre.charAt(i)) >= 0){
                throw new RutaInvalidaException("El nombre tiene un caracter no permitido: " + nombre.charAt(i));
            }
        }
        if(nombre.endsWith(" ") || nombre.endsWith(".")){
            throw new RutaInvalidaException("El nombre no puede terminar en espacio o punto.");
        }
    }

    private void verificarCarpeta(File carpeta) throws RutaInvalidaException{
        if (carpeta == null || !carpeta.isDirectory()) {
            throw new RutaInvalidaException("La carpeta indicada no es válida.");
        }
    }
    
    public File crearCarpeta(File padre, String ruta) throws RutaInvalidaException{
        verificarCarpeta(padre);
        if (ruta == null) {
            throw new RutaInvalidaException("El nombre no puede estar vacío.");
        }
        String limpia = ruta.trim();
        String[] partes = limpia.split("[/\\\\]");
        for (String parte : partes) {
            validarNombre(parte);
        }
        File nueva = new File(padre, limpia);
        if (nueva.exists()) {
            throw new RutaInvalidaException("Ya existe: " + nueva.getAbsolutePath());
        }
        if (!nueva.mkdirs()) {
            throw new RutaInvalidaException("No se pudo crear la carpeta.");
        }
        bitacora.registrar("CREAR_CARPETA", nueva.getAbsolutePath());
        return nueva;
    }
    
    public File crearArchivoTexto(File carpeta, String nombre, String contenido, boolean sobrescribir)
            throws RutaInvalidaException, IOException{
        verificarCarpeta(carpeta);
        validarNombre(nombre);
        File archivo = new File(carpeta, nombre.trim());
        if(archivo.exists() && !sobrescribir){
            throw new RutaInvalidaException("Ya existe: " + archivo.getAbsolutePath());
        }
        try(FileWriter escritor = new FileWriter(archivo)){
            escritor.write(contenido);
        }
        bitacora.registrar("CREAR_ARCHIVO", archivo.getAbsolutePath());
        return archivo;
    }
    
    public File renombrar(File origen, String nuevoNombre) throws RutaInvalidaException{
        if(origen == null || !origen.exists()){
            throw new RutaInvalidaException("El elemento a renombrar no existe.");
        }
        validarNombre(nuevoNombre);
        File destino = new File(origen.getParentFile(), nuevoNombre.trim());
        if(destino.exists()){
            throw new RutaInvalidaException("Ya existe un elemento con ese nombre.");
        }
        if(!origen.renameTo(destino)){
            throw new RutaInvalidaException("No se pudo renombrar.");
        }
        bitacora.registrar("RENOMBRAR", origen.getAbsolutePath() + " -> " + destino.getAbsolutePath());
        return destino;
    }
    
    public File copiar(File origen, File carpetaDestino, boolean sobrescribir)
            throws RutaInvalidaException, IOException{
        if(origen == null || !origen.isFile()){
            throw new RutaInvalidaException("Solo se pueden copiar archivos (no carpetas).");
        }
        verificarCarpeta(carpetaDestino);
        File destino = new File(carpetaDestino, origen.getName());
        if(destino.getCanonicalPath().equals(origen.getCanonicalPath())){
            throw new RutaInvalidaException("El origen y el destino son el mismo archivo.");
        }
        if(destino.exists() && !sobrescribir){
            throw new RutaInvalidaException("Ya existe en el destino: " + destino.getAbsolutePath());
        }
        try (FileInputStream entrada = new FileInputStream(origen);
                FileOutputStream salida = new FileOutputStream(destino)) {
            byte[] buffer = new byte[4096];
            int leidos;
            while ((leidos = entrada.read(buffer)) != -1) {
                salida.write(buffer, 0, leidos);
            }
        }
        bitacora.registrar("COPIAR", origen.getAbsolutePath() + " -> " + destino.getAbsolutePath());
        return destino;
    }
    
    public File mover(File origen, File carpetaDestino, boolean sobrescribir)
            throws RutaInvalidaException, IOException{
        if(origen == null || !origen.exists()){
            throw new RutaInvalidaException("El elemento a mover no existe.");
        }
        verificarCarpeta(carpetaDestino);
        File destino = new File(carpetaDestino, origen.getName());
        if (destino.getCanonicalPath().equals(origen.getCanonicalPath())){
            throw new RutaInvalidaException("El origen y el destino son el mismo elemento.");
        }
        if (destino.exists()){
            if(!sobrescribir){
                throw new RutaInvalidaException("Ya existe en el destino: " + destino.getAbsolutePath());
            }
            eliminarRecursivo(destino);
        }
        if(!origen.renameTo(destino)){
            if (origen.isFile()){
                copiar(origen, carpetaDestino, true);
                if (!origen.delete()){
                    throw new RutaInvalidaException("Se copio, pero no se pudo borrar el original.");
                }
            }else{
                throw new RutaInvalidaException("No se pudo mover la carpeta.");
            }
        }
        bitacora.registrar("MOVER", origen.getAbsolutePath() + " -> " + destino.getAbsolutePath());
        return destino;
    }
    
    public void eliminar(File objetivo) throws RutaInvalidaException {
        if(objetivo == null || !objetivo.exists()){
            throw new RutaInvalidaException("El elemento a eliminar no existe.");
        }
        eliminarRecursivo(objetivo);
        bitacora.registrar("BORRAR", objetivo.getAbsolutePath());
    }

    private void eliminarRecursivo(File f) throws RutaInvalidaException{
        if(f.isDirectory() && !Utilidades.esEnlaceSimbolico(f)){
            File[] hijos = f.listFiles();
            if(hijos == null){
                throw new RutaInvalidaException("Sin permiso para leer: " + f.getAbsolutePath());
            }
            for(File hijo : hijos){
                eliminarRecursivo(hijo);
            }
        }
        if(!f.delete()){
            throw new RutaInvalidaException("No se pudo eliminar: " + f.getAbsolutePath());
        }
    }
}