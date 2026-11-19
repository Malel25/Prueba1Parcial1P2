
package modelo;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public class Filtros {
    private String extension = "";
    private Long minKB = null;
    private Long maxKB = null;
    private Long fechaDesde = null;
    private Long fechaHasta = null;
    private int profundidadMax = 0;
    
    public static Filtros crear(String ext, String min, String max,
            String desde, String hasta, String prof) throws DatoInvalidoException{
        Filtros f = new Filtros();
        f.extension = normalizarExtension(ext);
        f.minKB = parsearTamano(min, "Tamaño mínimo");
        f.maxKB = parsearTamano(max, "Tamaño maximo");
        f.fechaDesde = parsearFecha(desde, false, "Fecha desde");
        f.fechaHasta = parsearFecha(hasta, true, "Fecha hasta");
        f.profundidadMax = parsearProfundidad(prof);
        f.validar();
        return f;
    }
    
    public static int parsearProfundidad(String texto) throws DatoInvalidoException{
        String t = (texto == null) ? "" : texto.trim();
        if(t.isEmpty()){
            return 0;
        }
        try{
            int p = Integer.parseInt(t);
            if(p < 0){
                throw new DatoInvalidoException("La profundidad no puede ser negativa.");
            }
            return p;
        }catch (NumberFormatException e){
            throw new DatoInvalidoException("La profundidad debe ser un numero entero.");
        }
    }

    private static String normalizarExtension(String texto) throws DatoInvalidoException{
        String t = (texto == null) ? "" : texto.trim().toLowerCase();
        if(t.startsWith(".")){
            t = t.substring(1);
        }
        if(t.contains(".") || t.contains("/") || t.contains("\\")){
            throw new DatoInvalidoException("La extension no es valida...");
        }
        return t;
    }

    private static Long parsearTamano(String texto, String campo) throws DatoInvalidoException{
        String t = (texto == null) ? "" : texto.trim();
        if(t.isEmpty()){
            return null;
        }
        try{
            long valor = Long.parseLong(t);
            if(valor < 0){
                throw new DatoInvalidoException(campo + " no puede ser negativo...");
            }
            return Long.valueOf(valor);
        } catch (NumberFormatException e){
            throw new DatoInvalidoException(campo + " debe ser un número entero...");
        }
    }

    private static Long parsearFecha(String texto, boolean finDelDia, String campo)
            throws DatoInvalidoException{
        String t = (texto == null) ? "" : texto.trim();
        if(t.isEmpty()){
            return null;
        }
        try{
            LocalDate dia = LocalDate.parse(t); 
            ZoneId zona = ZoneId.systemDefault();
            long inicio = dia.atStartOfDay(zona).toInstant().toEpochMilli();
            if (finDelDia) {
                return Long.valueOf(dia.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli() - 1);
            }
            return Long.valueOf(inicio);
        }catch (DateTimeParseException e){
            throw new DatoInvalidoException(campo + " debe tener el formato aaaa-mm-dd.");
        }
    }

    public void validar() throws DatoInvalidoException{
        if (minKB != null && maxKB != null && minKB > maxKB){
            throw new DatoInvalidoException("El tamaño mínimo no puede ser mayor que el máximo.");
        }
        if(fechaDesde != null && fechaHasta != null && fechaDesde > fechaHasta){
            throw new DatoInvalidoException("La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
    }

    public boolean cumple(File archivo){
        if(!extension.isEmpty() && !Utilidades.obtenerExtension(archivo).equals(extension)){
            return false;
        }
        long bytes = archivo.length();
        if(minKB != null && bytes < minKB * 1024){
            return false;
        }
        if(maxKB != null && bytes > maxKB * 1024){
            return false;
        }
        long fecha = archivo.lastModified();
        if(fechaDesde != null && fecha < fechaDesde){
            return false;
        }
        if(fechaHasta != null && fecha > fechaHasta){
            return false;
        }
        return true;
    }

    public boolean hayFiltros(){
        return !extension.isEmpty() || minKB != null || maxKB != null
                || fechaDesde != null || fechaHasta != null;
    }

    public int getProfundidadMax(){
        return profundidadMax;
    }
}
