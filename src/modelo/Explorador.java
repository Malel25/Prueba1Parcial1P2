
package modelo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Explorador{
    public static final boolean INCLUIR_OCULTOS = false;

    private volatile boolean cancelado = false;
    private ProgresoListener listener;
    private int procesados = 0;
    private Bitacora bitacora;

    public Explorador(Bitacora bitacora){
        this.bitacora = bitacora;
    }

    public void setListener(ProgresoListener listener){
        this.listener = listener;
    }
    
    public void cancelar(){
        cancelado = true;
    }

    public void reiniciar(){
        cancelado = false;
        procesados = 0;
    }

    public boolean estaCancelado(){
        return cancelado;
    }
    
    public File validarRaiz(String ruta) throws RutaInvalidaException{
        if (ruta == null || ruta.trim().isEmpty()){
            throw new RutaInvalidaException("La ruta no puede estar vacia.");
        }
        File raiz = new File(ruta.trim());
        if(!raiz.exists()){
            throw new RutaInvalidaException("La ruta no existe: " + ruta.trim());
        }
        if(!raiz.isDirectory()){
            throw new RutaInvalidaException("La ruta no es un directorio.");
        }
        return raiz;
    }
    
    public File[] listarSeguro(File carpeta, boolean registrar){
        File[] hijos = carpeta.listFiles();
        if(hijos == null && registrar){
            bitacora.registrar("OMITIDA", "Sin permiso: " + carpeta.getAbsolutePath());
        }
        return hijos;
    }
    
    public boolean esVisible(File archivo){
        return INCLUIR_OCULTOS || !archivo.isHidden();
    }
    
    public Estadisticas analizar(File raiz, int maxProf){
        Estadisticas e = new Estadisticas();
        analizarCarpeta(raiz, 0, maxProf, e);
        return e;
    }

    private void analizarCarpeta(File carpeta, int nivel, int maxProf, Estadisticas e){
        if(cancelado){
            return;
        }
        File[] hijos = listarSeguro(carpeta, true);
        if(hijos == null){
            e.registrarCarpeta(nivel, false);
            return;
        }
        e.registrarCarpeta(nivel, hijos.length == 0);
        for(File hijo : hijos){
            if(cancelado){
                return;
            }
            if(!esVisible(hijo)){
                continue;
            }
            if(hijo.isDirectory()){
                if(Utilidades.esEnlaceSimbolico(hijo)){
                    bitacora.registrar("OMITIDA", "Enlace simbolico: " + hijo.getAbsolutePath());
                }else if(maxProf == 0 || nivel + 1 <= maxProf){
                    analizarCarpeta(hijo, nivel + 1, maxProf, e);
                }
            } else if(hijo.isFile()){
                e.registrarArchivo(hijo);
                avanzar();
            }
        }
    }
    
    private void recolectar(File carpeta, int nivel, int maxProf, List<File> destino){
        if(cancelado){
            return;
        }
        File[] hijos = listarSeguro(carpeta, true);
        if(hijos == null){
            return;
        }
        for(File hijo : hijos){
            if (cancelado){
                return;
            }
            if(!esVisible(hijo)){
                continue;
            }
            if(hijo.isDirectory()){
                if(Utilidades.esEnlaceSimbolico(hijo)){
                    bitacora.registrar("OMITIDA", "Enlace simbolico: " + hijo.getAbsolutePath());
                }else if (maxProf == 0 || nivel + 1 <= maxProf){
                    recolectar(hijo, nivel + 1, maxProf, destino);
                }
            }else if (hijo.isFile()){
                destino.add(hijo);
                avanzar();
            }
        }
    }

    private void avanzar(){
        procesados++;
        if(listener != null && procesados % 25 == 0){
            listener.avance(procesados);
        }
    }
    
    public List<File> buscar(File raiz, String texto, Filtros filtros) {
        List<File> todos = new ArrayList<File>();
        recolectar(raiz, 0, filtros.getProfundidadMax(), todos);
        String buscado = texto.trim().toLowerCase();
        List<File> resultado = new ArrayList<File>();
        for(File f : todos){
            if(cancelado){
                break;
            }
            if(f.getName().toLowerCase().contains(buscado) && filtros.cumple(f)){
                resultado.add(f);
            }
        }
        return resultado;
    }
    
    public Map<String, List<File>> encontrarDuplicados(File raiz, int maxProf){
        List<File> todos = new ArrayList<File>();
        recolectar(raiz, 0, maxProf, todos);

        Map<String, List<File>> grupos = new HashMap<String, List<File>>();
        for(File f : todos){
            String clave = f.getName() + "|" + f.length();
            List<File> lista = grupos.get(clave);
            if(lista == null){
                lista = new ArrayList<File>();
                grupos.put(clave, lista);
            }
            lista.add(f);
        }

        Map<String, List<File>> duplicados = new TreeMap<String, List<File>>();
        for(Map.Entry<String, List<File>> entrada : grupos.entrySet()){
            if (entrada.getValue().size() > 1) {
                duplicados.put(entrada.getKey(), entrada.getValue());
            }
        }
        return duplicados;
    }
}
