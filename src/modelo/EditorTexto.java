
package modelo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class EditorTexto{
    public static final long MAX_BYTES = 1024 * 1024;
    
    public String leer(File archivo) throws RutaInvalidaException, IOException{
        if(archivo == null || !archivo.isFile()){
            throw new RutaInvalidaException("Selecciona un archivo valido.");
        }
        if(archivo.length() > MAX_BYTES){
            throw new RutaInvalidaException("El archivo es demasiado grande (maximo 1 MB) para el editor.");
        }
        StringBuilder sb = new StringBuilder();
        try(FileReader lector = new FileReader(archivo)){
            char[] buffer = new char[4096];
            int leidos;
            while((leidos = lector.read(buffer)) != -1){
                sb.append(buffer, 0, leidos);
            }
        }
        if(sb.indexOf("\u0000") >= 0){
            throw new RutaInvalidaException("El archivo no parece ser de texto.");
        }
        return sb.toString();
    }

    public void guardar(File archivo, String contenido) throws IOException{
        try(FileWriter escritor = new FileWriter(archivo)){
            escritor.write(contenido);
        }
    }
}