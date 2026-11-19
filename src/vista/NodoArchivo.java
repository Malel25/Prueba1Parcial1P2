
package vista;

import java.io.File;

public class NodoArchivo {
    private File archivo;

    public NodoArchivo(File archivo){
        this.archivo = archivo;
    }

    public File getArchivo(){
        return archivo;
    }
    
    @Override
    public String toString(){
        String nombre = archivo.getName();
        return nombre.isEmpty() ? archivo.getPath() : nombre;
    }
}
