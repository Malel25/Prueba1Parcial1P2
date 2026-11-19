
package modelo;

import java.io.File;

public class Estadisticas {
    public static final String[] CATEGORIAS = {".txt", ".java", ".pdf", "otros"};

    private int[] cantidad = new int[CATEGORIAS.length];
    private long[] bytes = new long[CATEGORIAS.length];
    private File masGrande;
    private File masReciente;
    private int profundidadMaxima = 0;
    private int totalCarpetas = 0;
    private int carpetasVacias = 0;

    
    public void registrarArchivo(File archivo){
        int i = indiceCategoria(archivo);
        cantidad[i]++;
        bytes[i] += archivo.length();
        if(masGrande == null || archivo.length() > masGrande.length()){
            masGrande = archivo;
        }
        if(masReciente == null || archivo.lastModified() > masReciente.lastModified()){
            masReciente = archivo;
        }
    }
    
    public void registrarCarpeta(int nivel, boolean vacia){
        totalCarpetas++;
        if(vacia){
            carpetasVacias++;
        }
        if(nivel > profundidadMaxima){
            profundidadMaxima = nivel;
        }
    }

    private int indiceCategoria(File archivo){
        String ext = "." + Utilidades.obtenerExtension(archivo);
        for (int i = 0; i < CATEGORIAS.length - 1; i++) {
            if (CATEGORIAS[i].equals(ext)) {
                return i;
            }
        }
        return CATEGORIAS.length - 1; 
    }

    public int getCantidad(int categoria){
        return cantidad[categoria];
    }

    public long getBytes(int categoria){
        return bytes[categoria];
    }

    public int getTotalArchivos(){
        int total = 0;
        for (int c : cantidad){
            total += c;
        }
        return total;
    }

    public long getTotalBytes(){
        long total = 0;
        for (long b : bytes){
            total += b;
        }
        return total;
    }

    public double getPorcentajeArchivos(int categoria){
        int total = getTotalArchivos();
        return total == 0 ? 0 : 100.0 * cantidad[categoria] / total;
    }

    public double getPorcentajeBytes(int categoria){
        long total = getTotalBytes();
        return total == 0 ? 0 : 100.0 * bytes[categoria] / total;
    }
    
    public String resumenGlobal(){
        StringBuilder sb = new StringBuilder();
        sb.append("Total: ").append(getTotalArchivos()).append(" archivos, ")
                .append(Utilidades.formatearTamano(getTotalBytes())).append("\n");
        if(masGrande != null){
            sb.append("Archivo mas grande: ").append(masGrande.getAbsolutePath())
                    .append(" (").append(Utilidades.formatearTamano(masGrande.length())).append(")\n");
        }
        if(masReciente != null){
            sb.append("Archivo mas reciente: ").append(masReciente.getAbsolutePath())
                    .append(" (").append(Utilidades.formatearFecha(masReciente.lastModified())).append(")\n");
        }
        sb.append("Profundidad maxima alcanzada: ").append(profundidadMaxima).append("\n");
        sb.append("Carpetas: ").append(totalCarpetas)
                .append(" | Carpetas vacías: ").append(carpetasVacias);
        return sb.toString();
    }
}
