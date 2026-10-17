import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Main extends JFrame {
    private final Gestor gestor = new Gestor();
    private final DefaultTableModel modeloTractores = new DefaultTableModel(
            new Object[]{"ID", "Tractor", "Prioridad", "Tiempo restante", "Horas uso", "Combustible (L)", "Mantenimiento", "Estado"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final DefaultTableModel modeloHistorial = new DefaultTableModel(
            new Object[]{"Fecha y hora", "Evento", "Detalle"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tablaTractores = new JTable(modeloTractores);
    private final JTable tablaHistorial = new JTable(modeloHistorial);
    private final JTextArea areaResumen = new JTextArea(7, 35);
    private final JTextField campoNombre = new JTextField(12);
    private final JTextField campoTiempo = new JTextField("10", 5);
    private final JTextField campoPrioridad = new JTextField("2", 5);
    private final JTextField campoCombustible = new JTextField("3.5", 5);
    private final JTextField campoUmbral = new JTextField("20", 5);
    private final JTextField campoQuantum = new JTextField("3", 5);
    private final JLabel etiquetaEstado = new JLabel("Sistema listo.");

    public Main() {
        super("Gestor de Planificacion de Maquinaria Agricola");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 740);
        setLocationRelativeTo(null);
        construirInterfaz();
        agregarEjemplos();
        actualizarVista();
    }

    private void construirInterfaz() {
        JPanel principal = new JPanel(new BorderLayout(10, 10));
        principal.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        principal.setBackground(new Color(242, 247, 242));

        JLabel titulo = new JLabel("GESTOR DE PLANIFICACION DE MAQUINARIA AGRICOLA");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(new Color(32, 87, 58));
        principal.add(titulo, BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Maquinaria y planificacion", crearPanelMaquinaria());
        pestanas.addTab("Historial y estadisticas", crearPanelEstadisticas());
        principal.add(pestanas, BorderLayout.CENTER);
        etiquetaEstado.setBorder(BorderFactory.createEmptyBorder(5, 4, 0, 4));
        principal.add(etiquetaEstado, BorderLayout.SOUTH);
        setContentPane(principal);
    }

    private JPanel crearPanelMaquinaria() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createTitledBorder("Registrar maquinaria / configurar simulacion"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 5, 4, 5);
        c.anchor = GridBagConstraints.WEST;

        agregarCampo(formulario, c, 0, 0, "Nombre del tractor:", campoNombre);
        agregarCampo(formulario, c, 2, 0, "Tiempo de trabajo (min):", campoTiempo);
        agregarCampo(formulario, c, 0, 1, "Prioridad (1 alta, 3 baja):", campoPrioridad);
        agregarCampo(formulario, c, 2, 1, "Consumo (L/h):", campoCombustible);
        agregarCampo(formulario, c, 0, 2, "Umbral mantenimiento (h):", campoUmbral);
        agregarCampo(formulario, c, 2, 2, "Quantum Round Robin (min):", campoQuantum);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAgregar = new JButton("Agregar tractor");
        JButton btnPaso = new JButton("Ejecutar un turno");
        JButton btnCompleto = new JButton("Ejecutar planificacion");
        JButton btnMantenimiento = new JButton("Registrar mantenimiento");
        JButton btnEliminar = new JButton("Retirar tractor seleccionado");
        JButton btnRefrescar = new JButton("Actualizar");
        botones.add(btnAgregar);
        botones.add(btnPaso);
        botones.add(btnCompleto);
        botones.add(btnMantenimiento);
        botones.add(btnEliminar);
        botones.add(btnRefrescar);

        btnAgregar.addActionListener(e -> agregarTractor());
        btnPaso.addActionListener(e -> ejecutarTurno());
        btnCompleto.addActionListener(e -> ejecutarPlanificacion());
        btnMantenimiento.addActionListener(e -> registrarMantenimiento());
        btnEliminar.addActionListener(e -> retirarTractor());
        btnRefrescar.addActionListener(e -> actualizarVista());

        tablaTractores.setRowHeight(24);
        tablaTractores.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(tablaTractores);
        scroll.setBorder(BorderFactory.createTitledBorder("Inventario y estado de la maquinaria"));

        JTextArea ayuda = new JTextArea(
                "Instrucciones:\n" +
                "1. Registre cada tractor con su tiempo de trabajo, prioridad y consumo.\n" +
                "2. La prioridad 1 se atiende antes que la 2 y la 3. Dentro de cada nivel se usa Round Robin.\n" +
                "3. Cada turno ejecuta hasta el quantum configurado o el tiempo restante.\n" +
                "4. El sistema calcula horas de uso, combustible, tiempos muertos y alertas de mantenimiento.\n" +
                "5. El boton de planificacion ejecuta turnos hasta terminar los trabajos pendientes.");
        ayuda.setEditable(false);
        ayuda.setLineWrap(true);
        ayuda.setWrapStyleWord(true);
        ayuda.setBackground(new Color(250, 252, 248));
        ayuda.setBorder(BorderFactory.createTitledBorder("Como funciona"));
        ayuda.setRows(5);

        panel.add(formulario, BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout(5, 5));
        centro.add(botones, BorderLayout.NORTH);
        centro.add(scroll, BorderLayout.CENTER);
        centro.add(ayuda, BorderLayout.SOUTH);
        panel.add(centro, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelEstadisticas() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        areaResumen.setEditable(false);
        areaResumen.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        areaResumen.setLineWrap(true);
        areaResumen.setWrapStyleWord(true);
        JScrollPane resumenScroll = new JScrollPane(areaResumen);
        resumenScroll.setBorder(BorderFactory.createTitledBorder("Indicadores matematicos y resumen"));
        JScrollPane historialScroll = new JScrollPane(tablaHistorial);
        historialScroll.setBorder(BorderFactory.createTitledBorder("Historial de eventos"));
        JButton btnEstadisticas = new JButton("Calcular estadisticas");
        btnEstadisticas.addActionListener(e -> actualizarVista());
        panel.add(resumenScroll, BorderLayout.NORTH);
        panel.add(historialScroll, BorderLayout.CENTER);
        panel.add(btnEstadisticas, BorderLayout.SOUTH);
        return panel;
    }

    private void agregarCampo(JPanel panel, GridBagConstraints c, int x, int y, String texto, JComponent campo) {
        c.gridx = x; c.gridy = y; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(texto), c);
        c.gridx = x + 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(campo, c);
    }

    private void agregarEjemplos() {
        gestor.agregarTractor("Tractor T-01", 18, 1, 4.0, 20);
        gestor.agregarTractor("Tractor T-02", 25, 2, 3.2, 20);
        gestor.agregarTractor("Tractor T-03", 12, 3, 2.8, 20);
    }

    private void agregarTractor() {
        try {
            String nombre = campoNombre.getText().trim();
            int tiempo = Integer.parseInt(campoTiempo.getText().trim());
            int prioridad = Integer.parseInt(campoPrioridad.getText().trim());
            double consumo = Double.parseDouble(campoCombustible.getText().trim());
            double umbral = Double.parseDouble(campoUmbral.getText().trim());
            if (nombre.isEmpty()) throw new IllegalArgumentException("Escriba el nombre del tractor.");
            if (tiempo <= 0) throw new IllegalArgumentException("El tiempo debe ser mayor que cero.");
            if (prioridad < 1 || prioridad > 3) throw new IllegalArgumentException("La prioridad debe ser 1, 2 o 3.");
            if (consumo < 0 || umbral <= 0) throw new IllegalArgumentException("Consumo no valido o umbral incorrecto.");
            gestor.agregarTractor(nombre, tiempo, prioridad, consumo, umbral);
            campoNombre.setText("");
            actualizarVista();
            estado("Tractor registrado correctamente.");
        } catch (NumberFormatException ex) {
            mostrarError("Revise los campos numericos. Use punto para decimales, por ejemplo 3.5.");
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private int obtenerQuantum() {
        int quantum = Integer.parseInt(campoQuantum.getText().trim());
        if (quantum <= 0) throw new IllegalArgumentException("El quantum debe ser mayor que cero.");
        return quantum;
    }

    private void ejecutarTurno() {
        try {
            boolean huboTrabajo = gestor.ejecutarTurno(obtenerQuantum());
            actualizarVista();
            estado(huboTrabajo ? "Turno ejecutado." : "No hay trabajos pendientes.");
        } catch (RuntimeException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void ejecutarPlanificacion() {
        try {
            int quantum = obtenerQuantum();
            int turnos = 0;
            while (gestor.hayPendientes() && turnos < 10000) {
                gestor.ejecutarTurno(quantum);
                turnos++;
            }
            actualizarVista();
            estado("Planificacion terminada. Turnos ejecutados: " + turnos);
        } catch (RuntimeException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void registrarMantenimiento() {
        int fila = tablaTractores.getSelectedRow();
        if (fila < 0) {
            mostrarError("Seleccione un tractor de la tabla.");
            return;
        }
        int filaModelo = tablaTractores.convertRowIndexToModel(fila);
        int id = Integer.parseInt(modeloTractores.getValueAt(filaModelo, 0).toString());
        Tractor t = gestor.buscarPorId(id);
        if (t != null) {
            gestor.registrarMantenimiento(t);
            actualizarVista();
            estado("Mantenimiento registrado para " + t.nombre + ".");
        }
    }

    private void retirarTractor() {
        int fila = tablaTractores.getSelectedRow();
        if (fila < 0) {
            mostrarError("Seleccione un tractor de la tabla.");
            return;
        }
        int filaModelo = tablaTractores.convertRowIndexToModel(fila);
        int id = Integer.parseInt(modeloTractores.getValueAt(filaModelo, 0).toString());
        Tractor t = gestor.buscarPorId(id);
        if (t != null && JOptionPane.showConfirmDialog(this,
                "Desea retirar " + t.nombre + " del inventario?", "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            gestor.retirarTractor(t);
            actualizarVista();
            estado("Tractor retirado del inventario.");
        }
    }

    private void actualizarVista() {
        modeloTractores.setRowCount(0);
        for (Tractor t : gestor.obtenerTractores()) {
            String mantenimiento = t.horasUso >= t.umbralMantenimiento ? "REQUERIDO" :
                    String.format("%.1f h restantes", Math.max(0, t.umbralMantenimiento - t.horasUso));
            modeloTractores.addRow(new Object[]{t.id, t.nombre, t.prioridad, t.tiempoRestante,
                    String.format("%.2f", t.horasUso), String.format("%.2f", t.combustibleUsado),
                    mantenimiento, t.tiempoRestante == 0 ? "Terminado" : "Pendiente"});
        }
        modeloHistorial.setRowCount(0);
        for (Evento ev : gestor.historial.obtenerTodos()) {
            modeloHistorial.addRow(new Object[]{ev.fecha, ev.tipo, ev.detalle});
        }
        areaResumen.setText(gestor.generarResumen());
    }

    private void estado(String texto) { etiquetaEstado.setText(texto); }
    private void mostrarError(String texto) {
        JOptionPane.showMessageDialog(this, texto, "Validacion", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }

    // ===================== MODELO Y GESTION =====================

    static class Tractor {
        int id;
        String nombre;
        int prioridad;
        int tiempoInicial;
        int tiempoRestante;
        double horasUso;
        double combustibleUsado;
        double consumoLitrosHora;
        double umbralMantenimiento;
        int cantidadMantenimientos;

        Tractor(int id, String nombre, int tiempo, int prioridad, double consumo, double umbral) {
            this.id = id;
            this.nombre = nombre;
            this.tiempoInicial = tiempo;
            this.tiempoRestante = tiempo;
            this.prioridad = prioridad;
            this.consumoLitrosHora = consumo;
            this.umbralMantenimiento = umbral;
        }
    }

    static class NodoTractor {
        Tractor dato;
        NodoTractor siguiente;
        NodoTractor anterior;
        NodoTractor(Tractor dato) { this.dato = dato; }
    }

    // Lista simplemente enlazada: inventario
    static class ListaSimple {
        NodoTractor cabeza;
        void agregar(Tractor t) {
            NodoTractor n = new NodoTractor(t);
            if (cabeza == null) cabeza = n;
            else {
                NodoTractor actual = cabeza;
                while (actual.siguiente != null) actual = actual.siguiente;
                actual.siguiente = n;
            }
        }
        boolean eliminar(Tractor t) {
            NodoTractor actual = cabeza, anterior = null;
            while (actual != null) {
                if (actual.dato == t) {
                    if (anterior == null) cabeza = actual.siguiente;
                    else anterior.siguiente = actual.siguiente;
                    return true;
                }
                anterior = actual; actual = actual.siguiente;
            }
            return false;
        }
        List<Tractor> obtenerTodos() {
            List<Tractor> r = new ArrayList<>();
            NodoTractor a = cabeza;
            while (a != null) { r.add(a.dato); a = a.siguiente; }
            return r;
        }
    }

    // Lista doblemente enlazada: historial de eventos
    static class ListaDobleEventos {
        NodoEvento cabeza, cola;
        void agregar(Evento e) {
            NodoEvento n = new NodoEvento(e);
            if (cabeza == null) cabeza = cola = n;
            else { cola.siguiente = n; n.anterior = cola; cola = n; }
        }
        List<Evento> obtenerTodos() {
            List<Evento> r = new ArrayList<>();
            NodoEvento a = cabeza;
            while (a != null) { r.add(a.dato); a = a.siguiente; }
            return r;
        }
    }
    static class NodoEvento {
        Evento dato; NodoEvento siguiente, anterior;
        NodoEvento(Evento dato) { this.dato = dato; }
    }

    // Lista circular: turnos Round Robin dentro de cada nivel de prioridad
    static class ColaCircular {
        NodoTractor ultimo;
        boolean estaVacia() { return ultimo == null; }
        void encolar(Tractor t) {
            NodoTractor n = new NodoTractor(t);
            if (ultimo == null) { ultimo = n; n.siguiente = n; }
            else { n.siguiente = ultimo.siguiente; ultimo.siguiente = n; ultimo = n; }
        }
        Tractor desencolar() {
            if (ultimo == null) return null;
            NodoTractor primero = ultimo.siguiente;
            if (primero == ultimo) ultimo = null;
            else ultimo.siguiente = primero.siguiente;
            return primero.dato;
        }
        void rotar() { if (ultimo != null) ultimo = ultimo.siguiente; }
        boolean contiene(Tractor t) {
            if (ultimo == null) return false;
            NodoTractor actual = ultimo.siguiente;
            do { if (actual.dato == t) return true; actual = actual.siguiente; }
            while (actual != ultimo.siguiente);
            return false;
        }
        List<Tractor> obtenerTodos() {
            List<Tractor> r = new ArrayList<>();
            if (ultimo == null) return r;
            NodoTractor a = ultimo.siguiente;
            do { r.add(a.dato); a = a.siguiente; } while (a != ultimo.siguiente);
            return r;
        }
    }

    // Pila LIFO: conserva eventos de cambio de contexto
    static class PilaContexto {
        NodoPila cima;
        void push(String texto) { NodoPila n = new NodoPila(texto); n.siguiente = cima; cima = n; }
        String pop() {
            if (cima == null) return null;
            String s = cima.dato; cima = cima.siguiente; return s;
        }
        int tamano() { int n = 0; for (NodoPila a = cima; a != null; a = a.siguiente) n++; return n; }
    }
    static class NodoPila {
        String dato; NodoPila siguiente;
        NodoPila(String dato) { this.dato = dato; }
    }

    // Cola FIFO sencilla para registrar trabajos que llegan al sistema
    static class ColaLlegadas {
        NodoLlegada frente, fin;
        void encolar(Tractor t) {
            NodoLlegada n = new NodoLlegada(t);
            if (fin == null) frente = fin = n;
            else { fin.siguiente = n; fin = n; }
        }
        Tractor desencolar() {
            if (frente == null) return null;
            Tractor t = frente.dato; frente = frente.siguiente;
            if (frente == null) fin = null;
            return t;
        }
        boolean estaVacia() { return frente == null; }
    }
    static class NodoLlegada {
        Tractor dato; NodoLlegada siguiente;
        NodoLlegada(Tractor dato) { this.dato = dato; }
    }

    // Vector unidimensional y matriz bidimensional para indicadores
    static class Gestor {
        private int siguienteId = 1;
        final ListaSimple inventario = new ListaSimple();
        final ListaDobleEventos historial = new ListaDobleEventos();
        final ColaCircular[] niveles = {new ColaCircular(), new ColaCircular(), new ColaCircular()};
        final ColaLlegadas colaLlegadas = new ColaLlegadas();
        final PilaContexto pilaContextos = new PilaContexto();
        final List<Tractor> vectorTractores = new ArrayList<>();
        final double[][] matrizEstadisticas = new double[3][4];
        int totalTurnos;
        double tiempoTotalMinutos;
        double tiempoMuertoMinutos;
        double combustibleTotal;

        void agregarTractor(String nombre, int tiempo, int prioridad, double consumo, double umbral) {
            Tractor t = new Tractor(siguienteId++, nombre, tiempo, prioridad, consumo, umbral);
            inventario.agregar(t);
            vectorTractores.add(t);
            colaLlegadas.encolar(t);
            registrar("Registro", nombre + " registrado con prioridad " + prioridad + ".");
            // Los trabajos nuevos entran por la cola FIFO y luego pasan al nivel correspondiente.
            procesarLlegadas();
        }

        private void procesarLlegadas() {
            while (!colaLlegadas.estaVacia()) {
                Tractor t = colaLlegadas.desencolar();
                niveles[t.prioridad - 1].encolar(t);
                registrar("Cola multinivel", t.nombre + " agregado al nivel " + t.prioridad + ".");
            }
        }

        boolean hayPendientes() {
            for (ColaCircular nivel : niveles) {
                for (Tractor t : nivel.obtenerTodos()) if (t.tiempoRestante > 0) return true;
            }
            return false;
        }

        boolean ejecutarTurno(int quantum) {
            procesarLlegadas();
            int nivelElegido = -1;
            for (int i = 0; i < niveles.length; i++) {
                if (!niveles[i].estaVacia()) {
                    for (Tractor t : niveles[i].obtenerTodos()) {
                        if (t.tiempoRestante > 0) { nivelElegido = i; break; }
                    }
                    if (nivelElegido != -1) break;
                }
            }
            if (nivelElegido == -1) return false;

            ColaCircular cola = niveles[nivelElegido];
            // Retira trabajos terminados que puedan estar al frente.
            int revisados = 0;
            while (!cola.estaVacia() && cola.ultimo.siguiente.dato.tiempoRestante <= 0 && revisados < 1000) {
                cola.desencolar(); revisados++;
            }
            if (cola.estaVacia()) return ejecutarTurno(quantum);

            Tractor t = cola.desencolar();
            if (t == null || t.tiempoRestante <= 0) return ejecutarTurno(quantum);
            int ejecutado = Math.min(quantum, t.tiempoRestante);
            t.tiempoRestante -= ejecutado;
            double horas = ejecutado / 60.0;
            t.horasUso += horas;
            double combustible = horas * t.consumoLitrosHora;
            t.combustibleUsado += combustible;
            combustibleTotal += combustible;
            tiempoTotalMinutos += ejecutado;
            // Tiempo de espera agregado de los otros tractores que siguen pendientes.
            for (Tractor otro : vectorTractores) {
                if (otro != t && otro.tiempoRestante > 0) tiempoMuertoMinutos += ejecutado;
            }
            totalTurnos++;
            // Si queda trabajo, vuelve al final de la cola circular del mismo nivel.
            if (t.tiempoRestante > 0) cola.encolar(t);
            else registrar("Trabajo terminado", t.nombre + " finalizo su tarea.");
            if (t.horasUso >= t.umbralMantenimiento) {
                registrar("Alerta", t.nombre + " alcanzo el umbral de mantenimiento.");
            }
            pilaContextos.push("Turno " + totalTurnos + ": " + t.nombre + ", " + ejecutado + " min.");
            registrar("Round Robin", t.nombre + " trabajo " + ejecutado + " min; quedan " + t.tiempoRestante + " min.");
            actualizarMatriz(t, ejecutado, combustible);
            return true;
        }

        private void actualizarMatriz(Tractor t, int minutos, double combustibleTurno) {
            int p = t.prioridad - 1;
            matrizEstadisticas[p][0] += 1;                  // turnos ejecutados
            matrizEstadisticas[p][1] += minutos;            // minutos trabajados
            matrizEstadisticas[p][2] += combustibleTurno;   // combustible de este turno
            matrizEstadisticas[p][3] += t.tiempoRestante;   // tiempo pendiente tras el turno
        }

        void registrarMantenimiento(Tractor t) {
            t.horasUso = 0;
            t.cantidadMantenimientos++;
            registrar("Mantenimiento", "Se realizo mantenimiento preventivo a " + t.nombre + ".");
        }

        void retirarTractor(Tractor t) {
            inventario.eliminar(t);
            vectorTractores.remove(t);
            for (ColaCircular c : niveles) {
                List<Tractor> actuales = c.obtenerTodos();
                while (!c.estaVacia()) c.desencolar();
                for (Tractor actual : actuales) {
                    if (actual != t) c.encolar(actual);
                }
            }
            registrar("Retiro", t.nombre + " retirado del inventario y de las colas.");
        }

        Tractor buscarPorId(int id) {
            for (Tractor t : inventario.obtenerTodos()) if (t.id == id) return t;
            return null;
        }

        List<Tractor> obtenerTractores() { return inventario.obtenerTodos(); }

        private void registrar(String tipo, String detalle) {
            String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            historial.agregar(new Evento(fecha, tipo, detalle));
        }

        String generarResumen() {
            int total = vectorTractores.size(), terminados = 0, pendientes = 0, alertas = 0;
            double horas = 0, combustible = 0, tiempoPendiente = 0;
            double[] vectorHorasPorTractor = new double[vectorTractores.size()];
            double[][] matriz = new double[vectorTractores.size()][3];
            for (int i = 0; i < vectorTractores.size(); i++) {
                Tractor t = vectorTractores.get(i);
                vectorHorasPorTractor[i] = t.horasUso;
                matriz[i][0] = t.tiempoInicial;
                matriz[i][1] = t.tiempoRestante;
                matriz[i][2] = t.combustibleUsado;
                horas += t.horasUso;
                combustible += t.combustibleUsado;
                tiempoPendiente += t.tiempoRestante;
                if (t.tiempoRestante == 0) terminados++; else pendientes++;
                if (t.horasUso >= t.umbralMantenimiento) alertas++;
            }
            double promedioHoras = total == 0 ? 0 : horas / total;
            double promedioCombustible = total == 0 ? 0 : combustible / total;
            StringBuilder sb = new StringBuilder();
            sb.append("RESUMEN GENERAL\n");
            sb.append("Tractores registrados: ").append(total).append("\n");
            sb.append("Trabajos terminados: ").append(terminados).append(" | Pendientes: ").append(pendientes).append("\n");
            sb.append(String.format("Tiempo productivo acumulado: %.2f horas (%.0f minutos)\n", horas, tiempoTotalMinutos));
            sb.append(String.format("Combustible total estimado: %.2f litros\n", combustible));
            sb.append(String.format("Promedio de horas de uso por tractor: %.2f\n", promedioHoras));
            sb.append(String.format("Promedio de combustible por tractor: %.2f litros\n", promedioCombustible));
            sb.append(String.format("Tiempo pendiente total: %.0f minutos\n", tiempoPendiente));
            sb.append("Turnos ejecutados: ").append(totalTurnos).append("\n");
            sb.append("Cambios de contexto guardados en pila: ").append(pilaContextos.tamano()).append("\n");
            sb.append("Alertas de mantenimiento actuales: ").append(alertas).append("\n");
            sb.append(String.format("Tiempo de espera agregado de maquinaria: %.0f minutos-maquina\n", tiempoMuertoMinutos));
            sb.append("\nESTADISTICAS POR PRIORIDAD (matriz 3 x 4)\n");
            sb.append("Prioridad | Turnos | Minutos | Combustible acumulado* | Tiempo pendiente acumulado\n");
            for (int i = 0; i < 3; i++) {
                sb.append(String.format("    %d     | %.0f | %.0f | %.2f | %.0f\n",
                        i + 1, matrizEstadisticas[i][0], matrizEstadisticas[i][1],
                        matrizEstadisticas[i][2], matrizEstadisticas[i][3]));
            }
            sb.append("*El combustible por prioridad es una suma acumulada de las lecturas guardadas en cada turno.\n");
            sb.append("\nFORMULAS UTILIZADAS\n");
            sb.append("Horas de uso = minutos ejecutados / 60.\n");
            sb.append("Combustible = horas de uso x consumo en litros por hora.\n");
            sb.append("Tiempo pendiente = tiempo inicial - minutos ya ejecutados.\n");
            sb.append("Promedio = suma de los valores / numero de tractores.");
            return sb.toString();
        }
    }

    static class Evento {
        String fecha, tipo, detalle;
        Evento(String fecha, String tipo, String detalle) {
            this.fecha = fecha; this.tipo = tipo; this.detalle = detalle;
        }
    }
}
