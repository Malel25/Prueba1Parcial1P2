
package modelo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class Reporte {
     public void exportar(File destino, Estadisticas e, String raiz) throws IOException{
        boolean csv = destino.getName().toLowerCase().endsWith(".csv");
        String salto = System.lineSeparator();
        String ahora = Utilidades.formatearFecha(System.currentTimeMillis());

        try(FileWriter escritor = new FileWriter(destino)){
            if(csv){
                escritor.write("fecha,directorio" + salto);
                escritor.write(ahora + ",\"" + raiz + "\"" + salto);
                escritor.write("categoria,cantidad,bytes,porcentaje_archivos,porcentaje_tamano" + salto);
                for(int i = 0; i < Estadisticas.CATEGORIAS.length; i++){
                    escritor.write(String.format(Locale.US, "%s,%d,%d,%.2f,%.2f%s",
                            Estadisticas.CATEGORIAS[i], e.getCantidad(i), e.getBytes(i),
                            e.getPorcentajeArchivos(i), e.getPorcentajeBytes(i), salto));
                }
            }else{
                escritor.write("REPORTE DE ESTADÍSTICAS" + salto);
                escritor.write("Fecha y hora: " + ahora + salto);
                escritor.write("Directorio: " + raiz + salto + salto);
                for (int i = 0; i < Estadisticas.CATEGORIAS.length; i++) {
                    escritor.write(String.format("%-7s %5d archivos  %12s  %6.1f%% archivos  %6.1f%% tamaño%s",
                            Estadisticas.CATEGORIAS[i], e.getCantidad(i),
                            Utilidades.formatearTamano(e.getBytes(i)),
                            e.getPorcentajeArchivos(i), e.getPorcentajeBytes(i), salto));
                }
                escritor.write(salto + e.resumenGlobal() + salto);
            }
        }
    }
}
