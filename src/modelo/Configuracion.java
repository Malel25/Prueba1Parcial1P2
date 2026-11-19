
package modelo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Configuracion {
     private File archivo;

    public Configuracion(String nombreArchivo){
        this.archivo = new File(nombreArchivo);
    }

    public void guardar(Map<String, String> datos) throws IOException{
        try(FileWriter escritor = new FileWriter(archivo)){
            for(Map.Entry<String, String> entrada : datos.entrySet()){
                escritor.write(entrada.getKey() + "=" + entrada.getValue() + System.lineSeparator());
            }
        }
    }
    
    public Map<String, String> cargar(){
        Map<String, String> datos = new HashMap<String, String>();
        if(!archivo.exists()){
            return datos;
        }
        try(BufferedReader lector = new BufferedReader(new FileReader(archivo))){
            String linea;
            while((linea = lector.readLine()) != null){
                int igual = linea.indexOf('=');
                if (igual > 0){
                    datos.put(linea.substring(0, igual), linea.substring(igual + 1));
                }
            }
        } catch (IOException e){
        }
        return datos;
    }
}
