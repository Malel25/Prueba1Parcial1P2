
package vista;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

public class VentanaPrincipal extends JFrame{
    private List<JButton> botones = new ArrayList<JButton>();
    
    private JTextField txtRuta = new JTextField(40);
    private JTextField txtBusqueda = new JTextField(12);
    private JTextField txtExtension = new JTextField(5);
    private JTextField txtMin = new JTextField(5);
    private JTextField txtMax = new JTextField(5);
    private JTextField txtDesde = new JTextField(8);
    private JTextField txtHasta = new JTextField(8);
    private JTextField txtProfundidad = new JTextField("0", 3);
    
    private JButton btnAnalizar;
    private JButton btnBuscar;
    private JButton btnDuplicados;
    private JButton btnCancelar;
    
    private JTree arbol = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("Sin datos")));
    private DefaultTableModel modeloStats;
    private JTextArea txtGlobales = new JTextArea(6, 50);
    private DefaultTableModel modeloResultados;
    private JLabel lblResultados = new JLabel(" ");
    private JTextArea txtDuplicados = new JTextArea();
    private JTextArea txtEditor = new JTextArea();
    private JLabel lblArchivoEditor = new JLabel("Ningun archivo abierto");
    private JTextArea txtBitacora = new JTextArea();
    
    private JProgressBar barra = new JProgressBar();
    private JLabel lblEstado = new JLabel("Listo.");

    public VentanaPrincipal(){
        super("Explorador, analizador y gestor de archivos");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(crearPanelEntradas(), BorderLayout.NORTH);
        add(crearPestanas(), BorderLayout.CENTER);
        add(crearPanelEstado(), BorderLayout.SOUTH);
    }

    private JButton crearBoton(String texto, String comando){
        JButton boton = new JButton(texto);
        boton.setActionCommand(comando);
        botones.add(boton);
        return boton;
    }

    private JPanel crearPanelEntradas(){
        JPanel panel = new JPanel(new GridLayout(3, 1));

        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fila1.add(new JLabel("Directorio raiz:"));
        fila1.add(txtRuta);
        fila1.add(crearBoton("Examinar...", "EXAMINAR"));

        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fila2.add(new JLabel("Texto a buscar:"));
        fila2.add(txtBusqueda);
        fila2.add(new JLabel("Extension:"));
        fila2.add(txtExtension);
        fila2.add(new JLabel("Mín (KB):"));
        fila2.add(txtMin);
        fila2.add(new JLabel("Max (KB):"));
        fila2.add(txtMax);

        JPanel fila3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fila3.add(new JLabel("Desde (aaaa-mm-dd):"));
        fila3.add(txtDesde);
        fila3.add(new JLabel("Hasta:"));
        fila3.add(txtHasta);
        fila3.add(new JLabel("Profundidad (0 = ilimitada):"));
        fila3.add(txtProfundidad);
        btnAnalizar = crearBoton("Analizar", "ANALIZAR");
        btnBuscar = crearBoton("Buscar", "BUSCAR");
        btnDuplicados = crearBoton("Duplicados", "DUPLICADOS");
        btnCancelar = crearBoton("Cancelar", "CANCELAR");
        btnCancelar.setEnabled(false);
        fila3.add(btnAnalizar);
        fila3.add(btnBuscar);
        fila3.add(btnDuplicados);
        fila3.add(btnCancelar);

        panel.add(fila1);
        panel.add(fila2);
        panel.add(fila3);
        return panel;
    }

    private JTabbedPane crearPestanas(){
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Arbol", crearPanelArbol());
        pestanas.addTab("Estadísticas", crearPanelEstadisticas());
        pestanas.addTab("Bssqueda", crearPanelBusqueda());
        pestanas.addTab("Duplicados", crearPanelDuplicados());
        pestanas.addTab("Gestion", crearPanelGestion());
        pestanas.addTab("Editor", crearPanelEditor());
        pestanas.addTab("Bitacora", crearPanelBitacora());
        return pestanas;
    }

    private JPanel crearPanelArbol(){
        arbol.setCellRenderer(new RenderizadorArbol());
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(arbol), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelEstadisticas(){
        modeloStats = new DefaultTableModel(
                new Object[]{"Categoria", "Cantidad", "Tamaño total", "% archivos", "% tamaño"}, 0){
            @Override
            public boolean isCellEditable(int fila, int columna){
                return false;
            }
        };
        txtGlobales.setEditable(false);
        txtGlobales.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(new JScrollPane(txtGlobales), BorderLayout.CENTER);
        sur.add(crearBoton("Exportar reporte", "EXPORTAR"), BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(new JTable(modeloStats)), BorderLayout.CENTER);
        panel.add(sur, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelBusqueda(){
        modeloResultados = new DefaultTableModel(
                new Object[]{"Nombre", "Ruta absoluta", "Tamaño (bytes)", "Modificado"}, 0){
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columna){
                return columna == 2 ? Long.class : String.class;
            }
        };
        JTable tabla = new JTable(modeloResultados);
        tabla.setAutoCreateRowSorter(true);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(lblResultados, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelDuplicados(){
        txtDuplicados.setEditable(false);
        txtDuplicados.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(txtDuplicados), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelGestion(){
        JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));
        panel.add(crearBoton("Crear carpeta", "CREAR_CARPETA"));
        panel.add(crearBoton("Crear archivo de texto", "CREAR_ARCHIVO"));
        panel.add(crearBoton("Renombrar", "RENOMBRAR"));
        panel.add(crearBoton("Copiar", "COPIAR"));
        panel.add(crearBoton("Mover", "MOVER"));
        panel.add(crearBoton("Eliminar", "ELIMINAR"));
        JPanel contenedor = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 40));
        contenedor.add(panel);
        return contenedor;
    }

    private JPanel crearPanelEditor(){
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(crearBoton("Abrir archivo", "ABRIR_EDITOR"));
        norte.add(crearBoton("Guardar", "GUARDAR_EDITOR"));
        norte.add(lblArchivoEditor);
        txtEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(norte, BorderLayout.NORTH);
        panel.add(new JScrollPane(txtEditor), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelBitacora(){
        txtBitacora.setEditable(false);
        txtBitacora.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(crearBoton("Actualizar bitacora", "REFRESCAR_BITACORA"));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(norte, BorderLayout.NORTH);
        panel.add(new JScrollPane(txtBitacora), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelEstado(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(lblEstado, BorderLayout.CENTER);
        panel.add(barra, BorderLayout.EAST);
        return panel;
    }
    
    public void setControlador(ActionListener listener){
        for(JButton boton : botones){
            boton.addActionListener(listener);
        }
    }

    public String getRuta(){
        return txtRuta.getText();
    }

    public void setRuta(String ruta){
        txtRuta.setText(ruta);
    }

    public String getTexto(){
        return txtBusqueda.getText();
    }

    public String getExtension(){
        return txtExtension.getText();
    }

    public String getMin(){
        return txtMin.getText();
    }

    public String getMax(){
        return txtMax.getText();
    }

    public String getDesde(){
        return txtDesde.getText();
    }

    public String getHasta(){
        return txtHasta.getText();
    }

    public String getProfundidad(){
        return txtProfundidad.getText();
    }
    
    public Map<String, String> getCampos(){
        Map<String, String> m = new HashMap<String, String>();
        m.put("ruta", txtRuta.getText());
        m.put("texto", txtBusqueda.getText());
        m.put("extension", txtExtension.getText());
        m.put("min", txtMin.getText());
        m.put("max", txtMax.getText());
        m.put("desde", txtDesde.getText());
        m.put("hasta", txtHasta.getText());
        m.put("profundidad", txtProfundidad.getText());
        return m;
    }
    
    public void setCampos(Map<String, String> m){
        txtRuta.setText(valor(m, "ruta", ""));
        txtBusqueda.setText(valor(m, "texto", ""));
        txtExtension.setText(valor(m, "extension", ""));
        txtMin.setText(valor(m, "min", ""));
        txtMax.setText(valor(m, "max", ""));
        txtDesde.setText(valor(m, "desde", ""));
        txtHasta.setText(valor(m, "hasta", ""));
        txtProfundidad.setText(valor(m, "profundidad", "0"));
    }

    private String valor(Map<String, String> m, String clave, String porDefecto){
        return m.containsKey(clave) ? m.get(clave) : porDefecto;
    }

    public void mostrarArbol(DefaultMutableTreeNode raiz){
        arbol.setModel(new DefaultTreeModel(raiz));
        arbol.expandRow(0);
    }

    public void mostrarEstadisticas(Object[][] filas, String globales){
        modeloStats.setRowCount(0);
        for(Object[] fila : filas){
            modeloStats.addRow(fila);
        }
        txtGlobales.setText(globales);
    }

    public void mostrarResultados(List<Object[]> filas){
        modeloResultados.setRowCount(0);
        for(Object[] fila : filas){
            modeloResultados.addRow(fila);
        }
        if(filas.isEmpty()){
            lblResultados.setText("No se encontraron archivos que coincidan con los criterios.");
        }else{
            lblResultados.setText(filas.size() + " archivo(s) encontrado(s).");
        }
    }

    public void mostrarDuplicados(String texto){
        txtDuplicados.setText(texto);
        txtDuplicados.setCaretPosition(0);
    }

    public void setTextoEditor(String texto, String ruta){
        txtEditor.setText(texto);
        txtEditor.setCaretPosition(0);
        lblArchivoEditor.setText(ruta);
    }

    public String getTextoEditor(){
        return txtEditor.getText();
    }

    public void setTextoBitacora(String texto){
        txtBitacora.setText(texto);
        txtBitacora.setCaretPosition(txtBitacora.getText().length());
    }

    public void setEstado(String texto){
        lblEstado.setText(texto);
    }
    
    public void setOcupado(boolean ocupado){
        btnAnalizar.setEnabled(!ocupado);
        btnBuscar.setEnabled(!ocupado);
        btnDuplicados.setEnabled(!ocupado);
        btnCancelar.setEnabled(ocupado);
        barra.setIndeterminate(ocupado);
    }
    
    public void mostrarError(String mensaje){
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void mostrarAviso(String mensaje){
        JOptionPane.showMessageDialog(this, mensaje, "Informacion", JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean confirmar(String mensaje){
        int r = JOptionPane.showConfirmDialog(this, mensaje, "Confirmacion",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return r == JOptionPane.YES_OPTION;
    }

    public String pedirTexto(String mensaje){
        return JOptionPane.showInputDialog(this, mensaje);
    }
    
    public String pedirContenido(){
        JTextArea area = new JTextArea(10, 40);
        int r = JOptionPane.showConfirmDialog(this, new JScrollPane(area),
                "Contenido del archivo", JOptionPane.OK_CANCEL_OPTION);
        return r == JOptionPane.OK_OPTION ? area.getText() : null;
    }
    
    public File elegir(File inicio, int modo, boolean guardar) {
        JFileChooser selector = new JFileChooser(inicio);
        selector.setFileSelectionMode(modo);
        int r = guardar ? selector.showSaveDialog(this) : selector.showOpenDialog(this);
        return r == JFileChooser.APPROVE_OPTION ? selector.getSelectedFile() : null;
    }

    private static class RenderizadorArbol extends DefaultTreeCellRenderer{
        @Override
        public Component getTreeCellRendererComponent(JTree arbol, Object valor, boolean seleccionado,
                boolean expandido, boolean hoja, int fila, boolean foco){
            super.getTreeCellRendererComponent(arbol, valor, seleccionado, expandido, hoja, fila, foco);
            Object dato = ((DefaultMutableTreeNode) valor).getUserObject();
            if(dato instanceof NodoArchivo){
                if(((NodoArchivo) dato).getArchivo().isDirectory()){
                    setIcon(UIManager.getIcon("FileView.directoryIcon"));
                }else{
                    setIcon(UIManager.getIcon("FileView.fileIcon"));
                }
            }
            return this;
        }
    }
}
