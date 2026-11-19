
package modelo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Bitacora {
    private File archivo;

    public Bitacora(String nombreArchivo){
        this.archivo = new File(nombreArchivo);
    }
    
    public synchronized void registrar(String accion, String ruta){
        String linea = Utilidades.formatearFecha(System.currentTimeMillis())
                + " | " + accion + " | " + ruta;
        try(FileWriter escritor = new FileWriter(archivo, true)){
            escritor.write(linea + System.lineSeparator());
        }catch(IOException e){
            System.err.println("No se pudo escribir en la bitacora: " + e.getMessage());
        }
    }
    
    public synchronized String leer(){
        if(!archivo.exists()){
            return "(La bitacora esta vacía)";
        }
        StringBuilder sb = new StringBuilder();
        try(BufferedReader lector = new BufferedReader(new FileReader(archivo))){
            String linea;
            while((linea = lector.readLine()) != null){
                sb.append(linea).append("\n");
            }
        }catch (IOException e){
            return "No se pudo leer la bitacora: " + e.getMessage();
        }
        return sb.toString();
    }
}
