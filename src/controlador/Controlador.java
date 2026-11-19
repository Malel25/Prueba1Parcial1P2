
package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import modelo.Bitacora;
import modelo.Configuracion;
import modelo.DatoInvalidoException;
import modelo.EditorTexto;
import modelo.Estadisticas;
import modelo.Explorador;
import modelo.Filtros;
import modelo.OperacionesArchivo;
import modelo.ProgresoListener;
import modelo.Reporte;
import modelo.RutaInvalidaException;
import modelo.Utilidades;
import vista.NodoArchivo;
import vista.VentanaPrincipal;

public class Controlador implements ActionListener{
    private VentanaPrincipal vista = new VentanaPrincipal();
    private Bitacora bitacora = new Bitacora("bitacora.log");
    private Configuracion config = new Configuracion("config.txt");
    private Explorador explorador = new Explorador(bitacora);
    private OperacionesArchivo operaciones = new OperacionesArchivo(bitacora);
    private EditorTexto editor = new EditorTexto();
    private Reporte reporte = new Reporte();

    private Estadisticas ultimaStats;
    private File ultimaRaiz;
    private File archivoEnEditor;
    private SwingWorker<Void, Integer> tareaActual;

    public Controlador(){
        vista.setControlador(this);
        vista.setCampos(config.cargar());
        vista.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
                guardarConfig();
            }
        });
        vista.setTextoBitacora(bitacora.leer());
        vista.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e){
        try{
            String comando = e.getActionCommand();
            if(comando.equals("EXAMINAR")){
                examinar();
            }else if(comando.equals("ANALIZAR")){
                iniciarAnalisis();
            }else if(comando.equals("BUSCAR")){
                iniciarBusqueda();
            }else if(comando.equals("DUPLICADOS")){
                iniciarDuplicados();
            }else if(comando.equals("CANCELAR")){
                explorador.cancelar();
                vista.setEstado("Cancelando...");
            }else if(comando.equals("EXPORTAR")){
                exportar();
            }else if(comando.equals("CREAR_CARPETA")){
                crearCarpeta();
            }else if(comando.equals("CREAR_ARCHIVO")){
                crearArchivo();
            }else if(comando.equals("RENOMBRAR")){
                renombrar();
            }else if(comando.equals("COPIAR")){
                copiarOMover(false);
            }else if(comando.equals("MOVER")){
                copiarOMover(true);
            }else if(comando.equals("ELIMINAR")){
                eliminar();
            }else if(comando.equals("ABRIR_EDITOR")){
                abrirEditor();
            }else if(comando.equals("GUARDAR_EDITOR")){
                guardarEditor();
            }else if(comando.equals("REFRESCAR_BITACORA")){
                vista.setTextoBitacora(bitacora.leer());
            }
        }catch (RutaInvalidaException ex){
            vista.mostrarError(ex.getMessage());
        }catch (DatoInvalidoException ex){
            vista.mostrarError(ex.getMessage());
        }catch (Exception ex){
            vista.mostrarError("Ocurrio un error: " + ex.getMessage());
        }
    }
    
    private File carpetaInicial(){
        File f = new File(vista.getRuta().trim());
        return f.isDirectory() ? f : new File(System.getProperty("user.home"));
    }

    private void examinar(){
        File carpeta = vista.elegir(carpetaInicial(), JFileChooser.DIRECTORIES_ONLY, false);
        if (carpeta != null){
            vista.setRuta(carpeta.getAbsolutePath());
        }
    }

    private void guardarConfig(){
        try{
            config.guardar(vista.getCampos());
        }catch (Exception ex){
            System.err.println("No se pudo guardar config.txt: " + ex.getMessage());
        }
    }
    
    private void iniciarAnalisis() throws Exception{
        File raiz = explorador.validarRaiz(vista.getRuta());
        int prof = Filtros.parsearProfundidad(vista.getProfundidad());
        lanzarTarea("ANALIZAR", raiz, prof, null, "");
    }

    private void iniciarBusqueda() throws Exception{
        File raiz = explorador.validarRaiz(vista.getRuta());
        Filtros filtros = Filtros.crear(vista.getExtension(), vista.getMin(), vista.getMax(),
                vista.getDesde(), vista.getHasta(), vista.getProfundidad());
        String texto = vista.getTexto().trim();
        if(texto.isEmpty() && !filtros.hayFiltros()){
            throw new DatoInvalidoException("Escribe un texto de búsqueda o activa algún filtro.");
        }
        lanzarTarea("BUSCAR", raiz, filtros.getProfundidadMax(), filtros, texto);
    }

    private void iniciarDuplicados() throws Exception{
        File raiz = explorador.validarRaiz(vista.getRuta());
        int prof = Filtros.parsearProfundidad(vista.getProfundidad());
        lanzarTarea("DUPLICADOS", raiz, prof, null, "");
    }

    private void lanzarTarea(String tipo, File raiz, int prof, Filtros filtros, String texto){
        if(tareaActual != null && !tareaActual.isDone()){
            vista.mostrarAviso("Ya hay una operación en curso.");
            return;
        }
        bitacora.registrar(tipo, raiz.getAbsolutePath() + (texto.isEmpty() ? "" : " | texto=" + texto));
        explorador.reiniciar();
        vista.setOcupado(true);
        vista.setEstado("Trabajando...");
        tareaActual = new TareaRecorrido(tipo, raiz, prof, filtros, texto);
        tareaActual.execute();
    }
    
    private DefaultMutableTreeNode construirNodo(File archivo, int nivel, int maxProf){
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(new NodoArchivo(archivo));
        if(!archivo.isDirectory() || explorador.estaCancelado()){
            return nodo;
        }
        if(maxProf != 0 && nivel >= maxProf){
            return nodo;
        }
        if(nivel > 0 && Utilidades.esEnlaceSimbolico(archivo)){
            return nodo;
        }
        File[] hijos = explorador.listarSeguro(archivo, false);
        if(hijos == null){
            return nodo;
        }
        Arrays.sort(hijos);
        for(File hijo : hijos){
            if(hijo.isDirectory() && explorador.esVisible(hijo)){
                nodo.add(construirNodo(hijo, nivel + 1, maxProf));
            }
        }
        for(File hijo : hijos){
            if(!hijo.isDirectory() && explorador.esVisible(hijo)){
                nodo.add(new DefaultMutableTreeNode(new NodoArchivo(hijo)));
            }
        }
        return nodo;
    }

    private Object[][] construirFilasStats(Estadisticas e){
        Object[][] filas = new Object[Estadisticas.CATEGORIAS.length][5];
        for (int i = 0; i < filas.length; i++) {
            filas[i][0] = Estadisticas.CATEGORIAS[i];
            filas[i][1] = e.getCantidad(i);
            filas[i][2] = Utilidades.formatearTamano(e.getBytes(i));
            filas[i][3] = String.format("%.1f%%", e.getPorcentajeArchivos(i));
            filas[i][4] = String.format("%.1f%%", e.getPorcentajeBytes(i));
        }
        return filas;
    }
    
    private class TareaRecorrido extends SwingWorker<Void, Integer>{
        private String tipo;
        private File raiz;
        private int prof;
        private Filtros filtros;
        private String texto;

        private Estadisticas stats;
        private DefaultMutableTreeNode nodoRaiz;
        private List<File> encontrados;
        private Map<String, List<File>> duplicados;

        TareaRecorrido(String tipo, File raiz, int prof, Filtros filtros, String texto){
            this.tipo = tipo;
            this.raiz = raiz;
            this.prof = prof;
            this.filtros = filtros;
            this.texto = texto;
        }

        @Override
        protected Void doInBackground(){
            explorador.setListener(new ProgresoListener(){
                public void avance(int archivosProcesados){
                    publish(archivosProcesados);
                }
            });
            if(tipo.equals("ANALIZAR")){
                stats = explorador.analizar(raiz, prof);
                nodoRaiz = construirNodo(raiz, 0, prof);
            }else if (tipo.equals("BUSCAR")){
                encontrados = explorador.buscar(raiz, texto, filtros);
            }else{
                duplicados = explorador.encontrarDuplicados(raiz, prof);
            }
            return null;
        }

        @Override
        protected void process(List<Integer> avances){
            vista.setEstado("Archivos procesados: " + avances.get(avances.size() - 1));
        }

        @Override
        protected void done(){
            vista.setOcupado(false);
            if (explorador.estaCancelado()) {
                vista.setEstado("Operación cancelada.");
                bitacora.registrar("CANCELADO", tipo + " " + raiz.getAbsolutePath());
                return;
            }
            try {
                get(); 
                if (tipo.equals("ANALIZAR")) {
                    ultimaStats = stats;
                    ultimaRaiz = raiz;
                    vista.mostrarArbol(nodoRaiz);
                    vista.mostrarEstadisticas(construirFilasStats(stats), stats.resumenGlobal());
                    vista.setEstado("Análisis completado: " + stats.getTotalArchivos() + " archivos.");
                } else if (tipo.equals("BUSCAR")) {
                    List<Object[]> filas = new ArrayList<Object[]>();
                    for (File f : encontrados) {
                        filas.add(new Object[]{f.getName(), f.getAbsolutePath(),
                            Long.valueOf(f.length()), Utilidades.formatearFecha(f.lastModified())});
                    }
                    vista.mostrarResultados(filas);
                    vista.setEstado("Búsqueda completada.");
                } else {
                    vista.mostrarDuplicados(textoDuplicados(duplicados));
                    vista.setEstado("Búsqueda de duplicados completada.");
                }
            } catch (Exception ex) {
                Throwable causa = (ex.getCause() != null) ? ex.getCause() : ex;
                vista.mostrarError("Error durante la operación: " + causa.getMessage());
                vista.setEstado("Error.");
            }
            vista.setTextoBitacora(bitacora.leer());
        }
    }

    private String textoDuplicados(Map<String, List<File>> duplicados){
        if(duplicados.isEmpty()){
            return "No se encontraron archivos duplicados.";
        }
        StringBuilder sb = new StringBuilder();
        for(List<File> grupo : duplicados.values()){
            File primero = grupo.get(0);
            sb.append(primero.getName()).append("  (")
                    .append(Utilidades.formatearTamano(primero.length())).append(")  -  ")
                    .append(grupo.size()).append(" copias\n");
            for(File f : grupo){
                sb.append("    ").append(f.getAbsolutePath()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    private void exportar() throws Exception{
        if(ultimaStats == null){
            throw new DatoInvalidoException("Primero ejecuta un análisis.");
        }
        File destino = vista.elegir(new File("."), JFileChooser.FILES_ONLY, true);
        if(destino == null){
            return;
        }
        String nombre = destino.getName().toLowerCase();
        if(!nombre.endsWith(".txt") && !nombre.endsWith(".csv")){
            destino = new File(destino.getParentFile(), destino.getName() + ".txt");
        }
        if(destino.exists() && !vista.confirmar("El archivo ya existe. ¿Deseas sobrescribirlo?")){
            return;
        }
        reporte.exportar(destino, ultimaStats, ultimaRaiz.getAbsolutePath());
        bitacora.registrar("EXPORTAR", destino.getAbsolutePath());
        vista.mostrarAviso("Reporte exportado en:\n" + destino.getAbsolutePath());
    }
    
    private void crearCarpeta() throws Exception{
        File padre = vista.elegir(carpetaInicial(), JFileChooser.DIRECTORIES_ONLY, false);
        if(padre == null){
            return;
        }
        String nombre = vista.pedirTexto("Nombre de la carpeta (para anidadas usa a/b/c):");
        if(nombre == null){
            return;
        }
        File creada = operaciones.crearCarpeta(padre, nombre);
        vista.mostrarAviso("Carpeta creada:\n" + creada.getAbsolutePath());
    }

    private void crearArchivo() throws Exception{
        File carpeta = vista.elegir(carpetaInicial(), JFileChooser.DIRECTORIES_ONLY, false);
        if(carpeta == null){
            return;
        }
        String nombre = vista.pedirTexto("Nombre del archivo (ej. notas.txt):");
        if(nombre == null){
            return;
        }
        String contenido = vista.pedirContenido();
        if(contenido == null){
            return;
        }
        boolean sobrescribir = false;
        if(new File(carpeta, nombre.trim()).exists()){
            if(!vista.confirmar("El archivo ya existe. ¿Desea sobrescribirlo?")){
                return;
            }
            sobrescribir = true;
        }
        File creado = operaciones.crearArchivoTexto(carpeta, nombre, contenido, sobrescribir);
        vista.mostrarAviso("Archivo creado:\n" + creado.getAbsolutePath());
    }

    private void renombrar() throws Exception{
        File origen = vista.elegir(carpetaInicial(), JFileChooser.FILES_AND_DIRECTORIES, false);
        if(origen == null){
            return;
        }
        String nuevo = vista.pedirTexto("Nuevo nombre para \"" + origen.getName() + "\":");
        if(nuevo == null){
            return;
        }
        File renombrado = operaciones.renombrar(origen, nuevo);
        vista.mostrarAviso("Renombrado a:\n" + renombrado.getAbsolutePath());
    }

    private void copiarOMover(boolean mover) throws Exception{
        int modoOrigen = mover ? JFileChooser.FILES_AND_DIRECTORIES : JFileChooser.FILES_ONLY;
        File origen = vista.elegir(carpetaInicial(), modoOrigen, false);
        if(origen == null){
            return;
        }
        File destino = vista.elegir(carpetaInicial(), JFileChooser.DIRECTORIES_ONLY, false);
        if(destino == null){
            return;
        }
        boolean sobrescribir = false;
        if(new File(destino, origen.getName()).exists()){
            if(!vista.confirmar("En el destino ya existe \"" + origen.getName() + "\". ¿Desea sobrescribirlo?")){
                return;
            }
            sobrescribir = true;
        }
        File resultado;
        if(mover){
            resultado = operaciones.mover(origen, destino, sobrescribir);
        }else{
            resultado = operaciones.copiar(origen, destino, sobrescribir);
        }
        vista.mostrarAviso("Operacion realizada:\n" + resultado.getAbsolutePath());
    }

    private void eliminar() throws Exception{
        File objetivo = vista.elegir(carpetaInicial(), JFileChooser.FILES_AND_DIRECTORIES, false);
        if(objetivo == null){
            return;
        }
        if(!vista.confirmar("¿Seguro que deseas eliminar:\n" + objetivo.getAbsolutePath()
                + "\n\nEsta acción no se puede deshacer.")) {
            return;
        }
        operaciones.eliminar(objetivo);
        vista.mostrarAviso("Eliminado correctamente.");
    }
    
    private void abrirEditor() throws Exception{
        File archivo = vista.elegir(carpetaInicial(), JFileChooser.FILES_ONLY, false);
        if (archivo == null) {
            return;
        }
        String contenido = editor.leer(archivo);
        archivoEnEditor = archivo;
        vista.setTextoEditor(contenido, archivo.getAbsolutePath());
        bitacora.registrar("ABRIR", archivo.getAbsolutePath());
    }
    
    private void guardarEditor() throws Exception{
        if(archivoEnEditor == null){
            throw new DatoInvalidoException("Primero abre un archivo.");
        }
        if(!vista.confirmar("Se sobrescribirá:\n" + archivoEnEditor.getAbsolutePath() + "\n¿Continuar?")){
            return;
        }
        editor.guardar(archivoEnEditor, vista.getTextoEditor());
        bitacora.registrar("GUARDAR", archivoEnEditor.getAbsolutePath());
        vista.mostrarAviso("Archivo guardado.");
    }
}
